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

    /** 节点被标记为不健康后的冷却时间（毫秒）。冷却期内优先避免向其发起网络调用。 */
    private static final long COOLDOWN_MILLIS = 2000L;

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    /** 节点健康状态：key = host:port:tls，value = 最近一次失败时间戳（0 = 健康）。 */
    private final Map<String, Long> unhealthySince = new ConcurrentHashMap<>();

    /** 累计故障转移次数（供监控/告警）。 */
    private final java.util.concurrent.atomic.LongAdder failoverCount = new java.util.concurrent.atomic.LongAdder();
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
     * 获取（或创建）到指定节点的 channel，使用默认传输方式（构造时的全局 {@code tlsEnabled}）。
     */
    public ManagedChannel channel(String host, int port) {
        return channel(host, port, this.tlsEnabled);
    }

    /**
     * 获取（或创建）到指定节点的 channel，按节点选择传输方式。
     * <p>支持明文 / TLS 混合部署：channel 缓存键包含 TLS 标记，因此同一连接池可同时持有
     * 到「明文节点」与「TLS 节点」的连接，无需两端全局一致，支撑滚动升级灰度 TLS。</p>
     */
    public ManagedChannel channel(String host, int port, boolean tls) {
        String key = host + ":" + port + ":" + tls;
        return channels.computeIfAbsent(key, k -> buildChannel(host, port, tls));
    }

    private ManagedChannel buildChannel(String host, int port, boolean tls) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port)
                .maxInboundMessageSize(maxInboundMessageSize);
        // 鉴权拦截器（token 为空时不启用）
        io.grpc.ClientInterceptor auth = AuthInterceptors.client(authToken);
        if (auth != null) {
            builder.intercept(auth);
        }
        if (tls) {
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

    // ==================== 节点健康状态（快速失败 + 退避） ====================

    private String healthKey(String host, int port, boolean tls) {
        return host + ":" + port + ":" + tls;
    }

    /**
     * 判断节点当前是否健康。冷却期内的节点视为不可用（快速失败），
     * 避免在目录尚未剔除陈旧节点前，每次调用都对同一宕机节点发起网络超时。
     */
    public boolean isHealthy(String host, int port, boolean tls) {
        Long failedAt = unhealthySince.get(healthKey(host, port, tls));
        if (failedAt == null) {
            return true;
        }
        return System.currentTimeMillis() - failedAt > COOLDOWN_MILLIS;
    }

    /**
     * 记录节点调用失败（传输层不可达）。进入冷却期，配合 {@link #isHealthy} 快速失败。
     */
    public void markFailure(String host, int port, boolean tls) {
        unhealthySince.put(healthKey(host, port, tls), System.currentTimeMillis());
    }

    /**
     * 记录节点调用成功，解除冷却状态。
     */
    public void markSuccess(String host, int port, boolean tls) {
        unhealthySince.remove(healthKey(host, port, tls));
    }

    /**
     * 累计一次故障转移。EXPOSE 供监控/actuator 指标采样节点不可用次数。
     */
    public void recordFailover() {
        failoverCount.increment();
    }

    /**
     * 当前累计的故障转移次数（线程安全）。
     */
    public long failoverCount() {
        return failoverCount.sum();
    }

    public void evict(String host, int port) {
        // 迁移/重建：清掉该节点在明文与 TLS 两种传输下的所有 channel
        evict(host, port, true);
        evict(host, port, false);
    }

    public void evict(String host, int port, boolean tls) {
        String key = host + ":" + port + ":" + tls;
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