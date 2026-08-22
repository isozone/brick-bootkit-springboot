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


package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.distributed.metrics.DistributedStatusProvider;
import com.zqzqq.bootkits.distributed.metrics.DistributedStatusProvider.DistributedStatus;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.MonitorWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MonitorController#distributed()} 端点单元测试：
 * 覆盖「分布式模块未启用 → disabled」「已启用 → 实时状态聚合」两条核心路径。
 *
 * @since 4.0.9 分布式可观测性（方向一）
 */
@DisplayName("MonitorController#distributed Test")
class MonitorControllerDistributedTest {

    private MonitorWebService monitorWebService;
    private PluginWebAuthorizationService authorizationService;
    private ObjectProvider<MonitorWebService> monitorWebServiceProvider;
    private ObjectProvider<DistributedStatusProvider> statusProviderProvider;
    private MonitorController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        monitorWebService = mock(MonitorWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        monitorWebServiceProvider = mock(ObjectProvider.class);
        statusProviderProvider = mock(ObjectProvider.class);
        doNothing().when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
        controller = new MonitorController(
                monitorWebServiceProvider, authorizationService, statusProviderProvider);
    }

    @Test
    @DisplayName("分布式模块未启用时返回 disabled=true（不打 500，便于运维区分模块缺失）")
    void distributedEndpointShouldReturnDisabledWhenModuleAbsent() {
        when(statusProviderProvider.getIfAvailable()).thenReturn(null);

        ApiResult<Map<String, Object>> result = controller.distributed();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("disabled", true);
        assertThat(result.getData()).containsKey("message");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("分布式模块启用时聚合 DistributedStatus 并返回所有关键指标")
    void distributedEndpointShouldAggregateRuntimeStatusWhenModuleEnabled() {
        DistributedStatusProvider provider = mock(DistributedStatusProvider.class);
        when(provider.isHealthy()).thenReturn(true);
        DistributedStatus status = sampleStatus();
        when(provider.status()).thenReturn(status);
        when(statusProviderProvider.getIfAvailable()).thenReturn(provider);

        ApiResult<Map<String, Object>> result = controller.distributed();

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = result.getData();
        assertThat(data).containsEntry("disabled", false);
        assertThat(data).containsEntry("healthy", true);
        assertThat(data).containsEntry("role", "WORKER");
        assertThat(data).containsEntry("nodeId", "10.0.0.1:9090");
        assertThat(data).containsEntry("remoteCalls", 120L);
        assertThat(data).containsEntry("remoteCallSuccess", 118L);
        assertThat(data).containsEntry("errorRate", 0.016666666666666666);
        assertThat(data).containsEntry("failoverCount", 3L);
        assertThat(data).containsEntry("circuitBreakerTripped", 0L);
        assertThat(data).containsEntry("avgCallMillis", 42.5);
        assertThat(data).containsEntry("activeChannels", 2);
        assertThat(data).containsEntry("registryLookupSuccess", 80L);
        assertThat(data).containsEntry("registryFallbackUsed", 1L);
        assertThat(data).containsEntry("registryFallbackHit", 1L);
        assertThat(data).containsEntry("registryFallbackMiss", 0L);
        assertThat(data).containsEntry("registryAvailability", 0.9876543209876543);
        assertThat(data).containsKey("registeredServiceInterfaces");
        assertThat(data).containsKey("nodes");
        verify(provider).status();
    }

    /**
     * 构造一个覆盖所有指标的样本 {@link DistributedStatus}：
     * 80 次 lookup 成功 + 1 次 fallback → 可用率 80/81。
     */
    private static DistributedStatus sampleStatus() {
        DistributedStatus s = new DistributedStatus();
        s.role = "WORKER";
        s.nodeId = "10.0.0.1:9090";
        s.remoteCallCount = 120;
        s.remoteCallSuccessCount = 118;
        s.errorRate = (120 - 118) / 120.0;
        s.failoverCount = 3;
        s.circuitBreakerTrippedCount = 0;
        s.avgRemoteCallMillis = 42.5;
        s.activeChannels = 2;
        s.registryLookupSuccessCount = 80;
        s.registryFallbackUsedCount = 1;
        s.registryFallbackHitCount = 1;
        s.registryFallbackMissCount = 0;
        s.registryAvailability = 80.0 / (80 + 1);
        s.serviceInterfaces = java.util.Collections.singleton("com.example.UserService");
        Map<String, Boolean> nodes = new LinkedHashMap<>();
        nodes.put("10.0.0.1:9090", true);
        nodes.put("10.0.0.2:9090", false);
        s.nodes = nodes;
        return s;
    }
}
