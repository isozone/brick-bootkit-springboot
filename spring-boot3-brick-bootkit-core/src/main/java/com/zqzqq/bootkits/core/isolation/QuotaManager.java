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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配额管理器
 * 管理插件的资源配额配置和应用
 */
public class QuotaManager {
    
    private static final PluginLogger logger = PluginLogger.getLogger(QuotaManager.class);
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, ResourceQuota> pluginQuotas = new ConcurrentHashMap<>();
    private final ResourceQuota defaultQuota;
    private final ResourceQuota systemQuota;
    
    // 全局配额统计
    private volatile long totalMemoryAllocated;
    private volatile int totalThreadsAllocated;
    private volatile int totalConnectionsAllocated;
    
    public QuotaManager() {
        this(ResourceQuota.defaultQuota());
    }
    
    public QuotaManager(ResourceQuota defaultQuota) {
        this.defaultQuota = defaultQuota;
        this.systemQuota = ResourceQuota.newBuilder()
                .setMaxMemoryBytes(getTotalSystemMemory())
                .setMaxThreads(getTotalSystemThreads())
                .setMaxNetworkConnections(65536)
                .setMaxFileDescriptors(65536)
                .build();
    }
    
    /**
     * 为插件设置配额
     */
    public void setPluginQuota(String pluginId, ResourceQuota quota) {
        lock.writeLock().lock();
        try {
            // 验证配额设置
            validateQuotaAgainstSystem(quota);
            
            // 更新配额
            pluginQuotas.put(pluginId, quota);
            
            // 更新统计
            updateQuotaStatistics(pluginId, null, quota);
            
            logger.info("system", "设置插件配额", pluginId, "quota", quota);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取插件配额
     */
    public ResourceQuota getPluginQuota(String pluginId) {
        lock.readLock().lock();
        try {
            return pluginQuotas.getOrDefault(pluginId, defaultQuota);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 移除插件配额设置
     */
    public void removePluginQuota(String pluginId) {
        lock.writeLock().lock();
        try {
            ResourceQuota oldQuota = pluginQuotas.remove(pluginId);
            if (oldQuota != null) {
                // 更新统计
                updateQuotaStatistics(pluginId, oldQuota, null);
                logger.info("system", "移除插件配额", pluginId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 验证配额是否超限
     */
    public boolean validateQuota(ResourceQuota quota) {
        try {
            validateQuotaAgainstSystem(quota);
            return true;
        } catch (IllegalArgumentException e) {
            logger.warn("system", "配额验证失败", "reason", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证配额是否超限
     */
    public void validateQuotaOrThrow(ResourceQuota quota) {
        validateQuotaAgainstSystem(quota);
    }
    
    /**
     * 获取系统总内存
     */
    private long getTotalSystemMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory();
    }
    
    /**
     * 获取系统总线程数
     */
    private int getTotalSystemThreads() {
        return Runtime.getRuntime().availableProcessors() * 4; // 假设每个核心4个线程
    }
    
    /**
     * 验证配额是否超过系统限制
     */
    private void validateQuotaAgainstSystem(ResourceQuota quota) {
        long systemMemory = getTotalSystemMemory();
        int systemThreads = getTotalSystemThreads();
        
        if (quota.getMaxMemoryBytes() > systemMemory) {
            throw new IllegalArgumentException(
                String.format("内存配额 %dMB 超过系统总内存 %dMB", 
                    quota.getMaxMemoryBytes() / (1024 * 1024),
                    systemMemory / (1024 * 1024))
            );
        }
        
        if (quota.getMaxThreads() > systemThreads) {
            throw new IllegalArgumentException(
                String.format("线程配额 %d 超过系统总线程数 %d", 
                    quota.getMaxThreads(), systemThreads)
            );
        }
        
        if (quota.getMaxMemoryBytes() < 0 || quota.getMaxThreads() < 0 ||
            quota.getMaxFileDescriptors() < 0 || quota.getMaxNetworkConnections() < 0) {
            throw new IllegalArgumentException("配额值不能为负数");
        }
        
        if (quota.getMaxCpuPercent() > 100) {
            throw new IllegalArgumentException("CPU使用率配额不能超过100%");
        }
    }
    
    /**
     * 更新配额统计
     */
    private void updateQuotaStatistics(String pluginId, ResourceQuota oldQuota, ResourceQuota newQuota) {
        // 减少旧配额的统计
        if (oldQuota != null) {
            totalMemoryAllocated -= oldQuota.getMaxMemoryBytes();
            totalThreadsAllocated -= oldQuota.getMaxThreads();
            totalConnectionsAllocated -= oldQuota.getMaxNetworkConnections();
        }
        
        // 增加新配额的统计
        if (newQuota != null) {
            totalMemoryAllocated += newQuota.getMaxMemoryBytes();
            totalThreadsAllocated += newQuota.getMaxThreads();
            totalConnectionsAllocated += newQuota.getMaxNetworkConnections();
        }
    }
    
    /**
     * 获取所有插件配额
     */
    public Map<String, ResourceQuota> getAllPluginQuotas() {
        lock.readLock().lock();
        try {
            return new ConcurrentHashMap<>(pluginQuotas);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取默认配额
     */
    public ResourceQuota getDefaultQuota() {
        return defaultQuota;
    }
    
    /**
     * 获取系统配额
     */
    public ResourceQuota getSystemQuota() {
        return systemQuota;
    }
    
    /**
     * 获取总内存分配量
     */
    public long getTotalMemoryAllocated() {
        return totalMemoryAllocated;
    }
    
    /**
     * 获取总线程分配量
     */
    public int getTotalThreadsAllocated() {
        return totalThreadsAllocated;
    }
    
    /**
     * 获取总连接分配量
     */
    public int getTotalConnectionsAllocated() {
        return totalConnectionsAllocated;
    }
    
    /**
     * 检查系统资源使用情况
     */
    public SystemResourceUsage checkSystemResourceUsage() {
        long systemMemory = getTotalSystemMemory();
        int systemThreads = getTotalSystemThreads();
        
        double memoryUsagePercent = (double) totalMemoryAllocated / systemMemory * 100;
        double threadUsagePercent = (double) totalThreadsAllocated / systemThreads * 100;
        
        boolean isNearLimit = memoryUsagePercent > 80 || threadUsagePercent > 80;
        boolean isOverLimit = memoryUsagePercent > 95 || threadUsagePercent > 95;
        
        return new SystemResourceUsage(
            systemMemory, totalMemoryAllocated, memoryUsagePercent,
            systemThreads, totalThreadsAllocated, threadUsagePercent,
            totalConnectionsAllocated,
            isNearLimit, isOverLimit
        );
    }
    
    /**
     * 系统资源使用情况
     */
    public static class SystemResourceUsage {
        private final long totalSystemMemory;
        private final long allocatedMemory;
        private final double memoryUsagePercent;
        private final int totalSystemThreads;
        private final int allocatedThreads;
        private final double threadUsagePercent;
        private final int totalConnections;
        private final boolean nearLimit;
        private final boolean overLimit;
        
        public SystemResourceUsage(long totalSystemMemory, long allocatedMemory, double memoryUsagePercent,
                                 int totalSystemThreads, int allocatedThreads, double threadUsagePercent,
                                 int totalConnections, boolean nearLimit, boolean overLimit) {
            this.totalSystemMemory = totalSystemMemory;
            this.allocatedMemory = allocatedMemory;
            this.memoryUsagePercent = memoryUsagePercent;
            this.totalSystemThreads = totalSystemThreads;
            this.allocatedThreads = allocatedThreads;
            this.threadUsagePercent = threadUsagePercent;
            this.totalConnections = totalConnections;
            this.nearLimit = nearLimit;
            this.overLimit = overLimit;
        }
        
        // Getter方法
        public long getTotalSystemMemory() { return totalSystemMemory; }
        public long getAllocatedMemory() { return allocatedMemory; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public int getTotalSystemThreads() { return totalSystemThreads; }
        public int getAllocatedThreads() { return allocatedThreads; }
        public double getThreadUsagePercent() { return threadUsagePercent; }
        public int getTotalConnections() { return totalConnections; }
        public boolean isNearLimit() { return nearLimit; }
        public boolean isOverLimit() { return overLimit; }
        
        @Override
        public String toString() {
            return String.format("SystemResourceUsage{memory=%.1f%%(%dMB/%dMB), threads=%.1f%%(%d/%d), connections=%d, nearLimit=%s, overLimit=%s}",
                    memoryUsagePercent, allocatedMemory / (1024 * 1024), totalSystemMemory / (1024 * 1024),
                    threadUsagePercent, allocatedThreads, totalSystemThreads,
                    totalConnections, nearLimit, overLimit);
        }
    }
}