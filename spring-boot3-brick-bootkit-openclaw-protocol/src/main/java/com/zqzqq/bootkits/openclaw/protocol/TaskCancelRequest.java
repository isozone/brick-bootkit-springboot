package com.zqzqq.bootkits.openclaw.protocol;

public class TaskCancelRequest {

    private String requestedBy;
    private String reason;

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
}
