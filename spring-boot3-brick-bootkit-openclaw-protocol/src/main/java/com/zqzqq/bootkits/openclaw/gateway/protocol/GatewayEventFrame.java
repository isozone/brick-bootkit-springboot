package com.zqzqq.bootkits.openclaw.gateway.protocol;

public class GatewayEventFrame {

    private String type = OpenClawGatewayConstants.FRAME_EVENT;
    private String event;
    private Object payload;
    private Long seq;
    private Long stateVersion;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? OpenClawGatewayConstants.FRAME_EVENT : type;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public Long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(Long stateVersion) {
        this.stateVersion = stateVersion;
    }
}
