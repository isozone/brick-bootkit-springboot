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

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件监控管理器
 * 统一管理插件监控功能
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginMonitoringManager {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginMonitoringManager.class);
    
    private final PluginMetrics pluginMetrics;
    private final PluginPerformanceMonitor performanceMonitor;
    private final PluginMonitoringConfiguration configuration;
    private final ScheduledExecutorService scheduler;
    
    // 监控状态
    private volatile boolean monitoringActive = false;
    private final ConcurrentHashMap<String, PluginMonitoringSession> activeSessions = new ConcurrentHashMap<>();
    
    public PluginMonitoringManager(PluginMetrics pluginMetrics, 
                                 PluginPerformanceMonitor performanceMonitor,
                                 PluginMonitoringConfiguration configuration) {
        this.pluginMetrics = pluginMetrics;
        this.performanceMonitor = performanceMonitor;
        this.configuration = configuration;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "plugin-monitoring-manager");
            t.setDaemon(true);
            return t;
        });
        
        initialize();
    }
    
    private void initialize() {
        if (configuration.isEnabled()) {
            startMonitoring();
        }
        
        // 定期生成性能报告
        if (configuration.isPerformanceReportEnabled()) {
            scheduler.scheduleAtFixedRate(
                this::generatePerformanceReport,
                configuration.getPerformanceReportInterval(),
                configuration.getPerformanceReportInterval(),
                TimeUnit.MINUTES
            );
        }
        
        // 定期清理历史数据
        scheduler.scheduleAtFixedRate(
            this::cleanupHistoryData,
            24, // 每天执行一次
            24,
            TimeUnit.HOURS
        );
        
        log.info("Plugin monitoring manager initialized with configuration: {}", configuration);
    }
    
    /**
     * 启动监控
     */
    public void startMonitoring() {
        if (monitoringActive) {
            log.warn("Plugin monitoring is already active");
            return;
        }
        
        monitoringActive = true;
        performanceMonitor.setMonitoringEnabled(true);
        performanceMonitor.setMonitoringInterval(configuration.getCollectionInterval());
        
        log.info("Plugin monitoring started");
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        if (!monitoringActive) {
            log.warn("Plugin monitoring is not active");
            return;
        }
        
        monitoringActive = false;
        performanceMonitor.setMonitoringEnabled(false);
        
        // 清理所有活跃会话
        activeSessions.clear();
        
        log.info("Plugin monitoring stopped");
    }
    
    /**
     * 开始监控插件
     */
    public void startPluginMonitoring(String pluginId, ClassLoader classLoader) {
        if (!monitoringActive) {
            log.debug("Monitoring is not active, skipping plugin monitoring for: {}", pluginId);
            return;
        }
        
        PluginMonitoringSession session = new PluginMonitoringSession(pluginId, classLoader);
        activeSessions.put(pluginId, session);
        
        performanceMonitor.startMonitoringPlugin(pluginId, classLoader);
        
        log.info("Started monitoring plugin: {}", pluginId);
    }
    
    /**
     * 停止监控插件
     */
    public void stopPluginMonitoring(String pluginId) {
        PluginMonitoringSession session = activeSessions.remove(pluginId);
        if (session != null) {
            session.close();
            performanceMonitor.stopMonitoringPlugin(pluginId);
            log.info("Stopped monitoring plugin: {}", pluginId);
        }
    }
    
    /**
     * 获取插件监控会话
     */
    public PluginMonitoringSession getMonitoringSession(String pluginId) {
        return activeSessions.get(pluginId);
    }
    
    /**
     * 检查监控状态
     */
    public boolean isMonitoringActive() {
        return monitoringActive;
    }
    
    /**
     * 获取活跃监控会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * 生成性能报告
     */
    private void generatePerformanceReport() {
        try {
            PluginPerformanceMonitor.PluginResourceSummary summary = performanceMonitor.getResourceSummary();
            
            log.info("Plugin Performance Report - Plugins: {}, Total Memory: {} MB, Total Threads: {}, Avg CPU: {}%",
                    summary.getPluginCount(),
                    summary.getTotalMemoryUsage() / (1024 * 1024),
                    summary.getTotalThreadCount(),
                    String.format("%.2f", summary.getAverageCpuUsage()));
            
            // 检查阈值警告
            checkThresholds(summary);
            
        } catch (Exception e) {
            log.error("Error generating performance report", e);
        }
    }
    
    /**
     * 检查阈值警告
     */
    private void checkThresholds(PluginPerformanceMonitor.PluginResourceSummary summary) {
        // 检查内存使用率
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsagePercent > configuration.getMemoryCriticalThreshold()) {
            log.error("CRITICAL: Memory usage is {}%, exceeding critical threshold {}%", 
                    String.format("%.2f", memoryUsagePercent), configuration.getMemoryCriticalThreshold());
        } else if (memoryUsagePercent > configuration.getMemoryWarningThreshold()) {
            log.warn("WARNING: Memory usage is {}%, exceeding warning threshold {}%", 
                    String.format("%.2f", memoryUsagePercent), configuration.getMemoryWarningThreshold());
        }
        
        // 检查线程数
        if (summary.getTotalThreadCount() > configuration.getThreadCriticalThreshold()) {
            log.error("CRITICAL: Thread count is {}, exceeding critical threshold {}", 
                    summary.getTotalThreadCount(), configuration.getThreadCriticalThreshold());
        } else if (summary.getTotalThreadCount() > configuration.getThreadWarningThreshold()) {
            log.warn("WARNING: Thread count is {}, exceeding warning threshold {}", 
                    summary.getTotalThreadCount(), configuration.getThreadWarningThreshold());
        }
    }
    
    /**
     * 清理历史数据
     */
    private void cleanupHistoryData() {
        try {
            // 这里可以实现历史数据清理逻辑
            log.debug("Cleaning up monitoring history data older than {} days", 
                    configuration.getHistoryRetentionDays());
        } catch (Exception e) {
            log.error("Error cleaning up history data", e);
        }
    }
    
    /**
     * 关闭监控管理器
     */
    public void shutdown() {
        stopMonitoring();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        performanceMonitor.shutdown();
        
        log.info("Plugin monitoring manager shutdown completed");
    }
    
    /**
     * 插件监控会话
     */
    public static class PluginMonitoringSession {
        private final String pluginId;
        private final ClassLoader classLoader;
        private final long startTime;
        
        public PluginMonitoringSession(String pluginId, ClassLoader classLoader) {
            this.pluginId = pluginId;
            this.classLoader = classLoader;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public ClassLoader getClassLoader() {
            return classLoader;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
        
        public void close() {
            // 清理会话资源
        }
    }
}