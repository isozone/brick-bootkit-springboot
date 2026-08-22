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
 * 性能基线
 * 定义插件的正常性能基线指标
 */
public class PerformanceBaseline {
    
    private final String pluginId;
    private final double averageMemoryUsage;
    private final double averageThreadCount;
    private final double averageCpuUsage;
    private final double averageNetworkConnections;
    private final double baselinePerformanceScore;
    private final LocalDateTime baselineTime;
    private final String description;
    
    public PerformanceBaseline(String pluginId, double averageMemoryUsage, double averageThreadCount,
                             double averageCpuUsage, double averageNetworkConnections,
                             double baselinePerformanceScore, LocalDateTime baselineTime,
                             String description) {
        this.pluginId = pluginId;
        this.averageMemoryUsage = averageMemoryUsage;
        this.averageThreadCount = averageThreadCount;
        this.averageCpuUsage = averageCpuUsage;
        this.averageNetworkConnections = averageNetworkConnections;
        this.baselinePerformanceScore = baselinePerformanceScore;
        this.baselineTime = baselineTime;
        this.description = description;
    }
    
    public static BaselineBuilder newBuilder() {
        return new BaselineBuilder();
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public double getAverageMemoryUsage() {
        return averageMemoryUsage;
    }
    
    public double getAverageThreadCount() {
        return averageThreadCount;
    }
    
    public double getAverageCpuUsage() {
        return averageCpuUsage;
    }
    
    public double getAverageNetworkConnections() {
        return averageNetworkConnections;
    }
    
    public double getBaselinePerformanceScore() {
        return baselinePerformanceScore;
    }
    
    public LocalDateTime getBaselineTime() {
        return baselineTime;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 基线构建器
     */
    public static class BaselineBuilder {
        private String pluginId;
        private double averageMemoryUsage;
        private double averageThreadCount;
        private double averageCpuUsage;
        private double averageNetworkConnections;
        private double baselinePerformanceScore;
        private LocalDateTime baselineTime = LocalDateTime.now();
        private String description = "";
        
        public BaselineBuilder setPluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }
        
        public BaselineBuilder setAverageMemoryUsage(double averageMemoryUsage) {
            this.averageMemoryUsage = averageMemoryUsage;
            return this;
        }
        
        public BaselineBuilder setAverageThreadCount(double averageThreadCount) {
            this.averageThreadCount = averageThreadCount;
            return this;
        }
        
        public BaselineBuilder setAverageCpuUsage(double averageCpuUsage) {
            this.averageCpuUsage = averageCpuUsage;
            return this;
        }
        
        public BaselineBuilder setAverageNetworkConnections(double averageNetworkConnections) {
            this.averageNetworkConnections = averageNetworkConnections;
            return this;
        }
        
        public BaselineBuilder setBaselinePerformanceScore(double baselinePerformanceScore) {
            this.baselinePerformanceScore = baselinePerformanceScore;
            return this;
        }
        
        public BaselineBuilder setBaselineTime(LocalDateTime baselineTime) {
            this.baselineTime = baselineTime;
            return this;
        }
        
        public BaselineBuilder setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public PerformanceBaseline build() {
            if (pluginId == null || pluginId.trim().isEmpty()) {
                throw new IllegalArgumentException("插件ID不能为空");
            }
            if (baselinePerformanceScore < 0 || baselinePerformanceScore > 100) {
                throw new IllegalArgumentException("性能评分必须在0-100之间");
            }
            
            return new PerformanceBaseline(
                pluginId, averageMemoryUsage, averageThreadCount,
                averageCpuUsage, averageNetworkConnections,
                baselinePerformanceScore, baselineTime, description
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceBaseline{pluginId='%s', memory=%.2fMB, threads=%.2f, cpu=%.2f%%, connections=%.2f, score=%.2f}",
                pluginId, averageMemoryUsage, averageThreadCount, averageCpuUsage, averageNetworkConnections, baselinePerformanceScore);
    }
}