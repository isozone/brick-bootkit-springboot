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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件健康检查报告
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class PluginHealthReport {
    
    private final String pluginId;
    private final PluginHealthStatus overallStatus;
    private final LocalDateTime checkTime;
    private final long responseTime;
    private final List<HealthCheckResult> checkResults;
    private final String message;
    private final Exception error;

    public PluginHealthReport(String pluginId, PluginHealthStatus overallStatus, 
                            LocalDateTime checkTime, long responseTime,
                            List<HealthCheckResult> checkResults, String message, Exception error) {
        this.pluginId = pluginId;
        this.overallStatus = overallStatus;
        this.checkTime = checkTime;
        this.responseTime = responseTime;
        this.checkResults = checkResults != null ? checkResults : new ArrayList<>();
        this.message = message;
        this.error = error;
    }

    /**
     * 创建成功报告
     */
    public static PluginHealthReport createHealthy(String pluginId, List<HealthCheckResult> checkResults) {
        return new PluginHealthReport(pluginId, PluginHealthStatus.HEALTHY, 
                                    LocalDateTime.now(), 0, checkResults, 
                                    "插件运行正常", null);
    }

    /**
     * 创建警告报告
     */
    public static PluginHealthReport createWarning(String pluginId, String message, List<HealthCheckResult> checkResults) {
        return new PluginHealthReport(pluginId, PluginHealthStatus.WARNING, 
                                    LocalDateTime.now(), 0, checkResults, 
                                    message, null);
    }

    /**
     * 创建危险报告
     */
    public static PluginHealthReport createCritical(String pluginId, String message, List<HealthCheckResult> checkResults) {
        return new PluginHealthReport(pluginId, PluginHealthStatus.CRITICAL, 
                                    LocalDateTime.now(), 0, checkResults, 
                                    message, null);
    }

    /**
     * 创建死亡报告
     */
    public static PluginHealthReport createDead(String pluginId, String message, Exception error) {
        return new PluginHealthReport(pluginId, PluginHealthStatus.DEAD, 
                                    LocalDateTime.now(), 0, new ArrayList<>(), 
                                    message, error);
    }

    /**
     * 创建未知状态报告
     */
    public static PluginHealthReport createUnknown(String pluginId, String message, Exception error) {
        return new PluginHealthReport(pluginId, PluginHealthStatus.UNKNOWN, 
                                    LocalDateTime.now(), 0, new ArrayList<>(), 
                                    message, error);
    }

    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取整体健康状态
     */
    public PluginHealthStatus getOverallStatus() {
        return overallStatus;
    }

    /**
     * 获取检查时间
     */
    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    /**
     * 获取响应时间（毫秒）
     */
    public long getResponseTime() {
        return responseTime;
    }

    /**
     * 获取检查结果列表
     */
    public List<HealthCheckResult> getCheckResults() {
        return checkResults;
    }

    /**
     * 获取消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取错误信息
     */
    public Exception getError() {
        return error;
    }

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * 检查是否需要自动恢复
     */
    public boolean needsAutoRecovery() {
        return overallStatus.needsAutoRecovery();
    }

    /**
     * 检查是否需要人工干预
     */
    public boolean needsManualIntervention() {
        return overallStatus.needsManualIntervention();
    }

    /**
     * 获取严重程度级别
     */
    public int getSeverityLevel() {
        return overallStatus.getSeverityLevel();
    }

    @Override
    public String toString() {
        return "PluginHealthReport{" +
                "pluginId='" + pluginId + '\'' +
                ", overallStatus=" + overallStatus +
                ", checkTime=" + checkTime +
                ", responseTime=" + responseTime + "ms" +
                ", checkResults=" + checkResults.size() +
                ", message='" + message + '\'' +
                ", error=" + (error != null ? error.getMessage() : "none") +
                '}';
    }

    /**
     * 健康检查单个结果
     */
    public static class HealthCheckResult {
        private final String checkName;
        private final boolean passed;
        private final String message;
        private final long executionTime;
        private final Exception error;

        public HealthCheckResult(String checkName, boolean passed, String message, 
                               long executionTime, Exception error) {
            this.checkName = checkName;
            this.passed = passed;
            this.message = message;
            this.executionTime = executionTime;
            this.error = error;
        }

        public static HealthCheckResult success(String checkName, String message, long executionTime) {
            return new HealthCheckResult(checkName, true, message, executionTime, null);
        }

        public static HealthCheckResult failure(String checkName, String message, Exception error) {
            return new HealthCheckResult(checkName, false, message, 0, error);
        }

        public static HealthCheckResult failure(String checkName, String message) {
            return new HealthCheckResult(checkName, false, message, 0, null);
        }

        public String getCheckName() {
            return checkName;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public Exception getError() {
            return error;
        }

        public boolean hasError() {
            return error != null;
        }

        @Override
        public String toString() {
            return "HealthCheckResult{" +
                    "checkName='" + checkName + '\'' +
                    ", passed=" + passed +
                    ", message='" + message + '\'' +
                    ", executionTime=" + executionTime + "ms" +
                    ", error=" + (error != null ? error.getMessage() : "none") +
                    '}';
        }
    }
}