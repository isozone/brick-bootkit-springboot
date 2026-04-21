package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.control.service.DefaultTaskRoutingPolicy;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawControlService;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawIntegrationRegistry;
import com.zqzqq.bootkits.openclaw.control.store.InMemoryClientStateStore;
import com.zqzqq.bootkits.openclaw.control.store.InMemoryTaskStateStore;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenClawControlWebSocketHandlerTest {

    @Test
    void shouldRegisterClientAndPushAssignedTask() throws Exception {
        OpenClawControlProperties properties = new OpenClawControlProperties();
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        OpenClawControlService controlService = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                120L
        );
        OpenClawControlWebSocketSessionRegistry sessionRegistry = new OpenClawControlWebSocketSessionRegistry();
        OpenClawControlWebSocketHandler handler = new OpenClawControlWebSocketHandler(
                controlService,
                new OpenClawIntegrationRegistry(List.of()),
                sessionRegistry,
                objectMapper,
                new NoopOpenClawClientAuthVerifier()
        );
        OpenClawControlTaskPushBridge taskPushBridge = new OpenClawControlTaskPushBridge(
                sessionRegistry,
                objectMapper,
                properties
        );

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");
        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(session);

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-ws");
        request.setDisplayName("Client WS");
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setType(ControlMessageType.REGISTER);
        envelope.setRequestId("req-1");
        envelope.setPayload(request);
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(envelope)));

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        ControlMessageEnvelope response = objectMapper.readValue(messageCaptor.getValue().getPayload(), ControlMessageEnvelope.class);
        assertThat(response.getType()).isEqualTo(ControlMessageType.REGISTERED);

        TaskDispatchRequest dispatchRequest = new TaskDispatchRequest();
        dispatchRequest.setTaskType("gateway.tasks.sync");
        dispatchRequest.setTargetClientId("client-ws");
        taskPushBridge.onTaskDispatched(controlService.dispatchTask(dispatchRequest));

        verify(session, org.mockito.Mockito.times(2)).sendMessage(messageCaptor.capture());
        List<TextMessage> allMessages = messageCaptor.getAllValues();
        ControlMessageEnvelope pushed = objectMapper.readValue(allMessages.get(allMessages.size() - 1).getPayload(), ControlMessageEnvelope.class);
        assertThat(pushed.getType()).isEqualTo(ControlMessageType.TASK_ASSIGNED);
    }
}
