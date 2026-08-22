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

import com.zqzqq.bootkits.core.isolation.PluginResourceMonitor;
import com.zqzqq.bootkits.core.isolation.PluginResourceUsage;
import com.zqzqq.bootkits.core.isolation.ResourceQuota;
import com.zqzqq.bootkits.core.isolation.SystemResourceInfo;
import com.zqzqq.bootkits.core.performance.PerformanceAnalysis;
import com.zqzqq.bootkits.core.performance.PerformanceBaseline;
import com.zqzqq.bootkits.core.performance.PerformanceComparison;
import com.zqzqq.bootkits.core.performance.PerformanceIssue;
import com.zqzqq.bootkits.core.performance.PerformanceSnapshot;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.PerformanceWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 性能分析 Controller 单元测试。
 */
@DisplayName("PerformanceController Test")
class PerformanceControllerTest {

    private PerformanceWebService performanceWebService;
    private PluginWebAuthorizationService authorizationService;
    private PerformanceController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        performanceWebService = mock(PerformanceWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<PerformanceWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(performanceWebService);
        controller = new PerformanceController(provider, authorizationService);
    }

    @Test
    @DisplayName("分析插件性能")
    void analyzeShouldReturnAnalysis() {
        PerformanceAnalysis analysis = new PerformanceAnalysis(
                "plugin-a", 95.0, Collections.emptyList(), Collections.emptyList(),
                java.time.LocalDateTime.now());
        when(performanceWebService.analyzePlugin("plugin-a")).thenReturn(analysis);

        ApiResult<PerformanceAnalysis> result = controller.analyze("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getPerformanceScore()).isEqualTo(95.0);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");
    }

    @Test
    @DisplayName("获取插件资源使用情况")
    void usageShouldReturnData() {
        PluginResourceUsage usage = mock(PluginResourceUsage.class);
        when(performanceWebService.getResourceUsage("plugin-a")).thenReturn(usage);

        ApiResult<PluginResourceUsage> result = controller.usage("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(usage);
    }

    @Test
    @DisplayName("获取所有插件资源使用情况")
    void allUsageShouldReturnMap() {
        when(performanceWebService.getAllResourceUsage()).thenReturn(Collections.emptyMap());

        ApiResult<Map<String, PluginResourceUsage>> result = controller.allUsage();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("获取资源监控摘要")
    void summaryShouldReturnData() {
        PluginResourceMonitor.PluginResourceSummary summary = mock(PluginResourceMonitor.PluginResourceSummary.class);
        when(performanceWebService.getResourceSummary()).thenReturn(summary);

        ApiResult<PluginResourceMonitor.PluginResourceSummary> result = controller.summary();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(summary);
    }

    @Test
    @DisplayName("获取系统资源信息")
    void systemShouldReturnData() {
        SystemResourceInfo info = mock(SystemResourceInfo.class);
        when(performanceWebService.getSystemResourceInfo()).thenReturn(info);

        ApiResult<SystemResourceInfo> result = controller.system();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(info);
    }

    @Test
    @DisplayName("获取插件性能历史")
    void historyShouldReturnList() {
        when(performanceWebService.getPerformanceHistory("plugin-a", 20))
                .thenReturn(Collections.emptyList());

        ApiResult<List<PerformanceSnapshot>> result = controller.history("plugin-a", 20);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("获取所有插件性能评分")
    void scoresShouldReturnMap() {
        when(performanceWebService.getAllPerformanceScores())
                .thenReturn(Collections.singletonMap("plugin-a", 90.0));

        ApiResult<Map<String, Double>> result = controller.scores();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("plugin-a", 90.0);
    }

    @Test
    @DisplayName("获取插件配额")
    void quotaShouldReturnData() {
        ResourceQuota quota = ResourceQuota.defaultQuota();
        when(performanceWebService.getPluginQuota("plugin-a")).thenReturn(quota);

        ApiResult<ResourceQuota> result = controller.quota("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("设置插件配额")
    void setQuotaShouldInvokeService() {
        ResourceQuota quota = ResourceQuota.defaultQuota();

        ApiResult<Void> result = controller.setQuota("plugin-a", quota);

        assertThat(result.isSuccess()).isTrue();
        verify(performanceWebService).setPluginQuota("plugin-a", quota);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_INSTALL, "plugin-a");
    }

    @Test
    @DisplayName("对比插件性能与基线")
    void compareBaselineShouldReturnData() {
        PerformanceComparison comparison = mock(PerformanceComparison.class);
        when(performanceWebService.compareWithBaseline("plugin-a")).thenReturn(comparison);

        ApiResult<PerformanceComparison> result = controller.compareBaseline("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(comparison);
    }

    @Test
    @DisplayName("获取插件性能基线")
    void baselineShouldReturnData() {
        PerformanceBaseline baseline = mock(PerformanceBaseline.class);
        when(performanceWebService.getBaseline("plugin-a")).thenReturn(baseline);

        ApiResult<PerformanceBaseline> result = controller.baseline("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(baseline);
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void analyzeShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");

        assertThatThrownBy(() -> controller.analyze("plugin-a"))
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(performanceWebService);
    }
}
