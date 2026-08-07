package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.isolation.PluginResourceIsolation;
import com.zqzqq.bootkits.core.isolation.PluginResourceMonitor;
import com.zqzqq.bootkits.core.isolation.PluginResourceUsage;
import com.zqzqq.bootkits.core.isolation.QuotaManager;
import com.zqzqq.bootkits.core.isolation.ResourceQuota;
import com.zqzqq.bootkits.core.isolation.SystemResourceInfo;
import com.zqzqq.bootkits.core.performance.PerformanceAnalysis;
import com.zqzqq.bootkits.core.performance.PerformanceBaseline;
import com.zqzqq.bootkits.core.performance.PerformanceComparison;
import com.zqzqq.bootkits.core.performance.PerformanceSnapshot;
import com.zqzqq.bootkits.core.performance.PluginPerformanceAnalyzer;
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
 * 性能分析 Web 服务单元测试。
 */
@DisplayName("PerformanceWebService Test")
class PerformanceWebServiceTest {

    private PluginPerformanceAnalyzer analyzer;
    private PluginResourceMonitor monitor;
    private PluginResourceIsolation isolation;
    private QuotaManager quotaManager;
    private PerformanceWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        analyzer = mock(PluginPerformanceAnalyzer.class);
        monitor = mock(PluginResourceMonitor.class);
        isolation = mock(PluginResourceIsolation.class);
        quotaManager = mock(QuotaManager.class);

        ObjectProvider<PluginPerformanceAnalyzer> aProvider = mock(ObjectProvider.class);
        when(aProvider.getIfAvailable()).thenReturn(analyzer);
        ObjectProvider<PluginResourceMonitor> mProvider = mock(ObjectProvider.class);
        when(mProvider.getIfAvailable()).thenReturn(monitor);
        ObjectProvider<PluginResourceIsolation> iProvider = mock(ObjectProvider.class);
        when(iProvider.getIfAvailable()).thenReturn(isolation);
        ObjectProvider<QuotaManager> qProvider = mock(ObjectProvider.class);
        when(qProvider.getIfAvailable()).thenReturn(quotaManager);

        service = new PerformanceWebService(aProvider, mProvider, iProvider, qProvider);
    }

    @Test
    @DisplayName("分析插件性能")
    void analyzePluginShouldReturnAnalysis() {
        PluginResourceUsage usage = mock(PluginResourceUsage.class);
        when(monitor.getPluginResourceUsage("plugin-a")).thenReturn(usage);
        PerformanceAnalysis analysis = new PerformanceAnalysis(
                "plugin-a", 90.0, Collections.emptyList(), Collections.emptyList(),
                java.time.LocalDateTime.now());
        when(analyzer.analyzePlugin("plugin-a", usage)).thenReturn(analysis);

        PerformanceAnalysis actual = service.analyzePlugin("plugin-a");

        assertThat(actual.getPerformanceScore()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("插件资源数据缺失时抛出异常")
    void analyzePluginShouldFailWhenUsageMissing() {
        when(monitor.getPluginResourceUsage("plugin-a")).thenReturn(null);

        assertThatThrownBy(() -> service.analyzePlugin("plugin-a"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("获取插件资源使用情况")
    void getResourceUsageShouldReturnData() {
        PluginResourceUsage usage = mock(PluginResourceUsage.class);
        when(monitor.getPluginResourceUsage("plugin-a")).thenReturn(usage);

        PluginResourceUsage actual = service.getResourceUsage("plugin-a");

        assertThat(actual).isSameAs(usage);
    }

    @Test
    @DisplayName("获取所有插件资源使用情况")
    void getAllResourceUsageShouldReturnMap() {
        when(monitor.getAllPluginResourceUsage()).thenReturn(Collections.emptyMap());

        Map<String, PluginResourceUsage> actual = service.getAllResourceUsage();

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("获取资源监控摘要")
    void getResourceSummaryShouldReturnData() {
        PluginResourceMonitor.PluginResourceSummary summary = mock(PluginResourceMonitor.PluginResourceSummary.class);
        when(monitor.getResourceSummary()).thenReturn(summary);

        PluginResourceMonitor.PluginResourceSummary actual = service.getResourceSummary();

        assertThat(actual).isSameAs(summary);
    }

    @Test
    @DisplayName("获取系统资源信息")
    void getSystemResourceInfoShouldReturnData() {
        SystemResourceInfo info = mock(SystemResourceInfo.class);
        when(isolation.getSystemResourceInfo()).thenReturn(info);

        SystemResourceInfo actual = service.getSystemResourceInfo();

        assertThat(actual).isSameAs(info);
    }

    @Test
    @DisplayName("获取插件性能历史")
    void getPerformanceHistoryShouldReturnList() {
        when(analyzer.getPerformanceHistory("plugin-a", 20)).thenReturn(Collections.emptyList());

        List<PerformanceSnapshot> actual = service.getPerformanceHistory("plugin-a", 20);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("获取所有插件性能评分")
    void getAllPerformanceScoresShouldReturnMap() {
        when(analyzer.getAllPluginPerformanceScores())
                .thenReturn(Collections.singletonMap("plugin-a", 95.0));

        Map<String, Double> actual = service.getAllPerformanceScores();

        assertThat(actual).containsEntry("plugin-a", 95.0);
    }

    @Test
    @DisplayName("获取插件配额")
    void getPluginQuotaShouldReturnQuota() {
        ResourceQuota quota = ResourceQuota.defaultQuota();
        when(quotaManager.getPluginQuota("plugin-a")).thenReturn(quota);

        ResourceQuota actual = service.getPluginQuota("plugin-a");

        assertThat(actual).isNotNull();
    }

    @Test
    @DisplayName("设置插件配额")
    void setPluginQuotaShouldSyncIsolation() {
        ResourceQuota quota = ResourceQuota.defaultQuota();

        service.setPluginQuota("plugin-a", quota);

        verify(quotaManager).setPluginQuota("plugin-a", quota);
        verify(isolation).setPluginQuota("plugin-a", quota);
    }

    @Test
    @DisplayName("配额为空时抛出异常")
    void setPluginQuotaShouldFailOnNull() {
        assertThatThrownBy(() -> service.setPluginQuota("plugin-a", null))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("对比插件性能与基线")
    void compareWithBaselineShouldReturnData() {
        PluginResourceUsage usage = mock(PluginResourceUsage.class);
        when(monitor.getPluginResourceUsage("plugin-a")).thenReturn(usage);
        PerformanceAnalysis analysis = new PerformanceAnalysis(
                "plugin-a", 90.0, Collections.emptyList(), Collections.emptyList(),
                java.time.LocalDateTime.now());
        when(analyzer.analyzePlugin("plugin-a", usage)).thenReturn(analysis);
        PerformanceComparison comparison = mock(PerformanceComparison.class);
        when(analyzer.compareWithBaseline("plugin-a", analysis)).thenReturn(comparison);

        PerformanceComparison actual = service.compareWithBaseline("plugin-a");

        assertThat(actual).isSameAs(comparison);
    }

    @Test
    @DisplayName("获取插件性能基线")
    void getBaselineShouldReturnData() {
        PerformanceBaseline baseline = mock(PerformanceBaseline.class);
        when(analyzer.getPerformanceBaseline("plugin-a")).thenReturn(baseline);

        PerformanceBaseline actual = service.getBaseline("plugin-a");

        assertThat(actual).isSameAs(baseline);
    }
}
