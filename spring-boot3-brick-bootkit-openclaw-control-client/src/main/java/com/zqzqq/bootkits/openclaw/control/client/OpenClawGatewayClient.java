package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.gateway.protocol.AdminRpcRequest;
import com.zqzqq.bootkits.openclaw.gateway.protocol.AdminRpcResponse;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayConnectParams;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayHelloOk;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayResponseFrame;
import com.zqzqq.bootkits.openclaw.gateway.protocol.ToolInvokeRequest;
import com.zqzqq.bootkits.openclaw.gateway.protocol.ToolInvokeResponse;

public interface OpenClawGatewayClient extends AutoCloseable {

    GatewayHelloOk connect(GatewayConnectParams params);

    GatewayResponseFrame rpc(String method, Object params);

    ToolInvokeResponse invokeTool(ToolInvokeRequest request);

    AdminRpcResponse adminRpc(AdminRpcRequest request);

    boolean isConnected();

    @Override
    void close();
}
