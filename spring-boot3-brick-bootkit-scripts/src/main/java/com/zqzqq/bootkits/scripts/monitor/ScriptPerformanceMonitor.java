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


package com.zqzqq.bootkits.scripts.monitor;

import com.zqzqq.bootkits.scripts.core.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本性能监控器接口
 * 负责监控脚本执行的性能指标
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptPerformanceMonitor {
    
    /**
     * 开始性能监控
     *
     * @param scriptPath 脚本路径
     * @param scriptType 脚本类型
     * @param userId 用户ID
     * @return 监控会话ID
     */
    String startMonitoring(String scriptPath, ScriptType scriptType, String userId);
    
    /**
     * 结束性能监控
     *
     * @param sessionId 监控会话ID
     * @param result 执行结果
     * @return 性能报告
     */
    PerformanceReport endMonitoring(String sessionId, ScriptExecutionResult result);
    
    /**
     * 记录性能指标
     *
     * @param sessionId 监控会话ID
     * @param metric 性能指标
     * @param value 指标值
     * @param unit 单位
     */
    void recordMetric(String sessionId, String metric, double value, String unit);
    
    /**
     * 记录内存使用情况
     *
     * @param sessionId 监控会话ID
     * @param usedMemory 已使用内存
     * @param maxMemory 最大内存
     */
    void recordMemoryUsage(String sessionId, long usedMemory, long maxMemory);
    
    /**
     * 记录CPU使用情况
     *
     * @param sessionId 监控会话ID
     * @param cpuUsage CPU使用率
     */
    void recordCpuUsage(String sessionId, double cpuUsage);
    
    /**
     * 获取性能统计
     *
     * @param query 查询条件
     * @return 性能统计信息
     */
    PerformanceStatistics getStatistics(PerformanceQuery query);
    
    /**
     * 获取性能报告
     *
     * @param sessionId 监控会话ID
     * @return 性能报告
     */
    PerformanceReport getReport(String sessionId);
    
    /**
     * 获取脚本性能历史
     *
     * @param scriptPath 脚本路径
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性能历史记录
     */
    List<PerformanceRecord> getPerformanceHistory(String scriptPath, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取最慢的脚本执行
     *
     * @param limit 限制数量
     * @return 慢执行列表
     */
    List<PerformanceRecord> getSlowestExecutions(int limit);
    
    /**
     * 获取最频繁的脚本执行
     *
     * @param limit 限制数量
     * @return 频繁执行列表
     */
    List<PerformanceRecord> getMostFrequentExecutions(int limit);
    
    /**
     * 获取性能异常
     *
     * @param query 查询条件
     * @return 性能异常列表
     */
    List<PerformanceAnomaly> getAnomalies(PerformanceQuery query);
    
    /**
     * 生成性能报告
     *
     * @param query 查询条件
     * @param reportType 报告类型
     * @return 性能报告
     */
    PerformanceReport generateReport(PerformanceQuery query, PerformanceReportType reportType);
    
    /**
     * 设置性能阈值
     *
     * @param scriptType 脚本类型
     * @param thresholds 性能阈值
     */
    void setPerformanceThresholds(ScriptType scriptType, PerformanceThresholds thresholds);
    
    /**
     * 获取性能阈值
     *
     * @param scriptType 脚本类型
     * @return 性能阈值
     */
    PerformanceThresholds getPerformanceThresholds(ScriptType scriptType);
    
    /**
     * 清理旧性能数据
     *
     * @param retentionDays 保留天数
     * @return 清理的记录数
     */
    int cleanupOldData(int retentionDays);
    
    /**
     * 性能监控会话
     */
    class MonitoringSession {
        private final String sessionId;
        private final String scriptPath;
        private final ScriptType scriptType;
        private final String userId;
        private final LocalDateTime startTime;
        private final Map<String, Object> metadata;
        
        private LocalDateTime endTime;
        private PerformanceMetrics finalMetrics;
        private boolean completed = false;
        
        public MonitoringSession(String sessionId, String scriptPath, ScriptType scriptType, 
                               String userId, LocalDateTime startTime, Map<String, Object> metadata) {
            this.sessionId = sessionId;
            this.scriptPath = scriptPath;
            this.scriptType = scriptType;
            this.userId = userId;
            this.startTime = startTime;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        public String getSessionId() { return sessionId; }
        public String getScriptPath() { return scriptPath; }
        public ScriptType getScriptType() { return scriptType; }
        public String getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public PerformanceMetrics getFinalMetrics() { return finalMetrics; }
        public boolean isCompleted() { return completed; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public void setFinalMetrics(PerformanceMetrics finalMetrics) { this.finalMetrics = finalMetrics; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
    
    /**
     * 性能指标
     */
    class PerformanceMetrics {
        private long executionTimeMs;
        private long cpuTimeMs;
        private long memoryUsedBytes;
        private long peakMemoryUsedBytes;
        private double cpuUsagePercent;
        private int exitCode;
        private long outputSizeBytes;
        private long errorOutputSizeBytes;
        private Map<String, Object> customMetrics;
        
        public PerformanceMetrics() {
            this.customMetrics = new HashMap<>();
        }
        
        public PerformanceMetrics(long executionTimeMs, long cpuTimeMs, long memoryUsedBytes, 
                                long peakMemoryUsedBytes, double cpuUsagePercent, int exitCode,
                                long outputSizeBytes, long errorOutputSizeBytes) {
            this();
            this.executionTimeMs = executionTimeMs;
            this.cpuTimeMs = cpuTimeMs;
            this.memoryUsedBytes = memoryUsedBytes;
            this.peakMemoryUsedBytes = peakMemoryUsedBytes;
            this.cpuUsagePercent = cpuUsagePercent;
            this.exitCode = exitCode;
            this.outputSizeBytes = outputSizeBytes;
            this.errorOutputSizeBytes = errorOutputSizeBytes;
        }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        
        public long getCpuTimeMs() { return cpuTimeMs; }
        public void setCpuTimeMs(long cpuTimeMs) { this.cpuTimeMs = cpuTimeMs; }
        
        public long getMemoryUsedBytes() { return memoryUsedBytes; }
        public void setMemoryUsedBytes(long memoryUsedBytes) { this.memoryUsedBytes = memoryUsedBytes; }
        
        public long getPeakMemoryUsedBytes() { return peakMemoryUsedBytes; }
        public void setPeakMemoryUsedBytes(long peakMemoryUsedBytes) { this.peakMemoryUsedBytes = peakMemoryUsedBytes; }
        
        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
        
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        
        public long getOutputSizeBytes() { return outputSizeBytes; }
        public void setOutputSizeBytes(long outputSizeBytes) { this.outputSizeBytes = outputSizeBytes; }
        
        public long getErrorOutputSizeBytes() { return errorOutputSizeBytes; }
        public void setErrorOutputSizeBytes(long errorOutputSizeBytes) { this.errorOutputSizeBytes = errorOutputSizeBytes; }
        
        public Map<String, Object> getCustomMetrics() { return new HashMap<>(customMetrics); }
        public void setCustomMetrics(Map<String, Object> customMetrics) { this.customMetrics = customMetrics != null ? customMetrics : new HashMap<>(); }
        
        public void addCustomMetric(String key, Object value) {
            customMetrics.put(key, value);
        }
        
        public Object getCustomMetric(String key) {
            return customMetrics.get(key);
        }
    }
    
    /**
     * 性能报告
     */
    class PerformanceReport {
        private final String sessionId;
        private final MonitoringSession session;
        private final PerformanceMetrics metrics;
        private final PerformanceAnalysis analysis;
        private final LocalDateTime generatedAt;
        private final List<String> recommendations;
        
        public PerformanceReport(String sessionId, MonitoringSession session, PerformanceMetrics metrics,
                               PerformanceAnalysis analysis, List<String> recommendations) {
            this.sessionId = sessionId;
            this.session = session;
            this.metrics = metrics;
            this.analysis = analysis;
            this.generatedAt = LocalDateTime.now();
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
        
        public String getSessionId() { return sessionId; }
        public MonitoringSession getSession() { return session; }
        public PerformanceMetrics getMetrics() { return metrics; }
        public PerformanceAnalysis getAnalysis() { return analysis; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
    }
    
    /**
     * 性能分析
     */
    class PerformanceAnalysis {
        private final PerformanceScore overallScore;
        private final List<PerformanceIssue> issues;
        private final List<PerformanceStrength> strengths;
        private final Map<String, Object> analysisData;
        private final String summary;
        
        public PerformanceAnalysis(PerformanceScore overallScore, List<PerformanceIssue> issues,
                                 List<PerformanceStrength> strengths, Map<String, Object> analysisData,
                                 String summary) {
            this.overallScore = overallScore;
            this.issues = issues != null ? issues : new ArrayList<>();
            this.strengths = strengths != null ? strengths : new ArrayList<>();
            this.analysisData = analysisData != null ? analysisData : new HashMap<>();
            this.summary = summary;
        }
        
        public PerformanceScore getOverallScore() { return overallScore; }
        public List<PerformanceIssue> getIssues() { return new ArrayList<>(issues); }
        public List<PerformanceStrength> getStrengths() { return new ArrayList<>(strengths); }
        public Map<String, Object> getAnalysisData() { return new HashMap<>(analysisData); }
        public String getSummary() { return summary; }
    }
    
    /**
     * 性能统计
     */
    class PerformanceStatistics {
        private final long totalExecutions;
        private final double averageExecutionTime;
        private final long minExecutionTime;
        private final long maxExecutionTime;
        private final double averageCpuUsage;
        private final long averageMemoryUsage;
        private final Map<ScriptType, Long> executionCountByType;
        private final Map<String, Long> executionCountByUser;
        private final Map<String, Long> executionCountByPath;
        private final List<PerformancePercentile> percentiles;
        
        public PerformanceStatistics(long totalExecutions, double averageExecutionTime, long minExecutionTime,
                                   long maxExecutionTime, double averageCpuUsage, long averageMemoryUsage,
                                   Map<ScriptType, Long> executionCountByType, Map<String, Long> executionCountByUser,
                                   Map<String, Long> executionCountByPath, List<PerformancePercentile> percentiles) {
            this.totalExecutions = totalExecutions;
            this.averageExecutionTime = averageExecutionTime;
            this.minExecutionTime = minExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.averageCpuUsage = averageCpuUsage;
            this.averageMemoryUsage = averageMemoryUsage;
            this.executionCountByType = executionCountByType != null ? executionCountByType : new HashMap<>();
            this.executionCountByUser = executionCountByUser != null ? executionCountByUser : new HashMap<>();
            this.executionCountByPath = executionCountByPath != null ? executionCountByPath : new HashMap<>();
            this.percentiles = percentiles != null ? percentiles : new ArrayList<>();
        }
        
        public long getTotalExecutions() { return totalExecutions; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public long getMinExecutionTime() { return minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public double getAverageCpuUsage() { return averageCpuUsage; }
        public long getAverageMemoryUsage() { return averageMemoryUsage; }
        public Map<ScriptType, Long> getExecutionCountByType() { return new HashMap<>(executionCountByType); }
        public Map<String, Long> getExecutionCountByUser() { return new HashMap<>(executionCountByUser); }
        public Map<String, Long> getExecutionCountByPath() { return new HashMap<>(executionCountByPath); }
        public List<PerformancePercentile> getPercentiles() { return new ArrayList<>(percentiles); }
    }
    
    /**
     * 性能记录
     */
    class PerformanceRecord {
        private final String sessionId;
        private final String scriptPath;
        private final ScriptType scriptType;
        private final String userId;
        private final LocalDateTime timestamp;
        private final PerformanceMetrics metrics;
        private final double performanceScore;
        
        public PerformanceRecord(String sessionId, String scriptPath, ScriptType scriptType, 
                               String userId, LocalDateTime timestamp, PerformanceMetrics metrics,
                               double performanceScore) {
            this.sessionId = sessionId;
            this.scriptPath = scriptPath;
            this.scriptType = scriptType;
            this.userId = userId;
            this.timestamp = timestamp;
            this.metrics = metrics;
            this.performanceScore = performanceScore;
        }
        
        public String getSessionId() { return sessionId; }
        public String getScriptPath() { return scriptPath; }
        public ScriptType getScriptType() { return scriptType; }
        public String getUserId() { return userId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public PerformanceMetrics getMetrics() { return metrics; }
        public double getPerformanceScore() { return performanceScore; }
    }
    
    /**
     * 性能异常
     */
    class PerformanceAnomaly {
        private final String sessionId;
        private final AnomalyType type;
        private final String description;
        private final double severity;
        private final LocalDateTime detectedAt;
        private final Map<String, Object> anomalyData;
        
        public PerformanceAnomaly(String sessionId, AnomalyType type, String description,
                                double severity, LocalDateTime detectedAt, Map<String, Object> anomalyData) {
            this.sessionId = sessionId;
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.detectedAt = detectedAt;
            this.anomalyData = anomalyData != null ? anomalyData : new HashMap<>();
        }
        
        public String getSessionId() { return sessionId; }
        public AnomalyType getType() { return type; }
        public String getDescription() { return description; }
        public double getSeverity() { return severity; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
        public Map<String, Object> getAnomalyData() { return new HashMap<>(anomalyData); }
    }
    
    /**
     * 性能查询
     */
    class PerformanceQuery {
        private String scriptPath;
        private ScriptType scriptType;
        private String userId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long minExecutionTime;
        private Long maxExecutionTime;
        private Double minCpuUsage;
        private Double maxCpuUsage;
        private int limit = 100;
        private int offset = 0;
        
        public PerformanceQuery() {}
        
        public PerformanceQuery scriptPath(String scriptPath) { this.scriptPath = scriptPath; return this; }
        public PerformanceQuery scriptType(ScriptType scriptType) { this.scriptType = scriptType; return this; }
        public PerformanceQuery userId(String userId) { this.userId = userId; return this; }
        public PerformanceQuery startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public PerformanceQuery endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public PerformanceQuery minExecutionTime(Long minExecutionTime) { this.minExecutionTime = minExecutionTime; return this; }
        public PerformanceQuery maxExecutionTime(Long maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; return this; }
        public PerformanceQuery minCpuUsage(Double minCpuUsage) { this.minCpuUsage = minCpuUsage; return this; }
        public PerformanceQuery maxCpuUsage(Double maxCpuUsage) { this.maxCpuUsage = maxCpuUsage; return this; }
        public PerformanceQuery limit(int limit) { this.limit = limit; return this; }
        public PerformanceQuery offset(int offset) { this.offset = offset; return this; }
        
        public String getScriptPath() { return scriptPath; }
        public ScriptType getScriptType() { return scriptType; }
        public String getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Long getMinExecutionTime() { return minExecutionTime; }
        public Long getMaxExecutionTime() { return maxExecutionTime; }
        public Double getMinCpuUsage() { return minCpuUsage; }
        public Double getMaxCpuUsage() { return maxCpuUsage; }
        public int getLimit() { return limit; }
        public int getOffset() { return offset; }
    }
    
    /**
     * 性能阈值
     */
    class PerformanceThresholds {
        private long maxExecutionTimeMs = 30000; // 30秒
        private long maxMemoryBytes = 512 * 1024 * 1024; // 512MB
        private double maxCpuUsagePercent = 80.0;
        private long maxOutputSizeBytes = 10 * 1024 * 1024; // 10MB
        private boolean enableAnomalyDetection = true;
        private double anomalyThreshold = 2.0; // 2个标准差
        
        public PerformanceThresholds() {}
        
        public PerformanceThresholds(long maxExecutionTimeMs, long maxMemoryBytes, double maxCpuUsagePercent,
                                   long maxOutputSizeBytes, boolean enableAnomalyDetection, double anomalyThreshold) {
            this.maxExecutionTimeMs = maxExecutionTimeMs;
            this.maxMemoryBytes = maxMemoryBytes;
            this.maxCpuUsagePercent = maxCpuUsagePercent;
            this.maxOutputSizeBytes = maxOutputSizeBytes;
            this.enableAnomalyDetection = enableAnomalyDetection;
            this.anomalyThreshold = anomalyThreshold;
        }
        
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        public void setMaxExecutionTimeMs(long maxExecutionTimeMs) { this.maxExecutionTimeMs = maxExecutionTimeMs; }
        
        public long getMaxMemoryBytes() { return maxMemoryBytes; }
        public void setMaxMemoryBytes(long maxMemoryBytes) { this.maxMemoryBytes = maxMemoryBytes; }
        
        public double getMaxCpuUsagePercent() { return maxCpuUsagePercent; }
        public void setMaxCpuUsagePercent(double maxCpuUsagePercent) { this.maxCpuUsagePercent = maxCpuUsagePercent; }
        
        public long getMaxOutputSizeBytes() { return maxOutputSizeBytes; }
        public void setMaxOutputSizeBytes(long maxOutputSizeBytes) { this.maxOutputSizeBytes = maxOutputSizeBytes; }
        
        public boolean isEnableAnomalyDetection() { return enableAnomalyDetection; }
        public void setEnableAnomalyDetection(boolean enableAnomalyDetection) { this.enableAnomalyDetection = enableAnomalyDetection; }
        
        public double getAnomalyThreshold() { return anomalyThreshold; }
        public void setAnomalyThreshold(double anomalyThreshold) { this.anomalyThreshold = anomalyThreshold; }
    }
    
    // 枚举类型
    enum PerformanceScore {
        EXCELLENT("EXCELLENT", 5, "优秀"),
        GOOD("GOOD", 4, "良好"),
        AVERAGE("AVERAGE", 3, "一般"),
        POOR("POOR", 2, "较差"),
        CRITICAL("CRITICAL", 1, "严重");
        
        private final String name;
        private final int score;
        private final String description;
        
        PerformanceScore(String name, int score, String description) {
            this.name = name;
            this.score = score;
            this.description = description;
        }
        
        public String getName() { return name; }
        public int getScore() { return score; }
        public String getDescription() { return description; }
    }
    
    enum PerformanceReportType {
        EXECUTION_SUMMARY("EXECUTION_SUMMARY", "执行摘要"),
        PERFORMANCE_TREND("PERFORMANCE_TREND", "性能趋势"),
        RESOURCE_USAGE("RESOURCE_USAGE", "资源使用"),
        BOTTLENECK_ANALYSIS("BOTTLENECK_ANALYSIS", "瓶颈分析"),
        COMPREHENSIVE("COMPREHENSIVE", "综合报告");
        
        private final String name;
        private final String description;
        
        PerformanceReportType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    enum AnomalyType {
        SLOW_EXECUTION("SLOW_EXECUTION", "执行缓慢"),
        HIGH_MEMORY_USAGE("HIGH_MEMORY_USAGE", "内存使用过高"),
        HIGH_CPU_USAGE("HIGH_CPU_USAGE", "CPU使用过高"),
        FREQUENT_EXECUTIONS("FREQUENT_EXECUTIONS", "执行过于频繁"),
        ERROR_RATE_SPIKE("ERROR_RATE_SPIKE", "错误率激增"),
        RESOURCE_EXHAUSTION("RESOURCE_EXHAUSTION", "资源耗尽");
        
        private final String name;
        private final String description;
        
        AnomalyType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    // 辅助类
    class PerformanceIssue {
        private final String type;
        private final String description;
        private final String severity;
        private final String recommendation;
        
        public PerformanceIssue(String type, String description, String severity, String recommendation) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.recommendation = recommendation;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public String getRecommendation() { return recommendation; }
    }
    
    class PerformanceStrength {
        private final String type;
        private final String description;
        private final String metric;
        
        public PerformanceStrength(String type, String description, String metric) {
            this.type = type;
            this.description = description;
            this.metric = metric;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getMetric() { return metric; }
    }
    
    class PerformancePercentile {
        private final double percentile;
        private final long value;
        
        public PerformancePercentile(double percentile, long value) {
            this.percentile = percentile;
            this.value = value;
        }
        
        public double getPercentile() { return percentile; }
        public long getValue() { return value; }
    }
}