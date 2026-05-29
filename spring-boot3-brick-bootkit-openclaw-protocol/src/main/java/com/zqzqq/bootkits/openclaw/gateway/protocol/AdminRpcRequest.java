package com.zqzqq.bootkits.openclaw.gateway.protocol;

import java.util.UUID;

public class AdminRpcRequest {

    private String id = UUID.randomUUID().toString();
    private String method;
    private Object params;

    public static AdminRpcRequest request(String method, Object params) {
        AdminRpcRequest request = new AdminRpcRequest();
        request.setMethod(method);
        request.setParams(params);
        return request;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
