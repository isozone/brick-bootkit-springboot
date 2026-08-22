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


package com.zqzqq.bootkits.distributed.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 分布式插件模块的<b>自包含指标</b>累加器。
 * <p>
 * 不依赖 Micrometer，纯 {@link LongAdder} / {@link ConcurrentHashMap} 实现，因此：
 * <ul>
 *   <li>可脱离 Spring/Actuator 独立运行与单元测试；</li>
 *   <li>被 {@code GrpcClientProvider} / {@code RemoteInvocationHandler} /
 *       {@code RedisServiceDirectory} 在调用热路径上记录事件；</li>
 *   <li>对外通过 {@link #snapshot()} 提供一致的快照，供 actuator HealthIndicator、
 *       Micrometer 桥接、以及 web 管理端点按需采集。</li>
 * </ul>
 * <p>
 * 线程安全：所有计数器均为 {@link LongAdder}（无锁累加），节点健康快照用
 * {@link ConcurrentHashMap} 只读采样，记录侧不做任何加锁。
 */
public class DistributedMetrics {

    // ==================== 调用与故障转移 ====================

    /** 远端调用总次数。 */
    private final LongAdder remoteCallCount = new LongAdder();

    /** 业务/传输成功返回的次数。 */
    private final LongAdder remoteCallSuccessCount = new LongAdder();

    /** 故障转移次数（节点传输层不可达后切换副本的次数）。 */
    private final LongAdder failoverCount = new LongAdder();

    /** 熔断触发次数（方向二；此处预留增量计数）。 */
    private final LongAdder circuitBreakerTrippedCount = new LongAdder();

    /** 远端调用总耗时（毫秒）累加，配合 {@link #remoteCallCount} 计算平均耗时。 */
    private final LongAdder remoteCallMillis = new LongAdder();

    // ==================== 服务目录 / Redis 兜底 ====================

    /** Redis 目录查询成功次数。 */
    private final LongAdder registryLookupSuccessCount = new LongAdder();

    /** Redis 目录查询失败（触发本地兜底）次数。 */
    private final LongAdder registryFallbackUsedCount = new LongAdder();

    /** 兜底缓存命中（返回了 last-known-good 快照）次数。 */
    private final LongAdder registryFallbackHitCount = new LongAdder();

    /** 兜底缓存未命中（目录故障且无可用快照）次数。 */
    private final LongAdder registryFallbackMissCount = new LongAdder();

    // ==================== 节点健康 ====================

    /**
     * 节点健康快照：key=host:port:tls，value=健康信息。
     * 由 {@code GrpcClientProvider} 维护的最新健康状态采样而来；此处仅做只读采样缓存。
     */
    private final Map<String, NodeHealthInfo> nodeHealth = new ConcurrentHashMap<>();

    /** 当前活跃（已建立）的连接数采样值。 */
    private volatile int activeChannels = 0;

    // ==================== 记录方法 ====================

    /** 记录一次远端调用开始（配合 {@link #recordRemoteCallEnd}）。 */
    public void recordRemoteCallBegin() {
        remoteCallCount.increment();
    }

    /**
     * 记录一次远端调用结束。
     *
     * @param success  是否成功（业务或传输成功；failover 成功切换后的成功也算成功）
     * @param elapsedMillis 本次调用耗时（毫秒）
     */
    public void recordRemoteCallEnd(boolean success, long elapsedMillis) {
        if (success) {
            remoteCallSuccessCount.increment();
        }
        remoteCallMillis.add(elapsedMillis);
    }

    /** 记录一次故障转移。 */
    public void recordFailover() {
        failoverCount.increment();
    }

    /** 记录一次熔断触发（方向二）。 */
    public void recordCircuitBreakerTrip() {
        circuitBreakerTrippedCount.increment();
    }

    /** 记录一次 Redis 目录查询成功。 */
    public void recordRegistryLookupSuccess() {
        registryLookupSuccessCount.increment();
    }

    /** 记录一次 Redis 目录查询失败并触发本地兜底。 */
    public void recordRegistryFallbackUsed() {
        registryFallbackUsedCount.increment();
    }

    /** 记录一次兜底缓存命中。 */
    public void recordRegistryFallbackHit() {
        registryFallbackHitCount.increment();
    }

    /** 记录一次兜底缓存未命中。 */
    public void recordRegistryFallbackMiss() {
        registryFallbackMissCount.increment();
    }

    /** 更新某节点的健康快照。 */
    public void updateNodeHealth(String host, int port, boolean tls, boolean healthy, long cooldownMillis) {
        String key = healthKey(host, port, tls);
        nodeHealth.put(key, new NodeHealthInfo(host, port, tls, healthy, cooldownMillis));
    }

    /** 移除某节点的健康快照（节点已下线/成功恢复）。 */
    public void removeNodeHealth(String host, int port, boolean tls) {
        nodeHealth.remove(healthKey(host, port, tls));
    }

    /** 更新当前活跃连接数采样值。 */
    public void updateActiveChannels(int active) {
        this.activeChannels = active;
    }

    // ==================== 只读访问 ====================

    public long remoteCallCount() {
        return remoteCallCount.sum();
    }

    public long remoteCallSuccessCount() {
        return remoteCallSuccessCount.sum();
    }

    public long failoverCount() {
        return failoverCount.sum();
    }

    public long circuitBreakerTrippedCount() {
        return circuitBreakerTrippedCount.sum();
    }

    public long registryLookupSuccessCount() {
        return registryLookupSuccessCount.sum();
    }

    public long registryFallbackUsedCount() {
        return registryFallbackUsedCount.sum();
    }

    public long registryFallbackHitCount() {
        return registryFallbackHitCount.sum();
    }

    public long registryFallbackMissCount() {
        return registryFallbackMissCount.sum();
    }

    public int activeChannels() {
        return activeChannels;
    }

    /** Redis 目录可用率（0..1）；无查询时为 1。 */
    public double registryAvailability() {
        long success = registryLookupSuccessCount.sum();
        long fallback = registryFallbackUsedCount.sum();
        long total = success + fallback;
        return total == 0 ? 1.0 : (double) success / total;
    }

    /** 远端调用平均耗时（毫秒）；无调用时为 0。 */
    public double avgRemoteCallMillis() {
        long calls = remoteCallCount.sum();
        return calls == 0 ? 0.0 : (double) remoteCallMillis.sum() / calls;
    }

    private static String healthKey(String host, int port, boolean tls) {
        return host + ":" + port + ":" + tls;
    }

    /** 当前所有节点的健康快照（host:port 去重、按地址排序）。 */
    public List<NodeHealthInfo> nodeHealthSnapshot() {
        List<NodeHealthInfo> list = new ArrayList<>(nodeHealth.values());
        list.sort(Comparator.comparing(NodeHealthInfo::getAddress));
        return list;
    }

    /**
     * 汇总当前全部指标为一份不可变快照。
     */
    public Snapshot snapshot() {
        return new Snapshot(
                remoteCallCount(), remoteCallSuccessCount(), failoverCount(),
                circuitBreakerTrippedCount(), avgRemoteCallMillis(),
                registryLookupSuccessCount(), registryFallbackUsedCount(),
                registryFallbackHitCount(), registryFallbackMissCount(),
                registryAvailability(), activeChannels(),
                new ArrayList<>(nodeHealthSnapshot()));
    }

    // ==================== 值对象 ====================

    /** 单指标快照（不可变）。 */
    public static class Snapshot {
        public final long remoteCallCount;
        public final long remoteCallSuccessCount;
        public final long failoverCount;
        public final long circuitBreakerTrippedCount;
        public final double avgRemoteCallMillis;
        public final long registryLookupSuccessCount;
        public final long registryFallbackUsedCount;
        public final long registryFallbackHitCount;
        public final long registryFallbackMissCount;
        public final double registryAvailability;
        public final int activeChannels;
        public final List<NodeHealthInfo> nodes;

        public Snapshot(long remoteCallCount, long remoteCallSuccessCount, long failoverCount,
                        long circuitBreakerTrippedCount, double avgRemoteCallMillis,
                        long registryLookupSuccessCount, long registryFallbackUsedCount,
                        long registryFallbackHitCount, long registryFallbackMissCount,
                        double registryAvailability, int activeChannels, List<NodeHealthInfo> nodes) {
            this.remoteCallCount = remoteCallCount;
            this.remoteCallSuccessCount = remoteCallSuccessCount;
            this.failoverCount = failoverCount;
            this.circuitBreakerTrippedCount = circuitBreakerTrippedCount;
            this.avgRemoteCallMillis = avgRemoteCallMillis;
            this.registryLookupSuccessCount = registryLookupSuccessCount;
            this.registryFallbackUsedCount = registryFallbackUsedCount;
            this.registryFallbackHitCount = registryFallbackHitCount;
            this.registryFallbackMissCount = registryFallbackMissCount;
            this.registryAvailability = registryAvailability;
            this.activeChannels = activeChannels;
            this.nodes = nodes;
        }
    }

    /** 节点健康信息（不可变）。 */
    public static class NodeHealthInfo {
        public final String host;
        public final int port;
        public final boolean tls;
        public final boolean healthy;
        public final long cooldownMillis;

        public NodeHealthInfo(String host, int port, boolean tls, boolean healthy, long cooldownMillis) {
            this.host = host;
            this.port = port;
            this.tls = tls;
            this.healthy = healthy;
            this.cooldownMillis = cooldownMillis;
        }

        public String getAddress() {
            return host + ":" + port;
        }
    }
}