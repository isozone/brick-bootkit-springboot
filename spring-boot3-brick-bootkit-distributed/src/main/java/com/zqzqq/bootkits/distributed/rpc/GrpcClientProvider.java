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

import com.zqzqq.bootkits.distributed.metrics.DistributedMetrics;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(GrpcClientProvider.class);

    /** 节点被标记为不健康后的冷却时间（毫秒）。冷却期内优先避免向其发起网络调用。 */
    private static final long COOLDOWN_BASE_MILLIS = 2000L;

    /** 冷却上限（毫秒）：指数退避封顶，避免宕机节点被长期高频率重试。 */
    private static final long COOLDOWN_MAX_MILLIS = 60_000L;

    /**
     * 熔断开启阈值：连续失败次数达到该值后节点进入 OPEN，短路后续网络调用。
     * 与健康退避叠加使用——退避负责「存在健康节点时跳过」，熔断负责「全部节点都宕机时
     * 快速失败、不再反复发起真实网络超时」。
     */
    private static final int OPEN_THRESHOLD = 3;

    /** 熔断 OPEN 窗口（毫秒）：窗口结束进入半开(HALF-OPEN)探测。 */
    private static final long OPEN_COOLDOWN_MILLIS = 10_000L;

    /** 客户端 keepAlive 默认值（与 gRPC 官方推荐一致）。 */
    private static final long DEFAULT_KEEPALIVE_TIME_MILLIS = 60_000L;       // 60s 发一次 ping
    private static final long DEFAULT_KEEPALIVE_TIMEOUT_MILLIS = 20_000L;    // ping 20s 没回 ACK 视为死连接

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    /**
     * 节点健康状态：key = host:port:tls，value = 最近一次失败的时间戳 + 连续失败次数。
     * 依据连续失败次数计算<b>指数退避</b>冷却窗口；成功即清除记录（视为健康）。
     */
    private final Map<String, NodeHealth> nodeHealth = new ConcurrentHashMap<>();

    /** 累计故障转移次数（供监控/告警）。 */
    private final java.util.concurrent.atomic.LongAdder failoverCount = new java.util.concurrent.atomic.LongAdder();
    private final int maxInboundMessageSize;
    private final long callTimeoutMillis;
    private final boolean tlsEnabled;
    private final String caCertPath;
    private final String authToken;

    /**
     * keepAlive 配置：长连接探测间隔与超时（毫秒，<=0 表示不启用 keepAlive，由 netty/gRPC 默认值兜底）。
     * 跨网段/NAT/公网部署建议显式开启（默认开启），避免 idle 超时被中间设备断开后客户端无感知、
     * 第一次真实调用才发现连接已死造成额外超时。
     */
    private final long keepAliveTimeMillis;
    private final long keepAliveTimeoutMillis;
    /** 是否在无活跃调用期间也发 keepAlive ping（保活长连接，避免反复重建）。 */
    private final boolean keepAliveWithoutCalls;

    /** 分布式指标累加器（可为空，为空时不记录指标，保持纯连接池语义）。 */
    private final DistributedMetrics metrics;

    /** 节点的健康快照：最近失败时间 + 连续失败次数。 */
    private static final class NodeHealth {
        final long failedAt;
        final int failures;

        NodeHealth(long failedAt, int failures) {
            this.failedAt = failedAt;
            this.failures = failures;
        }
    }

    public GrpcClientProvider(int maxInboundMessageSize,
                              long callTimeoutMillis,
                              boolean tlsEnabled,
                              String caCertPath,
                              String authToken) {
        this(maxInboundMessageSize, callTimeoutMillis, tlsEnabled, caCertPath, authToken, null);
    }

    /**
     * 全参构造，可注入指标累加器（为空则视为不采集指标）。keepAlive 用默认值。
     */
    public GrpcClientProvider(int maxInboundMessageSize,
                              long callTimeoutMillis,
                              boolean tlsEnabled,
                              String caCertPath,
                              String authToken,
                              DistributedMetrics metrics) {
        this(maxInboundMessageSize, callTimeoutMillis, tlsEnabled, caCertPath, authToken,
                DEFAULT_KEEPALIVE_TIME_MILLIS, DEFAULT_KEEPALIVE_TIMEOUT_MILLIS, true, metrics);
    }

    /**
     * 全参构造（含 keepAlive 配置）。供自动配置注入，让 keepAlive 与超时可调。
     *
     * @param keepAliveTimeMillis     keepAlive ping 间隔；<=0 表示关闭
     * @param keepAliveTimeoutMillis  ping 等待 ACK 超时；仅 keepAliveTimeMillis>0 时生效
     * @param keepAliveWithoutCalls   是否在无活跃调用时也发 ping
     */
    public GrpcClientProvider(int maxInboundMessageSize,
                              long callTimeoutMillis,
                              boolean tlsEnabled,
                              String caCertPath,
                              String authToken,
                              long keepAliveTimeMillis,
                              long keepAliveTimeoutMillis,
                              boolean keepAliveWithoutCalls,
                              DistributedMetrics metrics) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.callTimeoutMillis = callTimeoutMillis;
        this.tlsEnabled = tlsEnabled;
        this.caCertPath = caCertPath;
        this.authToken = authToken;
        this.keepAliveTimeMillis = keepAliveTimeMillis;
        this.keepAliveTimeoutMillis = keepAliveTimeoutMillis;
        this.keepAliveWithoutCalls = keepAliveWithoutCalls;
        this.metrics = metrics;
    }

    /**
     * 兼容构造：明文、无鉴权。
     */
    public GrpcClientProvider(int maxInboundMessageSize, long callTimeoutMillis) {
        this(maxInboundMessageSize, callTimeoutMillis, false, "", "");
    }

    /**
     * 返回注入的指标累加器（可能为 null）。
     */
    public DistributedMetrics metrics() {
        return metrics;
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
        ManagedChannel channel = channels.computeIfAbsent(key, k -> buildChannel(host, port, tls));
        if (metrics != null) {
            metrics.updateActiveChannels(channels.size());
        }
        return channel;
    }

    private ManagedChannel buildChannel(String host, int port, boolean tls) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port)
                .maxInboundMessageSize(maxInboundMessageSize);
        // 鉴权拦截器（token 为空时不启用）
        io.grpc.ClientInterceptor auth = AuthInterceptors.client(authToken);
        if (auth != null) {
            builder.intercept(auth);
        }
        // keepAlive：跨 NAT/公网/容器网络时长连接可能被中间设备静默断开，
        // 客户端无感知的情况下首次真实调用会吃一次完整超时。开启 keepAlive 让
        // gRPC 在空闲期主动探测、尽早发现死连接。<=0 关闭（保留与旧版兼容的语义）。
        if (keepAliveTimeMillis > 0) {
            builder.keepAliveTime(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
            builder.keepAliveTimeout(Math.max(keepAliveTimeoutMillis, 1L), TimeUnit.MILLISECONDS);
            builder.keepAliveWithoutCalls(keepAliveWithoutCalls);
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

    /** keepAlive ping 间隔（毫秒）；<=0 表示关闭。供测试与监控读取。 */
    public long getKeepAliveTimeMillis() {
        return keepAliveTimeMillis;
    }

    /** keepAlive ping 等待 ACK 超时（毫秒）。 */
    public long getKeepAliveTimeoutMillis() {
        return keepAliveTimeoutMillis;
    }

    /** 是否在无活跃调用时也发 keepAlive ping。 */
    public boolean isKeepAliveWithoutCalls() {
        return keepAliveWithoutCalls;
    }

    // ==================== 节点健康状态（快速失败 + 退避） ====================

    private String healthKey(String host, int port, boolean tls) {
        return host + ":" + port + ":" + tls;
    }

    /**
     * 判断节点当前是否健康。
     * <p>不健康节点在<b>指数退避</b>冷却窗口内被视为不可用（快速失败），
     * 避免在目录尚未剔除陈旧节点前，每次调用都对同一宕机节点发起网络超时。
     * 连续失败次数越高，冷却窗口越长（base * 2^(failures-1)，封顶 {@link #COOLDOWN_MAX_MILLIS}），
     * 从而对持续宕机的节点平滑降频；一旦退出冷却或调用成功即恢复评估。</p>
     */
    public boolean isHealthy(String host, int port, boolean tls) {
        NodeHealth health = nodeHealth.get(healthKey(host, port, tls));
        if (health == null) {
            return true;
        }
        long cooldown = cooldownMillis(health.failures);
        return System.currentTimeMillis() - health.failedAt >= cooldown;
    }

    /** 依据连续失败次数计算指数退避冷却窗口（毫秒），封顶 COOLDOWN_MAX_MILLIS。 */
    private static long cooldownMillis(int failures) {
        if (failures <= 1) {
            return COOLDOWN_BASE_MILLIS;
        }
        // 2^n 递增但封顶：2s,4s,8s,...,60s。
        // 用 clamp 防止 (base << (failures-1)) 在失败次数很高时左移溢出为负数。
        int shift = Math.min(failures - 1, 30);
        long window = COOLDOWN_BASE_MILLIS << shift;
        return Math.min(window, COOLDOWN_MAX_MILLIS);
    }

    /**
     * 记录节点调用失败（传输层不可达）。连续失败计数递增，配合 {@link #isHealthy}
     * 触发指数退避；线程安全（CAS 更新，避免并发读改写丢计数）。
     */
    public void markFailure(String host, int port, boolean tls) {
        String key = healthKey(host, port, tls);
        nodeHealth.merge(key, new NodeHealth(System.currentTimeMillis(), 1), (old, add) ->
                new NodeHealth(System.currentTimeMillis(),
                        old.failures >= Integer.MAX_VALUE - 1 ? old.failures : old.failures + 1));
        if (metrics != null) {
            NodeHealth health = nodeHealth.get(key);
            metrics.updateNodeHealth(host, port, tls, false,
                    health != null ? cooldownMillis(health.failures) : cooldownMillis(1));
        }
    }

    /**
     * 记录节点调用成功，解除冷却状态（清空健康记录，连续失败计数归零）。
     */
    public void markSuccess(String host, int port, boolean tls) {
        nodeHealth.remove(healthKey(host, port, tls));
        if (metrics != null) {
            metrics.removeNodeHealth(host, port, tls);
        }
    }

    // ==================== 熔断器（Circuit Breaker） ====================

    /**
     * 判断节点是否处于 OPEN（熔断开启）：连续失败次数达到 {@link #OPEN_THRESHOLD}，
     * 且仍处于 OPEN 窗口内。OPEN 期间应<b>短路</b>——不再发起真实网络调用，
     * 直接快速失败，避免反复撞击全部宕机节点造成无谓超时。
     *
     * @return true 表示已熔断（应短路该节点的调用）
     */
    public boolean isTripped(String host, int port, boolean tls) {
        NodeHealth health = nodeHealth.get(healthKey(host, port, tls));
        if (health == null || health.failures < OPEN_THRESHOLD) {
            return false;
        }
        long openElapsed = System.currentTimeMillis() - health.failedAt;
        // 处于 OPEN 窗口内 → 短路；窗口结束 → 半开，允许探测（返回 false，放行一次）
        return openElapsed < OPEN_COOLDOWN_MILLIS;
    }

    /**
     * 是否允许对该节点发起一次真实网络调用。
     * <p>规则：
     * <ul>
     *   <li>未熔断（连续失败 &lt; 阈值 或 已在半开探测期）→ 允许；</li>
     *   <li>已熔断且仍在 OPEN 窗口 → 短路，不允许。</li>
     * </ul>
     */
    public boolean allowAttempt(String host, int port, boolean tls) {
        return !isTripped(host, port, tls);
    }

    /**
     * 触发一次熔断短路。（供调用侧在 {@code allowAttempt()==false} 时记录指标）
     */
    public void recordTrip(String host, int port, boolean tls) {
        if (metrics != null) {
            metrics.recordCircuitBreakerTrip();
        }
        log.debug("节点 {}:{} 已熔断短路（连续失败≥{}）", host, port, OPEN_THRESHOLD);
    }

    /** 熔断开启阈值（供测试/监控）。 */
    public int openThreshold() {
        return OPEN_THRESHOLD;
    }

    /** 熔断 OPEN 窗口毫秒（供测试/监控）。 */
    public long openCooldownMillis() {
        return OPEN_COOLDOWN_MILLIS;
    }

    /**
     * 当前节点是否处于冷却期（供监控）。
     */
    public boolean isInCooldown(String host, int port, boolean tls) {
        NodeHealth health = nodeHealth.get(healthKey(host, port, tls));
        if (health == null) {
            return false;
        }
        return System.currentTimeMillis() - health.failedAt < cooldownMillis(health.failures);
    }

    /**
     * 累计一次故障转移。EXPOSE 供监控/actuator 指标采样节点不可用次数。
     */
    public void recordFailover() {
        failoverCount.increment();
        if (metrics != null) {
            metrics.recordFailover();
        }
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
        if (metrics != null) {
            metrics.updateActiveChannels(channels.size());
        }
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        channels.clear();
        if (metrics != null) {
            metrics.updateActiveChannels(0);
        }
    }

    public void shutdownNow() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdownNow();
        }
        channels.clear();
        if (metrics != null) {
            metrics.updateActiveChannels(0);
        }
    }
}