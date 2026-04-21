package com.zqzqq.bootkits.openclaw.control.client;

import java.time.Duration;

public class OpenClawControlClientProperties {

    private String baseUrl;
    private String apiBasePath = "/openclaw/control";
    private String webSocketUrl;
    private String webSocketPath = "/openclaw/control/ws";
    private String clientId;
    private String authToken;
    private String authSecret;
    private OpenClawClientAuthMode authMode = OpenClawClientAuthMode.TOKEN;
    private OpenClawControlTransport preferredTransport = OpenClawControlTransport.REST;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Duration webSocketRequestTimeout = Duration.ofSeconds(10);
    private Duration heartbeatInterval = Duration.ofSeconds(30);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public String getWebSocketPath() {
        return webSocketPath;
    }

    public void setWebSocketPath(String webSocketPath) {
        this.webSocketPath = webSocketPath;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthSecret() {
        return authSecret;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    public OpenClawClientAuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(OpenClawClientAuthMode authMode) {
        this.authMode = authMode == null ? OpenClawClientAuthMode.TOKEN : authMode;
    }

    public OpenClawControlTransport getPreferredTransport() {
        return preferredTransport;
    }

    public void setPreferredTransport(OpenClawControlTransport preferredTransport) {
        this.preferredTransport = preferredTransport == null ? OpenClawControlTransport.REST : preferredTransport;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
    }

    public Duration getWebSocketRequestTimeout() {
        return webSocketRequestTimeout;
    }

    public void setWebSocketRequestTimeout(Duration webSocketRequestTimeout) {
        this.webSocketRequestTimeout = webSocketRequestTimeout == null ? Duration.ofSeconds(10) : webSocketRequestTimeout;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(30) : heartbeatInterval;
    }
}
