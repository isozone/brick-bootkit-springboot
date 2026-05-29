package com.zqzqq.bootkits.openclaw.gateway.protocol;

public class ToolInvokeResponse {

    private Boolean ok;
    private Object result;
    private GatewayError error;

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public GatewayError getError() {
        return error;
    }

    public void setError(GatewayError error) {
        this.error = error;
    }
}
