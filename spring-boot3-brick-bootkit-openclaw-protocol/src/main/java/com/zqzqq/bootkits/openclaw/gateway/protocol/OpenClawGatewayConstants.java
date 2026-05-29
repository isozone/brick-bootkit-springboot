package com.zqzqq.bootkits.openclaw.gateway.protocol;

public final class OpenClawGatewayConstants {

    public static final String DEFAULT_HTTP_URL = "http://127.0.0.1:18789";
    public static final String DEFAULT_WS_URL = "ws://127.0.0.1:18789";
    public static final String FRAME_REQUEST = "req";
    public static final String FRAME_RESPONSE = "res";
    public static final String FRAME_EVENT = "event";
    public static final String METHOD_CONNECT = "connect";
    public static final String HELLO_OK = "hello-ok";
    public static final String TOOL_INVOKE_PATH = "/tools/invoke";
    public static final String ADMIN_RPC_PATH = "/api/v1/admin/rpc";
    public static final int MIN_PROTOCOL = 3;
    public static final int MAX_PROTOCOL = 4;

    private OpenClawGatewayConstants() {
    }
}
