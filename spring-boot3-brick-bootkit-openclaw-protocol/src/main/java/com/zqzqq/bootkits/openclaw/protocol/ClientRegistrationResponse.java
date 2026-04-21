package com.zqzqq.bootkits.openclaw.protocol;

import java.time.Instant;

public class ClientRegistrationResponse {

    private boolean accepted;
    private String message;
    private String sessionId;
    private Instant serverTime;
    private long heartbeatIntervalSeconds;
    private ClientSnapshot client;

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }

    public long getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(long heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public ClientSnapshot getClient() {
        return client;
    }

    public void setClient(ClientSnapshot client) {
        this.client = client;
    }
}
