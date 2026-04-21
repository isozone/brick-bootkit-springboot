package com.zqzqq.bootkits.openclaw.protocol;

public class TaskLeaseRenewRequest {

    private String clientId;
    private String sessionId;
    private String authToken;
    private String authTimestamp;
    private String authNonce;
    private String authSignature;
    private Long leaseSeconds;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthTimestamp() {
        return authTimestamp;
    }

    public void setAuthTimestamp(String authTimestamp) {
        this.authTimestamp = authTimestamp;
    }

    public String getAuthNonce() {
        return authNonce;
    }

    public void setAuthNonce(String authNonce) {
        this.authNonce = authNonce;
    }

    public String getAuthSignature() {
        return authSignature;
    }

    public void setAuthSignature(String authSignature) {
        this.authSignature = authSignature;
    }

    public Long getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(Long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }
}
