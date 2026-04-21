package com.zqzqq.bootkits.openclaw.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClientCapabilityDescriptor {

    private String capabilityId;
    private String displayName;
    private String category;
    private String version;
    private Boolean enabled = Boolean.TRUE;
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public ClientCapabilityDescriptor() {
    }

    public ClientCapabilityDescriptor(ClientCapabilityDescriptor source) {
        if (source == null) {
            return;
        }
        this.capabilityId = source.capabilityId;
        this.displayName = source.displayName;
        this.category = source.category;
        this.version = source.version;
        this.enabled = source.enabled;
        this.attributes = new LinkedHashMap<>(source.attributes);
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }
}
