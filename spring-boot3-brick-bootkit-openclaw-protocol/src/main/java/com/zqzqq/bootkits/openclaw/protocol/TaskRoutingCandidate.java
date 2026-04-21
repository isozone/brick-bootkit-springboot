package com.zqzqq.bootkits.openclaw.protocol;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TaskRoutingCandidate {

    private String clientId;
    private String displayName;
    private ClientStatus status;
    private boolean eligible;
    private int score;
    private int openTaskCount;
    private Set<String> matchedCapabilities = new LinkedHashSet<>();
    private List<String> reasons = new ArrayList<>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getOpenTaskCount() {
        return openTaskCount;
    }

    public void setOpenTaskCount(int openTaskCount) {
        this.openTaskCount = openTaskCount;
    }

    public Set<String> getMatchedCapabilities() {
        return matchedCapabilities;
    }

    public void setMatchedCapabilities(Set<String> matchedCapabilities) {
        this.matchedCapabilities = matchedCapabilities == null ? new LinkedHashSet<>() : new LinkedHashSet<>(matchedCapabilities);
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null ? new ArrayList<>() : new ArrayList<>(reasons);
    }
}
