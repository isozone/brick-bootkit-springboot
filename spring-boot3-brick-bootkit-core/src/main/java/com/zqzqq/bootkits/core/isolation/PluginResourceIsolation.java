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

import com.zqzqq.bootkits.core.exception.ResourceQuotaExceededException;
import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件资源隔离管理器
 * 提供插件之间的资源隔离和配额控制
 */
public class PluginResourceIsolation {
    
    private static final PluginLogger logger = PluginLogger.getLogger(PluginResourceIsolation.class);
    
    private final MemoryMXBean memoryMXBean;
    private final QuotaManager quotaManager;
    private final PluginResourceMonitor resourceMonitor;
    
    // 插件资源使用追踪
    private final ConcurrentHashMap<String, PluginResourceUsage> pluginResourceUsage = new ConcurrentHashMap<>();
    
    // 系统总资源
    private volatile long totalSystemMemory;
    private volatile int totalSystemThreads;
    
    public PluginResourceIsolation(QuotaManager quotaManager, PluginResourceMonitor resourceMonitor) {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.quotaManager = quotaManager;
        this.resourceMonitor = resourceMonitor;
        initializeSystemResources();
    }
    
    /**
     * 初始化系统资源
     */
    private void initializeSystemResources() {
        Runtime runtime = Runtime.getRuntime();
        totalSystemMemory = runtime.maxMemory();
        totalSystemThreads = runtime.availableProcessors() * 2; // 假设每个核心2个线程
        
        logger.info("system", "初始化系统资源", 
                   "totalMemory", totalSystemMemory / (1024 * 1024) + "MB",
                   "totalThreads", totalSystemThreads);
    }
    
    /**
     * 为插件分配资源隔离
     */
    public void allocateResourceIsolation(String pluginId, ResourceQuota quota) {
        logger.info("system", String.format("为插件 %s 分配资源隔离 (配额: %s)", pluginId, quota.toString()));
        
        // 检查配额是否合理
        validateQuota(quota);
        
        // 创建资源使用跟踪
        PluginResourceUsage usage = new PluginResourceUsage(pluginId, quota);
        pluginResourceUsage.put(pluginId, usage);
        
        // 启动资源监控
        resourceMonitor.startMonitoring(pluginId, usage);
        
        logger.info("system", "插件资源隔离分配成功", pluginId);
    }
    
    /**
     * 释放插件资源隔离
     */
    public void releaseResourceIsolation(String pluginId) {
        logger.info("system", "释放插件资源隔离", pluginId);
        
        PluginResourceUsage usage = pluginResourceUsage.remove(pluginId);
        if (usage != null) {
            usage.reset();
            resourceMonitor.stopMonitoring(pluginId);
            logger.info("system", "插件资源隔离释放成功", pluginId);
        }
    }
    
    /**
     * 检查内存使用是否超限
     */
    public void checkMemoryUsage(String pluginId) throws ResourceQuotaExceededException {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        if (usage == null) {
            return; // 没有配额限制
        }
        
        long currentMemory = usage.getCurrentMemoryUsage();
        long memoryQuota = usage.getQuota().getMaxMemoryBytes();
        
        if (currentMemory > memoryQuota) {
            logger.error("system", String.format("插件 %s 内存使用超限: 已使用 %dMB, 限制 %dMB",
                    pluginId, currentMemory / (1024 * 1024), memoryQuota / (1024 * 1024)));
            
            throw new ResourceQuotaExceededException(
                "PLUGIN_MEMORY_QUOTA_EXCEEDED",
                String.format("插件 %s 内存使用超限: 已使用 %dMB, 限制 %dMB",
                    pluginId, currentMemory / (1024 * 1024), memoryQuota / (1024 * 1024))
            );
        }
    }
    
    /**
     * 检查线程数是否超限
     */
    public void checkThreadUsage(String pluginId) throws ResourceQuotaExceededException {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        if (usage == null) {
            return; // 没有配额限制
        }
        
        int currentThreads = usage.getCurrentThreadCount();
        int threadQuota = usage.getQuota().getMaxThreads();
        
        if (currentThreads > threadQuota) {
            logger.error("system", String.format("插件 %s 线程数使用超限: 当前 %d, 限制 %d",
                    pluginId, currentThreads, threadQuota));
            
            throw new ResourceQuotaExceededException(
                "PLUGIN_THREAD_QUOTA_EXCEEDED",
                String.format("插件 %s 线程数使用超限: 当前 %d, 限制 %d",
                    pluginId, currentThreads, threadQuota)
            );
        }
    }
    
