package com.zqzqq.bootkits.distributed.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分布式模块的 actuator 健康探针（可选）。
 * <p>
 * 仅在 classpath 存在 <code>spring-boot-actuator</code>（宿主已引入 actuator）时启用，
 * 由自动配置通过 {@code @ConditionalOnClass(HealthIndicator.class)} 注册。
 * 探针输出为 {@code brick.distributed} 组件，携带关键运行指标，供
 * {@code /actuator/health} 汇总与外部探活。
 */
public class DistributedHealthIndicator implements HealthIndicator {

    private final DistributedStatusProvider statusProvider;

    public DistributedHealthIndicator(DistributedStatusProvider statusProvider) {
        this.statusProvider = statusProvider;
    }

    @Override
    public Health health() {
        DistributedStatusProvider.DistributedStatus s = statusProvider.status();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", s.role);
        details.put("nodeId", s.nodeId);
        details.put("remoteCalls", s.remoteCallCount);
        details.put("failoverCount", s.failoverCount);
        details.put("avgCallMillis", s.avgRemoteCallMillis);
        details.put("activeChannels", s.activeChannels);
        details.put("registryAvailability", s.registryAvailability);
        details.put("registeredServices", s.serviceInterfaces);

        boolean healthy = statusProvider.isHealthy();
        Health.Builder builder = healthy
                ? Health.up()
                : Health.down();
        return builder.withDetails(details).build();
    }
}