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