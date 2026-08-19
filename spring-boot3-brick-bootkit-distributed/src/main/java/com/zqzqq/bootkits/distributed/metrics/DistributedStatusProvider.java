package com.zqzqq.bootkits.distributed.metrics;

import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分布式模块运行时状态聚合器（面向管理端点 / actuator / 审计日志）。
 * <p>
 * 汇总 {@link DistributedMetrics} 的指标快照、当前节点健康、目录接口清单等，
 * 输出统一的 {@link DistributedStatus} DTO，供：
 * <ul>
 *   <li>actuator {@code HealthIndicator}（Optional）取 {@code UP/DOWN + 关键指标}；</li>
 *   <li>Micrometer 桥接绑定到 {@code MeterRegistry}（Optional）；</li>
 *   <li>web 管理端点按需展示。</li>
 * </ul>
 * <p>本身不依赖 web/actuator，是纯 POJO + 注入依赖的组件。</p>
 */
public class DistributedStatusProvider {

    private final DistributedMetrics metrics;
    private final GrpcClientProvider clients;
    private final ServiceDirectory directory;
    private final String role;
    private final String nodeId;

    public DistributedStatusProvider(DistributedMetrics metrics,
                                     GrpcClientProvider clients,
                                     ServiceDirectory directory,
                                     String role,
                                     String nodeId) {
        this.metrics = metrics;
        this.clients = clients;
        this.directory = directory;
        this.role = role;
        this.nodeId = nodeId;
    }

    /**
     * 采集当前分布式运行状态。
     */
    public DistributedStatus status() {
        DistributedMetrics.Snapshot s = metrics.snapshot();
        DistributedStatus status = new DistributedStatus();
        status.role = role;
        status.nodeId = nodeId;
        // 指标
        status.remoteCallCount = s.remoteCallCount;
        status.remoteCallSuccessCount = s.remoteCallSuccessCount;
        status.errorRate = errorRate(s.remoteCallCount, s.remoteCallSuccessCount);
        status.failoverCount = s.failoverCount;
        status.circuitBreakerTrippedCount = s.circuitBreakerTrippedCount;
        status.avgRemoteCallMillis = s.avgRemoteCallMillis;
        status.activeChannels = s.activeChannels;
        // 目录
        status.registryLookupSuccessCount = s.registryLookupSuccessCount;
        status.registryFallbackUsedCount = s.registryFallbackUsedCount;
        status.registryFallbackHitCount = s.registryFallbackHitCount;
        status.registryFallbackMissCount = s.registryFallbackMissCount;
        status.registryAvailability = s.registryAvailability;
        status.serviceInterfaces = directory.allServiceInterfaces();
        // 节点健康
        status.nodes = new LinkedHashMap<>();
        for (DistributedMetrics.NodeHealthInfo node : s.nodes) {
            status.nodes.put(node.getAddress(), node.healthy);
        }
        return status;
    }

    private static double errorRate(long total, long success) {
        if (total == 0) {
            return 0.0;
        }
        return (double) Math.max(0, total - success) / total;
    }

    /** 判断分布式模块当前是否「健康」：无持续故障转移、目录可用。 */
    public boolean isHealthy() {
        DistributedStatus s = status();
        // failover 激增（>50）或 Redis 目录不可用（可用率 < 0.5）视为不健康；
        // 阈值仅作工程近似，实际业务可通过告警阈值自行评估。
        if (s.failoverCount > 50) {
            return false;
        }
        if (s.registryAvailability < 0.5) {
            return false;
        }
        return true;
    }

    // ==================== DTO ====================

    /** 分布式模块运行时状态（一次性快照 DTO）。 */
    public static class DistributedStatus {
        public String role;
        public String nodeId;
        public long remoteCallCount;
        public long remoteCallSuccessCount;
        public double errorRate;
        public long failoverCount;
        public long circuitBreakerTrippedCount;
        public double avgRemoteCallMillis;
        public int activeChannels;
        public long registryLookupSuccessCount;
        public long registryFallbackUsedCount;
        public long registryFallbackHitCount;
        public long registryFallbackMissCount;
        public double registryAvailability;
        public java.util.Set<String> serviceInterfaces;
        public Map<String, Boolean> nodes;

        public String getRole() {
            return role;
        }

        public String getNodeId() {
            return nodeId;
        }

        public long getRemoteCallCount() {
            return remoteCallCount;
        }

        public long getRemoteCallSuccessCount() {
            return remoteCallSuccessCount;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public long getFailoverCount() {
            return failoverCount;
        }

        public long getCircuitBreakerTrippedCount() {
            return circuitBreakerTrippedCount;
        }

        public double getAvgRemoteCallMillis() {
            return avgRemoteCallMillis;
        }

        public int getActiveChannels() {
            return activeChannels;
        }

        public long getRegistryLookupSuccessCount() {
            return registryLookupSuccessCount;
        }

        public long getRegistryFallbackUsedCount() {
            return registryFallbackUsedCount;
        }

        public long getRegistryFallbackHitCount() {
            return registryFallbackHitCount;
        }

        public long getRegistryFallbackMissCount() {
            return registryFallbackMissCount;
        }

        public double getRegistryAvailability() {
            return registryAvailability;
        }

        public java.util.Set<String> getServiceInterfaces() {
            return serviceInterfaces;
        }

        public Map<String, Boolean> getNodes() {
            return nodes;
        }
    }
}