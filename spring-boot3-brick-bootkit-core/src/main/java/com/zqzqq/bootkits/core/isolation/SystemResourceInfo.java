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

/**
 * 系统资源信息
 * 提供系统总资源的详细信息
 */
public class SystemResourceInfo {
    
    private final long totalMemory;
    private final int totalThreads;
    private final long usedMemory;
    private final int usedThreads;
    
    public SystemResourceInfo(long totalMemory, int totalThreads, long usedMemory, int usedThreads) {
        this.totalMemory = totalMemory;
        this.totalThreads = totalThreads;
        this.usedMemory = usedMemory;
        this.usedThreads = usedThreads;
    }
    
    /**
     * 获取总内存大小 (字节)
     */
    public long getTotalMemory() {
        return totalMemory;
    }
    
    /**
     * 获取总线程数
     */
    public int getTotalThreads() {
        return totalThreads;
    }
    
    /**
     * 获取已使用内存 (字节)
     */
    public long getUsedMemory() {
        return usedMemory;
    }
    
    /**
     * 获取已使用线程数
     */
    public int getUsedThreads() {
        return usedThreads;
    }
    
    /**
     * 获取可用内存 (字节)
     */
    public long getAvailableMemory() {
        return totalMemory - usedMemory;
    }
    
    /**
     * 获取可用线程数
     */
    public int getAvailableThreads() {
        return totalThreads - usedThreads;
    }
    
    /**
     * 获取内存使用率 (百分比)
     */
    public double getMemoryUsagePercent() {
        if (totalMemory == 0) {
            return 0;
        }
        return (double) usedMemory / totalMemory * 100;
    }
    
    /**
     * 获取线程使用率 (百分比)
     */
    public double getThreadUsagePercent() {
        if (totalThreads == 0) {
            return 0;
        }
        return (double) usedThreads / totalThreads * 100;
    }
    
    /**
     * 检查内存使用是否接近限制
     */
    public boolean isMemoryNearLimit(double thresholdPercent) {
        return getMemoryUsagePercent() >= thresholdPercent;
    }
    
    /**
     * 检查线程使用是否接近限制
     */
    public boolean isThreadNearLimit(double thresholdPercent) {
        return getThreadUsagePercent() >= thresholdPercent;
    }
    
    /**
     * 检查内存使用是否超过限制
     */
    public boolean isMemoryOverLimit(double limitPercent) {
        return getMemoryUsagePercent() > limitPercent;
    }
    
    /**
     * 检查线程使用是否超过限制
     */
    public boolean isThreadOverLimit(double limitPercent) {
        return getThreadUsagePercent() > limitPercent;
    }
    
    /**
     * 检查系统资源是否接近限制
     */
    public boolean isNearLimit(double thresholdPercent) {
        return isMemoryNearLimit(thresholdPercent) || isThreadNearLimit(thresholdPercent);
    }
    
    /**
     * 检查系统资源是否超过限制
     */
    public boolean isOverLimit(double limitPercent) {
        return isMemoryOverLimit(limitPercent) || isThreadOverLimit(limitPercent);
    }
    
    /**
     * 获取内存使用率状态
     */
    public ResourceUsageStatus getMemoryUsageStatus() {
        double usage = getMemoryUsagePercent();
        if (usage < 50) {
            return ResourceUsageStatus.NORMAL;
        } else if (usage < 80) {
            return ResourceUsageStatus.WARNING;
        } else if (usage < 95) {
            return ResourceUsageStatus.CRITICAL;
        } else {
            return ResourceUsageStatus.DANGER;
        }
    }
    
    /**
     * 获取线程使用率状态
     */
    public ResourceUsageStatus getThreadUsageStatus() {
        double usage = getThreadUsagePercent();
        if (usage < 50) {
            return ResourceUsageStatus.NORMAL;
        } else if (usage < 80) {
            return ResourceUsageStatus.WARNING;
        } else if (usage < 95) {
            return ResourceUsageStatus.CRITICAL;
        } else {
            return ResourceUsageStatus.DANGER;
        }
    }
    
    /**
     * 获取整体系统状态
     */
    public ResourceUsageStatus getOverallSystemStatus() {
        ResourceUsageStatus memoryStatus = getMemoryUsageStatus();
        ResourceUsageStatus threadStatus = getThreadUsageStatus();
        
        // 取更严重的状态
        if (memoryStatus == ResourceUsageStatus.DANGER || threadStatus == ResourceUsageStatus.DANGER) {
            return ResourceUsageStatus.DANGER;
        } else if (memoryStatus == ResourceUsageStatus.CRITICAL || threadStatus == ResourceUsageStatus.CRITICAL) {
            return ResourceUsageStatus.CRITICAL;
        } else if (memoryStatus == ResourceUsageStatus.WARNING || threadStatus == ResourceUsageStatus.WARNING) {
            return ResourceUsageStatus.WARNING;
        } else {
            return ResourceUsageStatus.NORMAL;
        }
    }
    
    @Override
    public String toString() {
        return String.format("SystemResourceInfo{totalMemory=%dMB, usedMemory=%dMB(%.1f%%), totalThreads=%d, usedThreads=%d(%.1f%%)}",
                totalMemory / (1024 * 1024),
                usedMemory / (1024 * 1024),
                getMemoryUsagePercent(),
                totalThreads,
                usedThreads,
                getThreadUsagePercent());
    }
    
    /**
     * 资源使用状态枚举
     */
    public enum ResourceUsageStatus {
        NORMAL("正常"),
        WARNING("警告"),
        CRITICAL("严重"),
        DANGER("危险");
        
        private final String description;
        
        ResourceUsageStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isNormal() {
            return this == NORMAL;
        }
        
        public boolean isWarning() {
            return this == WARNING;
        }
        
        public boolean isCritical() {
            return this == CRITICAL;
        }
        
        public boolean isDanger() {
            return this == DANGER;
        }
        
        @Override
        public String toString() {
            return name() + "(" + description + ")";
        }
    }
}