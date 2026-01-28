package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO;
import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO.PluginPerformanceDTO;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.ThreadDetailDTO;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统监控服务（完整功能，需要 PluginManager）
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@ConditionalOnBean(PluginManager.class)
public class MonitorWebService {

    private final PluginManager pluginManager;
    private final MeterRegistry meterRegistry;
    
    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    
    public MonitorWebService(PluginManager pluginManager, MeterRegistry meterRegistry) {
        this.pluginManager = pluginManager;
        this.meterRegistry = meterRegistry;
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * 获取系统监控概览
     */
    public MonitorOverviewDTO getOverview() {
        // 插件统计
        List<PluginInfo> plugins = pluginManager.getPlugins();
        int total = plugins.size();
        int started = (int) plugins.stream()
                .filter(p -> p.getPluginState() != null && 
                            p.getPluginState().name().equals("STARTED"))
                .count();
        int stopped = total - started;
        int failed = 0; // 可以从状态枚举中获取失败状态
        
        MonitorOverviewDTO.PluginStatistics stats = MonitorOverviewDTO.PluginStatistics.builder()
                .total(total)
                .started(started)
                .stopped(stopped)
                .failed(failed)
                .build();
        
        // JVM 内存
        MonitorOverviewDTO.MemoryInfo memoryInfo = getMemoryInfo();
        
        // CPU 信息
        MonitorOverviewDTO.CpuInfo cpuInfo = getCpuInfo();
        
        // 线程信息
        MonitorOverviewDTO.ThreadInfo threadInfo = getThreadInfo();
        
        // 系统信息
        MonitorOverviewDTO.SystemInfo systemInfo = getSystemInfo();
        
        // GC 收集器信息
        List<MonitorOverviewDTO.GCInfo> gcCollectors = getGCCollectors();
        
        // 插件性能列表
        List<PluginPerformanceDTO> pluginPerformances = getPluginPerformances(plugins);
        
        return MonitorOverviewDTO.builder()
                .pluginStatistics(stats)
                .memory(memoryInfo)
                .cpu(cpuInfo)
                .threads(threadInfo)
                .system(systemInfo)
                .gcCollectors(gcCollectors)
                .pluginPerformances(pluginPerformances)
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
            // 无法获取系统内存
        }
        
        // 回退
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
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getProcessCpuTime");
            processCpuTime = (Long) method.invoke(osMXBean);
        } catch (Exception e) {
            // 方法不可用
        }
        
