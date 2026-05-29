package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultCommand;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class DefaultOpenClawControlClient implements OpenClawControlClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOpenClawControlClient.class);

    private final OpenClawControlClientProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenClawClientAuthSigner authSigner;
    private final ConcurrentMap<String, CompletableFuture<ControlMessageEnvelope>> pendingRequests = new ConcurrentHashMap<>();
    private volatile StandardWebSocketClient webSocketClient;
    private volatile WebSocketSession webSocketSession;
    private volatile OpenClawControlMessageListener messageListener;
    private volatile String sessionId;

    public DefaultOpenClawControlClient(OpenClawControlClientProperties properties) {
        this(properties, JsonMapper.builder().findAndAddModules().build());
    }

    public DefaultOpenClawControlClient(OpenClawControlClientProperties properties, ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = objectMapper == null ? JsonMapper.builder().findAndAddModules().build() : objectMapper;
        this.authSigner = new OpenClawClientAuthSigner(properties, this.objectMapper);
        this.restClient = RestClient.builder()
                .baseUrl(requiredBaseUrl())
                .requestFactory(requestFactory(this.properties.getRequestTimeout()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
        ClientRegistrationRequest payload = ensureRegistrationRequest(request);
        OpenClawSignedRequest signed = authSigner.sign(payload.getClientId(), null, payload);
        ClientRegistrationResponse response = post("/clients/register", signed, ClientRegistrationResponse.class);
        this.sessionId = response == null ? null : response.getSessionId();
        return response;
    }

    @Override
    public ClientRegistrationResponse registerClientOverWebSocket(ClientRegistrationRequest request) {
        ClientRegistrationRequest payload = ensureRegistrationRequest(request);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.REGISTER,
                payload.getClientId(),
                payload,
                null
        );
        ClientRegistrationResponse result = objectMapper.convertValue(response.getPayload(), ClientRegistrationResponse.class);
        this.sessionId = result == null ? null : result.getSessionId();
        return result;
    }

    @Override
    public ClientHeartbeatResponse heartbeat(ClientHeartbeatRequest request) {
        ClientHeartbeatRequest payload = ensureHeartbeatRequest(request);
        OpenClawSignedRequest signed = authSigner.sign(requiredClientId(), payload.getSessionId(), payload);
        return post("/clients/" + requiredClientId() + "/heartbeat", signed, ClientHeartbeatResponse.class);
    }

    @Override
    public ClientHeartbeatResponse heartbeatOverWebSocket(ClientHeartbeatRequest request) {
        ClientHeartbeatRequest payload = ensureHeartbeatRequest(request);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.HEARTBEAT,
                requiredClientId(),
                payload,
                null
        );
        return objectMapper.convertValue(response.getPayload(), ClientHeartbeatResponse.class);
    }

    @Override
    public TaskClaimResponse claimNextTask(TaskClaimRequest request) {
        TaskClaimRequest payload = ensureTaskClaimRequest(request);
        OpenClawSignedRequest signed = authSigner.sign(requiredClientId(), payload.getSessionId(), payload);
        return post("/clients/" + requiredClientId() + "/tasks/claim", signed, TaskClaimResponse.class);
    }

    @Override
    public TaskClaimResponse claimNextTaskOverWebSocket(TaskClaimRequest request) {
        TaskClaimRequest payload = ensureTaskClaimRequest(request);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.CLAIM_TASK,
                requiredClientId(),
                payload,
                null
        );
        return objectMapper.convertValue(response.getPayload(), TaskClaimResponse.class);
    }

    @Override
    public TaskSnapshot renewTaskLease(String taskId, TaskLeaseRenewRequest request) {
        TaskLeaseRenewRequest payload = ensureTaskLeaseRenewRequest(request);
        OpenClawSignedRequest signed = authSigner.sign(requiredClientId(), payload.getSessionId(), payload);
        return post("/tasks/" + taskId + "/lease-renew", signed, TaskSnapshot.class);
    }

    @Override
    public TaskSnapshot renewTaskLeaseOverWebSocket(String taskId, TaskLeaseRenewRequest request) {
        TaskLeaseRenewRequest payload = ensureTaskLeaseRenewRequest(request);
        TaskLeaseRenewCommand command = new TaskLeaseRenewCommand();
        command.setTaskId(taskId);
        command.setRequest(payload);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.TASK_LEASE_RENEW,
                requiredClientId(),
                command,
                null
        );
        return objectMapper.convertValue(response.getPayload(), TaskSnapshot.class);
    }

    @Override
    public TaskSnapshot reportProgress(String taskId, TaskProgressReport report) {
        OpenClawSignedRequest signed = authSigner.sign(requiredClientId(), currentSessionId(), report);
        return post("/tasks/" + taskId + "/progress", signed, TaskSnapshot.class);
    }

    @Override
    public TaskSnapshot reportProgressOverWebSocket(String taskId, TaskProgressReport report) {
        TaskProgressCommand command = new TaskProgressCommand();
        command.setTaskId(taskId);
        command.setReport(report);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.TASK_PROGRESS,
                requiredClientId(),
                command,
                null
        );
        return objectMapper.convertValue(response.getPayload(), TaskSnapshot.class);
    }

    @Override
    public TaskSnapshot completeTask(String taskId, TaskResultReport report) {
        OpenClawSignedRequest signed = authSigner.sign(requiredClientId(), currentSessionId(), report);
        return post("/tasks/" + taskId + "/result", signed, TaskSnapshot.class);
    }

    @Override
    public TaskSnapshot completeTaskOverWebSocket(String taskId, TaskResultReport report) {
        TaskResultCommand command = new TaskResultCommand();
        command.setTaskId(taskId);
        command.setReport(report);
        ControlMessageEnvelope response = sendWebSocketRequest(
                ControlMessageType.TASK_RESULT,
                requiredClientId(),
                command,
                null
        );
        return objectMapper.convertValue(response.getPayload(), TaskSnapshot.class);
    }

    @Override
    public List<IntegrationSnapshot> listIntegrations() {
        return restClient.get()
                .uri(normalizePath("/integrations"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public void connectWebSocket(OpenClawControlMessageListener listener) {
        if (isWebSocketConnected()) {
            this.messageListener = listener;
            return;
        }
        this.messageListener = listener;
        try {
            StandardWebSocketClient client = webSocketClient;
            if (client == null) {
                client = new StandardWebSocketClient();
                webSocketClient = client;
            }
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            if (!isBlank(properties.getClientId())) {
                headers.add(OpenClawClientAuthSigner.HEADER_CLIENT_ID, properties.getClientId());
            }
            this.webSocketSession = client.execute(
                    new ClientWebSocketHandler(),
                    headers,
                    URI.create(resolveWebSocketUrl())
            ).get(properties.getWebSocketRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new OpenClawControlClientException("failed to connect websocket control channel", ex);
        }
    }

    @Override
    public boolean isWebSocketConnected() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void close() {
        pendingRequests.values().forEach(future -> future.completeExceptionally(
                new OpenClawControlClientException("client closed")));
        pendingRequests.clear();
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                webSocketSession.close(CloseStatus.NORMAL);
            } catch (IOException ex) {
                log.debug("Failed to close websocket session", ex);
            }
        }
    }

    private <T> T post(String path, OpenClawSignedRequest signed, Class<T> type) {
        try {
            return restClient.post()
                    .uri(normalizePath(path))
                    .headers(headers -> signed.getHeaders().forEach(headers::add))
                    .body(signed.getPayload())
                    .retrieve()
                    .body(type);
        } catch (Exception ex) {
            throw new OpenClawControlClientException("control request failed: POST " + path, ex);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = toTimeoutMillis(timeout);
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return requestFactory;
    }

    private int toTimeoutMillis(Duration timeout) {
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        long millis = Math.max(1L, effectiveTimeout.toMillis());
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }

    private ControlMessageEnvelope sendWebSocketRequest(ControlMessageType type,
                                                        String clientId,
                                                        Object payload,
                                                        Map<String, Object> customHeaders) {
        if (!isWebSocketConnected()) {
            throw new OpenClawControlClientException("websocket control channel is not connected");
        }
        String requestId = UUID.randomUUID().toString();
        OpenClawSignedRequest signed = authSigner.sign(clientId, currentSessionId(), payload);
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setRequestId(requestId);
        envelope.setClientId(clientId);
        envelope.setType(type);
        envelope.setHeaders(new LinkedHashMap<>());
        if (customHeaders != null) {
            envelope.getHeaders().putAll(customHeaders);
        }
        copyAuthHeaders(envelope.getHeaders(), signed.getHeaders());
        envelope.setPayload(signed.getPayload());

        CompletableFuture<ControlMessageEnvelope> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        try {
            webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
            ControlMessageEnvelope response = future.get(
                    properties.getWebSocketRequestTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new OpenClawControlClientException(response.getMessage());
            }
            return response;
        } catch (OpenClawControlClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenClawControlClientException("websocket control request failed: " + type, ex);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    private void copyAuthHeaders(Map<String, Object> target, Map<String, String> source) {
        copyIfPresent(target, "token", source.get(OpenClawClientAuthSigner.HEADER_CLIENT_TOKEN));
        copyIfPresent(target, "timestamp", source.get(OpenClawClientAuthSigner.HEADER_AUTH_TIMESTAMP));
        copyIfPresent(target, "nonce", source.get(OpenClawClientAuthSigner.HEADER_AUTH_NONCE));
        copyIfPresent(target, "signature", source.get(OpenClawClientAuthSigner.HEADER_AUTH_SIGNATURE));
        copyIfPresent(target, "sessionId", source.get(OpenClawClientAuthSigner.HEADER_SESSION_ID));
        copyIfPresent(target, "clientId", source.get(OpenClawClientAuthSigner.HEADER_CLIENT_ID));
    }

    private void copyIfPresent(Map<String, Object> target, String key, String value) {
        if (!isBlank(value)) {
            target.put(key, value);
        }
    }

    private ClientRegistrationRequest ensureRegistrationRequest(ClientRegistrationRequest request) {
        ClientRegistrationRequest payload = request == null ? new ClientRegistrationRequest() : request;
        if (isBlank(payload.getClientId())) {
            payload.setClientId(requiredClientId());
        }
        return payload;
    }

    private ClientHeartbeatRequest ensureHeartbeatRequest(ClientHeartbeatRequest request) {
        ClientHeartbeatRequest payload = request == null ? new ClientHeartbeatRequest() : request;
        if (isBlank(payload.getSessionId())) {
            payload.setSessionId(currentSessionId());
        }
        return payload;
    }

    private TaskClaimRequest ensureTaskClaimRequest(TaskClaimRequest request) {
        TaskClaimRequest payload = request == null ? new TaskClaimRequest() : request;
        if (isBlank(payload.getSessionId())) {
            payload.setSessionId(currentSessionId());
        }
        return payload;
    }

    private TaskLeaseRenewRequest ensureTaskLeaseRenewRequest(TaskLeaseRenewRequest request) {
        TaskLeaseRenewRequest payload = request == null ? new TaskLeaseRenewRequest() : request;
        if (isBlank(payload.getClientId())) {
            payload.setClientId(requiredClientId());
        }
        if (isBlank(payload.getSessionId())) {
            payload.setSessionId(currentSessionId());
        }
        return payload;
    }

    private String normalizePath(String path) {
        String base = properties.getApiBasePath();
        String normalizedBase = (base == null || base.isBlank()) ? "" : (base.startsWith("/") ? base : "/" + base);
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String requiredBaseUrl() {
        if (isBlank(properties.getBaseUrl())) {
            throw new OpenClawControlClientException("baseUrl is required");
        }
        return properties.getBaseUrl();
    }

    private String requiredClientId() {
        if (isBlank(properties.getClientId())) {
            throw new OpenClawControlClientException("clientId is required");
        }
        return properties.getClientId();
    }

    private String currentSessionId() {
        return sessionId;
    }

    private String resolveWebSocketUrl() {
        if (!isBlank(properties.getWebSocketUrl())) {
            return properties.getWebSocketUrl();
        }
        String baseUrl = requiredBaseUrl();
        String wsBase = baseUrl.startsWith("https://")
                ? "wss://" + baseUrl.substring("https://".length())
                : baseUrl.startsWith("http://")
                ? "ws://" + baseUrl.substring("http://".length())
                : baseUrl;
        String path = properties.getWebSocketPath();
        return wsBase + (path.startsWith("/") ? path : "/" + path);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private class ClientWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            if (messageListener != null) {
                messageListener.onConnected();
            }
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            ControlMessageEnvelope envelope = objectMapper.readValue(message.getPayload(), ControlMessageEnvelope.class);
            if (envelope.getRequestId() != null) {
                CompletableFuture<ControlMessageEnvelope> future = pendingRequests.get(envelope.getRequestId());
                if (future != null) {
                    future.complete(envelope);
                    return;
                }
            }
            if (messageListener != null) {
                messageListener.onMessage(envelope);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            if (messageListener != null) {
                messageListener.onError(exception);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            webSocketSession = null;
            if (messageListener != null) {
                messageListener.onDisconnected();
            }
        }
    }
}
