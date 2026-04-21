package com.zqzqq.bootkits.openclaw.protocol;

import java.time.Instant;

public class ClientHeartbeatResponse {

    private boolean accepted;
    private String message;
    private Instant serverTime;
    private long nextHeartbeatIntervalSeconds;
    private int queuedTaskCount;
    private ClientStatus clientStatus;

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

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }

    public long getNextHeartbeatIntervalSeconds() {
        return nextHeartbeatIntervalSeconds;
    }

    public void setNextHeartbeatIntervalSeconds(long nextHeartbeatIntervalSeconds) {
        this.nextHeartbeatIntervalSeconds = nextHeartbeatIntervalSeconds;
    }

    public int getQueuedTaskCount() {
        return queuedTaskCount;
    }

    public void setQueuedTaskCount(int queuedTaskCount) {
        this.queuedTaskCount = queuedTaskCount;
    }

    public ClientStatus getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(ClientStatus clientStatus) {
        this.clientStatus = clientStatus;
    }
}
