package com.zqzqq.bootkits.openclaw.control.client;

import java.time.Duration;

public class OpenClawAgentRuntimeProperties {

    private boolean autoClaimOnAssigned = true;
    private boolean pollEnabled = true;
    private Duration claimPollInterval = Duration.ofSeconds(15);
    private Duration leaseRenewAhead = Duration.ofSeconds(30);
    private int executorThreads = 2;

    public boolean isAutoClaimOnAssigned() {
        return autoClaimOnAssigned;
    }

    public void setAutoClaimOnAssigned(boolean autoClaimOnAssigned) {
        this.autoClaimOnAssigned = autoClaimOnAssigned;
    }

    public boolean isPollEnabled() {
        return pollEnabled;
    }

    public void setPollEnabled(boolean pollEnabled) {
        this.pollEnabled = pollEnabled;
    }

    public Duration getClaimPollInterval() {
        return claimPollInterval;
    }

    public void setClaimPollInterval(Duration claimPollInterval) {
        this.claimPollInterval = claimPollInterval == null ? Duration.ofSeconds(15) : claimPollInterval;
    }

    public Duration getLeaseRenewAhead() {
        return leaseRenewAhead;
    }

    public void setLeaseRenewAhead(Duration leaseRenewAhead) {
        this.leaseRenewAhead = leaseRenewAhead == null ? Duration.ofSeconds(30) : leaseRenewAhead;
    }

    public int getExecutorThreads() {
        return executorThreads;
    }

    public void setExecutorThreads(int executorThreads) {
        this.executorThreads = executorThreads <= 0 ? 2 : executorThreads;
    }
}
