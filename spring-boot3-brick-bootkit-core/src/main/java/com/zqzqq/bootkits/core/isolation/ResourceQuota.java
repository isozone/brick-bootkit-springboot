package com.zqzqq.bootkits.core.isolation;

/**
 * 资源配额配置
 * 定义插件可以使用的各种资源配额限制
 */
public class ResourceQuota {
    
    // 内存配额 (字节)
    private final long maxMemoryBytes;
    
    // 线程数配额
    private final int maxThreads;
    
    // 文件描述符配额
    private final int maxFileDescriptors;
    
    // CPU使用率配额 (百分比)
    private final double maxCpuPercent;
    
    // 网络连接配额
    private final int maxNetworkConnections;
    
    // 磁盘I/O配额 (字节/秒)
    private final long maxDiskIOBytesPerSecond;
    
    private ResourceQuota(Builder builder) {
        this.maxMemoryBytes = builder.maxMemoryBytes;
        this.maxThreads = builder.maxThreads;
        this.maxFileDescriptors = builder.maxFileDescriptors;
        this.maxCpuPercent = builder.maxCpuPercent;
        this.maxNetworkConnections = builder.maxNetworkConnections;
        this.maxDiskIOBytesPerSecond = builder.maxDiskIOBytesPerSecond;
    }
    
    /**
     * 创建默认配额
     */
    public static ResourceQuota defaultQuota() {
        return new Builder()
                .setMaxMemoryBytes(256 * 1024 * 1024) // 256MB
                .setMaxThreads(20)
                .setMaxFileDescriptors(1000)
                .setMaxCpuPercent(50.0)
                .setMaxNetworkConnections(100)
                .setMaxDiskIOBytesPerSecond(10 * 1024 * 1024) // 10MB/s
                .build();
    }
    
    /**
     * 创建严格配额
     */
    public static ResourceQuota strictQuota() {
        return new Builder()
                .setMaxMemoryBytes(128 * 1024 * 1024) // 128MB
                .setMaxThreads(10)
                .setMaxFileDescriptors(500)
                .setMaxCpuPercent(25.0)
                .setMaxNetworkConnections(50)
                .setMaxDiskIOBytesPerSecond(5 * 1024 * 1024) // 5MB/s
                .build();
    }
    
    /**
     * 创建宽松配额
     */
    public static ResourceQuota relaxedQuota() {
        return new Builder()
                .setMaxMemoryBytes(512 * 1024 * 1024) // 512MB
                .setMaxThreads(50)
                .setMaxFileDescriptors(2000)
                .setMaxCpuPercent(80.0)
                .setMaxNetworkConnections(500)
                .setMaxDiskIOBytesPerSecond(50 * 1024 * 1024) // 50MB/s
                .build();
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    // Getter方法
    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public int getMaxFileDescriptors() {
        return maxFileDescriptors;
    }
    
    public double getMaxCpuPercent() {
        return maxCpuPercent;
    }
    
    public int getMaxNetworkConnections() {
        return maxNetworkConnections;
    }
    
    public long getMaxDiskIOBytesPerSecond() {
        return maxDiskIOBytesPerSecond;
    }
    
    /**
     * 检查是否使用了配额检查
     */
    public boolean hasMemoryLimit() {
        return maxMemoryBytes > 0;
    }
    
    public boolean hasThreadLimit() {
        return maxThreads > 0;
    }
    
    public boolean hasFileDescriptorLimit() {
        return maxFileDescriptors > 0;
    }
    
    public boolean hasCpuLimit() {
        return maxCpuPercent > 0;
    }
    
    public boolean hasNetworkConnectionLimit() {
        return maxNetworkConnections > 0;
    }
    
    public boolean hasDiskIOLimit() {
        return maxDiskIOBytesPerSecond > 0;
    }
    
    @Override
    public String toString() {
        return String.format("ResourceQuota{memory=%dMB, threads=%d, fileDescriptors=%d, cpu=%.1f%%, network=%d, diskIO=%dMB/s}",
                maxMemoryBytes / (1024 * 1024),
                maxThreads,
                maxFileDescriptors,
                maxCpuPercent,
                maxNetworkConnections,
                maxDiskIOBytesPerSecond / (1024 * 1024));
    }
    
    /**
     * 资源配额构建器
     */
    public static class Builder {
        private long maxMemoryBytes = 0;
        private int maxThreads = 0;
        private int maxFileDescriptors = 0;
        private double maxCpuPercent = 0;
        private int maxNetworkConnections = 0;
        private long maxDiskIOBytesPerSecond = 0;
        
        /**
         * 设置最大内存配额
         */
        public Builder setMaxMemoryBytes(long bytes) {
            if (bytes < 0) {
                throw new IllegalArgumentException("内存配额不能为负数");
            }
            this.maxMemoryBytes = bytes;
            return this;
        }
        
        /**
         * 设置最大线程配额
         */
        public Builder setMaxThreads(int threads) {
            if (threads < 0) {
                throw new IllegalArgumentException("线程配额不能为负数");
            }
            this.maxThreads = threads;
            return this;
        }
        
        /**
         * 设置最大文件描述符配额
         */
        public Builder setMaxFileDescriptors(int fileDescriptors) {
            if (fileDescriptors < 0) {
                throw new IllegalArgumentException("文件描述符配额不能为负数");
            }
            this.maxFileDescriptors = fileDescriptors;
            return this;
        }
        
        /**
         * 设置最大CPU使用率配额
         */
        public Builder setMaxCpuPercent(double percent) {
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("CPU使用率配额必须在0-100之间");
            }
            this.maxCpuPercent = percent;
            return this;
        }
        
        /**
         * 设置最大网络连接配额
         */
        public Builder setMaxNetworkConnections(int connections) {
            if (connections < 0) {
                throw new IllegalArgumentException("网络连接配额不能为负数");
            }
            this.maxNetworkConnections = connections;
            return this;
        }
        
        /**
         * 设置最大磁盘I/O配额
         */
        public Builder setMaxDiskIOBytesPerSecond(long bytesPerSecond) {
            if (bytesPerSecond < 0) {
                throw new IllegalArgumentException("磁盘I/O配额不能为负数");
            }
            this.maxDiskIOBytesPerSecond = bytesPerSecond;
            return this;
        }
        
        /**
         * 构建ResourceQuota实例
         */
        public ResourceQuota build() {
            return new ResourceQuota(this);
        }
    }
}
