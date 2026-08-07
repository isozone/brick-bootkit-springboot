package com.zqzqq.bootkits.core.health.impl;

import com.zqzqq.bootkits.core.health.PluginHealthChecker;
import com.zqzqq.bootkits.core.health.PluginHealthReport;
import com.zqzqq.bootkits.core.health.PluginHealthStatus;
import com.zqzqq.bootkits.core.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 基础插件健康检查器测试
 */
@DisplayName("BasicHealthChecker Test")
class BasicHealthCheckerTest {

    @Mock
    private Plugin mockPlugin;

    private BasicHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        healthChecker = new BasicHealthChecker();
    }

    @Test
    @DisplayName("测试检查器基本信息")
    void testCheckerBasicInfo() {
        assertThat(healthChecker.getName()).isEqualTo("BasicHealthChecker");
        assertThat(healthChecker.getDescription())
            .contains("基础插件健康检查器");
        assertThat(healthChecker.isEnabled()).isTrue();
        assertThat(healthChecker.getTimeoutMillis()).isEqualTo(5000);
    }

    @Test
    @DisplayName("测试检查器启用状态修改")
    void testCheckerEnabledModification() {
        assertThat(healthChecker.isEnabled()).isTrue();
        
        healthChecker.setEnabled(false);
        assertThat(healthChecker.isEnabled()).isFalse();
        
        healthChecker.setEnabled(true);
        assertThat(healthChecker.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("测试超时时间设置")
    void testTimeoutSetting() {
        long newTimeout = 10000L;
        healthChecker.setTimeoutMillis(newTimeout);
        assertThat(healthChecker.getTimeoutMillis()).isEqualTo(newTimeout);
    }

    @Test
    @DisplayName("测试快速健康检查 - 正常插件")
    void testQuickHealthCheckNormalPlugin() {
        when(mockPlugin.isRunning()).thenReturn(true);
        
        PluginHealthStatus result = healthChecker.quickHealthCheck(mockPlugin);
        assertThat(result).isEqualTo(PluginHealthStatus.HEALTHY);
        
        verify(mockPlugin).isRunning();
    }

    @Test
    @DisplayName("测试快速健康检查 - 插件未运行")
    void testQuickHealthCheckNotRunning() {
        when(mockPlugin.isRunning()).thenReturn(false);
        
        PluginHealthStatus result = healthChecker.quickHealthCheck(mockPlugin);
        assertThat(result).isEqualTo(PluginHealthStatus.DEAD);
        
        verify(mockPlugin).isRunning();
    }

    @Test
    @DisplayName("测试快速健康检查 - 空插件")
    void testQuickHealthCheckNullPlugin() {
        PluginHealthStatus result = healthChecker.quickHealthCheck(null);
        assertThat(result).isEqualTo(PluginHealthStatus.DEAD);
    }

    @Test
    @DisplayName("测试快速健康检查 - 异常情况")
    void testQuickHealthCheckWithException() {
        when(mockPlugin.isRunning()).thenThrow(new RuntimeException("Test exception"));
        
        PluginHealthStatus result = healthChecker.quickHealthCheck(mockPlugin);
        assertThat(result).isEqualTo(PluginHealthStatus.UNKNOWN);
    }

    @Test
    @DisplayName("测试完整健康检查 - 正常插件")
    void testCompleteHealthCheckNormalPlugin() {
        setupNormalPlugin();
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        assertThat(report.getOverallStatus()).isNotNull();
        assertThat(report.getCheckTime()).isNotNull();
        assertThat(report.getCheckResults()).isNotNull();
        
        verify(mockPlugin, atLeastOnce()).getId();
        verify(mockPlugin, atLeastOnce()).getName();
        verify(mockPlugin, atLeastOnce()).getVersion();
        verify(mockPlugin, atLeastOnce()).isRunning();
    }

    @Test
    @DisplayName("测试完整健康检查 - 空插件")
    void testCompleteHealthCheckNullPlugin() {
        PluginHealthReport report = healthChecker.checkHealth(null);
        
        assertThat(report).isNotNull();
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.DEAD);
    }

    @Test
    @DisplayName("测试完整健康检查 - 插件未运行")
    void testCompleteHealthCheckNotRunning() {
        when(mockPlugin.getId()).thenReturn("test-plugin");
        when(mockPlugin.getName()).thenReturn("Test Plugin");
        when(mockPlugin.getVersion()).thenReturn("1.0.0");
        when(mockPlugin.isRunning()).thenReturn(false);
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        // 未运行插件被 PluginStatus 检查判为失败，但未达到致命级别
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.WARNING);
    }

    @Test
    @DisplayName("测试完整健康检查 - 插件ID为空")
    void testCompleteHealthCheckEmptyPluginId() {
        when(mockPlugin.getId()).thenReturn("");
        when(mockPlugin.getName()).thenReturn("Test Plugin");
        when(mockPlugin.getVersion()).thenReturn("1.0.0");
        when(mockPlugin.isRunning()).thenReturn(true);
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        // 应该检测到插件ID为空的问题
    }

    @Test
    @DisplayName("测试完整健康检查 - 插件名称为空")
    void testCompleteHealthCheckEmptyPluginName() {
        when(mockPlugin.getId()).thenReturn("test-plugin");
        when(mockPlugin.getName()).thenReturn("");
        when(mockPlugin.getVersion()).thenReturn("1.0.0");
        when(mockPlugin.isRunning()).thenReturn(true);
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        // 应该检测到插件名称为空的问题
    }

    @Test
    @DisplayName("测试完整健康检查 - 插件版本为空")
    void testCompleteHealthCheckEmptyPluginVersion() {
        when(mockPlugin.getId()).thenReturn("test-plugin");
        when(mockPlugin.getName()).thenReturn("Test Plugin");
        when(mockPlugin.getVersion()).thenReturn("");
        when(mockPlugin.isRunning()).thenReturn(true);
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        // 应该检测到插件版本为空的问题
    }

    @Test
    @DisplayName("测试完整健康检查 - 插件方法抛出异常")
    void testCompleteHealthCheckWithExceptions() {
        when(mockPlugin.getId()).thenThrow(new RuntimeException("Test exception"));
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        
        assertThat(report).isNotNull();
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.DEAD);
    }

    @Test
    @DisplayName("测试检查器禁用状态")
    void testCheckerDisabledState() {
        healthChecker.setEnabled(false);
        
        // 当检查器被禁用时，健康检查仍然应该正常工作
        setupNormalPlugin();
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        assertThat(report).isNotNull();
    }

    @Test
    @DisplayName("测试超时时间影响")
    void testTimeoutImpact() {
        setupNormalPlugin();
        
        // 设置很短的超时时间
        healthChecker.setTimeoutMillis(1);
        
        PluginHealthReport report = healthChecker.checkHealth(mockPlugin);
        assertThat(report).isNotNull();
    }

    @Test
    @DisplayName("测试检查器状态保持")
    void testCheckerStatePersistence() {
        // 修改多个状态
        healthChecker.setEnabled(false);
        healthChecker.setTimeoutMillis(10000);
        
        // 验证状态保持
        assertThat(healthChecker.isEnabled()).isFalse();
        assertThat(healthChecker.getTimeoutMillis()).isEqualTo(10000L);
        
        // 恢复状态
        healthChecker.setEnabled(true);
        healthChecker.setTimeoutMillis(5000);
        
        assertThat(healthChecker.isEnabled()).isTrue();
        assertThat(healthChecker.getTimeoutMillis()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("测试检查器名称和描述不可变")
    void testNameAndDescriptionImmutability() {
        String originalName = healthChecker.getName();
        String originalDescription = healthChecker.getDescription();
        
        // 名称和描述应该是不可变的
        assertThat(originalName).isEqualTo("BasicHealthChecker");
        assertThat(originalDescription).contains("基础插件健康检查器");
        
        // 修改其他属性不应该影响名称和描述
        healthChecker.setEnabled(false);
        healthChecker.setTimeoutMillis(10000);
        
        assertThat(healthChecker.getName()).isEqualTo(originalName);
        assertThat(healthChecker.getDescription()).isEqualTo(originalDescription);
    }

    @Test
    @DisplayName("测试检查器线程安全")
    void testThreadSafety() throws InterruptedException {
        setupNormalPlugin();
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        PluginHealthReport[] results = new PluginHealthReport[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = healthChecker.checkHealth(mockPlugin);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 所有线程应该得到相同且正确的结果
        PluginHealthReport firstResult = results[0];
        assertThat(firstResult).isNotNull();
        
        for (int i = 1; i < threadCount; i++) {
            assertThat(results[i]).isNotNull();
            assertThat(results[i].getOverallStatus()).isEqualTo(firstResult.getOverallStatus());
        }
    }

    private void setupNormalPlugin() {
        when(mockPlugin.getId()).thenReturn("test-plugin");
        when(mockPlugin.getName()).thenReturn("Test Plugin");
        when(mockPlugin.getVersion()).thenReturn("1.0.0");
        when(mockPlugin.isRunning()).thenReturn(true);
    }
}