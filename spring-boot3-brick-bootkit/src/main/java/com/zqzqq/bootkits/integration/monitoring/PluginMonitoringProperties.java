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


package com.zqzqq.bootkits.integration.monitoring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件监控配置属性
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Data
@ConfigurationProperties(prefix = "plugin.monitoring")
public class PluginMonitoringProperties {
    
    /**
     * 是否启用监控
     */
    private boolean enabled = true;
    
    /**
     * 监控数据收集间隔（秒）
     */
    private long collectionInterval = 30;
    
    /**
     * 是否启用内存监控
     */
    private boolean memoryMonitoringEnabled = true;
    
    /**
     * 是否启用CPU监控
     */
    private boolean cpuMonitoringEnabled = true;
    
    /**
     * 是否启用线程监控
     */
    private boolean threadMonitoringEnabled = true;
    
    /**
     * 是否启用类加载监控
     */
    private boolean classLoadingMonitoringEnabled = true;
    
    /**
     * 是否启用GC监控
     */
    private boolean gcMonitoringEnabled = true;
    
    /**
     * 内存使用警告阈值（百分比）
     */
    private double memoryWarningThreshold = 80.0;
    
    /**
     * 内存使用严重警告阈值（百分比）
     */
    private double memoryCriticalThreshold = 95.0;
    
    /**
     * CPU使用警告阈值（百分比）
     */
    private double cpuWarningThreshold = 80.0;
    
    /**
     * CPU使用严重警告阈值（百分比）
     */
    private double cpuCriticalThreshold = 95.0;
    
    /**
     * 线程数警告阈值
     */
    private int threadWarningThreshold = 100;
    
    /**
     * 线程数严重警告阈值
     */
    private int threadCriticalThreshold = 200;
    
    /**
     * 是否启用性能报告
     */
    private boolean performanceReportEnabled = true;
    
    /**
     * 性能报告生成间隔（分钟）
     */
    private long performanceReportInterval = 60;
    
    /**
     * 是否启用慢操作检测
     */
    private boolean slowOperationDetectionEnabled = true;
    
    /**
     * 慢操作阈值（毫秒）
     */
    private long slowOperationThreshold = 5000;
    
    /**
     * 历史数据保留天数
     */
    private int historyRetentionDays = 7;
    
    /**
     * 是否启用实时监控
     */
    private boolean realTimeMonitoringEnabled = false;
    
    /**
     * 实时监控更新间隔（秒）
     */
    private long realTimeUpdateInterval = 5;
    
    public boolean isSlowOperationDetectionEnabled() {
        return slowOperationDetectionEnabled;
    }
    
    public long getSlowOperationThreshold() {
        return slowOperationThreshold;
    }
    
    public int getHistoryRetentionDays() {
        return historyRetentionDays;
    }
    
    public boolean isRealTimeMonitoringEnabled() {
        return realTimeMonitoringEnabled;
    }
    
    public long getRealTimeUpdateInterval() {
        return realTimeUpdateInterval;
    }
    
    public boolean isPerformanceReportEnabled() {
        return performanceReportEnabled;
    }
    
    public long getPerformanceReportInterval() {
        return performanceReportInterval;
    }
    
    public int getThreadCriticalThreshold() {
        return threadCriticalThreshold;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public long getCollectionInterval() {
        return collectionInterval;
    }
    
    public boolean isMemoryMonitoringEnabled() {
        return memoryMonitoringEnabled;
    }
    
    public boolean isCpuMonitoringEnabled() {
        return cpuMonitoringEnabled;
    }
    
    public boolean isThreadMonitoringEnabled() {
        return threadMonitoringEnabled;
    }
    
    public boolean isClassLoadingMonitoringEnabled() {
        return classLoadingMonitoringEnabled;
    }
    
    public boolean isGcMonitoringEnabled() {
        return gcMonitoringEnabled;
    }
    
    public double getMemoryWarningThreshold() {
        return memoryWarningThreshold;
    }
    
    public double getMemoryCriticalThreshold() {
        return memoryCriticalThreshold;
    }
    
    public double getCpuWarningThreshold() {
        return cpuWarningThreshold;
    }
    
    public double getCpuCriticalThreshold() {
        return cpuCriticalThreshold;
    }
    
    public int getThreadWarningThreshold() {
        return threadWarningThreshold;
    }
}