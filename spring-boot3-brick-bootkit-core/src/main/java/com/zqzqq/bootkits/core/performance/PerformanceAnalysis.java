package com.zqzqq.bootkits.core.performance;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能分析结果
 * 包含插件性能分析的完整结果
 */
public class PerformanceAnalysis {
    
    private final String pluginId;
    private final double performanceScore;
    private final List<PerformanceIssue> issues;
    private final List<OptimizationRecommendation> recommendations;
    private final LocalDateTime analysisTime;
    
    public PerformanceAnalysis(String pluginId, double performanceScore,
                             List<PerformanceIssue> issues,
                             List<OptimizationRecommendation> recommendations,
                             LocalDateTime analysisTime) {
        this.pluginId = pluginId;
        this.performanceScore = performanceScore;
        this.issues = issues;
        this.recommendations = recommendations;
        this.analysisTime = analysisTime;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public double getPerformanceScore() {
        return performanceScore;
    }
    
    public List<PerformanceIssue> getIssues() {
        return issues;
    }
    
    public List<OptimizationRecommendation> getRecommendations() {
        return recommendations;
    }
    
    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }
    
    public boolean hasCriticalIssues() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == PerformanceIssue.Severity.CRITICAL);
    }
    
    public boolean hasWarnings() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == PerformanceIssue.Severity.WARNING);
    }
    
    public int getCriticalIssueCount() {
        return (int) issues.stream().filter(issue -> issue.getSeverity() == PerformanceIssue.Severity.CRITICAL).count();
    }
    
    public int getWarningCount() {
        return (int) issues.stream().filter(issue -> issue.getSeverity() == PerformanceIssue.Severity.WARNING).count();
    }
    
    public String getStatus() {
        if (hasCriticalIssues()) {
            return "CRITICAL";
        } else if (hasWarnings()) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceAnalysis{pluginId='%s', score=%.2f, status='%s', issues=%d, recommendations=%d}",
                pluginId, performanceScore, getStatus(), issues.size(), recommendations.size());
    }
}