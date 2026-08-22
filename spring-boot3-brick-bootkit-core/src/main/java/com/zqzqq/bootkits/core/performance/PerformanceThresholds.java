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

/**
 * 性能阈值配置
 * 定义各种性能指标的警告和临界阈值
 */
public class PerformanceThresholds {
    
    private final double memoryWarningThreshold;
    private final double memoryCriticalThreshold;
    private final double threadWarningThreshold;
    private final double threadCriticalThreshold;
    private final double cpuWarningThreshold;
    private final double cpuCriticalThreshold;
    private final int networkWarningThreshold;
    private final int networkCriticalThreshold;
    private final double memoryGrowthRateThreshold;
    private final double threadGrowthRateThreshold;
    private final double efficiencyCriticalThreshold;
    
    private PerformanceThresholds(Builder builder) {
        this.memoryWarningThreshold = builder.memoryWarningThreshold;
        this.memoryCriticalThreshold = builder.memoryCriticalThreshold;
        this.threadWarningThreshold = builder.threadWarningThreshold;
        this.threadCriticalThreshold = builder.threadCriticalThreshold;
        this.cpuWarningThreshold = builder.cpuWarningThreshold;
        this.cpuCriticalThreshold = builder.cpuCriticalThreshold;
        this.networkWarningThreshold = builder.networkWarningThreshold;
        this.networkCriticalThreshold = builder.networkCriticalThreshold;
        this.memoryGrowthRateThreshold = builder.memoryGrowthRateThreshold;
        this.threadGrowthRateThreshold = builder.threadGrowthRateThreshold;
        this.efficiencyCriticalThreshold = builder.efficiencyCriticalThreshold;
    }
    
    /**
     * 创建默认阈值配置
     */
    public static PerformanceThresholds defaultThresholds() {
        return new Builder()
                .setMemoryWarningThreshold(60.0)
                .setMemoryCriticalThreshold(80.0)
                .setThreadWarningThreshold(60.0)
                .setThreadCriticalThreshold(80.0)
                .setCpuWarningThreshold(70.0)
                .setCpuCriticalThreshold(90.0)
                .setNetworkWarningThreshold(50)
                .setNetworkCriticalThreshold(100)
                .setMemoryGrowthRateThreshold(5.0) // 5% per check
                .setThreadGrowthRateThreshold(2.0) // 2 threads per check
                .setEfficiencyCriticalThreshold(30.0)
                .build();
    }
    
    /**
     * 创建严格阈值配置
     */
    public static PerformanceThresholds strictThresholds() {
        return new Builder()
                .setMemoryWarningThreshold(40.0)
                .setMemoryCriticalThreshold(60.0)
                .setThreadWarningThreshold(40.0)
                .setThreadCriticalThreshold(60.0)
                .setCpuWarningThreshold(50.0)
                .setCpuCriticalThreshold(70.0)
                .setNetworkWarningThreshold(30)
                .setNetworkCriticalThreshold(60)
                .setMemoryGrowthRateThreshold(2.0) // 2% per check
                .setThreadGrowthRateThreshold(1.0) // 1 thread per check
                .setEfficiencyCriticalThreshold(50.0)
                .build();
    }
    
    /**
     * 创建宽松阈值配置
     */
    public static PerformanceThresholds relaxedThresholds() {
        return new Builder()
                .setMemoryWarningThreshold(80.0)
                .setMemoryCriticalThreshold(95.0)
                .setThreadWarningThreshold(80.0)
                .setThreadCriticalThreshold(95.0)
                .setCpuWarningThreshold(85.0)
                .setCpuCriticalThreshold(95.0)
                .setNetworkWarningThreshold(100)
                .setNetworkCriticalThreshold(200)
                .setMemoryGrowthRateThreshold(10.0) // 10% per check
                .setThreadGrowthRateThreshold(5.0) // 5 threads per check
                .setEfficiencyCriticalThreshold(20.0)
                .build();
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    // Getter方法
    public double getMemoryWarningThreshold() { return memoryWarningThreshold; }
    public double getMemoryCriticalThreshold() { return memoryCriticalThreshold; }
    public double getThreadWarningThreshold() { return threadWarningThreshold; }
    public double getThreadCriticalThreshold() { return threadCriticalThreshold; }
    public double getCpuWarningThreshold() { return cpuWarningThreshold; }
    public double getCpuCriticalThreshold() { return cpuCriticalThreshold; }
    public int getNetworkWarningThreshold() { return networkWarningThreshold; }
    public int getNetworkCriticalThreshold() { return networkCriticalThreshold; }
    public double getMemoryGrowthRateThreshold() { return memoryGrowthRateThreshold; }
    public double getThreadGrowthRateThreshold() { return threadGrowthRateThreshold; }
    public double getEfficiencyCriticalThreshold() { return efficiencyCriticalThreshold; }
    
    /**
     * 检查内存使用是否超过警告阈值
     */
    public boolean isMemoryWarning(double usagePercent) {
        return usagePercent >= memoryWarningThreshold;
    }
    
    /**
     * 检查内存使用是否超过临界阈值
     */
    public boolean isMemoryCritical(double usagePercent) {
        return usagePercent >= memoryCriticalThreshold;
    }
    
    /**
     * 检查线程使用是否超过警告阈值
     */
    public boolean isThreadWarning(double usagePercent) {
        return usagePercent >= threadWarningThreshold;
    }
    
    /**
     * 检查线程使用是否超过临界阈值
     */
    public boolean isThreadCritical(double usagePercent) {
        return usagePercent >= threadCriticalThreshold;
    }
    
    /**
     * 检查CPU使用是否超过警告阈值
     */
    public boolean isCpuWarning(double usagePercent) {
        return usagePercent >= cpuWarningThreshold;
    }
    
    /**
     * 检查CPU使用是否超过临界阈值
     */
    public boolean isCpuCritical(double usagePercent) {
        return usagePercent >= cpuCriticalThreshold;
    }
    
    /**
     * 检查网络连接是否超过警告阈值
     */
    public boolean isNetworkWarning(int connections) {
        return connections >= networkWarningThreshold;
    }
    
    /**
     * 检查网络连接是否超过临界阈值
     */
    public boolean isNetworkCritical(int connections) {
        return connections >= networkCriticalThreshold;
    }
    
    /**
     * 检查资源效率是否过低
     */
    public boolean isEfficiencyCritical(double efficiency) {
        return efficiency <= efficiencyCriticalThreshold;
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceThresholds{memory=%s%%/%s%%, threads=%s%%/%s%%, cpu=%s%%/%s%%, network=%d/%d, growth=%.1f/%.1f, efficiency=%.1f%%}",
                memoryWarningThreshold, memoryCriticalThreshold,
                threadWarningThreshold, threadCriticalThreshold,
                cpuWarningThreshold, cpuCriticalThreshold,
                networkWarningThreshold, networkCriticalThreshold,
                memoryGrowthRateThreshold, threadGrowthRateThreshold,
                efficiencyCriticalThreshold);
    }
    
