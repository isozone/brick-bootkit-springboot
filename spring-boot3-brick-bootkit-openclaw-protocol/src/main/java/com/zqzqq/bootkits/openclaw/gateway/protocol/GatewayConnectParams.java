package com.zqzqq.bootkits.openclaw.gateway.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GatewayConnectParams {

    private Integer minProtocol = OpenClawGatewayConstants.MIN_PROTOCOL;
    private Integer maxProtocol = OpenClawGatewayConstants.MAX_PROTOCOL;
    private GatewayClientDescriptor client = new GatewayClientDescriptor();
    private String role = "operator";
    private List<String> scopes = new ArrayList<>();
    private List<String> caps = new ArrayList<>();
    private List<String> commands = new ArrayList<>();
    private Map<String, Object> permissions = new LinkedHashMap<>();
    private GatewayAuth auth = new GatewayAuth();
    private String locale;
    private String userAgent;
    private GatewayDeviceIdentity device;

    public Integer getMinProtocol() {
        return minProtocol;
    }

    public void setMinProtocol(Integer minProtocol) {
        this.minProtocol = minProtocol == null ? OpenClawGatewayConstants.MIN_PROTOCOL : minProtocol;
    }

    public Integer getMaxProtocol() {
        return maxProtocol;
    }

    public void setMaxProtocol(Integer maxProtocol) {
        this.maxProtocol = maxProtocol == null ? OpenClawGatewayConstants.MAX_PROTOCOL : maxProtocol;
    }

    public GatewayClientDescriptor getClient() {
        return client;
    }

    public void setClient(GatewayClientDescriptor client) {
        this.client = client == null ? new GatewayClientDescriptor() : client;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null || role.isBlank() ? "operator" : role;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }

    public List<String> getCaps() {
        return caps;
    }

    public void setCaps(List<String> caps) {
        this.caps = caps == null ? new ArrayList<>() : new ArrayList<>(caps);
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
    }

    public Map<String, Object> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Object> permissions) {
        this.permissions = permissions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(permissions);
    }

    public GatewayAuth getAuth() {
        return auth;
    }

    public void setAuth(GatewayAuth auth) {
        this.auth = auth == null ? new GatewayAuth() : auth;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public GatewayDeviceIdentity getDevice() {
        return device;
    }

    public void setDevice(GatewayDeviceIdentity device) {
        this.device = device;
    }
}
