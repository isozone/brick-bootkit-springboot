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

import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DistributedMetrics} / {@link DistributedStatusProvider} 的单元测试：
 * 验证指标累加、节点健康跟踪、目录兜底、快照聚合与健康判定。
 */
class DistributedMetricsTest {

    @Test
    void remoteCallCountersAndAvg() {
        DistributedMetrics m = new DistributedMetrics();
        m.recordRemoteCallBegin();
        m.recordRemoteCallEnd(true, 100);
        m.recordRemoteCallBegin();
        m.recordRemoteCallEnd(false, 200);

        assertThat(m.remoteCallCount()).isEqualTo(2);
        assertThat(m.remoteCallSuccessCount()).isEqualTo(1);
        assertThat(m.avgRemoteCallMillis()).isEqualTo(150.0);
        DistributedMetrics.Snapshot s = m.snapshot();
        assertThat(s.remoteCallCount).isEqualTo(2);
        assertThat(s.remoteCallSuccessCount).isEqualTo(1);
    }

    @Test
    void nodeHealthTrackedAndRemoved() {
        DistributedMetrics m = new DistributedMetrics();
        m.updateNodeHealth("10.0.0.1", 9090, false, false, 4000);
        List<DistributedMetrics.NodeHealthInfo> nodes = m.nodeHealthSnapshot();
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getAddress()).isEqualTo("10.0.0.1:9090");
        assertThat(nodes.get(0).healthy).isFalse();

        m.updateNodeHealth("10.0.0.1", 9090, false, true, 0);
        assertThat(m.nodeHealthSnapshot().get(0).healthy).isTrue();

        m.removeNodeHealth("10.0.0.1", 9090, false);
        assertThat(m.nodeHealthSnapshot()).isEmpty();
    }

    @Test
    void fallbackCountersAndAvailability() {
        DistributedMetrics m = new DistributedMetrics();
        m.recordRegistryLookupSuccess();
        m.recordRegistryLookupSuccess();
        m.recordRegistryFallbackUsed();
        m.recordRegistryFallbackHit();
        m.recordRegistryFallbackMiss();

        assertThat(m.registryLookupSuccessCount()).isEqualTo(2);
        assertThat(m.registryFallbackUsedCount()).isEqualTo(1);
        assertThat(m.registryFallbackHitCount()).isEqualTo(1);
        assertThat(m.registryFallbackMissCount()).isEqualTo(1);
        // 可用率 = success / (success + fallback) = 2/3
        assertThat(m.registryAvailability()).isEqualTo(2.0 / 3.0);
    }

    @Test
    void statusProviderAggregatesAndDecidesHealth() {
        DistributedMetrics m = new DistributedMetrics();
        m.recordRemoteCallBegin();
        m.recordRemoteCallEnd(true, 50);
        m.updateNodeHealth("10.0.0.2", 9091, false, true, 0);

        GrpcClientProvider clients = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        ServiceDirectory dir = new ServiceDirectory() {
            @Override
            public void register(com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration r) {
            }

            @Override
            public void registerAll(List<com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration> list) {
            }

            @Override
            public void heartbeat(String s, String s2, String s3) {
            }

            @Override
            public List<com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration> lookup(String s) {
                return Collections.emptyList();
            }

            @Override
            public com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration lookup(String s, String s2) {
                return null;
            }

            @Override
            public void unregister(String s, String s2, String s3) {
            }

            @Override
            public void unregisterAllByNode(String s) {
            }

            @Override
            public Set<String> allServiceInterfaces() {
                return Collections.singleton("com.example.UserService");
            }
        };

        DistributedStatusProvider provider =
                new DistributedStatusProvider(m, clients, dir, "HOST", "node-1");
        DistributedStatusProvider.DistributedStatus s = provider.status();
        assertThat(s.role).isEqualTo("HOST");
        assertThat(s.nodeId).isEqualTo("node-1");
        assertThat(s.remoteCallCount).isEqualTo(1L);
        assertThat(s.nodes).containsKey("10.0.0.2:9091");
        assertThat(s.nodes.get("10.0.0.2:9091")).isTrue();
        assertThat(s.serviceInterfaces).contains("com.example.UserService");
        assertThat(provider.isHealthy()).isTrue();
    }

    // Snapshot 暴露 errorRate 需要的补充校验
    @Test
    void snapshotErrorRate() {
        DistributedMetrics m = new DistributedMetrics();
        m.recordRemoteCallBegin();
        m.recordRemoteCallEnd(false, 10);
        // 通过 status provider 计算 errorRate
        GrpcClientProvider clients = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        ServiceDirectory dir = new ServiceDirectory() {
            @Override
            public void register(com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration r) {
            }

            @Override
            public void registerAll(List<com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration> list) {
            }

            @Override
            public void heartbeat(String s, String s2, String s3) {
            }

            @Override
            public List<com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration> lookup(String s) {
                return Collections.emptyList();
            }

            @Override
            public com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration lookup(String s, String s2) {
                return null;
            }

            @Override
            public void unregister(String s, String s2, String s3) {
            }

            @Override
            public void unregisterAllByNode(String s) {
            }

            @Override
            public Set<String> allServiceInterfaces() {
                return Collections.emptySet();
            }
        };
        DistributedStatusProvider provider = new DistributedStatusProvider(m, clients, dir, "HOST", "n");
        assertThat(provider.status().errorRate).isEqualTo(1.0);
    }
}