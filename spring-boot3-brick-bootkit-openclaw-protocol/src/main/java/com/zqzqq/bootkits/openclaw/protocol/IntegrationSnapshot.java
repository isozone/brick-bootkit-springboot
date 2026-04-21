package com.zqzqq.bootkits.openclaw.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntegrationSnapshot {

    private String integrationId;
    private String displayName;
    private boolean available;
    private List<ClientCapabilityDescriptor> capabilities = new ArrayList<>();
    private Map<String, Object> details = new LinkedHashMap<>();

    public IntegrationSnapshot() {
    }

    public IntegrationSnapshot(IntegrationSnapshot source) {
        if (source == null) {
            return;
        }
        this.integrationId = source.integrationId;
        this.displayName = source.displayName;
        this.available = source.available;
        for (ClientCapabilityDescriptor capability : source.capabilities) {
            this.capabilities.add(new ClientCapabilityDescriptor(capability));
        }
        this.details = new LinkedHashMap<>(source.details);
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<ClientCapabilityDescriptor> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<ClientCapabilityDescriptor> capabilities) {
        this.capabilities = capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
    }
}
