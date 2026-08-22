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


package com.zqzqq.bootkits.core.performance;

import java.time.LocalDateTime;

/**
 * 性能快照
 * 保存某一时刻的插件性能指标
 */
public class PerformanceSnapshot {
    
    private final String pluginId;
    private final long memoryUsage;
    private final int threadCount;
    private final double cpuUsage;
    private final int networkConnections;
    private final double memoryUsagePercent;
    private final double threadUsagePercent;
    private final double performanceScore;
    private final LocalDateTime timestamp;
    
    public PerformanceSnapshot(String pluginId, long memoryUsage, int threadCount,
                             double cpuUsage, int networkConnections,
                             double memoryUsagePercent, double threadUsagePercent,
                             double performanceScore, LocalDateTime timestamp) {
        this.pluginId = pluginId;
        this.memoryUsage = memoryUsage;
        this.threadCount = threadCount;
        this.cpuUsage = cpuUsage;
        this.networkConnections = networkConnections;
        this.memoryUsagePercent = memoryUsagePercent;
        this.threadUsagePercent = threadUsagePercent;
        this.performanceScore = performanceScore;
        this.timestamp = timestamp;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public long getMemoryUsage() {
        return memoryUsage;
    }
    
    public int getThreadCount() {
        return threadCount;
    }
    
    public double getCpuUsage() {
        return cpuUsage;
    }
    
    public int getNetworkConnections() {
        return networkConnections;
    }
    
    public double getMemoryUsagePercent() {
        return memoryUsagePercent;
    }
    
    public double getThreadUsagePercent() {
        return threadUsagePercent;
    }
    
    public double getPerformanceScore() {
        return performanceScore;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取内存使用量（MB）
     */
    public long getMemoryUsageMB() {
        return memoryUsage / (1024 * 1024);
    }
    
    /**
     * 获取CPU使用率百分比格式
     */
    public double getCpuUsagePercent() {
        return cpuUsage;
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceSnapshot{pluginId='%s', memory=%dMB, threads=%d, cpu=%.1f%%, connections=%d, score=%.2f, time=%s}",
                pluginId, getMemoryUsageMB(), threadCount, cpuUsage, networkConnections, performanceScore, timestamp);
    }
}