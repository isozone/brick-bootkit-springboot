package com.zqzqq.bootkits.distributed.rpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 服务端（执行节点侧）启动器。
 * <p>
 * 将 {@link PluginInvocationServiceImpl} 绑定到指定端口，对外提供
 * 插件泛化调用入口，供宿主节点远程调用本节点承载的插件服务。
 */
public class GrpcServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerBootstrap.class);

    private final int port;
    private final int maxInboundMessageSize;
    private final PluginInvocationServiceImpl invocationService;

    private Server server;

    public GrpcServerBootstrap(int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService) {
        this.port = port;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.invocationService = invocationService;
    }

    /**
     * 启动 gRPC 服务端。
     */
    public synchronized void start() throws IOException {
        if (server != null && !server.isShutdown()) {
            return;
        }
        server = ServerBuilder.forPort(port)
                .maxInboundMessageSize(maxInboundMessageSize)
                .addService(invocationService)
                .build()
                .start();
        log.info("分布式插件 gRPC 服务端已启动，监听端口: {}", port);
    }

    public synchronized void shutdown() {
        if (server == null) {
            return;
        }
        try {
            server.shutdown().awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
        log.info("分布式插件 gRPC 服务端已关闭，端口: {}", port);
    }

    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }

    /**
     * 获取监听端口。
     */
    public int getPort() {
        return port;
    }
}