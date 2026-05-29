package com.zqzqq.bootkits.openclaw.gateway.protocol;

public class GatewayClientDescriptor {

    private String id;
    private String version;
    private String platform;
    private String mode = "operator";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null || mode.isBlank() ? "operator" : mode;
    }
}
