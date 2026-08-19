package com.zqzqq.bootkits.distributed.rpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端连接池：按「host:port」复用 ManagedChannel。
 * <p>
 * 宿主到多个执行节点之间会复用一个 channel，避免反复建连。
 * 支持可选的 TLS（{@code tlsEnabled} + {@code caCertPath}）与 token 鉴权（{@code authToken}）。
 */
public class GrpcClientProvider implements AutoCloseable {

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final int maxInboundMessageSize;
    private final long callTimeoutMillis;
    private final boolean tlsEnabled;
    private final String caCertPath;
    private final String authToken;

    public GrpcClientProvider(int maxInboundMessageSize,
                              long callTimeoutMillis,
                              boolean tlsEnabled,
                              String caCertPath,
                              String authToken) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.callTimeoutMillis = callTimeoutMillis;
        this.tlsEnabled = tlsEnabled;
        this.caCertPath = caCertPath;
        this.authToken = authToken;
    }

    /**
     * 兼容构造：明文、无鉴权。
     */
    public GrpcClientProvider(int maxInboundMessageSize, long callTimeoutMillis) {
        this(maxInboundMessageSize, callTimeoutMillis, false, "", "");
    }

    /**
     * 获取（或创建）到指定节点的 channel。
     */
    public ManagedChannel channel(String host, int port) {
        String key = host + ":" + port;
        return channels.computeIfAbsent(key, k -> buildChannel(host, port));
    }

    private ManagedChannel buildChannel(String host, int port) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port)
                .maxInboundMessageSize(maxInboundMessageSize);
        // 鉴权拦截器（token 为空时不启用）
        io.grpc.ClientInterceptor auth = AuthInterceptors.client(authToken);
        if (auth != null) {
            builder.intercept(auth);
        }
        if (tlsEnabled) {
            try {
                SslContextBuilder sslBuilder = GrpcSslContexts.forClient();
                if (caCertPath != null && !caCertPath.isEmpty()) {
                    sslBuilder.trustManager(new File(caCertPath));
                } else {
                    // 未提供 CA 时信任系统默认根证书
                    sslBuilder.trustManager();
                }
                builder.sslContext(sslBuilder.build());
                // useTransportSecurity() 对应启用 TLS（不互斥，需在 usePlaintext 之前明确）
                return builder.useTransportSecurity().build();
            } catch (IOException e) {
                throw new IllegalStateException(
                        "创建 gRPC TLS 客户端失败，请检查 tls-enabled / tls-ca-cert 配置", e);
            }
        }
        return builder.usePlaintext().build();
    }

    public long getCallTimeoutMillis() {
        return callTimeoutMillis;
    }

    public void evict(String host, int port) {
        String key = host + ":" + port;
        ManagedChannel channel = channels.remove(key);
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        channels.clear();
    }

    public void shutdownNow() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdownNow();
        }
        channels.clear();
    }
}