        return MonitorOverviewDTO.CpuInfo.builder()
                .systemPercent(systemCpuLoad >= 0 ? systemCpuLoad * 100 : 0)
                .processPercent(0)
                .availableProcessors(availableProcessors)
                .systemLoad(systemLoad >= 0 ? systemLoad : 0)
                .processCpuTime(processCpuTime)
                .build();
    }

    /**
     * 获取线程信息（包含状态统计）
     */
    public MonitorOverviewDTO.ThreadInfo getThreadInfo() {
        int total = threadMXBean.getThreadCount();
        int daemon = threadMXBean.getDaemonThreadCount();
        int peak = threadMXBean.getPeakThreadCount();

        // 获取所有线程信息以统计各状态数量
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        int newCount = 0;
        int runnableCount = 0;
        int blockedCount = 0;
        int waitingCount = 0;
        int timedWaitingCount = 0;
        int terminatedCount = 0;

        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;
            Thread.State state = info.getThreadState();
            switch (state) {
                case NEW:
                    newCount++;
                    break;
                case RUNNABLE:
                    runnableCount++;
                    break;
                case BLOCKED:
                    blockedCount++;
                    break;
                case WAITING:
                    waitingCount++;
                    break;
                case TIMED_WAITING:
                    timedWaitingCount++;
                    break;
                case TERMINATED:
                    terminatedCount++;
                    break;
            }
        }

        return MonitorOverviewDTO.ThreadInfo.builder()
                .total(total)
                .daemon(daemon)
                .peak(peak)
                .started(threadMXBean.getTotalStartedThreadCount())
                .newCount(newCount)
                .runnableCount(runnableCount)
                .blockedCount(blockedCount)
                .waitingCount(waitingCount)
                .timedWaitingCount(timedWaitingCount)
                .terminatedCount(terminatedCount)
                .build();
    }

    /**
     * 获取线程详细信息（包含线程列表和死锁检测）
     */
    public ThreadDetailDTO getThreadDetail() {
        MonitorOverviewDTO.ThreadInfo threadInfo = getThreadInfo();

        // 获取所有线程信息
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        List<ThreadDetailDTO.ThreadDetail> threads = new ArrayList<>();

        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;

            ThreadDetailDTO.ThreadDetail detail = ThreadDetailDTO.ThreadDetail.builder()
                    .threadId(info.getThreadId())
                    .threadName(info.getThreadName())
                    .priority(info.getPriority())
                    .threadState(info.getThreadState().name())
                    .daemon(info.isDaemon())
                    .cpuTime(threadMXBean.getThreadCpuTime(info.getThreadId()))
                    .blockedCount(info.getBlockedCount())
                    .blockedTime(info.getBlockedTime())
                    .waitedCount(info.getWaitedCount())
                    .waitedTime(info.getWaitedTime())
                    .lockName(info.getLockName())
                    .lockOwnerId(info.getLockOwnerId())
                    .lockOwnerName(info.getLockOwnerName())
                    .build();

            threads.add(detail);
        }

        // 检测死锁线程
        List<ThreadDetailDTO.DeadlockedThread> deadlockedThreads = new ArrayList<>();
        long[] deadlockedIds = threadMXBean.findDeadlockedThreads();
        if (deadlockedIds != null && deadlockedIds.length > 0) {
            ThreadInfo[] deadlockedInfos = threadMXBean.getThreadInfo(deadlockedIds, true, true);
            for (ThreadInfo info : deadlockedInfos) {
                if (info == null) continue;

                ThreadDetailDTO.DeadlockedThread deadlocked = ThreadDetailDTO.DeadlockedThread.builder()
                        .threadId(info.getThreadId())
                        .threadName(info.getThreadName())
                        .threadState(info.getThreadState().name())
                        .priority(info.getPriority())
                        .daemon(info.isDaemon())
                        .cpuTime(threadMXBean.getThreadCpuTime(info.getThreadId()))
                        .lockName(info.getLockName())
                        .lockWaitingName(info.getLockName())
                        .lockOwnerId(info.getLockOwnerId())
                        .lockOwnerName(info.getLockOwnerName())
                        .stackTrace(Arrays.stream(info.getStackTrace())
                                .map(StackTraceElement::toString)
                                .toArray(String[]::new))
                        .build();

                deadlockedThreads.add(deadlocked);
            }
        }

        return ThreadDetailDTO.builder()
                .threadInfo(threadInfo)
                .threads(threads)
                .deadlockedThreads(deadlockedThreads)
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
                .osArch(System.getProperty("os.arch"))
                .javaVersion(System.getProperty("java.version"))
                .uptime(uptime.toMillis())
                .build();
    }

    /**
     * 获取 GC 收集器信息
     */
    public List<MonitorOverviewDTO.GCInfo> getGCCollectors() {
        List<MonitorOverviewDTO.GCInfo> gcInfos = new ArrayList<>();
        
        for (java.lang.management.GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            MonitorOverviewDTO.GCInfo gcInfo = MonitorOverviewDTO.GCInfo.builder()
                    .name(gc.getName())
                    .count(gc.getCollectionCount())
                    .time(gc.getCollectionTime())
                    .maxTime(0)
                    .build();
            gcInfos.add(gcInfo);
        }
        
        return gcInfos;
    }

    /**
     * 获取插件性能数据
     */
    public List<PluginPerformanceDTO> getPluginPerformances(List<PluginInfo> plugins) {
        return plugins.stream()
                .map(this::convertToPerformanceDTO)
                .collect(Collectors.toList());
    }

    private PluginPerformanceDTO convertToPerformanceDTO(PluginInfo plugin) {
        // 估算内存使用
        long memoryUsage = 0;
        if (plugin.getPluginState() != null && 
            plugin.getPluginState().name().equals("STARTED")) {
            // 运行中的插件估算使用一定内存
            memoryUsage = 10 * 1024 * 1024; // 10MB 估算
        }
        
        return PluginPerformanceDTO.builder()
                .pluginId(plugin.getPluginId())
                .pluginName(plugin.getPluginDescriptor() != null ? 
                           plugin.getPluginDescriptor().getName() : plugin.getPluginId())
                .state(plugin.getPluginState() != null ? plugin.getPluginState().name() : "UNKNOWN")
                .memoryUsage(memoryUsage)
                .threadCount(0)
                .startTime(plugin.getStartTime())
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
