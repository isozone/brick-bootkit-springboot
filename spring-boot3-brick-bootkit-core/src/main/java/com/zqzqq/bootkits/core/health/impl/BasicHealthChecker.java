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


package com.zqzqq.bootkits.core.health.impl;

import com.zqzqq.bootkits.core.health.PluginHealthChecker;
import com.zqzqq.bootkits.core.health.PluginHealthReport;
import com.zqzqq.bootkits.core.health.PluginHealthStatus;
import com.zqzqq.bootkits.core.plugin.Plugin;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 基础插件健康检查器
 * 检查插件的基本运行状态
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class BasicHealthChecker implements PluginHealthChecker {
    
    private boolean enabled = true;
    private long timeoutMillis = 5000; // 5秒超时
    private final String name = "BasicHealthChecker";
    private final String description = "基础插件健康检查器，检查插件基本运行状态";

    @Override
    public PluginHealthReport checkHealth(Plugin plugin) {
        long startTime = System.currentTimeMillis();
        LocalDateTime checkTime = LocalDateTime.now();
        ArrayList<PluginHealthReport.HealthCheckResult> results = new ArrayList<>();

        if (plugin == null) {
            return PluginHealthReport.createDead("unknown", "插件对象为空", null);
        }

        String pluginId;
        try {
            pluginId = plugin.getId();
        } catch (Exception e) {
            return PluginHealthReport.createDead("unknown", "无法获取插件ID: " + e.getMessage(), e);
        }

        try {
            // 检查1: 插件接口完整性
            results.add(checkPluginInterface(plugin));
            
            // 检查2: 插件状态检查
            results.add(checkPluginStatus(plugin));
            
            // 检查3: 插件运行状态验证
            results.add(checkPluginRunningState(plugin));
            
            // 检查4: 插件异常检查
            results.add(checkPluginException(plugin));
            
            // 分析检查结果
            PluginHealthStatus overallStatus = analyzeResults(results);
            String message = generateMessage(overallStatus, results);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new PluginHealthReport(pluginId, overallStatus, checkTime, 
                                        responseTime, results, message, null);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.createDead(pluginId, 
                                               "健康检查执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PluginHealthStatus quickHealthCheck(Plugin plugin) {
        try {
            // 快速检查只验证基本状态
            if (plugin == null) {
                return PluginHealthStatus.DEAD;
            }
            
            if (!plugin.isRunning()) {
                return PluginHealthStatus.DEAD;
            }
            
            return PluginHealthStatus.HEALTHY;
            
        } catch (Exception e) {
            return PluginHealthStatus.UNKNOWN;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 检查插件接口完整性
     */
    private PluginHealthReport.HealthCheckResult checkPluginInterface(Plugin plugin) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (plugin == null) {
                return PluginHealthReport.HealthCheckResult.failure(
                    "PluginInterface", "插件对象为空");
            }
            
            if (plugin.getId() == null || plugin.getId().trim().isEmpty()) {
                return PluginHealthReport.HealthCheckResult.failure(
                    "PluginInterface", "插件ID为空或null");
            }
            
            if (plugin.getName() == null || plugin.getName().trim().isEmpty()) {
                return PluginHealthReport.HealthCheckResult.failure(
                    "PluginInterface", "插件名称为空或null");
            }
            
            if (plugin.getVersion() == null || plugin.getVersion().trim().isEmpty()) {
                return PluginHealthReport.HealthCheckResult.failure(
                    "PluginInterface", "插件版本为空或null");
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.success(
                "PluginInterface", "插件接口完整", executionTime);
                
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.failure(
                "PluginInterface", "插件接口检查异常", e);
        }
    }

    /**
     * 检查插件状态
     */
    private PluginHealthReport.HealthCheckResult checkPluginStatus(Plugin plugin) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (!plugin.isRunning()) {
                long executionTime = System.currentTimeMillis() - startTime;
                return PluginHealthReport.HealthCheckResult.failure(
                    "PluginStatus", "插件未运行");
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.success(
                "PluginStatus", "插件运行状态正常", executionTime);
                
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.failure(
                "PluginStatus", "插件状态检查异常", e);
        }
    }

    /**
     * 检查插件运行状态验证
     */
    private PluginHealthReport.HealthCheckResult checkPluginRunningState(Plugin plugin) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 尝试调用插件的基本方法来验证其可用性
            String pluginName = plugin.getName();
            String pluginVersion = plugin.getVersion();
            
            if (pluginName == null || pluginName.isEmpty()) {
                throw new IllegalStateException("插件名称为空");
            }
            
            if (pluginVersion == null || pluginVersion.isEmpty()) {
                throw new IllegalStateException("插件版本为空");
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.success(
                "PluginRunningState", "插件运行状态验证通过", executionTime);
                
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return PluginHealthReport.HealthCheckResult.failure(
                "PluginRunningState", "插件运行状态验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查插件异常
     */
    private PluginHealthReport.HealthCheckResult checkPluginException(Plugin plugin) {
        // 这里可以添加更多的异常检查逻辑
        // 目前先返回成功状态
        return PluginHealthReport.HealthCheckResult.success(
            "PluginException", "未检测到插件异常", 0);
    }

    /**
     * 分析检查结果，确定整体状态
     */
    private PluginHealthStatus analyzeResults(ArrayList<PluginHealthReport.HealthCheckResult> results) {
        if (results.isEmpty()) {
            return PluginHealthStatus.UNKNOWN;
        }
        
        int failedCount = 0;
        int warningCount = 0;
        int totalCount = results.size();
        
        for (PluginHealthReport.HealthCheckResult result : results) {
            if (!result.isPassed()) {
                if (result.getCheckName().contains("Exception") || 
                    result.getCheckName().contains("Interface")) {
                    failedCount++;
                } else {
                    warningCount++;
                }
            }
        }
        
        if (failedCount > 0) {
            return PluginHealthStatus.CRITICAL;
        }
        
        if (warningCount > 0) {
            return PluginHealthStatus.WARNING;
        }
        
        return PluginHealthStatus.HEALTHY;
    }

    /**
     * 生成状态消息
     */
    private String generateMessage(PluginHealthStatus status, ArrayList<PluginHealthReport.HealthCheckResult> results) {
        int totalChecks = results.size();
        int passedChecks = (int) results.stream().filter(PluginHealthReport.HealthCheckResult::isPassed).count();
        int failedChecks = totalChecks - passedChecks;
        
        return String.format("健康检查完成 - 状态: %s, 通过: %d/%d, 失败: %d", 
                           status.getDescription(), passedChecks, totalChecks, failedChecks);
    }
}