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
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 系统监控服务（完整功能，需要 PluginManager）
 * 注意：PluginManager 是延迟初始化的（在 ApplicationStartedEvent 时），
 * 因此使用 ObjectProvider 在运行时动态获取，而不是使用 @ConditionalOnBean
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class MonitorWebService {

    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final MeterRegistry meterRegistry;

    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;

    public MonitorWebService(ObjectProvider<PluginManager> pluginManagerProvider, MeterRegistry meterRegistry) {
        this.pluginManagerProvider = pluginManagerProvider;
        this.meterRegistry = meterRegistry;
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * 获取 PluginManager 实例
     */
    private PluginManager getPluginManager() {
        return pluginManagerProvider.getIfAvailable();
    }

    /**
     * 获取系统监控概览
     */
    public MonitorOverviewDTO getOverview() {
        // 插件统计
        PluginManager pluginManager = getPluginManager();
        MonitorOverviewDTO.PluginStatistics stats;
        if (pluginManager != null) {
            List<PluginInfo> plugins = pluginManager.getPlugins();
            int total = plugins.size();
            int started = (int) plugins.stream()
                    .filter(p -> p.getPluginState() != null &&
                                p.getPluginState().name().equals("STARTED"))
                    .count();
            int stopped = total - started;
            int failed = 0; // 可以从状态枚举中获取失败状态

            stats = MonitorOverviewDTO.PluginStatistics.builder()
                    .total(total)
                    .started(started)
                    .stopped(stopped)
                    .failed(failed)
                    .build();
        } else {
            // PluginManager 不可用时返回空统计
            stats = MonitorOverviewDTO.PluginStatistics.builder()
                    .total(0)
                    .started(0)
                    .stopped(0)
                    .failed(0)
                    .build();
        }
        
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
        List<PluginPerformanceDTO> pluginPerformances;
        if (pluginManager != null) {
            pluginPerformances = getPluginPerformances(pluginManager.getPlugins());
        } else {
            pluginPerformances = new ArrayList<>();
        }

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
        
        // 获取每个 CPU 核心的使用率
        List<Double> corePercents = getPerCoreCpuUsage(availableProcessors);
        
        return MonitorOverviewDTO.CpuInfo.builder()
                .systemPercent(systemCpuLoad >= 0 ? systemCpuLoad * 100 : 0)
                .processPercent(0)
                .availableProcessors(availableProcessors)
                .systemLoad(systemLoad >= 0 ? systemLoad : 0)
                .processCpuTime(processCpuTime)
                .corePercents(corePercents)
                .build();
    }
    
    /**
     * 获取每个 CPU 核心的使用率
     */
    private List<Double> getPerCoreCpuUsage(int availableProcessors) {
        List<Double> corePercents = new ArrayList<>();
        
        // 获取系统负载作为备用估算值
        double systemLoad = 0;
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                (com.sun.management.OperatingSystemMXBean) osMXBean;
            systemLoad = sunOsMXBean.getSystemLoadAverage();
        } else {
            systemLoad = osMXBean.getSystemLoadAverage();
        }
        
        // 尝试使用 OperatingSystemMXBean 获取每个核心的 CPU 负载
        try {
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                
                // 使用系统属性获取每个 CPU 的使用率
                String osName = System.getProperty("os.name").toLowerCase();
                
                if (osName.contains("linux")) {
                    // Linux 系统：读取 /proc/stat 获取每个 CPU 核心的使用情况
                    corePercents = getLinuxCpuUsage(availableProcessors);
                } else if (osName.contains("windows")) {
                    // Windows 系统：使用性能计数器近似计算
                    corePercents = getWindowsCpuUsage(availableProcessors);
                }
            }
        } catch (Exception e) {
            // 获取失败时返回空列表
            log.debug("获取每个 CPU 核心使用率失败", e);
        }
        
        // 如果上述方法失败，使用系统负载估算
        if (corePercents.isEmpty()) {
            for (int i = 0; i < availableProcessors; i++) {
                double estimatedLoad = systemLoad >= 0 ? Math.min(100, systemLoad * 10) : 0.0;
                corePercents.add(estimatedLoad);
            }
        }
        
        return corePercents;
    }
    
    /**
     * 获取 Linux 系统每个 CPU 核心的使用率
     */
    private List<Double> getLinuxCpuUsage(int availableProcessors) {
        List<Double> corePercents = new ArrayList<>();
        try {
            // 读取 /proc/stat 文件
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader("/proc/stat"));
            
            String line;
            int cpuCount = 0;
            
            while ((line = reader.readLine()) != null && cpuCount < availableProcessors) {
                if (line.startsWith("cpu") && line.length() > 3 && Character.isDigit(line.charAt(3))) {
                    // 解析 cpu0, cpu1, ... 行
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 8) {
                        // user, nice, system, idle, iowait, irq, softirq, steal, guest, guest_nice
                        long user = Long.parseLong(parts[1]);
                        long nice = Long.parseLong(parts[2]);
                        long system = Long.parseLong(parts[3]);
                        long idle = Long.parseLong(parts[4]);
                        long iowait = Long.parseLong(parts.length > 5 ? parts[5] : "0");
                        long irq = Long.parseLong(parts.length > 6 ? parts[6] : "0");
                        long softirq = Long.parseLong(parts.length > 7 ? parts[7] : "0");
                        
                        long total = user + nice + system + idle + iowait + irq + softirq;
                        long active = total - idle - iowait;
                        
                        double percent = total > 0 ? (double) active / total * 100 : 0;
                        corePercents.add(Math.round(percent * 100) / 100.0);
                        cpuCount++;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            log.debug("读取 Linux CPU 使用率失败", e);
        }
        return corePercents;
    }
    
    /**
     * 获取 Windows 系统每个 CPU 核心的使用率
     * 注意：Windows 下精确获取每个核心使用率比较复杂，这里使用近似方法
     */
    private List<Double> getWindowsCpuUsage(int availableProcessors) {
        List<Double> corePercents = new ArrayList<>();
        try {
            // 使用 PerformanceMXBean 获取每个处理器的使用率
            java.lang.management.OperatingSystemMXBean osBean = 
                ManagementFactory.getOperatingSystemMXBean();
            
            // 尝试使用 com.sun.management.OperatingSystemMXBean 的方法
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                // 获取系统 CPU 使用率作为参考
                double systemLoad = sunOsBean.getSystemCpuLoad();
                
                // 对于 Windows，我们尝试通过执行 wmic 命令获取每个核心的使用率
                ProcessBuilder pb = new ProcessBuilder(
                    "wmic", "CPU", "get", "LoadPercentage", "/value"
                );
                pb.redirectErrorStream(true);
                Process p = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                
                String line;
                int coreIndex = 0;
                while ((line = reader.readLine()) != null && coreIndex < availableProcessors) {
                    if (line.startsWith("LoadPercentage=")) {
                        String value = line.substring("LoadPercentage=".length());
                        try {
                            double load = Double.parseDouble(value);
                            corePercents.add(load);
                            coreIndex++;
                        } catch (NumberFormatException e) {
                            // 忽略
                        }
                    }
                }
                reader.close();
                p.waitFor();
                
                // 如果成功获取到每个核心的使用率，返回结果
                if (!corePercents.isEmpty()) {
                    return corePercents;
                }
            }
        } catch (Exception e) {
            log.debug("获取 Windows CPU 使用率失败", e);
        }
        
        // 如果无法获取，返回估算值
        return corePercents;
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

    /**
     * 获取线程池信息
     */
    public List<MonitorOverviewDTO.ThreadPoolInfo> getThreadPools() {
        List<MonitorOverviewDTO.ThreadPoolInfo> threadPools = new ArrayList<>();
        
        // 获取 JVM 线程池信息（通过 ManagementFactory）
        try {
            // 获取 ForkJoinPool 信息（Java 7+）
            addForkJoinPoolInfo(threadPools);
            
            // 获取通过 Executors 创建的常见线程池
            addCommonExecutorInfo(threadPools);
            
        } catch (Exception e) {
            log.debug("获取线程池信息失败", e);
        }
        
        return threadPools;
    }
    
    /**
     * 获取 ForkJoinPool 信息
     */
    private void addForkJoinPoolInfo(List<MonitorOverviewDTO.ThreadPoolInfo> threadPools) {
        try {
            // ForkJoinPool.commonPool() 可以获取公共 ForkJoinPool
            java.util.concurrent.ForkJoinPool commonPool = java.util.concurrent.ForkJoinPool.commonPool();
            if (commonPool != null) {
                MonitorOverviewDTO.ThreadPoolInfo info = MonitorOverviewDTO.ThreadPoolInfo.builder()
                        .poolName("ForkJoinPool-commonPool")
                        .corePoolSize(commonPool.getParallelism())
                        .maximumPoolSize(commonPool.getParallelism())
                        .activeCount(commonPool.getActiveThreadCount())
                        .poolSize(commonPool.getPoolSize())
                        .largestPoolSize(commonPool.getPoolSize()) // 使用当前池大小作为历史峰值
                        .queueSize(commonPool.getQueuedSubmissionCount())
                        .completedTaskCount(0) // ForkJoinPool 不直接提供此方法
                        .status(commonPool.isShutdown() ? "SHUTDOWN" : "RUNNING")
                        .build();
                threadPools.add(info);
            }
        } catch (Exception e) {
            log.debug("获取 ForkJoinPool 信息失败", e);
        }
    }
    
    /**
     * 获取常见线程池信息（基于 JVM 线程统计估算）
     */
    private void addCommonExecutorInfo(List<MonitorOverviewDTO.ThreadPoolInfo> threadPools) {
        // 通过 ThreadMXBean 获取线程统计信息
        int totalThreads = threadMXBean.getThreadCount();
        int peakThreads = threadMXBean.getPeakThreadCount();
        int daemonThreads = threadMXBean.getDaemonThreadCount();
        
        // JVM 内部线程（估算，每个线程消耗约 1MB 栈空间）
        int jvmInternalThreads = 20; // 估算 JVM 内部线程数量
        
        // 根据观察到的线程名称模式进行分类
        MonitorOverviewDTO.ThreadPoolInfo httpPool = MonitorOverviewDTO.ThreadPoolInfo.builder()
                .poolName("http-nio-pool")
                .corePoolSize(0)
                .maximumPoolSize(200) // Tomcat 默认最大线程数
                .activeCount(0)
                .poolSize(countThreadsByPattern("http-nio"))
                .largestPoolSize(countThreadsByPattern("http-nio"))
                .queueSize(0)
                .completedTaskCount(0)
                .status("RUNNING")
                .build();
        threadPools.add(httpPool);
        
        MonitorOverviewDTO.ThreadPoolInfo catalinaPool = MonitorOverviewDTO.ThreadPoolInfo.builder()
                .poolName("catalina-utility-pool")
                .corePoolSize(0)
                .maximumPoolSize(10)
                .activeCount(countThreadsByPattern("catalina-utility"))
                .poolSize(countThreadsByPattern("catalina-utility"))
                .largestPoolSize(countThreadsByPattern("catalina-utility"))
                .queueSize(0)
                .completedTaskCount(0)
                .status("RUNNING")
                .build();
        threadPools.add(catalinaPool);
        
        MonitorOverviewDTO.ThreadPoolInfo rmiPool = MonitorOverviewDTO.ThreadPoolInfo.builder()
                .poolName("rmi-scheduler-pool")
                .corePoolSize(0)
                .maximumPoolSize(10)
                .activeCount(countThreadsByPattern("RMI Scheduler"))
                .poolSize(countThreadsByPattern("RMI Scheduler"))
                .largestPoolSize(countThreadsByPattern("RMI Scheduler"))
                .queueSize(0)
                .completedTaskCount(0)
                .status("RUNNING")
                .build();
        threadPools.add(rmiPool);
        
        // 通用线程池统计
        int userThreads = totalThreads - daemonThreads - jvmInternalThreads;
        MonitorOverviewDTO.ThreadPoolInfo userPool = MonitorOverviewDTO.ThreadPoolInfo.builder()
                .poolName("user-thread-pool")
                .corePoolSize(0)
                .maximumPoolSize(totalThreads)
                .activeCount(userThreads)
                .poolSize(userThreads)
                .largestPoolSize(peakThreads)
                .queueSize(0)
                .completedTaskCount(threadMXBean.getTotalStartedThreadCount() - totalThreads)
                .status("RUNNING")
                .build();
        threadPools.add(userPool);
    }
    
    /**
     * 按线程名称模式统计线程数量
     */
    private int countThreadsByPattern(String pattern) {
        try {
            ThreadInfo[] threads = threadMXBean.dumpAllThreads(false, false);
            int count = 0;
            for (ThreadInfo info : threads) {
                if (info != null && info.getThreadName() != null && 
                    info.getThreadName().contains(pattern)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
