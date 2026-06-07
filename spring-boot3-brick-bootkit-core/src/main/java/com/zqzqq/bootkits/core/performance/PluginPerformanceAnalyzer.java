package com.zqzqq.bootkits.core.performance;

import com.zqzqq.bootkits.core.isolation.PluginResourceUsage;
import com.zqzqq.bootkits.core.isolation.ResourceQuota;
import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * 插件性能分析器
 * 分析插件的性能指标，识别性能问题和瓶颈
 */
public class PluginPerformanceAnalyzer {
    
    private static final PluginLogger logger = PluginLogger.getLogger(PluginPerformanceAnalyzer.class);
    
    // 性能历史数据缓存
    private final Map<String, List<PerformanceSnapshot>> performanceHistory = new ConcurrentHashMap<>();
    
    // 性能基线数据
    private final Map<String, PerformanceBaseline> performanceBaselines = new ConcurrentHashMap<>();
    
    // 性能阈值配置
    private final PerformanceThresholds thresholds;
    
    public PluginPerformanceAnalyzer(PerformanceThresholds thresholds) {
        this.thresholds = thresholds;
    }
    
    /**
     * 分析插件性能
     */
    public PerformanceAnalysis analyzePlugin(String pluginId, PluginResourceUsage usage) {
        logger.debug("system", "开始分析插件性能", pluginId);
        
        List<PerformanceIssue> issues = new ArrayList<>();
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // 获取历史数据
        List<PerformanceSnapshot> history = performanceHistory.getOrDefault(pluginId, new ArrayList<>());
        
        // 内存性能分析
        analyzeMemoryPerformance(pluginId, usage, issues, recommendations, history);
        
        // 线程性能分析
        analyzeThreadPerformance(pluginId, usage, issues, recommendations, history);
        
        // CPU性能分析
        analyzeCpuPerformance(pluginId, usage, issues, recommendations, history);
        
        // 网络性能分析
        analyzeNetworkPerformance(pluginId, usage, issues, recommendations, history);
        
        // 资源利用率分析
        analyzeResourceUtilization(pluginId, usage, issues, recommendations, history);
        
        // 性能趋势分析
        analyzePerformanceTrends(pluginId, issues, recommendations, history);
        
        // 生成综合性能评分
        double performanceScore = calculatePerformanceScore(issues);
        
        PerformanceAnalysis analysis = new PerformanceAnalysis(
            pluginId,
            performanceScore,
            issues,
            recommendations,
            LocalDateTime.now()
        );
        
        // 保存性能快照
        savePerformanceSnapshot(pluginId, usage, analysis);
        
        logger.info("system", "插件性能分析完成", 
                   pluginId, "score", String.format("%.2f", performanceScore),
                   "issues", issues.size(), "recommendations", recommendations.size());
        
        return analysis;
    }
    
