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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * 插件资源使用情况追踪
 * 实时记录插件的资源使用状态
 */
public class PluginResourceUsage {
    
    private final String pluginId;
    private final ResourceQuota quota;
    
    // 当前内存使用 (字节)
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);
    
    // 当前线程数
    private final AtomicInteger currentThreadCount = new AtomicInteger(0);
    
    // 当前文件描述符使用数
    private final AtomicInteger currentFileDescriptors = new AtomicInteger(0);
    
    // 当前CPU使用率
    private final AtomicLong currentCpuUsage = new AtomicLong(0);
    
    // 当前网络连接数
    private final AtomicInteger currentNetworkConnections = new AtomicInteger(0);
    
    // 当前磁盘I/O使用
    private final AtomicLong currentDiskIO = new AtomicLong(0);
    
    // 统计信息
    private volatile LocalDateTime lastUpdateTime;
    private volatile long peakMemoryUsage;
    private volatile int peakThreadCount;
    private volatile long totalMemoryAllocated;
    private volatile long totalMemoryDeallocated;
    
    public PluginResourceUsage(String pluginId, ResourceQuota quota) {
        this.pluginId = pluginId;
        this.quota = quota;
        this.lastUpdateTime = LocalDateTime.now();
        this.peakMemoryUsage = 0;
        this.peakThreadCount = 0;
        this.totalMemoryAllocated = 0;
        this.totalMemoryDeallocated = 0;
    }
    
    /**
     * 更新内存使用
     */
    public void updateMemoryUsage(long bytes) {
        currentMemoryUsage.set(bytes);
        lastUpdateTime = LocalDateTime.now();
        
        // 更新峰值
        if (bytes > peakMemoryUsage) {
            peakMemoryUsage = bytes;
        }
        
        if (bytes >= 0) {
            totalMemoryAllocated += bytes;
        } else {
            totalMemoryDeallocated += Math.abs(bytes);
        }
    }
    
    /**
     * 增加内存使用
     */
    public void addMemoryUsage(long bytes) {
        if (bytes > 0) {
            long newValue = currentMemoryUsage.addAndGet(bytes);
            lastUpdateTime = LocalDateTime.now();
            
            if (newValue > peakMemoryUsage) {
                peakMemoryUsage = newValue;
            }
            totalMemoryAllocated += bytes;
        }
    }
    
    /**
     * 减少内存使用
     */
    public void subtractMemoryUsage(long bytes) {
        if (bytes > 0) {
            long newValue = currentMemoryUsage.addAndGet(-bytes);
            lastUpdateTime = LocalDateTime.now();
            totalMemoryDeallocated += bytes;
        }
    }
    
    /**
     * 增加线程数
     */
    public void incrementThreadCount() {
        int newValue = currentThreadCount.incrementAndGet();
        lastUpdateTime = LocalDateTime.now();
        
        if (newValue > peakThreadCount) {
            peakThreadCount = newValue;
        }
    }
    
    /**
     * 减少线程数
     */
    public void decrementThreadCount() {
        int newValue = currentThreadCount.decrementAndGet();
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新线程数
     */
    public void setThreadCount(int count) {
        currentThreadCount.set(count);
        lastUpdateTime = LocalDateTime.now();
        
        if (count > peakThreadCount) {
            peakThreadCount = count;
        }
    }
    
    /**
     * 更新文件描述符使用
     */
    public void updateFileDescriptors(int count) {
        currentFileDescriptors.set(count);
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 增加文件描述符使用
     */
    public void addFileDescriptor() {
        currentFileDescriptors.incrementAndGet();
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 减少文件描述符使用
     */
    public void removeFileDescriptor() {
        currentFileDescriptors.decrementAndGet();
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新CPU使用率
     */
    public void updateCpuUsage(long percent) {
        currentCpuUsage.set(percent);
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新网络连接数
     */
    public void updateNetworkConnections(int count) {
        currentNetworkConnections.set(count);
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 增加网络连接
     */
    public void addNetworkConnection() {
        currentNetworkConnections.incrementAndGet();
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 减少网络连接
     */
    public void removeNetworkConnection() {
        currentNetworkConnections.decrementAndGet();
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新磁盘I/O使用
     */
    public void updateDiskIO(long bytes) {
        currentDiskIO.set(bytes);
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 重置使用统计
     */
    public void reset() {
        currentMemoryUsage.set(0);
        currentThreadCount.set(0);
        currentFileDescriptors.set(0);
        currentCpuUsage.set(0);
        currentNetworkConnections.set(0);
        currentDiskIO.set(0);
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 获取配额
     */
    public ResourceQuota getQuota() {
        return quota;
    }
    
    /**
     * 获取当前内存使用
     */
    public long getCurrentMemoryUsage() {
        return currentMemoryUsage.get();
    }
    
    /**
     * 获取当前线程数
     */
    public int getCurrentThreadCount() {
        return currentThreadCount.get();
    }
    
    /**
     * 获取当前文件描述符使用数
     */
    public int getCurrentFileDescriptors() {
        return currentFileDescriptors.get();
    }
    
    /**
     * 获取当前CPU使用率
     */
    public long getCurrentCpuUsage() {
        return currentCpuUsage.get();
    }
    
    /**
     * 获取当前网络连接数
     */
    public int getCurrentNetworkConnections() {
        return currentNetworkConnections.get();
    }
    
    /**
     * 获取当前磁盘I/O使用
     */
    public long getCurrentDiskIO() {
        return currentDiskIO.get();
    }
    
    /**
     * 获取峰值内存使用
     */
    public long getPeakMemoryUsage() {
        return peakMemoryUsage;
    }
    
    /**
     * 获取峰值线程数
     */
    public int getPeakThreadCount() {
        return peakThreadCount;
    }
    
    /**
     * 获取总内存分配量
     */
    public long getTotalMemoryAllocated() {
        return totalMemoryAllocated;
    }
    
    /**
     * 获取总内存释放量
     */
    public long getTotalMemoryDeallocated() {
        return totalMemoryDeallocated;
    }
    
    /**
     * 获取最后更新时间
     */
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * 获取内存使用率 (百分比)
     */
    public double getMemoryUsagePercent() {
        if (!quota.hasMemoryLimit() || quota.getMaxMemoryBytes() == 0) {
            return 0;
        }
        return (double) currentMemoryUsage.get() / quota.getMaxMemoryBytes() * 100;
    }
    
    /**
     * 获取线程使用率 (百分比)
     */
    public double getThreadUsagePercent() {
        if (!quota.hasThreadLimit() || quota.getMaxThreads() == 0) {
            return 0;
        }
        return (double) currentThreadCount.get() / quota.getMaxThreads() * 100;
    }
    
    /**
     * 获取CPU使用率 (百分比)
     */
    public double getCpuUsagePercent() {
        if (!quota.hasCpuLimit() || quota.getMaxCpuPercent() == 0) {
            return 0;
        }
        return (double) currentCpuUsage.get() / quota.getMaxCpuPercent() * 100;
    }
    
    /**
     * 检查是否接近配额限制
     */
    public boolean isNearQuotaLimit(double thresholdPercent) {
        double memoryPercent = getMemoryUsagePercent();
        double threadPercent = getThreadUsagePercent();
        double cpuPercent = getCpuUsagePercent();
        
        return memoryPercent >= thresholdPercent || 
               threadPercent >= thresholdPercent || 
               cpuPercent >= thresholdPercent;
    }
    
    /**
     * 检查是否达到配额限制
     */
    public boolean isQuotaExceeded() {
        return currentMemoryUsage.get() > quota.getMaxMemoryBytes() ||
               currentThreadCount.get() > quota.getMaxThreads() ||
               currentCpuUsage.get() > quota.getMaxCpuPercent() ||
               currentNetworkConnections.get() > quota.getMaxNetworkConnections() ||
               currentDiskIO.get() > quota.getMaxDiskIOBytesPerSecond();
    }
    
    @Override
    public String toString() {
        return String.format("PluginResourceUsage{pluginId='%s', memory=%dMB(%.1f%%), threads=%d(%.1f%%), cpu=%.1f%%, connections=%d, fds=%d}",
                pluginId,
                currentMemoryUsage.get() / (1024 * 1024),
                getMemoryUsagePercent(),
                currentThreadCount.get(),
                getThreadUsagePercent(),
                getCpuUsagePercent(),
                currentNetworkConnections.get(),
                currentFileDescriptors.get());
    }
}