package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.gateway.protocol.OpenClawGatewayConstants;

import java.time.Duration;

public class OpenClawGatewayClientProperties {

    private String baseUrl = OpenClawGatewayConstants.DEFAULT_HTTP_URL;
    private String webSocketUrl;
    private String authToken;
    private String authPassword;
    private boolean allowPlaintextRemoteGateway;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Duration webSocketRequestTimeout = Duration.ofSeconds(10);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = isBlank(baseUrl) ? OpenClawGatewayConstants.DEFAULT_HTTP_URL : baseUrl;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public boolean isAllowPlaintextRemoteGateway() {
        return allowPlaintextRemoteGateway;
    }

    public void setAllowPlaintextRemoteGateway(boolean allowPlaintextRemoteGateway) {
        this.allowPlaintextRemoteGateway = allowPlaintextRemoteGateway;
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

    public String resolveWebSocketUrl() {
        if (!isBlank(webSocketUrl)) {
            return webSocketUrl;
        }
        String url = getBaseUrl();
        if (url.startsWith("https://")) {
            return "wss://" + url.substring("https://".length());
        }
        if (url.startsWith("http://")) {
            return "ws://" + url.substring("http://".length());
        }
        return url;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
