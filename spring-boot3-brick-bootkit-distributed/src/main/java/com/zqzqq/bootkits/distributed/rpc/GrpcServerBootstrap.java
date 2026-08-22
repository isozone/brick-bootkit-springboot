/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

    /** 优雅停机时长（秒）：等待在途 gRPC 调用排空后再强制关闭，避免升级/关停时丢调用。 */
    private final long gracefulShutdownSeconds;

    /**
     * 服务端 keepAlive 配置（毫秒，<=0 表示不显式设置，沿用 netty/gRPC 默认值）。
     * 用于与客户端 keepAlive 配合，告知服务端「允许客户端在没有活跃调用时也发 ping」
     * （{@code permitKeepAliveWithoutCalls}），并设置 ping 的最小间隔
     * （{@code permitKeepAliveTime}，低于此值会被服务端拒绝导致连接关闭，防御恶意客户端）。
     */
    private final long keepAliveTimeMillis;
    private final long permitKeepAliveTimeMillis;
    private final boolean permitKeepAliveWithoutCalls;

    private Server server;

    public GrpcServerBootstrap(int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService) {
        this("", port, maxInboundMessageSize, invocationService, false, "", "", "", 10L);
    }

    public GrpcServerBootstrap(String host,
                               int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService,
                               boolean tlsEnabled,
                               String certChainPath,
                               String privateKeyPath,
                               String authToken) {
        this(host, port, maxInboundMessageSize, invocationService, tlsEnabled,
                certChainPath, privateKeyPath, authToken, 10L);
    }

    public GrpcServerBootstrap(String host,
                               int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService,
                               boolean tlsEnabled,
                               String certChainPath,
                               String privateKeyPath,
                               String authToken,
                               long gracefulShutdownSeconds) {
        this(host, port, maxInboundMessageSize, invocationService, tlsEnabled,
                certChainPath, privateKeyPath, authToken, gracefulShutdownSeconds,
                60_000L, 30_000L, true);
    }

    /**
     * 全参构造（含 keepAlive）。供自动配置注入，让服务端 keepAlive 可调。
     *
     * @param keepAliveTimeMillis        服务端发起 keepAlive ping 间隔；<=0 不设置
     * @param permitKeepAliveTimeMillis  允许客户端 ping 的最小间隔；<=0 不设置
     * @param permitKeepAliveWithoutCalls 是否允许客户端在无活跃调用时也 ping
     */
    public GrpcServerBootstrap(String host,
                               int port,
                               int maxInboundMessageSize,
                               PluginInvocationServiceImpl invocationService,
                               boolean tlsEnabled,
                               String certChainPath,
                               String privateKeyPath,
                               String authToken,
                               long gracefulShutdownSeconds,
                               long keepAliveTimeMillis,
                               long permitKeepAliveTimeMillis,
                               boolean permitKeepAliveWithoutCalls) {
        this.host = host;
        this.port = port;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.invocationService = invocationService;
        this.tlsEnabled = tlsEnabled;
        this.certChainPath = certChainPath;
        this.privateKeyPath = privateKeyPath;
        this.authToken = authToken;
        this.gracefulShutdownSeconds = Math.max(1L, gracefulShutdownSeconds);
        this.keepAliveTimeMillis = keepAliveTimeMillis;
        this.permitKeepAliveTimeMillis = permitKeepAliveTimeMillis;
        this.permitKeepAliveWithoutCalls = permitKeepAliveWithoutCalls;
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

        // keepAlive：与客户端配合，避免跨 NAT/公网时长连接被中间设备静默断开。
        // permitKeepAliveTime 用于防御恶意客户端高频 ping（默认 30s，低于该值会被服务端拒绝）。
        if (keepAliveTimeMillis > 0) {
            builder.keepAliveTime(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
        }
        if (permitKeepAliveTimeMillis > 0) {
            builder.permitKeepAliveTime(permitKeepAliveTimeMillis, TimeUnit.MILLISECONDS);
        }
        builder.permitKeepAliveWithoutCalls(permitKeepAliveWithoutCalls);

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
            log.info("开始优雅停机 gRPC 服务，等待在途调用排空，最长 {}s ...", gracefulShutdownSeconds);
            boolean drained = server.shutdown().awaitTermination(gracefulShutdownSeconds, TimeUnit.SECONDS);
            if (!drained) {
                log.warn("优雅停机超时（{}s），强制关闭仍在途的调用", gracefulShutdownSeconds);
                server.shutdownNow();
                server.awaitTermination(2, TimeUnit.SECONDS);
            }
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

    public long getKeepAliveTimeMillis() {
        return keepAliveTimeMillis;
    }

    public long getPermitKeepAliveTimeMillis() {
        return permitKeepAliveTimeMillis;
    }

    public boolean isPermitKeepAliveWithoutCalls() {
        return permitKeepAliveWithoutCalls;
    }
}