package com.zqzqq.bootkits.openclaw.protocol;

public class TaskRetryRequest {

    private String requestedBy;
    private String reason;
    private String targetClientId;
    private Long leaseSeconds;

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(String targetClientId) {
        this.targetClientId = targetClientId;
    }

    public Long getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(Long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }
}
