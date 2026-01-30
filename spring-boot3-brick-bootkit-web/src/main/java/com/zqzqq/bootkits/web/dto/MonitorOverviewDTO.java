package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 系统监控概览 DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorOverviewDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 插件统计信息
     */
    private PluginStatistics pluginStatistics;
    
    /**
     * JVM 内存信息
     */
    private MemoryInfo memory;
    
    /**
     * CPU 信息
     */
    private CpuInfo cpu;
    
    /**
     * 线程信息
     */
    private ThreadInfo threads;
    
    /**
     * 系统信息
     */
    private SystemInfo system;
    
    /**
     * 插件性能列表
     */
    private List<PluginPerformanceDTO> pluginPerformances;
    
    /**
     * GC 收集器信息列表
     */
    private List<GCInfo> gcCollectors;
    
    /**
     * 插件统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PluginStatistics implements Serializable {
        private static final long serialVersionUID = 1L;
        private int total;
        private int started;
        private int stopped;
        private int failed;
    }
    
    /**
     * 内存信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        // ===== 系统内存 =====
        /** 系统总内存 */
        private long systemTotal;
        /** 系统已使用内存 */
        private long systemUsed;
        /** 系统空闲内存 */
        private long systemFree;
        /** 系统内存使用率 */
        private double systemUsedPercent;
        
        // ===== JVM 堆内存 =====
        private long heapUsed;
        private long heapMax;
        private long heapUsedPercent;
        
        // ===== JVM 非堆内存 =====
        private long nonHeapUsed;
        
        // ===== 内存池详情 =====
        private Map<String, MemoryPool> memoryPools;
    }
    
    /**
     * 内存池信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryPool implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private long used;
        private long max;
    }
    
    /**
     * 系统内存信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMemory implements Serializable {
        private static final long serialVersionUID = 1L;
        private long total;
        private long used;
        private long free;
        private double usedPercent;
    }
    
    /**
     * CPU 信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 系统 CPU 使用率 (0-100%) */
        private double systemPercent;
        /** 进程 CPU 使用率 (0-100%) */
        private double processPercent;
        /** 可用处理器数量 */
        private int availableProcessors;
        /** 系统负载平均值 */
        private double systemLoad;
        /** 进程 CPU 时间（纳秒） */
        private long processCpuTime;
        /** 每个 CPU 核心的使用率列表 (0-100%) */
        private List<Double> corePercents;
    }
    
    /**
     * 线程信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 当前活动线程总数 */
        private int total;

        /** 守护线程数 */
        private int daemon;

        /** 历史峰值线程数 */
        private int peak;

        /** 自启动以来累计启动的线程总数 */
        private long started;

        /** NEW 状态线程数 */
        private int newCount;

        /** RUNNABLE 状态线程数 */
        private int runnableCount;

        /** BLOCKED 状态线程数 */
        private int blockedCount;

        /** WAITING 状态线程数 */
        private int waitingCount;

        /** TIMED_WAITING 状态线程数 */
        private int timedWaitingCount;

        /** TERMINATED 状态线程数 */
        private int terminatedCount;
    }
    
    /**
     * 系统信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String osName;
        private String osVersion;
        private String osArch;
        private String javaVersion;
        private long uptime;
    }
    
    /**
     * 插件性能 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PluginPerformanceDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String pluginId;
        private String pluginName;
        private String state;
        private long memoryUsage;
        private int threadCount;
        private long startTime;
    }
    
    /**
     * GC 收集器信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GCInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 收集器名称 */
        private String name;
        /** 收集次数 */
        private long count;
        /** 总耗时（毫秒） */
        private long time;
        /** 最大单次耗时（毫秒） */
        private long maxTime;
    }
    
    /**
     * 线程池信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadPoolInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 线程池名称 */
        private String poolName;
        /** 核心线程数 */
        private int corePoolSize;
        /** 最大线程数 */
        private int maximumPoolSize;
        /** 当前活跃线程数 */
        private int activeCount;
        /** 任务队列大小 */
        private int queueSize;
        /** 已完成任务数 */
        private long completedTaskCount;
        /** 当前线程数 */
        private int poolSize;
        /** 最大线程数历史峰值 */
        private int largestPoolSize;
        /** 线程池状态 */
        private String status;
    }
}