    /**
     * 记录插件内存使用
     */
    public void recordMemoryUsage(String pluginId, long bytes) {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        if (usage != null) {
            usage.updateMemoryUsage(bytes);
        }
    }
    
    /**
     * 记录插件线程创建
     */
    public void recordThreadCreation(String pluginId) {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        if (usage != null) {
            usage.incrementThreadCount();
        }
    }
    
    /**
     * 记录插件线程销毁
     */
    public void recordThreadDestruction(String pluginId) {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        if (usage != null) {
            usage.decrementThreadCount();
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
    public ConcurrentHashMap<String, PluginResourceUsage> getAllResourceUsage() {
        return new ConcurrentHashMap<>(pluginResourceUsage);
    }
    
    /**
     * 验证配额设置
     */
    private void validateQuota(ResourceQuota quota) {
        if (quota.getMaxMemoryBytes() > totalSystemMemory) {
            throw new IllegalArgumentException("插件内存配额不能超过系统总内存");
        }
        
        if (quota.getMaxThreads() > totalSystemThreads) {
            throw new IllegalArgumentException("插件线程配额不能超过系统总线程数");
        }
        
        if (quota.getMaxFileDescriptors() < 0 || quota.getMaxFileDescriptors() > 65536) {
            throw new IllegalArgumentException("文件描述符配额必须在0-65536之间");
        }
    }
    
    /**
     * 获取系统总资源信息
     */
    public SystemResourceInfo getSystemResourceInfo() {
        return new SystemResourceInfo(totalSystemMemory, totalSystemThreads, 
                                    getTotalUsedMemory(), getTotalUsedThreads());
    }
    
    private long getTotalUsedMemory() {
        return pluginResourceUsage.values().stream()
                .mapToLong(PluginResourceUsage::getCurrentMemoryUsage)
                .sum();
    }
    
    private int getTotalUsedThreads() {
        return pluginResourceUsage.values().stream()
                .mapToInt(PluginResourceUsage::getCurrentThreadCount)
                .sum();
    }
    
    /**
     * 关闭资源隔离管理器
     */
    public void shutdown() {
        logger.info("system", "关闭插件资源隔离管理器");
        
        // 停止所有监控
        for (String pluginId : pluginResourceUsage.keySet()) {
            resourceMonitor.stopMonitoring(pluginId);
        }
        
        pluginResourceUsage.clear();
        
        logger.info("system", "插件资源隔离管理器关闭完成");
    }
    
    /**
     * 获取资源监控器
     */
    public PluginResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }
    
    /**
     * 初始化插件（用于PluginManager集成）
     */
    public void initializePlugin(String pluginId, ClassLoader classLoader) {
        // 为新安装的插件分配默认配额
        ResourceQuota defaultQuota = ResourceQuota.defaultQuota();
        allocateResourceIsolation(pluginId, defaultQuota);
        
        logger.debug("system", "resource-isolation", String.format("初始化插件资源隔离: %s (默认配额: %s)", pluginId, defaultQuota.toString()));
    }
    
    /**
     * 清理插件（用于PluginManager集成）
     */
    public void cleanupPlugin(String pluginId) {
        releaseResourceIsolation(pluginId);
    }
    
    /**
     * 设置插件配额（用于PluginManager集成）
     */
    public void setPluginQuota(String pluginId, ResourceQuota quota) {
        // 先释放现有配额
        PluginResourceUsage oldUsage = pluginResourceUsage.remove(pluginId);
        if (oldUsage != null) {
            resourceMonitor.stopMonitoring(pluginId);
        }
        
        // 分配新配额
        allocateResourceIsolation(pluginId, quota);
    }
    
    /**
     * 获取插件配额（用于PluginManager集成）
     */
    public ResourceQuota getPluginQuota(String pluginId) {
        PluginResourceUsage usage = pluginResourceUsage.get(pluginId);
        return usage != null ? usage.getQuota() : null;
    }
    
    /**
     * 隔离插件（用于PluginManager集成）
     */
    public void isolatePlugin(String pluginId) {
        logger.warn("system", "隔离插件", pluginId);
        
        // 停止插件的资源监控
        resourceMonitor.stopMonitoring(pluginId);
        
        // 可以在这里添加额外的隔离逻辑，比如网络隔离等
        logger.info("system", "插件隔离完成", pluginId);
    }
}
