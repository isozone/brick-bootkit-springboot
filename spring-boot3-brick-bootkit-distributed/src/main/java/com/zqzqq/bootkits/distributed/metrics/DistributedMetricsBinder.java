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

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 把 {@link DistributedMetrics} 桥接到 Micrometer {@link MeterRegistry} 的可选组件。
 * <p>
 * 仅在 classpath 存在 micrometer-core（且已注入任何 {@link MeterRegistry}）时，
 * 由自动配置通过 {@code ObjectProvider<MeterRegistry>} 注册。宿主若引入了
 * {@code micrometer-registry-prometheus} 等 registry，本桥接的指标即可通过
 * actuator {@code /actuator/prometheus} 暴露，供 Prometheus 抓取与告警。
 * <p>
 * 指标命名统一前缀 {@code brick.distributed.*}。
 */
public class DistributedMetricsBinder {

    private final DistributedMetrics metrics;
    private final MeterRegistry meterRegistry;

    public DistributedMetricsBinder(DistributedMetrics metrics, MeterRegistry meterRegistry) {
        this.metrics = metrics;
        this.meterRegistry = meterRegistry;
        // 未提供 registry 时不绑定（保持无观测出口的正常运行），避免注册空指标。
        if (meterRegistry != null) {
            bind();
        }
    }

    private void bind() {
        // 调用
        meterRegistry.gauge("brick.distributed.calls.total", metrics,
                m -> m.remoteCallCount());
        meterRegistry.gauge("brick.distributed.calls.success", metrics,
                m -> m.remoteCallSuccessCount());
        meterRegistry.gauge("brick.distributed.calls.failover", metrics,
                m -> m.failoverCount());
        meterRegistry.gauge("brick.distributed.calls.avg.millis", metrics,
                m -> m.avgRemoteCallMillis());
        meterRegistry.gauge("brick.distributed.calls.error.rate", metrics,
                m -> errorRate(metrics));
        // 熔断
        meterRegistry.gauge("brick.distributed.circuit.tripped", metrics,
                m -> m.circuitBreakerTrippedCount());
        // 目录
        meterRegistry.gauge("brick.distributed.registry.lookup.success", metrics,
                m -> m.registryLookupSuccessCount());
        meterRegistry.gauge("brick.distributed.registry.fallback.used", metrics,
                m -> m.registryFallbackUsedCount());
        meterRegistry.gauge("brick.distributed.registry.fallback.hit", metrics,
                m -> m.registryFallbackHitCount());
        meterRegistry.gauge("brick.distributed.registry.availability", metrics,
                m -> m.registryAvailability());
        // 连接
        meterRegistry.gauge("brick.distributed.channels.active", metrics,
                m -> m.activeChannels());
        // 健康节点数 / 总节点数（实时采样节点健康快照）
        meterRegistry.gauge("brick.distributed.nodes.total", metrics,
                m -> m.nodeHealthSnapshot().size());
        meterRegistry.gauge("brick.distributed.nodes.healthy", metrics,
                m -> m.nodeHealthSnapshot().stream().filter(n -> n.healthy).count());
    }

    private static double errorRate(DistributedMetrics m) {
        long total = m.remoteCallCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) Math.max(0, total - m.remoteCallSuccessCount()) / total;
    }
}