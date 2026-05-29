package com.zqzqq.bootkits.core.performance;

/**
 * 性能对比结果
 * 将当前性能与基线进行对比分析
 */
public class PerformanceComparison {
    
    private final String pluginId;
    private final PerformanceBaseline baseline;
    private final PerformanceAnalysis current;
    
    // 对比差异
    private final double memoryUsageDiff;
    private final double threadCountDiff;
    private final double cpuUsageDiff;
    private final double networkConnectionsDiff;
    private final double performanceScoreDiff;
    
    public PerformanceComparison(String pluginId, PerformanceBaseline baseline, PerformanceAnalysis current) {
        this.pluginId = pluginId;
        this.baseline = baseline;
        this.current = current;
        
        // 计算差异（当前值 - 基线值）
        this.memoryUsageDiff = 0; // 这里需要从current中获取具体数据
        this.threadCountDiff = 0; // 这里需要从current中获取具体数据  
        this.cpuUsageDiff = 0; // 这里需要从current中获取具体数据
        this.networkConnectionsDiff = 0; // 这里需要从current中获取具体数据
        this.performanceScoreDiff = current.getPerformanceScore() - baseline.getBaselinePerformanceScore();
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public PerformanceBaseline getBaseline() {
        return baseline;
    }
    
    public PerformanceAnalysis getCurrent() {
        return current;
    }
    
    public double getMemoryUsageDiff() {
        return memoryUsageDiff;
    }
    
    public double getThreadCountDiff() {
        return threadCountDiff;
    }
    
    public double getCpuUsageDiff() {
        return cpuUsageDiff;
    }
    
    public double getNetworkConnectionsDiff() {
        return networkConnectionsDiff;
    }
    
    public double getPerformanceScoreDiff() {
        return performanceScoreDiff;
    }
    
    /**
     * 获取性能变化趋势
     */
    public Trend getPerformanceTrend() {
        if (performanceScoreDiff > 5) {
            return Trend.IMPROVING;
        } else if (performanceScoreDiff < -5) {
            return Trend.DEGRADING;
        } else {
            return Trend.STABLE;
        }
    }
    
    /**
     * 检查是否性能显著变化
     */
    public boolean isPerformanceSignificantlyChanged() {
        return Math.abs(performanceScoreDiff) > 10;
    }
    
    /**
     * 获取综合评价
     */
    public String getOverallAssessment() {
        if (isPerformanceSignificantlyChanged()) {
            if (getPerformanceTrend() == Trend.IMPROVING) {
                return "性能显著提升";
            } else if (getPerformanceTrend() == Trend.DEGRADING) {
                return "性能显著下降";
            }
        } else {
            return "性能相对稳定";
        }
        
        return "性能状态正常";
    }
    
    /**
     * 性能变化趋势
     */
    public enum Trend {
        IMPROVING("性能提升"),
        DEGRADING("性能下降"),
        STABLE("性能稳定");
        
        private final String description;
        
        Trend(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceComparison{pluginId='%s', trend=%s, scoreDiff=%.2f, assessment='%s'}",
                pluginId, getPerformanceTrend(), performanceScoreDiff, getOverallAssessment());
    }
}
