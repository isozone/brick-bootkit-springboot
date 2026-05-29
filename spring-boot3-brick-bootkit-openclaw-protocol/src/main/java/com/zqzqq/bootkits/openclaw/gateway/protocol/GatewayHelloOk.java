package com.zqzqq.bootkits.openclaw.gateway.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class GatewayHelloOk {

    private String type = OpenClawGatewayConstants.HELLO_OK;
    private Integer protocol;
    private GatewayServerInfo server;
    private GatewayFeatureSet features;
    private Map<String, Object> snapshot = new LinkedHashMap<>();
    private Map<String, Object> auth = new LinkedHashMap<>();
    private GatewayPolicy policy;
    private Map<String, String> pluginSurfaceUrls = new LinkedHashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? OpenClawGatewayConstants.HELLO_OK : type;
    }

    public Integer getProtocol() {
        return protocol;
    }

    public void setProtocol(Integer protocol) {
        this.protocol = protocol;
    }

    public GatewayServerInfo getServer() {
        return server;
    }

    public void setServer(GatewayServerInfo server) {
        this.server = server;
    }

    public GatewayFeatureSet getFeatures() {
        return features;
    }

    public void setFeatures(GatewayFeatureSet features) {
        this.features = features;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Map<String, Object> snapshot) {
        this.snapshot = snapshot == null ? new LinkedHashMap<>() : new LinkedHashMap<>(snapshot);
    }

    public Map<String, Object> getAuth() {
        return auth;
    }

    public void setAuth(Map<String, Object> auth) {
        this.auth = auth == null ? new LinkedHashMap<>() : new LinkedHashMap<>(auth);
    }

    public GatewayPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(GatewayPolicy policy) {
        this.policy = policy;
    }

    public Map<String, String> getPluginSurfaceUrls() {
        return pluginSurfaceUrls;
    }

    public void setPluginSurfaceUrls(Map<String, String> pluginSurfaceUrls) {
        this.pluginSurfaceUrls = pluginSurfaceUrls == null ? new LinkedHashMap<>() : new LinkedHashMap<>(pluginSurfaceUrls);
    }
}
