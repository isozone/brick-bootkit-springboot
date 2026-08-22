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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件性能监控器
 * 监控插件的CPU、内存、线程等资源使用情况
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginPerformanceMonitor {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginPerformanceMonitor.class);
    
    private final PluginMetrics pluginMetrics;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final ScheduledExecutorService scheduler;
    
    // 插件资源使用情况缓存
    private final ConcurrentHashMap<String, PluginResourceUsage> pluginResources = new ConcurrentHashMap<>();
    
    // 监控配置
    private volatile boolean monitoringEnabled = true;
    private volatile long monitoringInterval = 30; // 秒
    
    public PluginPerformanceMonitor(PluginMetrics pluginMetrics) {
        this.pluginMetrics = pluginMetrics;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "plugin-performance-monitor");
            t.setDaemon(true);
            return t;
        });
        
        startMonitoring();
    }
    
    /**
     * 检查是否正在监控指定插件
     */
    public boolean isMonitoring(String pluginId) {
        return pluginResources.containsKey(pluginId);
    }
    
    /**
     * 开始监控
     */
    private void startMonitoring() {
        // 定期收集系统资源使用情况
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, monitoringInterval, TimeUnit.SECONDS);
        
        // 定期收集插件资源使用情况
        scheduler.scheduleAtFixedRate(this::collectPluginMetrics, 5, monitoringInterval, TimeUnit.SECONDS);
        
        log.info("Plugin performance monitoring started with interval: {} seconds", monitoringInterval);
    }
    
    /**
     * 收集系统指标
     */
    private void collectSystemMetrics() {
        if (!monitoringEnabled) {
            return;
        }
        
        try {
            // 收集内存使用情况
            MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
            
            log.debug("System memory - Heap: used={}, max={}, NonHeap: used={}, max={}", 
                    heapMemory.getUsed(), heapMemory.getMax(),
                    nonHeapMemory.getUsed(), nonHeapMemory.getMax());
            
            // 收集线程信息
            int threadCount = threadMXBean.getThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            
            log.debug("System threads - Total: {}, Daemon: {}", threadCount, daemonThreadCount);
            
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
        }
    }
    
    /**
     * 收集插件指标
     */
    private void collectPluginMetrics() {
        if (!monitoringEnabled) {
            return;
        }
        
        try {
            for (String pluginId : pluginResources.keySet()) {
                PluginResourceUsage usage = pluginResources.get(pluginId);
                if (usage != null) {
                    // 更新插件内存使用量到指标系统
                    pluginMetrics.updatePluginMemoryUsage(pluginId, usage.getMemoryUsage());
                    
                    log.debug("Plugin {} - Memory: {} bytes, Threads: {}, CPU: {}%", 
                            pluginId, usage.getMemoryUsage(), usage.getThreadCount(), usage.getCpuUsage());
                }
            }
        } catch (Exception e) {
            log.error("Error collecting plugin metrics", e);
        }
    }
    
    /**
     * 开始监控插件
     */
    public void startMonitoringPlugin(String pluginId, ClassLoader pluginClassLoader) {
        PluginResourceUsage usage = new PluginResourceUsage(pluginId, pluginClassLoader);
        pluginResources.put(pluginId, usage);
        
        log.info("Started monitoring plugin: {}", pluginId);
    }
    
    /**
     * 停止监控插件
     */
    public void stopMonitoringPlugin(String pluginId) {
        PluginResourceUsage usage = pluginResources.remove(pluginId);
        if (usage != null) {
            usage.cleanup();
            log.info("Stopped monitoring plugin: {}", pluginId);
        }
    }
    
    /**
     * 获取插件资源使用情况
     */
    public PluginResourceUsage getPluginResourceUsage(String pluginId) {
        return pluginResources.get(pluginId);
    }
    
    /**
     * 设置监控间隔
     */
    public void setMonitoringInterval(long intervalSeconds) {
        this.monitoringInterval = intervalSeconds;
        log.info("Monitoring interval updated to: {} seconds", intervalSeconds);
    }
    
    /**
     * 启用/禁用监控
     */
    public void setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        log.info("Plugin monitoring {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * 获取所有插件的资源使用摘要
     */
    public PluginResourceSummary getResourceSummary() {
        long totalMemory = 0;
        int totalThreads = 0;
        double totalCpu = 0;
        int pluginCount = pluginResources.size();
        
        for (PluginResourceUsage usage : pluginResources.values()) {
            totalMemory += usage.getMemoryUsage();
            totalThreads += usage.getThreadCount();
            totalCpu += usage.getCpuUsage();
        }
        
        return new PluginResourceSummary(pluginCount, totalMemory, totalThreads, totalCpu);
    }
    
    /**
     * 关闭监控器
     */
    public void shutdown() {
        monitoringEnabled = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 清理所有插件资源
        pluginResources.values().forEach(PluginResourceUsage::cleanup);
        pluginResources.clear();
        
        log.info("Plugin performance monitor shutdown completed");
    }
    
    /**
     * 插件资源使用情况
     */
    public static class PluginResourceUsage {
        
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginResourceUsage.class);
        
        private final String pluginId;
        private final ClassLoader classLoader;
        private volatile long memoryUsage;
        private volatile int threadCount;
        private volatile double cpuUsage;
        private volatile long lastUpdateTime;
        
        public PluginResourceUsage(String pluginId, ClassLoader classLoader) {
            this.pluginId = pluginId;
            this.classLoader = classLoader;
            this.lastUpdateTime = System.currentTimeMillis();
            updateMetrics();
        }
        
        public void updateMetrics() {
            try {
                // 这里可以实现更精确的插件资源监控
                // 目前使用简化的实现
                this.memoryUsage = estimateMemoryUsage();
                this.threadCount = estimateThreadCount();
                this.cpuUsage = estimateCpuUsage();
                this.lastUpdateTime = System.currentTimeMillis();
            } catch (Exception e) {
                log.warn("Failed to update metrics for plugin: {}", pluginId, e);
            }
        }
        
        private long estimateMemoryUsage() {
            // 简化的内存使用估算
            // 实际实现中可以通过JVM工具API获取更精确的数据
            return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
        
        private int estimateThreadCount() {
            // 简化的线程数估算
            return Thread.activeCount();
        }
        
        private double estimateCpuUsage() {
            // 简化的CPU使用率估算
            // 实际实现中可以使用OperatingSystemMXBean获取更精确的数据
            return 0.0;
        }
        
        public void cleanup() {
            // 清理资源
        }
        
        // Getters
        public String getPluginId() { return pluginId; }
        public long getMemoryUsage() { return memoryUsage; }
        public int getThreadCount() { return threadCount; }
        public double getCpuUsage() { return cpuUsage; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
    
    /**
     * 插件资源使用摘要
     */
    public static class PluginResourceSummary {
        private final int pluginCount;
        private final long totalMemoryUsage;
        private final int totalThreadCount;
        private final double totalCpuUsage;
        
        public PluginResourceSummary(int pluginCount, long totalMemoryUsage, int totalThreadCount, double totalCpuUsage) {
            this.pluginCount = pluginCount;
            this.totalMemoryUsage = totalMemoryUsage;
            this.totalThreadCount = totalThreadCount;
            this.totalCpuUsage = totalCpuUsage;
        }
        
        // Getters
        public int getPluginCount() { return pluginCount; }
        public long getTotalMemoryUsage() { return totalMemoryUsage; }
        public int getTotalThreadCount() { return totalThreadCount; }
        public double getTotalCpuUsage() { return totalCpuUsage; }
        public double getAverageCpuUsage() { return pluginCount > 0 ? totalCpuUsage / pluginCount : 0; }
    }
}