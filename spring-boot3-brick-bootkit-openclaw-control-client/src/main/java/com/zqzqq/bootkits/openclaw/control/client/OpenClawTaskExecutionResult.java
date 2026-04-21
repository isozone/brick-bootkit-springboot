package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ArtifactDescriptor;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenClawTaskExecutionResult {

    private TaskStatus status = TaskStatus.SUCCEEDED;
    private String message;
    private String output;
    private String error;
    private Map<String, Object> result = new LinkedHashMap<>();
    private List<ArtifactDescriptor> artifacts = new ArrayList<>();

    public static OpenClawTaskExecutionResult succeeded() {
        return new OpenClawTaskExecutionResult().setStatus(TaskStatus.SUCCEEDED);
    }

    public static OpenClawTaskExecutionResult failed(String error) {
        return new OpenClawTaskExecutionResult().setStatus(TaskStatus.FAILED).setError(error);
    }

    public static OpenClawTaskExecutionResult cancelled(String message) {
        return new OpenClawTaskExecutionResult().setStatus(TaskStatus.CANCELLED).setMessage(message);
    }

    public TaskStatus getStatus() {
        return status;
    }

    public OpenClawTaskExecutionResult setStatus(TaskStatus status) {
        this.status = status == null ? TaskStatus.SUCCEEDED : status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OpenClawTaskExecutionResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getOutput() {
        return output;
    }

    public OpenClawTaskExecutionResult setOutput(String output) {
        this.output = output;
        return this;
    }

    public String getError() {
        return error;
    }

    public OpenClawTaskExecutionResult setError(String error) {
        this.error = error;
        return this;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public OpenClawTaskExecutionResult setResult(Map<String, Object> result) {
        this.result = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
        return this;
    }

    public List<ArtifactDescriptor> getArtifacts() {
        return artifacts;
    }

    public OpenClawTaskExecutionResult setArtifacts(List<ArtifactDescriptor> artifacts) {
        this.artifacts = artifacts == null ? new ArrayList<>() : new ArrayList<>(artifacts);
        return this;
    }
}
