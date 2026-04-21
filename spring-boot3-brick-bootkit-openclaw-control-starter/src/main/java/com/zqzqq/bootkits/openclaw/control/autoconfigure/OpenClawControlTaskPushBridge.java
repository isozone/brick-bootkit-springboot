package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.control.spi.TaskLifecycleListener;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public class OpenClawControlTaskPushBridge implements TaskLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(OpenClawControlTaskPushBridge.class);

    private final OpenClawControlWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final OpenClawControlProperties properties;

    public OpenClawControlTaskPushBridge(OpenClawControlWebSocketSessionRegistry sessionRegistry,
                                         ObjectMapper objectMapper,
                                         OpenClawControlProperties properties) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void onTaskDispatched(TaskSnapshot task) {
        if (!properties.getWebSocket().isPushTaskAssignments() || task == null || task.getAssignedClientId() == null) {
            return;
        }
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setType(ControlMessageType.TASK_ASSIGNED);
        envelope.setClientId(task.getAssignedClientId());
        envelope.setTimestamp(Instant.now());
        envelope.setSuccess(true);
        envelope.setMessage("task assigned");
        envelope.setPayload(task);
        try {
            sessionRegistry.sendToClient(task.getAssignedClientId(), objectMapper.writeValueAsString(envelope));
        } catch (IOException ex) {
            log.warn("Failed to push assigned task {} to client {}", task.getTaskId(), task.getAssignedClientId(), ex);
        }
    }

    @Override
    public void onTaskUpdated(TaskSnapshot task) {
        if (task == null || task.getAssignedClientId() == null || task.getStatus() == null) {
            return;
        }
        if (task.getStatus() != com.zqzqq.bootkits.openclaw.protocol.TaskStatus.CANCELLED) {
            return;
        }
        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setType(com.zqzqq.bootkits.openclaw.protocol.ControlMessageType.TASK_CANCELLED);
        envelope.setClientId(task.getAssignedClientId());
        envelope.setTimestamp(Instant.now());
        envelope.setSuccess(true);
        envelope.setMessage(task.getMessage() == null ? "task cancelled" : task.getMessage());
        envelope.setPayload(task);
        try {
            sessionRegistry.sendToClient(task.getAssignedClientId(), objectMapper.writeValueAsString(envelope));
        } catch (IOException ex) {
            log.warn("Failed to push cancelled task {} to client {}", task.getTaskId(), task.getAssignedClientId(), ex);
        }
    }
}
