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


package com.zqzqq.bootkits.core.monitoring;

/**
 * 插件监控配置
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class PluginMonitoringConfiguration {
    
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
    
    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (collectionInterval <= 0) {
            throw new IllegalArgumentException("Collection interval must be positive");
        }
        
        if (memoryWarningThreshold < 0 || memoryWarningThreshold > 100) {
            throw new IllegalArgumentException("Memory warning threshold must be between 0 and 100");
        }
        
        if (memoryCriticalThreshold < memoryWarningThreshold || memoryCriticalThreshold > 100) {
            throw new IllegalArgumentException("Memory critical threshold must be between warning threshold and 100");
        }
        
        if (cpuWarningThreshold < 0 || cpuWarningThreshold > 100) {
            throw new IllegalArgumentException("CPU warning threshold must be between 0 and 100");
        }
        
        if (cpuCriticalThreshold < cpuWarningThreshold || cpuCriticalThreshold > 100) {
            throw new IllegalArgumentException("CPU critical threshold must be between warning threshold and 100");
        }
        
        if (threadWarningThreshold <= 0) {
            throw new IllegalArgumentException("Thread warning threshold must be positive");
        }
        
        if (threadCriticalThreshold < threadWarningThreshold) {
            throw new IllegalArgumentException("Thread critical threshold must be greater than warning threshold");
        }
        
        if (performanceReportInterval <= 0) {
            throw new IllegalArgumentException("Performance report interval must be positive");
        }
        
        if (slowOperationThreshold <= 0) {
            throw new IllegalArgumentException("Slow operation threshold must be positive");
        }
        
        if (historyRetentionDays <= 0) {
            throw new IllegalArgumentException("History retention days must be positive");
        }
        
        if (realTimeUpdateInterval <= 0) {
            throw new IllegalArgumentException("Real-time update interval must be positive");
        }
    }
    
    /**
     * 创建默认配置
     */
    public static PluginMonitoringConfiguration createDefault() {
        PluginMonitoringConfiguration config = new PluginMonitoringConfiguration();
        config.validate();
        return config;
    }
    
    /**
     * 创建开发环境配置
     */
    public static PluginMonitoringConfiguration createDevelopmentConfig() {
        PluginMonitoringConfiguration config = new PluginMonitoringConfiguration();
        config.setCollectionInterval(10); // 更频繁的收集
        config.setRealTimeMonitoringEnabled(true);
        config.setRealTimeUpdateInterval(2);
        config.setPerformanceReportInterval(30); // 更频繁的报告
        config.validate();
        return config;
    }
    
    /**
     * 创建生产环境配置
     */
    public static PluginMonitoringConfiguration createProductionConfig() {
        PluginMonitoringConfiguration config = new PluginMonitoringConfiguration();
        config.setCollectionInterval(60); // 较少的收集频率
        config.setRealTimeMonitoringEnabled(false);
        config.setPerformanceReportInterval(120); // 较少的报告频率
        config.setHistoryRetentionDays(30); // 更长的历史保留
        config.validate();
        return config;
    }

    /**
     * 判断监控是否启用（public方法，确保兼容性）
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断性能报告是否启用（public方法，确保兼容性）
     */
    public boolean isPerformanceReportEnabled() {
        return performanceReportEnabled;
    }

    /**
     * 获取性能报告间隔（public方法，确保兼容性）
     */
    public long getPerformanceReportInterval() {
        return performanceReportInterval;
    }

    /**
     * 获取监控数据收集间隔（public方法，确保兼容性）
     */
    public long getCollectionInterval() {
        return collectionInterval;
    }

    /**
     * 获取内存严重警告阈值（public方法，确保兼容性）
     */
    public double getMemoryCriticalThreshold() {
        return memoryCriticalThreshold;
    }

    /**
     * 获取内存警告阈值（public方法，确保兼容性）
     */
    public double getMemoryWarningThreshold() {
        return memoryWarningThreshold;
    }

    /**
     * 获取线程严重警告阈值（public方法，确保兼容性）
     */
    public int getThreadCriticalThreshold() {
        return threadCriticalThreshold;
    }

    /**
     * 获取线程警告阈值（public方法，确保兼容性）
     */
    public int getThreadWarningThreshold() {
        return threadWarningThreshold;
    }

    /**
     * 获取历史数据保留天数（public方法，确保兼容性）
     */
    public int getHistoryRetentionDays() {
        return historyRetentionDays;
    }

    /**
     * 设置监控数据收集间隔（public方法，确保兼容性）
     */
    public void setCollectionInterval(long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    /**
     * 设置实时监控启用状态（public方法，确保兼容性）
     */
    public void setRealTimeMonitoringEnabled(boolean realTimeMonitoringEnabled) {
        this.realTimeMonitoringEnabled = realTimeMonitoringEnabled;
    }

    /**
     * 设置实时监控更新间隔（public方法，确保兼容性）
     */
    public void setRealTimeUpdateInterval(long realTimeUpdateInterval) {
        this.realTimeUpdateInterval = realTimeUpdateInterval;
    }

    /**
     * 设置性能报告间隔（public方法，确保兼容性）
     */
    public void setPerformanceReportInterval(long performanceReportInterval) {
        this.performanceReportInterval = performanceReportInterval;
    }

    /**
     * 设置历史数据保留天数（public方法，确保兼容性）
     */
    public void setHistoryRetentionDays(int historyRetentionDays) {
        this.historyRetentionDays = historyRetentionDays;
    }

    /**
     * 设置慢操作检测启用状态（public方法，确保兼容性）
     */
    public void setSlowOperationDetectionEnabled(boolean slowOperationDetectionEnabled) {
        this.slowOperationDetectionEnabled = slowOperationDetectionEnabled;
    }

    /**
     * 设置监控启用状态（public方法，确保兼容性）
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 设置内存监控启用状态（public方法，确保兼容性）
     */
    public void setMemoryMonitoringEnabled(boolean memoryMonitoringEnabled) {
        this.memoryMonitoringEnabled = memoryMonitoringEnabled;
    }

    /**
     * 设置CPU监控启用状态（public方法，确保兼容性）
     */
    public void setCpuMonitoringEnabled(boolean cpuMonitoringEnabled) {
        this.cpuMonitoringEnabled = cpuMonitoringEnabled;
    }

    /**
     * 设置线程监控启用状态（public方法，确保兼容性）
     */
    public void setThreadMonitoringEnabled(boolean threadMonitoringEnabled) {
        this.threadMonitoringEnabled = threadMonitoringEnabled;
    }

    /**
     * 设置类加载监控启用状态（public方法，确保兼容性）
     */
    public void setClassLoadingMonitoringEnabled(boolean classLoadingMonitoringEnabled) {
        this.classLoadingMonitoringEnabled = classLoadingMonitoringEnabled;
    }

    /**
     * 设置GC监控启用状态（public方法，确保兼容性）
     */
    public void setGcMonitoringEnabled(boolean gcMonitoringEnabled) {
        this.gcMonitoringEnabled = gcMonitoringEnabled;
    }

    /**
     * 设置慢操作阈值（public方法，确保兼容性）
     */
    public void setSlowOperationThreshold(long slowOperationThreshold) {
        this.slowOperationThreshold = slowOperationThreshold;
    }

    /**
     * 设置性能报告启用状态（public方法，确保兼容性）
     */
    public void setPerformanceReportEnabled(boolean performanceReportEnabled) {
        this.performanceReportEnabled = performanceReportEnabled;
    }

    /**
     * 设置线程严重警告阈值（public方法，确保兼容性）
     */
    public void setThreadCriticalThreshold(int threadCriticalThreshold) {
        this.threadCriticalThreshold = threadCriticalThreshold;
    }

    /**
     * 设置内存警告阈值（public方法，确保兼容性）
     */
    public void setMemoryWarningThreshold(double memoryWarningThreshold) {
        this.memoryWarningThreshold = memoryWarningThreshold;
    }

    /**
     * 设置内存严重警告阈值（public方法，确保兼容性）
     */
    public void setMemoryCriticalThreshold(double memoryCriticalThreshold) {
        this.memoryCriticalThreshold = memoryCriticalThreshold;
    }

    /**
     * 设置CPU警告阈值（public方法，确保兼容性）
     */
    public void setCpuWarningThreshold(double cpuWarningThreshold) {
        this.cpuWarningThreshold = cpuWarningThreshold;
    }

    /**
     * 设置CPU严重警告阈值（public方法，确保兼容性）
     */
    public void setCpuCriticalThreshold(double cpuCriticalThreshold) {
        this.cpuCriticalThreshold = cpuCriticalThreshold;
    }

    /**
     * 设置线程警告阈值（public方法，确保兼容性）
     */
    public void setThreadWarningThreshold(int threadWarningThreshold) {
        this.threadWarningThreshold = threadWarningThreshold;
    }
    
    /**
     * 获取慢操作阈值（public方法，确保兼容性）
     */
    public long getSlowOperationThreshold() {
        return slowOperationThreshold;
    }

    /**
     * 获取实时监控更新间隔（public方法，确保兼容性）
     */
    public long getRealTimeUpdateInterval() {
        return realTimeUpdateInterval;
    }

    /**
     * 获取是否启用实时监控（public方法，确保兼容性）
     */
    public boolean isRealTimeMonitoringEnabled() {
        return realTimeMonitoringEnabled;
    }

    /**
     * 获取是否启用慢操作检测（public方法，确保兼容性）
     */
    public boolean isSlowOperationDetectionEnabled() {
        return slowOperationDetectionEnabled;
    }
    
    }
