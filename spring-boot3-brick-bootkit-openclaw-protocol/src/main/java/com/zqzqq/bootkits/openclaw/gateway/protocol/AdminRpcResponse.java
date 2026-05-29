package com.zqzqq.bootkits.openclaw.gateway.protocol;

public class AdminRpcResponse {

    private String id;
    private Boolean ok;
    private Object payload;
    private GatewayError error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public GatewayError getError() {
        return error;
    }

    public void setError(GatewayError error) {
        this.error = error;
    }
}
