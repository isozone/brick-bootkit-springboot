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


package com.zqzqq.bootkits.core.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件健康报告测试
 */
@DisplayName("PluginHealthReport Test")
class PluginHealthReportTest {

    @Test
    @DisplayName("测试创建健康报告")
    void testCreateHealthyReport() {
        PluginHealthReport report = PluginHealthReport.createHealthy("test-plugin", Arrays.asList(
            PluginHealthReport.HealthCheckResult.success("connection", "连接正常", 100L),
            PluginHealthReport.HealthCheckResult.success("memory", "内存使用正常", 50L)
        ));
        
        assertThat(report.getPluginId()).isEqualTo("test-plugin");
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.HEALTHY);
        assertThat(report.getMessage()).isEqualTo("插件运行正常");
        assertThat(report.hasError()).isFalse();
        assertThat(report.getCheckResults()).hasSize(2);
    }

    @Test
    @DisplayName("测试创建警告报告")
    void testCreateWarningReport() {
        PluginHealthReport report = PluginHealthReport.createWarning("test-plugin", "警告：资源使用率较高", Arrays.asList(
            PluginHealthReport.HealthCheckResult.failure("memory", "内存使用率过高", new RuntimeException("Out of memory"))
        ));
        
        assertThat(report.getPluginId()).isEqualTo("test-plugin");
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.WARNING);
        assertThat(report.getMessage()).isEqualTo("警告：资源使用率较高");
        assertThat(report.hasError()).isFalse();
    }

    @Test
    @DisplayName("测试创建严重报告")
    void testCreateCriticalReport() {
        PluginHealthReport report = PluginHealthReport.createCritical("test-plugin", "严重错误：插件无响应", Arrays.asList(
            PluginHealthReport.HealthCheckResult.failure("response", "插件无响应", new RuntimeException("Timeout"))
        ));
        
        assertThat(report.getPluginId()).isEqualTo("test-plugin");
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.CRITICAL);
        assertThat(report.getMessage()).isEqualTo("严重错误：插件无响应");
    }

    @Test
    @DisplayName("测试创建死亡报告")
    void testCreateDeadReport() {
        Exception error = new RuntimeException("Plugin crashed");
        PluginHealthReport report = PluginHealthReport.createDead("test-plugin", "插件已死亡", error);
        
        assertThat(report.getPluginId()).isEqualTo("test-plugin");
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.DEAD);
        assertThat(report.getMessage()).isEqualTo("插件已死亡");
        assertThat(report.hasError()).isTrue();
        assertThat(report.getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("测试创建未知状态报告")
    void testCreateUnknownReport() {
        PluginHealthReport report = PluginHealthReport.createUnknown("test-plugin", "状态未知", new IllegalStateException("Unknown state"));
        
        assertThat(report.getPluginId()).isEqualTo("test-plugin");
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.UNKNOWN);
        assertThat(report.getMessage()).isEqualTo("状态未知");
        assertThat(report.hasError()).isTrue();
    }

    @Test
    @DisplayName("测试检查器状态方法")
    void testCheckerStatusMethods() {
        PluginHealthReport healthyReport = PluginHealthReport.createHealthy("test", Arrays.asList());
        assertThat(healthyReport.needsAutoRecovery()).isFalse();
        assertThat(healthyReport.needsManualIntervention()).isFalse();
        
        PluginHealthReport deadReport = PluginHealthReport.createDead("test", "死亡", new RuntimeException());
        assertThat(deadReport.needsAutoRecovery()).isTrue();
        assertThat(deadReport.needsManualIntervention()).isTrue();
    }

    @Test
    @DisplayName("测试严重程度级别")
    void testSeverityLevel() {
        PluginHealthReport healthyReport = PluginHealthReport.createHealthy("test", Arrays.asList());
        PluginHealthReport warningReport = PluginHealthReport.createWarning("test", "警告", Arrays.asList());
        PluginHealthReport criticalReport = PluginHealthReport.createCritical("test", "严重", Arrays.asList());
        PluginHealthReport deadReport = PluginHealthReport.createDead("test", "死亡", new RuntimeException());
        PluginHealthReport unknownReport = PluginHealthReport.createUnknown("test", "未知", new RuntimeException());
        
        assertThat(healthyReport.getSeverityLevel()).isEqualTo(0);
        assertThat(warningReport.getSeverityLevel()).isEqualTo(1);
        assertThat(criticalReport.getSeverityLevel()).isEqualTo(2);
        assertThat(deadReport.getSeverityLevel()).isEqualTo(3);
        assertThat(unknownReport.getSeverityLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("测试检查结果")
    void testHealthCheckResults() {
        PluginHealthReport.HealthCheckResult successResult = PluginHealthReport.HealthCheckResult.success("test-check", "检查通过", 100L);
        PluginHealthReport.HealthCheckResult failureResult = PluginHealthReport.HealthCheckResult.failure("test-check", "检查失败", new RuntimeException("Error"));
        
        assertThat(successResult.getCheckName()).isEqualTo("test-check");
        assertThat(successResult.isPassed()).isTrue();
        assertThat(successResult.getMessage()).isEqualTo("检查通过");
        assertThat(successResult.getExecutionTime()).isEqualTo(100L);
        assertThat(successResult.hasError()).isFalse();
        
        assertThat(failureResult.isPassed()).isFalse();
        assertThat(failureResult.getMessage()).isEqualTo("检查失败");
        assertThat(failureResult.hasError()).isTrue();
    }

    @Test
    @DisplayName("测试报告字符串表示")
    void testReportToString() {
        PluginHealthReport report = PluginHealthReport.createHealthy("test-plugin", Arrays.asList());
        String str = report.toString();
        
        assertThat(str).contains("test-plugin");
        assertThat(str).contains("healthy");
    }

    @Test
    @DisplayName("测试检查结果字符串表示")
    void testHealthCheckResultToString() {
        PluginHealthReport.HealthCheckResult result = PluginHealthReport.HealthCheckResult.success("test", "成功", 100L);
        String str = result.toString();
        
        assertThat(str).contains("test");
        assertThat(str).contains("成功");
    }

    @Test
    @DisplayName("测试获取插件ID")
    void testGetPluginId() {
        PluginHealthReport report = PluginHealthReport.createHealthy("my-plugin", Arrays.asList());
        assertThat(report.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    @DisplayName("测试获取整体状态")
    void testGetOverallStatus() {
        PluginHealthReport report = PluginHealthReport.createWarning("test", "警告", Arrays.asList());
        assertThat(report.getOverallStatus()).isEqualTo(PluginHealthStatus.WARNING);
    }

    @Test
    @DisplayName("测试获取检查时间")
    void testGetCheckTime() {
        PluginHealthReport report = PluginHealthReport.createHealthy("test", Arrays.asList());
        assertThat(report.getCheckTime()).isNotNull();
        assertThat(report.getCheckTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("测试获取响应时间")
    void testGetResponseTime() {
        PluginHealthReport report = PluginHealthReport.createHealthy("test", Arrays.asList());
        assertThat(report.getResponseTime()).isEqualTo(0L);
    }

    @Test
    @DisplayName("测试获取检查结果列表")
    void testGetCheckResults() {
        List<PluginHealthReport.HealthCheckResult> results = Arrays.asList(
            PluginHealthReport.HealthCheckResult.success("check1", "成功", 100L)
        );
        PluginHealthReport report = PluginHealthReport.createHealthy("test", results);
        
        assertThat(report.getCheckResults()).isEqualTo(results);
        assertThat(report.getCheckResults()).hasSize(1);
    }

    @Test
    @DisplayName("测试获取消息")
    void testGetMessage() {
        PluginHealthReport report = PluginHealthReport.createWarning("test", "自定义消息", Arrays.asList());
        assertThat(report.getMessage()).isEqualTo("自定义消息");
    }

    @Test
    @DisplayName("测试获取错误信息")
    void testGetError() {
        Exception error = new RuntimeException("Test error");
        PluginHealthReport report = PluginHealthReport.createDead("test", "死亡", error);
        
        assertThat(report.getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("测试获取检查结果名称")
    void testGetCheckResultName() {
        PluginHealthReport.HealthCheckResult result = PluginHealthReport.HealthCheckResult.success("my-check", "成功", 100L);
        assertThat(result.getCheckName()).isEqualTo("my-check");
    }

    @Test
    @DisplayName("测试获取检查结果消息")
    void testGetCheckResultMessage() {
        PluginHealthReport.HealthCheckResult result = PluginHealthReport.HealthCheckResult.success("check", "检查成功", 200L);
        assertThat(result.getMessage()).isEqualTo("检查成功");
    }

    @Test
    @DisplayName("测试获取检查结果执行时间")
    void testGetCheckResultExecutionTime() {
        PluginHealthReport.HealthCheckResult result = PluginHealthReport.HealthCheckResult.failure("check", "失败", new RuntimeException());
        assertThat(result.getExecutionTime()).isEqualTo(0L);
    }

    @Test
    @DisplayName("测试获取检查结果错误")
    void testGetCheckResultError() {
        Exception error = new RuntimeException("Check failed");
        PluginHealthReport.HealthCheckResult result = PluginHealthReport.HealthCheckResult.failure("check", "失败", error);
        
        assertThat(result.getError()).isEqualTo(error);
    }
}