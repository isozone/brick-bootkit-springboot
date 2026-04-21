package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawControlService;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawIntegrationRegistry;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenClawControlWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenClawControlWebSocketHandler.class);

    private final OpenClawControlService controlService;
    private final OpenClawIntegrationRegistry integrationRegistry;
    private final OpenClawControlWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final OpenClawClientAuthVerifier clientAuthVerifier;

    public OpenClawControlWebSocketHandler(OpenClawControlService controlService,
                                           OpenClawIntegrationRegistry integrationRegistry,
                                           OpenClawControlWebSocketSessionRegistry sessionRegistry,
                                           ObjectMapper objectMapper,
                                           OpenClawClientAuthVerifier clientAuthVerifier) {
        this.controlService = controlService;
        this.integrationRegistry = integrationRegistry;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
        this.clientAuthVerifier = clientAuthVerifier;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ControlMessageEnvelope envelope = objectMapper.readValue(message.getPayload(), ControlMessageEnvelope.class);
            handleEnvelope(session, envelope);
        } catch (ControlPlaneException ex) {
            sendError(session, null, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to handle control websocket message", ex);
            sendError(session, null, "invalid websocket control message");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    private void handleEnvelope(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        if (envelope == null || envelope.getType() == null) {
            throw new ControlPlaneException("message type is required");
        }
        switch (envelope.getType()) {
            case REGISTER -> handleRegister(session, envelope);
            case HEARTBEAT -> handleHeartbeat(session, envelope);
            case CLAIM_TASK -> handleClaimTask(session, envelope);
            case TASK_PROGRESS -> handleTaskProgress(session, envelope);
            case TASK_RESULT -> handleTaskResult(session, envelope);
            case TASK_LEASE_RENEW -> handleTaskLeaseRenew(session, envelope);
            case LIST_INTEGRATIONS -> writeEnvelope(session, successEnvelope(
                    envelope.getRequestId(),
                    ControlMessageType.INTEGRATIONS,
                    resolveClientId(envelope, session, false),
                    integrationRegistry.list(),
                    "integrations loaded"
            ));
            default -> throw new ControlPlaneException("unsupported websocket message type: " + envelope.getType());
        }
    }

    private void handleRegister(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        ClientRegistrationRequest request = convertPayload(envelope, ClientRegistrationRequest.class);
        if (request == null) {
            throw new ControlPlaneException("registration payload is required");
        }
        if (isBlank(request.getClientId())) {
            request.setClientId(envelope.getClientId());
        }
        clientAuthVerifier.verifyRegistration(request, authHeaders(envelope, session));
        var response = controlService.registerClient(request);
        sessionRegistry.bindClient(response.getClient().getClientId(), session);
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.REGISTERED,
                response.getClient().getClientId(),
                response,
                response.getMessage()
        ));
    }

    private void handleHeartbeat(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        ClientHeartbeatRequest request = convertPayload(envelope, ClientHeartbeatRequest.class);
        String clientId = resolveClientId(envelope, session, true);
        clientAuthVerifier.verifyClientAccess(clientId, request == null ? null : request.getSessionId(), request, authHeaders(envelope, session));
        var response = controlService.heartbeat(clientId, request);
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.HEARTBEAT_ACK,
                clientId,
                response,
                response.getMessage()
        ));
    }

    private void handleClaimTask(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        String clientId = resolveClientId(envelope, session, true);
        TaskClaimRequest request = convertPayload(envelope, TaskClaimRequest.class);
        clientAuthVerifier.verifyClientAccess(clientId, request == null ? null : request.getSessionId(), request, authHeaders(envelope, session));
        var response = controlService.claimNextTask(clientId, request);
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.TASK_CLAIMED,
                clientId,
                response,
                response.getMessage()
        ));
    }

    private void handleTaskProgress(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        TaskProgressCommand command = convertPayload(envelope, TaskProgressCommand.class);
        if (command == null || isBlank(command.getTaskId()) || command.getReport() == null) {
            throw new ControlPlaneException("task progress requires taskId and report");
        }
        TaskSnapshot task = controlService.getTask(command.getTaskId());
        Map<String, String> authHeaders = authHeaders(envelope, session);
        clientAuthVerifier.verifyClientAccess(
                task.getAssignedClientId(),
                OpenClawClientAuthSupport.header(authHeaders, OpenClawClientAuthSupport.HEADER_SESSION_ID),
                command.getReport(),
                authHeaders
        );
        TaskSnapshot snapshot = controlService.reportProgress(command.getTaskId(), command.getReport());
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.TASK_PROGRESS_ACK,
                resolveClientId(envelope, session, false),
                snapshot,
                "task progress accepted"
        ));
    }

    private void handleTaskResult(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        TaskResultCommand command = convertPayload(envelope, TaskResultCommand.class);
        if (command == null || isBlank(command.getTaskId()) || command.getReport() == null) {
            throw new ControlPlaneException("task result requires taskId and report");
        }
        TaskSnapshot task = controlService.getTask(command.getTaskId());
        Map<String, String> authHeaders = authHeaders(envelope, session);
        clientAuthVerifier.verifyClientAccess(
                task.getAssignedClientId(),
                OpenClawClientAuthSupport.header(authHeaders, OpenClawClientAuthSupport.HEADER_SESSION_ID),
                command.getReport(),
                authHeaders
        );
        TaskSnapshot snapshot = controlService.completeTask(command.getTaskId(), command.getReport());
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.TASK_RESULT_ACK,
                resolveClientId(envelope, session, false),
                snapshot,
                "task result accepted"
        ));
    }

    private void handleTaskLeaseRenew(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        TaskLeaseRenewCommand command = convertPayload(envelope, TaskLeaseRenewCommand.class);
        if (command == null || isBlank(command.getTaskId()) || command.getRequest() == null) {
            throw new ControlPlaneException("task lease renewal requires taskId and request");
        }
        TaskLeaseRenewRequest request = command.getRequest();
        if (isBlank(request.getClientId())) {
            throw new ControlPlaneException("task lease renewal requires clientId");
        }
        clientAuthVerifier.verifyClientAccess(request.getClientId(), request.getSessionId(), request, authHeaders(envelope, session));
        TaskSnapshot snapshot = controlService.renewTaskLease(command.getTaskId(), request);
        writeEnvelope(session, successEnvelope(
                envelope.getRequestId(),
                ControlMessageType.TASK_LEASE_RENEWED,
                request.getClientId(),
                snapshot,
                "task lease renewed"
        ));
    }

    private <T> T convertPayload(ControlMessageEnvelope envelope, Class<T> type) {
        if (envelope == null || envelope.getPayload() == null) {
            return null;
        }
        return objectMapper.convertValue(envelope.getPayload(), type);
    }

    private String resolveClientId(ControlMessageEnvelope envelope, WebSocketSession session, boolean required) {
        String clientId = envelope == null ? null : envelope.getClientId();
        if (isBlank(clientId)) {
            clientId = sessionRegistry.findClientId(session);
        }
        if (required && isBlank(clientId)) {
            throw new ControlPlaneException("clientId is required");
        }
        return clientId;
    }

    private void sendError(WebSocketSession session, String requestId, String message) throws IOException {
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setRequestId(requestId);
        envelope.setType(ControlMessageType.ERROR);
        envelope.setTimestamp(Instant.now());
        envelope.setSuccess(false);
        envelope.setCode("OPENCLAW_CONTROL_ERROR");
        envelope.setMessage(message);
        writeEnvelope(session, envelope);
    }

    private void writeEnvelope(WebSocketSession session, ControlMessageEnvelope envelope) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
    }

    private ControlMessageEnvelope successEnvelope(String requestId,
                                                   ControlMessageType responseType,
                                                   String clientId,
                                                   Object payload,
                                                   String message) {
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setRequestId(requestId);
        envelope.setClientId(clientId);
        envelope.setType(responseType);
        envelope.setTimestamp(Instant.now());
        envelope.setSuccess(true);
        envelope.setMessage(message);
        envelope.setPayload(payload);
        return envelope;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Map<String, String> authHeaders(ControlMessageEnvelope envelope, WebSocketSession session) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (envelope != null && envelope.getHeaders() != null) {
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_CLIENT_TOKEN, envelope.getHeaders().get("token"));
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_AUTH_TIMESTAMP, envelope.getHeaders().get("timestamp"));
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_AUTH_NONCE, envelope.getHeaders().get("nonce"));
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_AUTH_SIGNATURE, envelope.getHeaders().get("signature"));
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_SESSION_ID, envelope.getHeaders().get("sessionId"));
            copyHeader(headers, OpenClawClientAuthSupport.HEADER_CLIENT_ID, envelope.getHeaders().get("clientId"));
        }
        String boundClientId = sessionRegistry.findClientId(session);
        if (!isBlank(boundClientId)) {
            headers.putIfAbsent(OpenClawClientAuthSupport.HEADER_CLIENT_ID, boundClientId);
        }
        return headers;
    }

    private void copyHeader(Map<String, String> headers, String name, Object value) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            headers.put(name, stringValue);
        }
    }
}