    /**
     * 内存性能分析
     */
    private void analyzeMemoryPerformance(String pluginId, PluginResourceUsage usage, 
                                        List<PerformanceIssue> issues, 
                                        List<OptimizationRecommendation> recommendations,
                                        List<PerformanceSnapshot> history) {
        
        double memoryUsagePercent = usage.getMemoryUsagePercent();
        long currentMemory = usage.getCurrentMemoryUsage();
        long peakMemory = usage.getPeakMemoryUsage();
        long totalAllocated = usage.getTotalMemoryAllocated();
        
        // 内存使用率过高
        if (memoryUsagePercent > thresholds.getMemoryCriticalThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.CRITICAL,
                "MEMORY_USAGE_HIGH",
                String.format("内存使用率过高: %.1f%%", memoryUsagePercent),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.HIGH,
                "MEMORY_OPTIMIZATION",
                "内存使用优化",
                "建议检查内存泄漏，优化数据结构和算法，考虑使用对象池",
                OptimizationRecommendation.Category.MEMORY,
                Arrays.asList("检查内存泄漏", "优化数据结构", "使用对象池")
            ));
        } else if (memoryUsagePercent > thresholds.getMemoryWarningThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "MEMORY_USAGE_ELEVATED",
                String.format("内存使用率偏高: %.1f%%", memoryUsagePercent),
                pluginId
            ));
        }
        
        // 内存增长趋势异常
        if (history.size() >= 3) {
            analyzeMemoryGrowthTrend(pluginId, history, issues, recommendations);
        }
        
        // 峰值内存异常
        if (peakMemory > currentMemory * 1.5) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "MEMORY_SPIKE_DETECTED",
                String.format("检测到内存峰值: %dMB (当前: %dMB)", 
                    peakMemory / (1024 * 1024), currentMemory / (1024 * 1024)),
                pluginId
            ));
        }
    }
    
    /**
     * 线程性能分析
     */
    private void analyzeThreadPerformance(String pluginId, PluginResourceUsage usage,
                                        List<PerformanceIssue> issues,
                                        List<OptimizationRecommendation> recommendations,
                                        List<PerformanceSnapshot> history) {
        
        double threadUsagePercent = usage.getThreadUsagePercent();
        int currentThreads = usage.getCurrentThreadCount();
        int peakThreads = usage.getPeakThreadCount();
        
        // 线程使用率过高
        if (threadUsagePercent > thresholds.getThreadCriticalThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.CRITICAL,
                "THREAD_USAGE_HIGH",
                String.format("线程使用率过高: %.1f%%", threadUsagePercent),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.HIGH,
                "THREAD_OPTIMIZATION",
                "线程优化",
                "建议减少线程数量，使用线程池优化并发处理",
                OptimizationRecommendation.Category.THREAD,
                Arrays.asList("减少线程数量", "使用线程池", "优化并发逻辑")
            ));
        } else if (threadUsagePercent > thresholds.getThreadWarningThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "THREAD_USAGE_ELEVATED",
                String.format("线程使用率偏高: %.1f%%", threadUsagePercent),
                pluginId
            ));
        }
        
        // 线程数量异常增长
        if (history.size() >= 3) {
            analyzeThreadGrowthTrend(pluginId, history, issues, recommendations);
        }
        
        // 线程数峰值异常
        if (peakThreads > currentThreads * 1.3) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "THREAD_SPIKE_DETECTED",
                String.format("检测到线程峰值: %d (当前: %d)", peakThreads, currentThreads),
                pluginId
            ));
        }
    }
    
    /**
     * CPU性能分析
     */
    private void analyzeCpuPerformance(String pluginId, PluginResourceUsage usage,
                                     List<PerformanceIssue> issues,
                                     List<OptimizationRecommendation> recommendations,
                                     List<PerformanceSnapshot> history) {
        
        double cpuUsagePercent = usage.getCpuUsagePercent();
        
        // CPU使用率过高
        if (cpuUsagePercent > thresholds.getCpuCriticalThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.CRITICAL,
                "CPU_USAGE_HIGH",
                String.format("CPU使用率过高: %.1f%%", cpuUsagePercent),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.HIGH,
                "CPU_OPTIMIZATION",
                "CPU使用优化",
                "建议优化算法复杂度，减少计算密集型操作，考虑异步处理",
                OptimizationRecommendation.Category.CPU,
                Arrays.asList("优化算法复杂度", "减少计算密集型操作", "异步处理")
            ));
        } else if (cpuUsagePercent > thresholds.getCpuWarningThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "CPU_USAGE_ELEVATED",
                String.format("CPU使用率偏高: %.1f%%", cpuUsagePercent),
                pluginId
            ));
        }
    }
    
    /**
     * 网络性能分析
     */
    private void analyzeNetworkPerformance(String pluginId, PluginResourceUsage usage,
                                         List<PerformanceIssue> issues,
                                         List<OptimizationRecommendation> recommendations,
                                         List<PerformanceSnapshot> history) {
        
        int networkConnections = usage.getCurrentNetworkConnections();
        
        // 网络连接数过多
        if (networkConnections > thresholds.getNetworkCriticalThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.CRITICAL,
                "NETWORK_CONNECTIONS_HIGH",
                String.format("网络连接数过多: %d", networkConnections),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.HIGH,
                "NETWORK_OPTIMIZATION",
                "网络连接优化",
                "建议使用连接池，减少连接建立次数，优化网络请求",
                OptimizationRecommendation.Category.NETWORK,
                Arrays.asList("使用连接池", "减少连接建立次数", "优化网络请求")
            ));
        } else if (networkConnections > thresholds.getNetworkWarningThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "NETWORK_CONNECTIONS_ELEVATED",
                String.format("网络连接数偏高: %d", networkConnections),
                pluginId
            ));
        }
    }
    
    /**
     * 资源利用率分析
     */
    private void analyzeResourceUtilization(String pluginId, PluginResourceUsage usage,
                                          List<PerformanceIssue> issues,
                                          List<OptimizationRecommendation> recommendations,
                                          List<PerformanceSnapshot> history) {
        
        // 资源利用率计算
        double memoryEfficiency = calculateMemoryEfficiency(usage);
        double threadEfficiency = calculateThreadEfficiency(usage);
        double overallEfficiency = (memoryEfficiency + threadEfficiency) / 2;
        
        if (overallEfficiency < thresholds.getEfficiencyCriticalThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "LOW_RESOURCE_EFFICIENCY",
                String.format("资源利用效率较低: %.1f%%", overallEfficiency),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.MEDIUM,
                "EFFICIENCY_OPTIMIZATION",
                "资源利用效率优化",
                "建议检查资源使用合理性，优化代码执行效率",
                OptimizationRecommendation.Category.EFFICIENCY,
                Arrays.asList("检查资源使用合理性", "优化代码执行效率", "重构低效逻辑")
            ));
        }
    }
    
    /**
     * 性能趋势分析
     */
    private void analyzePerformanceTrends(String pluginId, List<PerformanceIssue> issues,
                                        List<OptimizationRecommendation> recommendations,
                                        List<PerformanceSnapshot> history) {
        
        if (history.size() < 5) {
            return; // 需要足够的历史数据
        }
        
        // 分析内存增长趋势
        analyzeMemoryGrowthTrend(pluginId, history, issues, recommendations);
        
        // 分析线程增长趋势
        analyzeThreadGrowthTrend(pluginId, history, issues, recommendations);
    }
    
    private void analyzeMemoryGrowthTrend(String pluginId, List<PerformanceSnapshot> history,
                                        List<PerformanceIssue> issues,
                                        List<OptimizationRecommendation> recommendations) {
        
        List<PerformanceSnapshot> recentHistory = history.subList(Math.max(0, history.size() - 5), history.size());
        
        // 计算内存增长趋势
        double trend = calculateGrowthTrend(recentHistory.stream()
            .mapToDouble(snapshot -> snapshot.getMemoryUsagePercent())
            .toArray());
        
        if (trend > thresholds.getMemoryGrowthRateThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "MEMORY_GROWTH_TREND",
                String.format("内存使用呈增长趋势: %.2f%%/检查", trend),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.MEDIUM,
                "MEMORY_LEAK_CHECK",
                "内存泄漏检查",
                "内存使用持续增长，建议检查是否存在内存泄漏",
                OptimizationRecommendation.Category.MEMORY,
                Arrays.asList("检查对象引用", "清理缓存", "监控内存增长")
            ));
        }
    }
    
    private void analyzeThreadGrowthTrend(String pluginId, List<PerformanceSnapshot> history,
                                        List<PerformanceIssue> issues,
                                        List<OptimizationRecommendation> recommendations) {
        
        List<PerformanceSnapshot> recentHistory = history.subList(Math.max(0, history.size() - 5), history.size());
        
        // 计算线程增长趋势
        double[] threadCounts = recentHistory.stream()
            .mapToInt(snapshot -> snapshot.getThreadCount())
            .asDoubleStream()
            .toArray();
        double trend = calculateGrowthTrend(threadCounts);
        
        if (trend > thresholds.getThreadGrowthRateThreshold()) {
            issues.add(new PerformanceIssue(
                PerformanceIssue.Severity.WARNING,
                "THREAD_GROWTH_TREND",
                String.format("线程数量呈增长趋势: %.2f/检查", trend),
                pluginId
            ));
            
            recommendations.add(new OptimizationRecommendation(
                OptimizationRecommendation.Priority.MEDIUM,
                "THREAD_LEAK_CHECK",
                "线程泄漏检查",
                "线程数量持续增长，建议检查是否存在线程泄漏",
                OptimizationRecommendation.Category.THREAD,
                Arrays.asList("检查线程池配置", "监控线程生命周期", "优化并发逻辑")
            ));
        }
    }
    
    /**
     * 计算内存效率
     */
    private double calculateMemoryEfficiency(PluginResourceUsage usage) {
        ResourceQuota quota = usage.getQuota();
        if (quota.getMaxMemoryBytes() == 0) {
            return 100.0; // 无限制时认为效率100%
        }
        
        double usagePercent = usage.getMemoryUsagePercent();
        return Math.max(0, 100 - Math.abs(usagePercent - 50)); // 理想使用率为50%
    }
    
    /**
     * 计算线程效率
     */
    private double calculateThreadEfficiency(PluginResourceUsage usage) {
        ResourceQuota quota = usage.getQuota();
        if (quota.getMaxThreads() == 0) {
            return 100.0; // 无限制时认为效率100%
        }
        
        double usagePercent = usage.getThreadUsagePercent();
        return Math.max(0, 100 - Math.abs(usagePercent - 40)); // 理想使用率为40%
    }
    
    /**
     * 计算增长率
     */
    private double calculateGrowthTrend(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        
        double sum = 0;
        for (int i = 1; i < values.length; i++) {
            sum += values[i] - values[i-1];
        }
        
        return sum / (values.length - 1);
    }
    
    /**
     * 计算性能评分
     */
    private double calculatePerformanceScore(List<PerformanceIssue> issues) {
        if (issues.isEmpty()) {
            return 100.0;
        }
        
        double score = 100.0;
        for (PerformanceIssue issue : issues) {
            switch (issue.getSeverity()) {
                case CRITICAL:
                    score -= 20;
                    break;
                case WARNING:
                    score -= 10;
                    break;
                case INFO:
                    score -= 5;
                    break;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 保存性能快照
     */
    private void savePerformanceSnapshot(String pluginId, PluginResourceUsage usage, PerformanceAnalysis analysis) {
        PerformanceSnapshot snapshot = new PerformanceSnapshot(
            pluginId,
            usage.getCurrentMemoryUsage(),
            usage.getCurrentThreadCount(),
            usage.getCurrentCpuUsage(),
            usage.getCurrentNetworkConnections(),
            usage.getMemoryUsagePercent(),
            usage.getThreadUsagePercent(),
            analysis.getPerformanceScore(),
            LocalDateTime.now()
        );
        
        performanceHistory.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(snapshot);
        
        // 保持历史数据大小限制
        List<PerformanceSnapshot> history = performanceHistory.get(pluginId);
        if (history.size() > 100) {
            history.remove(0); // 移除最旧的数据
        }
    }
    
    /**
     * 获取性能历史
     */
    public List<PerformanceSnapshot> getPerformanceHistory(String pluginId, int limit) {
        List<PerformanceSnapshot> history = performanceHistory.getOrDefault(pluginId, new ArrayList<>());
        if (limit > 0 && history.size() > limit) {
            return new ArrayList<>(history.subList(history.size() - limit, history.size()));
        }
        return new ArrayList<>(history);
    }
    
    /**
     * 设置性能基线
     */
    public void setPerformanceBaseline(String pluginId, PerformanceBaseline baseline) {
        performanceBaselines.put(pluginId, baseline);
    }
    
    /**
     * 获取性能基线
     */
    public PerformanceBaseline getPerformanceBaseline(String pluginId) {
        return performanceBaselines.get(pluginId);
    }
    
    /**
     * 与基线对比分析
     */
    public PerformanceComparison compareWithBaseline(String pluginId, PerformanceAnalysis current) {
        PerformanceBaseline baseline = performanceBaselines.get(pluginId);
        if (baseline == null) {
            return null;
        }
        
        return new PerformanceComparison(pluginId, baseline, current);
    }
    
    /**
     * 清理插件历史数据
     */
    public void clearPluginHistory(String pluginId) {
        performanceHistory.remove(pluginId);
        performanceBaselines.remove(pluginId);
    }
    
    /**
     * 获取所有插件性能摘要
     */
    public Map<String, Double> getAllPluginPerformanceScores() {
        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<String, List<PerformanceSnapshot>> entry : performanceHistory.entrySet()) {
            String pluginId = entry.getKey();
            List<PerformanceSnapshot> snapshots = entry.getValue();
            if (!snapshots.isEmpty()) {
                scores.put(pluginId, snapshots.get(snapshots.size() - 1).getPerformanceScore());
            }
        }
        return scores;
    }
}
