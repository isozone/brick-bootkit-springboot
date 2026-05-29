package com.zqzqq.bootkits.openclaw.gateway.protocol;

public class GatewayPolicy {

    private Long maxPayload;
    private Long maxBufferedBytes;
    private Long tickIntervalMs;

    public Long getMaxPayload() {
        return maxPayload;
    }

    public void setMaxPayload(Long maxPayload) {
        this.maxPayload = maxPayload;
    }

    public Long getMaxBufferedBytes() {
        return maxBufferedBytes;
    }

    public void setMaxBufferedBytes(Long maxBufferedBytes) {
        this.maxBufferedBytes = maxBufferedBytes;
    }

    public Long getTickIntervalMs() {
        return tickIntervalMs;
    }

    public void setTickIntervalMs(Long tickIntervalMs) {
        this.tickIntervalMs = tickIntervalMs;
    }
}
