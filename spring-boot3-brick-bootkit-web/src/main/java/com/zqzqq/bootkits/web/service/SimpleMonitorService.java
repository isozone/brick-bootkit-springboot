package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简化的系统监控服务（当没有 PluginManager 时使用）
 * 
 * @author brick-bootkit
 */
@Service
@ConditionalOnMissingBean(MonitorWebService.class)
public class SimpleMonitorService {

    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    
    public SimpleMonitorService() {
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * 获取系统监控概览（简化版）
     */
    public MonitorOverviewDTO getOverview() {
        // 插件统计（无插件）
        MonitorOverviewDTO.PluginStatistics stats = MonitorOverviewDTO.PluginStatistics.builder()
                .total(0)
                .started(0)
                .stopped(0)
                .failed(0)
                .build();
        
        // JVM 内存
        MonitorOverviewDTO.MemoryInfo memoryInfo = getMemoryInfo();
        
        // CPU 信息
        MonitorOverviewDTO.CpuInfo cpuInfo = getCpuInfo();
        
        // 线程信息
        MonitorOverviewDTO.ThreadInfo threadInfo = getThreadInfo();
        
        // 系统信息
        MonitorOverviewDTO.SystemInfo systemInfo = getSystemInfo();
        
        return MonitorOverviewDTO.builder()
                .pluginStatistics(stats)
                .memory(memoryInfo)
                .cpu(cpuInfo)
                .threads(threadInfo)
                .system(systemInfo)
                .pluginPerformances(new ArrayList<>())
                .build();
    }

    /**
     * 获取 JVM 内存信息
     */
    public MonitorOverviewDTO.MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long heapMax = runtime.maxMemory();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapUsedPercent = Math.round((double) heapUsed / heapMax * 100);
        
        // 非堆内存
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        // 系统内存
        MonitorOverviewDTO.SystemMemory systemMemory = getSystemMemory();
        
        Map<String, MonitorOverviewDTO.MemoryPool> memoryPools = new HashMap<>();
        
        // 堆内存池
        memoryPools.put("HEAP", MonitorOverviewDTO.MemoryPool.builder()
                .name("Heap Memory")
                .used(heapUsed)
                .max(heapMax)
                .build());
        
        // Metaspace
        memoryPools.put("METASPACE", MonitorOverviewDTO.MemoryPool.builder()
                .name("Metaspace")
                .used(memoryMXBean.getNonHeapMemoryUsage().getUsed())
                .max(memoryMXBean.getNonHeapMemoryUsage().getMax() == -1 ? 
                     memoryMXBean.getNonHeapMemoryUsage().getUsed() : 
                     memoryMXBean.getNonHeapMemoryUsage().getMax())
                .build());
        
        // Code Cache
        memoryPools.put("CODE_CACHE", MonitorOverviewDTO.MemoryPool.builder()
                .name("Code Cache")
                .used(getMemoryPoolUsage("Code Cache"))
                .max(getMemoryPoolMax("Code Cache"))
                .build());
        
        // Compressed Class Space
        memoryPools.put("COMPRESSED_CLASS_SPACE", MonitorOverviewDTO.MemoryPool.builder()
                .name("Compressed Class Space")
                .used(getMemoryPoolUsage("Compressed Class Space"))
                .max(getMemoryPoolMax("Compressed Class Space"))
                .build());
        
        return MonitorOverviewDTO.MemoryInfo.builder()
                .systemTotal(systemMemory.getTotal())
                .systemUsed(systemMemory.getUsed())
                .systemFree(systemMemory.getFree())
                .systemUsedPercent(systemMemory.getUsedPercent())
                .heapUsed(heapUsed)
                .heapMax(heapMax)
                .heapUsedPercent(heapUsedPercent)
                .nonHeapUsed(nonHeapUsed)
                .memoryPools(memoryPools)
                .build();
    }
    
