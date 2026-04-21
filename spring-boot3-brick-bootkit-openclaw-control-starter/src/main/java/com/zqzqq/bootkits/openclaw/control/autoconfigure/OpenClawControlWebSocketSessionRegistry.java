package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OpenClawControlWebSocketSessionRegistry {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> clientSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionClients = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void bindClient(String clientId, WebSocketSession session) {
        register(session);
        String previousSessionId = clientSessions.put(clientId, session.getId());
        if (previousSessionId != null) {
            sessionClients.remove(previousSessionId);
        }
        sessionClients.put(session.getId(), clientId);
    }

    public String findClientId(WebSocketSession session) {
        return sessionClients.get(session.getId());
    }

    public boolean sendToClient(String clientId, String payload) throws IOException {
        String sessionId = clientSessions.get(clientId);
        if (sessionId == null) {
            return false;
        }
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            cleanup(sessionId, clientId);
            return false;
        }
        session.sendMessage(new TextMessage(payload));
        return true;
    }

    public void unregister(WebSocketSession session) {
        String clientId = sessionClients.remove(session.getId());
        sessions.remove(session.getId());
        if (clientId != null && Objects.equals(clientSessions.get(clientId), session.getId())) {
            clientSessions.remove(clientId);
        }
    }

    private void cleanup(String sessionId, String clientId) {
        sessions.remove(sessionId);
        if (clientId != null && Objects.equals(clientSessions.get(clientId), sessionId)) {
            clientSessions.remove(clientId);
        }
        if (sessionId != null) {
            sessionClients.remove(sessionId);
        }
    }
}
