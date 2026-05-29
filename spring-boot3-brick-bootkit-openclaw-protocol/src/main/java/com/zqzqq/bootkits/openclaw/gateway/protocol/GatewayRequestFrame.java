package com.zqzqq.bootkits.openclaw.gateway.protocol;

import java.util.UUID;

public class GatewayRequestFrame {

    private String type = OpenClawGatewayConstants.FRAME_REQUEST;
    private String id = UUID.randomUUID().toString();
    private String method;
    private Object params;

    public static GatewayRequestFrame request(String method, Object params) {
        GatewayRequestFrame frame = new GatewayRequestFrame();
        frame.setMethod(method);
        frame.setParams(params);
        return frame;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? OpenClawGatewayConstants.FRAME_REQUEST : type;
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
