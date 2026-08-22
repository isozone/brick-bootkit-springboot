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


package com.zqzqq.bootkits.core.isolation;

import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件资源监控器
 * 监控插件的资源使用情况，包括内存、线程、CPU等
 */
public class PluginResourceMonitor {
    
    private static final PluginLogger logger = PluginLogger.getLogger(PluginResourceMonitor.class);
    
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final ScheduledExecutorService scheduler;
    
    // 监控状态
    private volatile boolean monitoringEnabled = true;
    private volatile long monitoringInterval = 10; // 秒
    
    // 插件监控状态
    private final Map<String, Boolean> monitoringPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginResourceUsage> pluginResourceUsage = new ConcurrentHashMap<>();
    
    // 线程计数统计
    private final AtomicInteger totalThreadCount = new AtomicInteger(0);
    
    public PluginResourceMonitor() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "plugin-resource-monitor");
            t.setDaemon(true);
            return t;
        });
        
        startMonitoring();
    }
    
    /**
     * 开始监控插件
     */
    public void startMonitoring(String pluginId, PluginResourceUsage usage) {
        logger.info("system", "开始监控插件资源", pluginId);
        
        monitoringPlugins.put(pluginId, true);
        pluginResourceUsage.put(pluginId, usage);
        
        // 立即执行一次监控
        updatePluginResourceUsage(pluginId, usage);
    }
    
    /**
     * 停止监控插件
     */
    public void stopMonitoring(String pluginId) {
        logger.info("system", "停止监控插件资源", pluginId);
        
        monitoringPlugins.remove(pluginId);
        pluginResourceUsage.remove(pluginId);
    }
    
    /**
     * 开始监控
     */
    private void startMonitoring() {
        if (monitoringEnabled) {
            scheduler.scheduleAtFixedRate(this::monitorAllPlugins, 
                                        monitoringInterval, monitoringInterval, TimeUnit.SECONDS);
            
            scheduler.scheduleAtFixedRate(this::updateSystemMetrics, 
                                        5, 30, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 监控所有插件
     */
    private void monitorAllPlugins() {
        for (Map.Entry<String, Boolean> entry : monitoringPlugins.entrySet()) {
            String pluginId = entry.getKey();
            Boolean isMonitoring = entry.getValue();
            
            if (Boolean.TRUE.equals(isMonitoring)) {
                PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
                if (usage != null) {
                    try {
                        updatePluginResourceUsage(pluginId, usage);
                    } catch (Exception e) {
                        logger.warn("system", "监控插件资源失败", pluginId, "error", e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * 更新插件资源使用情况
     */
    private void updatePluginResourceUsage(String pluginId, PluginResourceUsage usage) {
        try {
            // 更新内存使用
            long estimatedMemory = estimatePluginMemoryUsage(pluginId);
            usage.updateMemoryUsage(estimatedMemory);
            
            // 更新线程数
            int estimatedThreads = estimatePluginThreadCount(pluginId);
            usage.setThreadCount(estimatedThreads);
            
            // 更新文件描述符 (简化处理)
            usage.updateFileDescriptors(estimateFileDescriptorCount());
            
            // 更新CPU使用率 (简化处理)
            usage.updateCpuUsage(estimateCpuUsage(pluginId));
            
            // 更新网络连接数 (简化处理)
            usage.updateNetworkConnections(estimateNetworkConnections());
            
            // 更新磁盘I/O (简化处理)
            usage.updateDiskIO(estimateDiskIO(pluginId));
            
        } catch (Exception e) {
            logger.warn("system", "更新插件资源使用情况失败", pluginId, "error", e.getMessage(), e);
        }
    }
    
    /**
     * 估算插件内存使用
     */
    private long estimatePluginMemoryUsage(String pluginId) {
        // 这里可以实现更精确的内存使用估算
        // 目前返回JVM总内存的一个估计值
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long usedHeap = heapUsage.getUsed();
        
        // 简化估算：假设所有插件平均分配内存
        int monitoredPlugins = Math.max(monitoringPlugins.size(), 1);
        return usedHeap / monitoredPlugins;
    }
    
    /**
     * 估算插件线程数
     */
    private int estimatePluginThreadCount(String pluginId) {
        // 估算当前活跃线程数
        int liveThreadCount = threadMXBean.getThreadCount();
        totalThreadCount.set(liveThreadCount);
        
        // 简化估算：假设所有插件平均分配线程
        int monitoredPlugins = Math.max(monitoringPlugins.size(), 1);
        return Math.max(liveThreadCount / monitoredPlugins, 1);
    }
    
    /**
     * 估算文件描述符数量
     */
    private int estimateFileDescriptorCount() {
        // 简化处理：返回系统总文件描述符使用的一个估计值
        return totalThreadCount.get() * 2; // 假设每个线程平均2个文件描述符
    }
    
    /**
     * 估算CPU使用率
     */
    private long estimateCpuUsage(String pluginId) {
        // 简化处理：返回当前CPU使用率的一个估计值
        // 实际实现中应该使用更精确的CPU监控
        return (long) (Math.random() * 10); // 随机值0-10%
    }
    
    /**
     * 估算网络连接数
     */
    private int estimateNetworkConnections() {
        // 简化处理：返回一个估计值
        return totalThreadCount.get() / 2; // 假设每2个线程有1个网络连接
    }
    
    /**
     * 估算磁盘I/O使用
     */
    private long estimateDiskIO(String pluginId) {
        // 简化处理：返回磁盘I/O的一个估计值
        return (long) (Math.random() * 1024 * 1024); // 随机值0-1MB
    }
    
    /**
     * 更新系统指标
     */
    private void updateSystemMetrics() {
        try {
            // 更新全局线程数
            int liveThreadCount = threadMXBean.getThreadCount();
            totalThreadCount.set(liveThreadCount);
            
            // 检查系统资源使用情况
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            long usedHeap = heapUsage.getUsed();
            long maxHeap = heapUsage.getMax();
            
            double heapUsagePercent = (double) usedHeap / maxHeap * 100;
            
            if (heapUsagePercent > 80) {
                logger.warn("system", "系统堆内存使用率较高", 
                           "usage", String.format("%.1f%%", heapUsagePercent),
                           "used", usedHeap / (1024 * 1024) + "MB",
                           "max", maxHeap / (1024 * 1024) + "MB");
            }
            
            if (liveThreadCount > Runtime.getRuntime().availableProcessors() * 8) {
                logger.warn("system", "系统线程数较多", 
                           "threads", liveThreadCount,
                           "availableProcessors", Runtime.getRuntime().availableProcessors());
            }
            
        } catch (Exception e) {
            logger.warn("system", "更新系统指标失败", "error", e.getMessage());
        }
    }
    
    /**
     * 获取插件资源使用情况
     */
    public PluginResourceUsage getPluginResourceUsage(String pluginId) {
        return pluginResourceUsage.get(pluginId);
    }
    
    /**
     * 获取所有插件资源使用情况
     */
    public Map<String, PluginResourceUsage> getAllPluginResourceUsage() {
        return new ConcurrentHashMap<>(pluginResourceUsage);
    }
    
    /**
     * 检查监控状态
     */
    public boolean isMonitoringPlugin(String pluginId) {
        return Boolean.TRUE.equals(monitoringPlugins.get(pluginId));
    }
    
    /**
     * 获取监控插件数量
     */
    public int getMonitoredPluginCount() {
        return monitoringPlugins.size();
    }
    
    /**
     * 设置监控间隔
     */
    public void setMonitoringInterval(long intervalSeconds) {
        if (intervalSeconds < 1) {
            throw new IllegalArgumentException("监控间隔不能小于1秒");
        }
        this.monitoringInterval = intervalSeconds;
    }
    
    /**
     * 获取监控间隔
     */
    public long getMonitoringInterval() {
        return monitoringInterval;
    }
    
    /**
     * 启用/禁用监控
     */
    public void setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        if (enabled) {
            startMonitoring();
        }
    }
    
    /**
     * 检查监控是否启用
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * 关闭监控器
     */
    public void shutdown() {
        logger.info("system", "关闭插件资源监控器");
        
        monitoringEnabled = false;
        monitoringPlugins.clear();
        pluginResourceUsage.clear();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("system", "插件资源监控器关闭完成");
    }
    
    /**
     * 获取所有插件的资源使用摘要
     */
    public PluginResourceSummary getResourceSummary() {
        long totalMemory = 0;
        int totalThreads = 0;
        double totalCpu = 0;
        int pluginCount = pluginResourceUsage.size();
        
        for (PluginResourceUsage usage : pluginResourceUsage.values()) {
            totalMemory += usage.getCurrentMemoryUsage();
            totalThreads += usage.getCurrentThreadCount();
            totalCpu += usage.getCurrentCpuUsage();
        }
        
        return new PluginResourceSummary(pluginCount, totalMemory, totalThreads, totalCpu);
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
        
        /**
         * 获取内存使用量（MB）
         */
        public long getTotalMemoryUsageMB() {
            return totalMemoryUsage / (1024 * 1024);
        }
    }
}