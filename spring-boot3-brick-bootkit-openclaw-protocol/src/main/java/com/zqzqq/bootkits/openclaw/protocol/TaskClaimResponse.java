package com.zqzqq.bootkits.openclaw.protocol;

import java.time.Instant;

public class TaskClaimResponse {

    private boolean accepted;
    private String message;
    private Instant serverTime;
    private Instant leaseExpiresAt;
    private TaskSnapshot task;

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

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public TaskSnapshot getTask() {
        return task;
    }

    public void setTask(TaskSnapshot task) {
        this.task = task;
    }
}
