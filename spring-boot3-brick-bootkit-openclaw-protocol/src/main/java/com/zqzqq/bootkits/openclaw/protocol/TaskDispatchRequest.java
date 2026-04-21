package com.zqzqq.bootkits.openclaw.protocol;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TaskDispatchRequest {

    private String taskType;
    private String requestedBy;
    private String targetClientId;
    private Set<String> targetTags = new LinkedHashSet<>();
    private Set<String> requiredCapabilities = new LinkedHashSet<>();
    private Map<String, Object> payload = new LinkedHashMap<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();
    private Long timeoutSeconds;
    private Long leaseSeconds;
    private Integer maxRetries = 0;
    private Boolean allowStaleClient = Boolean.FALSE;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(String targetClientId) {
        this.targetClientId = targetClientId;
    }

    public Set<String> getTargetTags() {
        return targetTags;
    }

    public void setTargetTags(Set<String> targetTags) {
        this.targetTags = targetTags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(targetTags);
    }

    public Set<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(Set<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities == null ? new LinkedHashSet<>() : new LinkedHashSet<>(requiredCapabilities);
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Long getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(Long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Boolean getAllowStaleClient() {
        return allowStaleClient;
    }

    public void setAllowStaleClient(Boolean allowStaleClient) {
        this.allowStaleClient = allowStaleClient;
    }
}
