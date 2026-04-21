package com.zqzqq.bootkits.openclaw.protocol;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskSnapshot {

    private String taskId;
    private String taskType;
    private String requestedBy;
    private String assignedClientId;
    private TaskStatus status;
    private Set<String> requiredCapabilities = new LinkedHashSet<>();
    private Map<String, Object> payload = new LinkedHashMap<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();
    private Map<String, Object> details = new LinkedHashMap<>();
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private Map<String, Object> result = new LinkedHashMap<>();
    private List<ArtifactDescriptor> artifacts = new ArrayList<>();
    private String message;
    private String output;
    private String error;
    private Integer progressPercent;
    private Integer retryCount;
    private Integer maxRetries;
    private Long timeoutSeconds;
    private Long leaseSeconds;
    private String parentTaskId;
    private String cancelledBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant claimedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant leaseExpiresAt;
    private Instant cancelRequestedAt;

    public TaskSnapshot() {
    }

    public TaskSnapshot(TaskSnapshot source) {
        if (source == null) {
            return;
        }
        this.taskId = source.taskId;
        this.taskType = source.taskType;
        this.requestedBy = source.requestedBy;
        this.assignedClientId = source.assignedClientId;
        this.status = source.status;
        this.requiredCapabilities = new LinkedHashSet<>(source.requiredCapabilities);
        this.payload = new LinkedHashMap<>(source.payload);
        this.metadata = new LinkedHashMap<>(source.metadata);
        this.details = new LinkedHashMap<>(source.details);
        this.metrics = new LinkedHashMap<>(source.metrics);
        this.result = new LinkedHashMap<>(source.result);
        for (ArtifactDescriptor artifact : source.artifacts) {
            this.artifacts.add(new ArtifactDescriptor(artifact));
        }
        this.message = source.message;
        this.output = source.output;
        this.error = source.error;
        this.progressPercent = source.progressPercent;
        this.retryCount = source.retryCount;
        this.maxRetries = source.maxRetries;
        this.timeoutSeconds = source.timeoutSeconds;
        this.leaseSeconds = source.leaseSeconds;
        this.parentTaskId = source.parentTaskId;
        this.cancelledBy = source.cancelledBy;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
        this.claimedAt = source.claimedAt;
        this.startedAt = source.startedAt;
        this.finishedAt = source.finishedAt;
        this.leaseExpiresAt = source.leaseExpiresAt;
        this.cancelRequestedAt = source.cancelRequestedAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

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

    public String getAssignedClientId() {
        return assignedClientId;
    }

    public void setAssignedClientId(String assignedClientId) {
        this.assignedClientId = assignedClientId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
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

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metrics);
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    }

    public List<ArtifactDescriptor> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ArtifactDescriptor> artifacts) {
        this.artifacts = artifacts == null ? new ArrayList<>() : new ArrayList<>(artifacts);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
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

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Instant getCancelRequestedAt() {
        return cancelRequestedAt;
    }

    public void setCancelRequestedAt(Instant cancelRequestedAt) {
        this.cancelRequestedAt = cancelRequestedAt;
    }
}
