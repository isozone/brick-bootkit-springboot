package com.zqzqq.bootkits.distributed.rpc;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 服务端（执行节点侧）启动器。
 * <p>
 * 将 {@link PluginInvocationServiceImpl} 绑定到指定端口，对外提供插件泛化调用入口。
 * 支持：
 * <ul>
 *   <li>监听指定 IP（多网卡场景可显式配置 {@code plugin.distributed.host}）；</li>
 *   <li>可选 TLS（{@code tls-enabled} + 证书/私钥路径）；</li>
 *   <li>可选 token 鉴权（{@code auth-token}，通过 {@link AuthInterceptors} 拦截）。</li>
 * </ul>
 */
public class GrpcServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerBootstrap.class);

    private final String host;
    private final int port;
    private final int maxInboundMessageSize;
    private final boolean tlsEnabled;
    private final String certChainPath;
    private final String privateKeyPath;
    private final String authToken;
    private final PluginInvocationServiceImpl invocationService;

    private Server server;

    public GrpcServerBootstrap(int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService) {
        this("", port, maxInboundMessageSize, invocationService, false, "", "", "");
    }

    public GrpcServerBootstrap(String host,
                               int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService,
                               boolean tlsEnabled,
                               String certChainPath,
                               String privateKeyPath,
                               String authToken) {
        this.host = host;
        this.port = port;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.invocationService = invocationService;
        this.tlsEnabled = tlsEnabled;
        this.certChainPath = certChainPath;
        this.privateKeyPath = privateKeyPath;
        this.authToken = authToken;
    }

    /**
     * 启动 gRPC 服务端。
     */
    public synchronized void start() throws IOException {
        if (server != null && !server.isShutdown()) {
            return;
        }

        NettyServerBuilder builder = host != null && !host.isEmpty()
                ? NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                : NettyServerBuilder.forPort(port);
        builder.maxInboundMessageSize(maxInboundMessageSize);

        // 鉴权拦截器（token 为空时不启用）
        io.grpc.ServerInterceptor auth = AuthInterceptors.server(authToken);
        if (auth != null) {
            builder.addService(ServerInterceptors.intercept(invocationService, auth));
        } else {
            builder.addService(invocationService);
        }

        if (tlsEnabled) {
            if (certChainPath == null || certChainPath.isEmpty()
                    || privateKeyPath == null || privateKeyPath.isEmpty()) {
                throw new IOException("启用 TLS 时必须配置 tls-cert-chain 与 tls-private-key");
            }
            try {
                SslContextBuilder sslBuilder = GrpcSslContexts.forServer(
                        new File(certChainPath), new File(privateKeyPath));
                builder.sslContext(sslBuilder.build());
                log.info("分布式插件 gRPC 服务端已启用 TLS");
            } catch (IOException e) {
                throw new IOException("读取 TLS 证书/私钥失败，请检查 tls-cert-chain / tls-private-key 配置", e);
            }
        }

        server = builder.build().start();
        log.info("分布式插件 gRPC 服务端已启动，监听地址: [{}]:{}", resolveBindAddress(), port);
    }

    private String resolveBindAddress() {
        return host != null && !host.isEmpty() ? host : "0.0.0.0";
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