    /**
     * 性能阈值构建器
     */
    public static class Builder {
        private double memoryWarningThreshold = 60.0;
        private double memoryCriticalThreshold = 80.0;
        private double threadWarningThreshold = 60.0;
        private double threadCriticalThreshold = 80.0;
        private double cpuWarningThreshold = 70.0;
        private double cpuCriticalThreshold = 90.0;
        private int networkWarningThreshold = 50;
        private int networkCriticalThreshold = 100;
        private double memoryGrowthRateThreshold = 5.0;
        private double threadGrowthRateThreshold = 2.0;
        private double efficiencyCriticalThreshold = 30.0;
        
        /**
         * 设置内存警告阈值
         */
        public Builder setMemoryWarningThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("内存警告阈值必须在0-100之间");
            }
            this.memoryWarningThreshold = threshold;
            return this;
        }
        
        /**
         * 设置内存临界阈值
         */
        public Builder setMemoryCriticalThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("内存临界阈值必须在0-100之间");
            }
            this.memoryCriticalThreshold = threshold;
            return this;
        }
        
        /**
         * 设置线程警告阈值
         */
        public Builder setThreadWarningThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("线程警告阈值必须在0-100之间");
            }
            this.threadWarningThreshold = threshold;
            return this;
        }
        
        /**
         * 设置线程临界阈值
         */
        public Builder setThreadCriticalThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("线程临界阈值必须在0-100之间");
            }
            this.threadCriticalThreshold = threshold;
            return this;
        }
        
        /**
         * 设置CPU警告阈值
         */
        public Builder setCpuWarningThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("CPU警告阈值必须在0-100之间");
            }
            this.cpuWarningThreshold = threshold;
            return this;
        }
        
        /**
         * 设置CPU临界阈值
         */
        public Builder setCpuCriticalThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("CPU临界阈值必须在0-100之间");
            }
            this.cpuCriticalThreshold = threshold;
            return this;
        }
        
        /**
         * 设置网络连接警告阈值
         */
        public Builder setNetworkWarningThreshold(int threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException("网络连接警告阈值不能为负数");
            }
            this.networkWarningThreshold = threshold;
            return this;
        }
        
        /**
         * 设置网络连接临界阈值
         */
        public Builder setNetworkCriticalThreshold(int threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException("网络连接临界阈值不能为负数");
            }
            this.networkCriticalThreshold = threshold;
            return this;
        }
        
        /**
         * 设置内存增长率阈值
         */
        public Builder setMemoryGrowthRateThreshold(double threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException("内存增长率阈值不能为负数");
            }
            this.memoryGrowthRateThreshold = threshold;
            return this;
        }
        
        /**
         * 设置线程增长率阈值
         */
        public Builder setThreadGrowthRateThreshold(double threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException("线程增长率阈值不能为负数");
            }
            this.threadGrowthRateThreshold = threshold;
            return this;
        }
        
        /**
         * 设置效率临界阈值
         */
        public Builder setEfficiencyCriticalThreshold(double threshold) {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("效率临界阈值必须在0-100之间");
            }
            this.efficiencyCriticalThreshold = threshold;
            return this;
        }
        
        /**
         * 构建PerformanceThresholds实例
         */
        public PerformanceThresholds build() {
            // 验证阈值逻辑
            if (memoryWarningThreshold >= memoryCriticalThreshold) {
                throw new IllegalArgumentException("内存警告阈值必须小于临界阈值");
            }
            if (threadWarningThreshold >= threadCriticalThreshold) {
                throw new IllegalArgumentException("线程警告阈值必须小于临界阈值");
            }
            if (cpuWarningThreshold >= cpuCriticalThreshold) {
                throw new IllegalArgumentException("CPU警告阈值必须小于临界阈值");
            }
            if (networkWarningThreshold >= networkCriticalThreshold) {
                throw new IllegalArgumentException("网络连接警告阈值必须小于临界阈值");
            }
            
            return new PerformanceThresholds(this);
        }
    }
}
