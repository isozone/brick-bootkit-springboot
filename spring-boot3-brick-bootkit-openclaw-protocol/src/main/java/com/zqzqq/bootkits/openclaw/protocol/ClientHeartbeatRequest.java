package com.zqzqq.bootkits.openclaw.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientHeartbeatRequest {

    private String sessionId;
    private String authToken;
    private String authTimestamp;
    private String authNonce;
    private String authSignature;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private List<String> currentTaskIds = new ArrayList<>();
    private List<ClientCapabilityDescriptor> capabilities = new ArrayList<>();

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

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metrics);
    }

    public List<String> getCurrentTaskIds() {
        return currentTaskIds;
    }

    public void setCurrentTaskIds(List<String> currentTaskIds) {
        this.currentTaskIds = currentTaskIds == null ? new ArrayList<>() : new ArrayList<>(currentTaskIds);
    }

    public List<ClientCapabilityDescriptor> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<ClientCapabilityDescriptor> capabilities) {
        this.capabilities = capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
    }
}
