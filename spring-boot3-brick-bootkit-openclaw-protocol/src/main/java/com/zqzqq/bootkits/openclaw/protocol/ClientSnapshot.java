package com.zqzqq.bootkits.openclaw.protocol;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientSnapshot {

    private String clientId;
    private String machineId;
    private String displayName;
    private String version;
    private String sdkVersion;
    private String sessionId;
    private String hostName;
    private String osName;
    private String osVersion;
    private ClientStatus status;
    private Set<String> tags = new LinkedHashSet<>();
    private Map<String, Object> attributes = new LinkedHashMap<>();
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private List<ClientCapabilityDescriptor> capabilities = new ArrayList<>();
    private List<String> currentTaskIds = new ArrayList<>();
    private Instant registeredAt;
    private Instant lastSeenAt;
    private Instant updatedAt;

    public ClientSnapshot() {
    }

    public ClientSnapshot(ClientSnapshot source) {
        if (source == null) {
            return;
        }
        this.clientId = source.clientId;
        this.machineId = source.machineId;
        this.displayName = source.displayName;
        this.version = source.version;
        this.sdkVersion = source.sdkVersion;
        this.sessionId = source.sessionId;
        this.hostName = source.hostName;
        this.osName = source.osName;
        this.osVersion = source.osVersion;
        this.status = source.status;
        this.tags = new LinkedHashSet<>(source.tags);
        this.attributes = new LinkedHashMap<>(source.attributes);
        this.metrics = new LinkedHashMap<>(source.metrics);
        for (ClientCapabilityDescriptor capability : source.capabilities) {
            this.capabilities.add(new ClientCapabilityDescriptor(capability));
        }
        this.currentTaskIds = new ArrayList<>(source.currentTaskIds);
        this.registeredAt = source.registeredAt;
        this.lastSeenAt = source.lastSeenAt;
        this.updatedAt = source.updatedAt;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metrics);
    }

    public List<ClientCapabilityDescriptor> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<ClientCapabilityDescriptor> capabilities) {
        this.capabilities = capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
    }

    public List<String> getCurrentTaskIds() {
        return currentTaskIds;
    }

    public void setCurrentTaskIds(List<String> currentTaskIds) {
        this.currentTaskIds = currentTaskIds == null ? new ArrayList<>() : new ArrayList<>(currentTaskIds);
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
