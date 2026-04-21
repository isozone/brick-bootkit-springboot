package com.zqzqq.bootkits.openclaw.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TaskRoutingDecision {

    private String taskType;
    private Set<String> requiredCapabilities;
    private String policy;
    private String selectedClientId;
    private Integer selectedScore;
    private List<TaskRoutingCandidate> candidates = new ArrayList<>();

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Set<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(Set<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getSelectedClientId() {
        return selectedClientId;
    }

    public void setSelectedClientId(String selectedClientId) {
        this.selectedClientId = selectedClientId;
    }

    public Integer getSelectedScore() {
        return selectedScore;
    }

    public void setSelectedScore(Integer selectedScore) {
        this.selectedScore = selectedScore;
    }

    public List<TaskRoutingCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<TaskRoutingCandidate> candidates) {
        this.candidates = candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
    }
}