    /**
     * 获取系统内存信息
     */
    private MonitorOverviewDTO.SystemMemory getSystemMemory() {
        try {
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                
                long total = sunOsMXBean.getTotalMemorySize();
                long free = sunOsMXBean.getFreeMemorySize();
                long used = total - free;
                double usedPercent = Math.round((double) used / total * 100);
                
                return new MonitorOverviewDTO.SystemMemory(total, used, free, usedPercent);
            }
        } catch (Exception e) {
            // 无法获取系统内存，使用 JVM 可用内存作为参考
        }
        
        // 回退：使用 Runtime 获取可用内存
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedPercent = Math.round((double) usedMemory / maxMemory * 100);
        
        return new MonitorOverviewDTO.SystemMemory(maxMemory, usedMemory, freeMemory, usedPercent);
    }
    
    /**
     * 获取指定内存池的使用量
     */
    private long getMemoryPoolUsage(String poolName) {
        try {
            for (java.lang.management.MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getName().equals(poolName)) {
                    return pool.getUsage().getUsed();
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return 0;
    }
    
    /**
     * 获取指定内存池的最大值
     */
    private long getMemoryPoolMax(String poolName) {
        try {
            for (java.lang.management.MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getName().equals(poolName)) {
                    long max = pool.getUsage().getMax();
                    return max > 0 ? max : pool.getUsage().getUsed();
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return 0;
    }

    /**
     * 获取 CPU 信息
     */
    public MonitorOverviewDTO.CpuInfo getCpuInfo() {
        int availableProcessors = osMXBean.getAvailableProcessors();
        
        // 获取系统 CPU 负载
        double systemLoad = 0;
        double systemCpuLoad = 0;
        
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                (com.sun.management.OperatingSystemMXBean) osMXBean;
            // 系统 CPU 负载 (0.0 - 1.0)
            systemCpuLoad = sunOsMXBean.getSystemCpuLoad();
            // 系统负载平均值
            systemLoad = sunOsMXBean.getSystemLoadAverage();
        } else {
            // 回退到标准 MXBean
            systemLoad = osMXBean.getSystemLoadAverage();
        }
        
        // 进程 CPU 时间（纳秒转换为秒）
        long processCpuTime = 0;
        try {
            java.lang.management.OperatingSystemMXBean.class.getMethod("getProcessCpuTime");
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getProcessCpuTime");
            processCpuTime = (Long) method.invoke(osMXBean);
        } catch (Exception e) {
            // 方法不可用
        }
        
        return MonitorOverviewDTO.CpuInfo.builder()
                .systemPercent(systemCpuLoad >= 0 ? systemCpuLoad * 100 : 0)
                .processPercent(0) // 不再使用估算值
                .availableProcessors(availableProcessors)
                .systemLoad(systemLoad >= 0 ? systemLoad : 0)
                .processCpuTime(processCpuTime)
                .build();
    }

    /**
     * 获取线程信息
     */
    public MonitorOverviewDTO.ThreadInfo getThreadInfo() {
        return MonitorOverviewDTO.ThreadInfo.builder()
                .total(threadMXBean.getThreadCount())
                .daemon(threadMXBean.getDaemonThreadCount())
                .peak(threadMXBean.getPeakThreadCount())
                .build();
    }

    /**
     * 获取系统信息
     */
    public MonitorOverviewDTO.SystemInfo getSystemInfo() {
        Instant startTime = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
        Duration uptime = Duration.between(startTime, Instant.now());
        
        return MonitorOverviewDTO.SystemInfo.builder()
                .osName(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .javaVersion(System.getProperty("java.version"))
                .uptime(uptime.toMillis())
                .build();
    }

    /**
     * 获取历史监控数据（简化实现）
     */
    public Map<String, Object> getHistoryData(String type, long since) {
        Map<String, Object> history = new HashMap<>();
        history.put("type", type);
        history.put("since", since);
        history.put("dataPoints", new ArrayList<>());
        return history;
    }
}
