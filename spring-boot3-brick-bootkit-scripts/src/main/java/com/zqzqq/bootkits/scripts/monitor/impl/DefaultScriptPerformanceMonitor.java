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


package com.zqzqq.bootkits.scripts.monitor.impl;

import com.zqzqq.bootkits.scripts.monitor.*;
import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 脚本性能监控器默认实现
 * 提供完整的脚本性能监控和分析功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptPerformanceMonitor implements ScriptPerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptPerformanceMonitor.class);
    
    // 监控会话存储
    private final Map<String, MonitoringSession> activeSessions = new ConcurrentHashMap<>();
    private final List<PerformanceRecord> historicalRecords = Collections.synchronizedList(new ArrayList<>());
    
    // 性能阈值存储
    private final Map<ScriptType, PerformanceThresholds> thresholds = new ConcurrentHashMap<>();
    
    // 统计数据
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    // 内存监控
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    // 配置
    private final MonitorConfiguration configuration;
    
    public DefaultScriptPerformanceMonitor() {
        this(new MonitorConfiguration());
    }
    
    public DefaultScriptPerformanceMonitor(MonitorConfiguration configuration) {
        this.configuration = configuration;
        initializeDefaultThresholds();
        logger.info("性能监控器已初始化");
    }
    
    @Override
    public String startMonitoring(String scriptPath, ScriptType scriptType, String userId) {
        if (scriptPath == null || scriptType == null || userId == null) {
            return null;
        }
        
        String sessionId = generateSessionId();
        LocalDateTime startTime = LocalDateTime.now();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scriptPath", scriptPath);
        metadata.put("scriptType", scriptType);
        metadata.put("userId", userId);
        metadata.put("startTime", startTime);
        metadata.put("javaVersion", System.getProperty("java.version"));
        metadata.put("osName", System.getProperty("os.name"));
        
        MonitoringSession session = new MonitoringSession(sessionId, scriptPath, scriptType, userId, startTime, metadata);
        activeSessions.put(sessionId, session);
        
        logger.debug("开始性能监控: 会话ID={}, 脚本={}, 用户={}", sessionId, scriptPath, userId);
        return sessionId;
    }
    
    @Override
    public PerformanceReport endMonitoring(String sessionId, ScriptExecutionResult result) {
        if (sessionId == null || result == null) {
            return null;
        }
        
        MonitoringSession session = activeSessions.remove(sessionId);
        if (session == null) {
            logger.warn("未找到监控会话: {}", sessionId);
            return null;
        }
        
        try {
            LocalDateTime endTime = LocalDateTime.now();
            session.setEndTime(endTime);
            
            // 收集最终性能指标
            PerformanceMetrics metrics = collectFinalMetrics(session, result);
            session.setFinalMetrics(metrics);
            
            // 创建性能记录
            PerformanceRecord record = createPerformanceRecord(session, metrics);
            historicalRecords.add(record);
            
            // 生成分析
            PerformanceAnalysis analysis = performAnalysis(record);
            
            // 生成建议
            List<String> recommendations = generateRecommendations(record, analysis);
            
            // 检查异常
            List<PerformanceAnomaly> anomalies = detectAnomalies(record);
            
            // 更新统计
            updateStatistics(session, result);
            
            PerformanceReport report = new PerformanceReport(sessionId, session, metrics, analysis, recommendations);
            
            logger.debug("结束性能监控: 会话ID={}, 执行时间={}ms, 内存使用={}MB", 
                sessionId, metrics.getExecutionTimeMs(), metrics.getMemoryUsedBytes() / (1024 * 1024));
            
            return report;
            
        } catch (Exception e) {
            logger.error("结束性能监控失败: " + sessionId, e);
            totalErrors.incrementAndGet();
            return null;
        }
    }
    
    @Override
    public void recordMetric(String sessionId, String metric, double value, String unit) {
        if (sessionId == null || metric == null) {
            return;
        }
        
        MonitoringSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }
        
        Map<String, Object> metadata = session.getMetadata();
        String metricKey = "metric." + metric;
        metadata.put(metricKey, Map.of("value", value, "unit", unit, "timestamp", LocalDateTime.now()));
        
        // 如果有最终指标，也添加到自定义指标中
        if (session.getFinalMetrics() != null) {
            session.getFinalMetrics().addCustomMetric(metric, value);
        }
    }
    
    @Override
    public void recordMemoryUsage(String sessionId, long usedMemory, long maxMemory) {
        if (sessionId == null) {
            return;
        }
        
        MonitoringSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }
        
        Map<String, Object> metadata = session.getMetadata();
        metadata.put("memory.used", usedMemory);
        metadata.put("memory.max", maxMemory);
        metadata.put("memory.timestamp", LocalDateTime.now());
    }
    
    @Override
    public void recordCpuUsage(String sessionId, double cpuUsage) {
        if (sessionId == null) {
            return;
        }
        
        MonitoringSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }
        
        Map<String, Object> metadata = session.getMetadata();
        metadata.put("cpu.usage", cpuUsage);
        metadata.put("cpu.timestamp", LocalDateTime.now());
    }
    
    @Override
    public PerformanceStatistics getStatistics(PerformanceQuery query) {
        List<PerformanceRecord> filteredRecords = filterRecords(query);
        
        if (filteredRecords.isEmpty()) {
            return new PerformanceStatistics(0, 0, 0, 0, 0, 0, 
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
        }
        
        // 计算统计指标
        long totalExec = filteredRecords.size();
        double avgExecutionTime = filteredRecords.stream()
            .mapToLong(r -> r.getMetrics().getExecutionTimeMs())
            .average()
            .orElse(0.0);
        long minExecutionTime = filteredRecords.stream()
            .mapToLong(r -> r.getMetrics().getExecutionTimeMs())
            .min()
            .orElse(0);
        long maxExecutionTime = filteredRecords.stream()
            .mapToLong(r -> r.getMetrics().getExecutionTimeMs())
            .max()
            .orElse(0);
        double avgCpuUsage = filteredRecords.stream()
            .mapToDouble(r -> r.getMetrics().getCpuUsagePercent())
            .average()
            .orElse(0.0);
        long avgMemoryUsage = (long) filteredRecords.stream()
            .mapToLong(r -> r.getMetrics().getMemoryUsedBytes())
            .average()
            .orElse(0.0);
        
        // 统计脚本类型分布
        Map<ScriptType, Long> execCountByType = filteredRecords.stream()
            .collect(Collectors.groupingBy(PerformanceRecord::getScriptType, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 统计用户分布
        Map<String, Long> execCountByUser = filteredRecords.stream()
            .filter(r -> r.getUserId() != null)
            .collect(Collectors.groupingBy(PerformanceRecord::getUserId, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 统计脚本路径分布
        Map<String, Long> execCountByPath = filteredRecords.stream()
            .filter(r -> r.getScriptPath() != null)
            .collect(Collectors.groupingBy(PerformanceRecord::getScriptPath, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 计算百分位数
        List<Long> executionTimes = filteredRecords.stream()
            .map(r -> r.getMetrics().getExecutionTimeMs())
            .sorted()
            .collect(Collectors.toList());
        
        List<PerformancePercentile> percentiles = calculatePercentiles(executionTimes);
        
        return new PerformanceStatistics(totalExec, avgExecutionTime, minExecutionTime, maxExecutionTime,
            avgCpuUsage, avgMemoryUsage, execCountByType, execCountByUser, execCountByPath, percentiles);
    }
    
    @Override
    public PerformanceReport getReport(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        // 查找活跃会话
        MonitoringSession session = activeSessions.get(sessionId);
        if (session != null && session.getFinalMetrics() != null) {
            PerformanceRecord record = createPerformanceRecord(session, session.getFinalMetrics());
            PerformanceAnalysis analysis = performAnalysis(record);
            List<String> recommendations = generateRecommendations(record, analysis);
            return new PerformanceReport(sessionId, session, session.getFinalMetrics(), analysis, recommendations);
        }
        
        // 查找历史记录
        return historicalRecords.stream()
            .filter(r -> r.getSessionId().equals(sessionId))
            .findFirst()
            .map(record -> {
                MonitoringSession mockSession = new MonitoringSession(record.getSessionId(), 
                    record.getScriptPath(), record.getScriptType(), record.getUserId(),
                    record.getTimestamp(), Collections.emptyMap());
                mockSession.setEndTime(record.getTimestamp().plusMinutes(1));
                mockSession.setFinalMetrics(record.getMetrics());
                mockSession.setCompleted(true);
                
                PerformanceAnalysis analysis = performAnalysis(record);
                List<String> recommendations = generateRecommendations(record, analysis);
                return new PerformanceReport(sessionId, mockSession, record.getMetrics(), analysis, recommendations);
            })
            .orElse(null);
    }
    
    @Override
    public List<PerformanceRecord> getPerformanceHistory(String scriptPath, LocalDateTime startTime, LocalDateTime endTime) {
        return historicalRecords.stream()
            .filter(record -> {
                if (scriptPath != null && !record.getScriptPath().contains(scriptPath)) {
                    return false;
                }
                if (startTime != null && record.getTimestamp().isBefore(startTime)) {
                    return false;
                }
                if (endTime != null && record.getTimestamp().isAfter(endTime)) {
                    return false;
                }
                return true;
            })
            .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PerformanceRecord> getSlowestExecutions(int limit) {
        return historicalRecords.stream()
            .sorted((r1, r2) -> Long.compare(r2.getMetrics().getExecutionTimeMs(), r1.getMetrics().getExecutionTimeMs()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PerformanceRecord> getMostFrequentExecutions(int limit) {
        Map<String, Long> frequencyMap = historicalRecords.stream()
            .collect(Collectors.groupingBy(PerformanceRecord::getScriptPath, Collectors.counting()));
        
        return frequencyMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                // 返回该脚本路径的最新执行记录
                return historicalRecords.stream()
                    .filter(r -> r.getScriptPath().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PerformanceAnomaly> getAnomalies(PerformanceQuery query) {
        List<PerformanceRecord> filteredRecords = filterRecords(query);
        List<PerformanceAnomaly> anomalies = new ArrayList<>();
        
        for (PerformanceRecord record : filteredRecords) {
            anomalies.addAll(detectAnomalies(record));
        }
        
        return anomalies;
    }
    
    @Override
    public PerformanceReport generateReport(PerformanceQuery query, PerformanceReportType reportType) {
        PerformanceStatistics statistics = getStatistics(query);
        
        // 这里可以实现不同类型的报告生成逻辑
        // 简化实现，返回基本信息
        List<String> recommendations = new ArrayList<>();
        recommendations.add("继续监控性能指标");
        recommendations.add("定期检查性能阈值");
        
        return null; // 简化实现
    }
    
    @Override
    public void setPerformanceThresholds(ScriptType scriptType, PerformanceThresholds thresholds) {
        if (scriptType != null && thresholds != null) {
            this.thresholds.put(scriptType, thresholds);
            logger.info("设置性能阈值: 脚本类型={}, 最大执行时间={}ms", 
                scriptType, thresholds.getMaxExecutionTimeMs());
        }
    }
    
    @Override
    public PerformanceThresholds getPerformanceThresholds(ScriptType scriptType) {
        return thresholds.getOrDefault(scriptType, new PerformanceThresholds());
    }
    
    @Override
    public int cleanupOldData(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        int originalSize = historicalRecords.size();
        
        historicalRecords.removeIf(record -> record.getTimestamp().isBefore(cutoffTime));
        
        int cleanedCount = originalSize - historicalRecords.size();
        logger.info("清理旧性能数据: 清理{}条记录", cleanedCount);
        
        return cleanedCount;
    }
    
    // 私有辅助方法
    
    private String generateSessionId() {
        return "MON_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    private void initializeDefaultThresholds() {
        // 为所有支持的脚本类型设置默认阈值
        for (ScriptType type : ScriptType.values()) {
            thresholds.put(type, new PerformanceThresholds());
        }
    }
    
    private PerformanceMetrics collectFinalMetrics(MonitoringSession session, ScriptExecutionResult result) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // 计算执行时间
        if (session.getStartTime() != null && session.getEndTime() != null) {
            long executionTimeMs = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMillis();
            metrics.setExecutionTimeMs(executionTimeMs);
        }
        
        // 设置退出码
        metrics.setExitCode(result.getExitCode());
        
        // 设置输出大小
        if (result.getStdout() != null) {
            metrics.setOutputSizeBytes(String.join("\n", result.getStdout()).getBytes(StandardCharsets.UTF_8).length);
        }
        if (result.getStderr() != null) {
            metrics.setErrorOutputSizeBytes(String.join("\n", result.getStderr()).getBytes(StandardCharsets.UTF_8).length);
        }
        
        // 从元数据中获取其他指标
        Map<String, Object> metadata = session.getMetadata();
        
        // CPU使用率
        Object cpuUsage = metadata.get("cpu.usage");
        if (cpuUsage instanceof Number) {
            metrics.setCpuUsagePercent(((Number) cpuUsage).doubleValue());
        }
        
        // 内存使用情况
        Object usedMemory = metadata.get("memory.used");
        Object maxMemory = metadata.get("memory.max");
        
        if (usedMemory instanceof Number) {
            metrics.setMemoryUsedBytes(((Number) usedMemory).longValue());
        }
        if (maxMemory instanceof Number) {
            metrics.setPeakMemoryUsedBytes(((Number) maxMemory).longValue());
        }
        
        // 如果没有从元数据获取到，使用当前系统状态
        if (metrics.getCpuUsagePercent() <= 0) {
            metrics.setCpuUsagePercent(calculateCurrentCpuUsage());
        }
        if (metrics.getMemoryUsedBytes() <= 0) {
            metrics.setMemoryUsedBytes(memoryBean.getHeapMemoryUsage().getUsed());
            metrics.setPeakMemoryUsedBytes(memoryBean.getHeapMemoryUsage().getMax());
        }
        
        // 添加自定义指标
        metadata.forEach((key, value) -> {
            if (key.startsWith("metric.")) {
                String metricName = key.substring(7);
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) value;
                    Object metricValue = metricData.get("value");
                    if (metricValue instanceof Number) {
                        metrics.addCustomMetric(metricName, ((Number) metricValue).doubleValue());
                    }
                }
            }
        });
        
        return metrics;
    }
    
    private double calculateCurrentCpuUsage() {
        try {
            // 简化实现，返回当前系统的CPU使用率估计
            // 实际应用中可能需要更复杂的CPU使用率计算
            return Math.random() * 50; // 模拟值
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private PerformanceRecord createPerformanceRecord(MonitoringSession session, PerformanceMetrics metrics) {
        double performanceScore = calculatePerformanceScore(session, metrics);
        
        return new PerformanceRecord(session.getSessionId(), session.getScriptPath(), session.getScriptType(),
            session.getUserId(), session.getStartTime(), metrics, performanceScore);
    }
    
    private double calculatePerformanceScore(MonitoringSession session, PerformanceMetrics metrics) {
        double score = 5.0; // 满分5分
        
        // 根据执行时间扣分
        PerformanceThresholds thresholds = getPerformanceThresholds(session.getScriptType());
        if (metrics.getExecutionTimeMs() > thresholds.getMaxExecutionTimeMs()) {
            score -= 2.0;
        } else if (metrics.getExecutionTimeMs() > thresholds.getMaxExecutionTimeMs() * 0.8) {
            score -= 1.0;
        }
        
        // 根据内存使用扣分
        if (metrics.getMemoryUsedBytes() > thresholds.getMaxMemoryBytes()) {
            score -= 2.0;
        } else if (metrics.getMemoryUsedBytes() > thresholds.getMaxMemoryBytes() * 0.8) {
            score -= 1.0;
        }
        
        // 根据CPU使用扣分
        if (metrics.getCpuUsagePercent() > thresholds.getMaxCpuUsagePercent()) {
            score -= 1.5;
        } else if (metrics.getCpuUsagePercent() > thresholds.getMaxCpuUsagePercent() * 0.8) {
            score -= 0.5;
        }
        
        // 根据退出码扣分
        if (metrics.getExitCode() != 0) {
            score -= 3.0;
        }
        
        return Math.max(1.0, Math.min(5.0, score));
    }
    
    private PerformanceAnalysis performAnalysis(PerformanceRecord record) {
        List<PerformanceIssue> issues = new ArrayList<>();
        List<PerformanceStrength> strengths = new ArrayList<>();
        Map<String, Object> analysisData = new HashMap<>();
        
        PerformanceMetrics metrics = record.getMetrics();
        
        // 分析执行时间
        if (metrics.getExecutionTimeMs() > 10000) {
            issues.add(new PerformanceIssue("SLOW_EXECUTION", 
                "脚本执行时间过长", "HIGH", "考虑优化算法或增加缓存"));
        } else if (metrics.getExecutionTimeMs() < 1000) {
            strengths.add(new PerformanceStrength("FAST_EXECUTION", "脚本执行速度很快", "executionTime"));
        }
        
        // 分析内存使用
        if (metrics.getMemoryUsedBytes() > 100 * 1024 * 1024) { // 100MB
            issues.add(new PerformanceIssue("HIGH_MEMORY", 
                "内存使用量较高", "MEDIUM", "检查内存泄漏或优化数据结构"));
        }
        
        // 分析CPU使用
        if (metrics.getCpuUsagePercent() > 80) {
            issues.add(new PerformanceIssue("HIGH_CPU", 
                "CPU使用率过高", "MEDIUM", "考虑并行化或优化计算密集型操作"));
        }
        
        // 分析退出码
        if (metrics.getExitCode() != 0) {
            issues.add(new PerformanceIssue("NON_ZERO_EXIT", 
                "脚本执行返回错误码", "HIGH", "检查脚本错误并修复"));
        }
        
        // 生成分析数据
        analysisData.put("executionTimeRanking", calculateExecutionTimeRanking(record));
        analysisData.put("memoryEfficiency", calculateMemoryEfficiency(metrics));
        analysisData.put("resourceUtilization", calculateResourceUtilization(metrics));
        
        PerformanceScore overallScore = determineOverallScore(record.getPerformanceScore());
        String summary = generateAnalysisSummary(issues, strengths);
        
        return new PerformanceAnalysis(overallScore, issues, strengths, analysisData, summary);
    }
    
    private PerformanceScore determineOverallScore(double score) {
        if (score >= 4.5) return PerformanceScore.EXCELLENT;
        if (score >= 3.5) return PerformanceScore.GOOD;
        if (score >= 2.5) return PerformanceScore.AVERAGE;
        if (score >= 1.5) return PerformanceScore.POOR;
        return PerformanceScore.CRITICAL;
    }
    
    private String generateAnalysisSummary(List<PerformanceIssue> issues, List<PerformanceStrength> strengths) {
        StringBuilder summary = new StringBuilder();
        
        if (!strengths.isEmpty()) {
            summary.append("性能优势: ").append(strengths.size()).append("项; ");
        }
        
        if (!issues.isEmpty()) {
            summary.append("性能问题: ").append(issues.size()).append("项");
        }
        
        if (summary.length() == 0) {
            summary.append("性能表现一般");
        }
        
        return summary.toString();
    }
    
    private List<String> generateRecommendations(PerformanceRecord record, PerformanceAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();
        
        // 根据问题生成建议
        for (PerformanceIssue issue : analysis.getIssues()) {
            switch (issue.getType()) {
                case "SLOW_EXECUTION":
                    recommendations.add("优化脚本算法或添加缓存机制");
                    break;
                case "HIGH_MEMORY":
                    recommendations.add("检查内存泄漏，优化数据结构");
                    break;
                case "HIGH_CPU":
                    recommendations.add("考虑并行处理或算法优化");
                    break;
                case "NON_ZERO_EXIT":
                    recommendations.add("修复脚本错误，确保正常退出");
                    break;
            }
        }
        
        // 根据性能分数生成建议
        if (record.getPerformanceScore() < 3.0) {
            recommendations.add("建议进行全面的性能优化");
        }
        
        return recommendations;
    }
    
    private List<PerformanceAnomaly> detectAnomalies(PerformanceRecord record) {
        List<PerformanceAnomaly> anomalies = new ArrayList<>();
        
        // 检测执行时间异常
        if (isExecutionTimeAnomaly(record)) {
            anomalies.add(new PerformanceAnomaly(record.getSessionId(), AnomalyType.SLOW_EXECUTION,
                "执行时间异常", 0.8, LocalDateTime.now(), 
                Map.of("executionTime", record.getMetrics().getExecutionTimeMs())));
        }
        
        // 检测内存使用异常
        if (isMemoryAnomaly(record)) {
            anomalies.add(new PerformanceAnomaly(record.getSessionId(), AnomalyType.HIGH_MEMORY_USAGE,
                "内存使用异常", 0.7, LocalDateTime.now(),
                Map.of("memoryUsed", record.getMetrics().getMemoryUsedBytes())));
        }
        
        // 检测CPU使用异常
        if (isCpuAnomaly(record)) {
            anomalies.add(new PerformanceAnomaly(record.getSessionId(), AnomalyType.HIGH_CPU_USAGE,
                "CPU使用异常", 0.6, LocalDateTime.now(),
                Map.of("cpuUsage", record.getMetrics().getCpuUsagePercent())));
        }
        
        return anomalies;
    }
    
    private boolean isExecutionTimeAnomaly(PerformanceRecord record) {
        // 简化的异常检测逻辑
        return record.getMetrics().getExecutionTimeMs() > 30000; // 30秒
    }
    
    private boolean isMemoryAnomaly(PerformanceRecord record) {
        return record.getMetrics().getMemoryUsedBytes() > 500 * 1024 * 1024; // 500MB
    }
    
    private boolean isCpuAnomaly(PerformanceRecord record) {
        return record.getMetrics().getCpuUsagePercent() > 90.0;
    }
    
    private void updateStatistics(MonitoringSession session, ScriptExecutionResult result) {
        totalExecutions.incrementAndGet();
        if (result.getExitCode() != 0) {
            totalErrors.incrementAndGet();
        }
    }
    
    private List<PerformanceRecord> filterRecords(PerformanceQuery query) {
        if (query == null) {
            return new ArrayList<>(historicalRecords);
        }
        
        return historicalRecords.stream()
            .filter(record -> {
                if (query.getScriptPath() != null && !record.getScriptPath().contains(query.getScriptPath())) {
                    return false;
                }
                if (query.getScriptType() != null && record.getScriptType() != query.getScriptType()) {
                    return false;
                }
                if (query.getUserId() != null && !query.getUserId().equals(record.getUserId())) {
                    return false;
                }
                if (query.getStartTime() != null && record.getTimestamp().isBefore(query.getStartTime())) {
                    return false;
                }
                if (query.getEndTime() != null && record.getTimestamp().isAfter(query.getEndTime())) {
                    return false;
                }
                if (query.getMinExecutionTime() != null && 
                    record.getMetrics().getExecutionTimeMs() < query.getMinExecutionTime()) {
                    return false;
                }
                if (query.getMaxExecutionTime() != null && 
                    record.getMetrics().getExecutionTimeMs() > query.getMaxExecutionTime()) {
                    return false;
                }
                if (query.getMinCpuUsage() != null && 
                    record.getMetrics().getCpuUsagePercent() < query.getMinCpuUsage()) {
                    return false;
                }
                if (query.getMaxCpuUsage() != null && 
                    record.getMetrics().getCpuUsagePercent() > query.getMaxCpuUsage()) {
                    return false;
                }
                return true;
            })
            .skip(query.getOffset())
            .limit(query.getLimit())
            .collect(Collectors.toList());
    }
    
    private List<PerformancePercentile> calculatePercentiles(List<Long> values) {
        if (values.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PerformancePercentile> percentiles = new ArrayList<>();
        
        // 计算常用百分位数
        double[] percentileValues = {50, 75, 90, 95, 99};
        
        for (double p : percentileValues) {
            long index = (long) Math.ceil(values.size() * p / 100) - 1;
            index = Math.max(0, Math.min(values.size() - 1, index));
            percentiles.add(new PerformancePercentile(p, values.get((int) index)));
        }
        
        return percentiles;
    }
    
    private int calculateExecutionTimeRanking(PerformanceRecord record) {
        List<Long> executionTimes = historicalRecords.stream()
            .map(r -> r.getMetrics().getExecutionTimeMs())
            .sorted()
            .collect(Collectors.toList());
        
        long currentTime = record.getMetrics().getExecutionTimeMs();
        
        for (int i = 0; i < executionTimes.size(); i++) {
            if (executionTimes.get(i) >= currentTime) {
                return i + 1;
            }
        }
        
        return executionTimes.size();
    }
    
    private double calculateMemoryEfficiency(PerformanceMetrics metrics) {
        if (metrics.getExecutionTimeMs() == 0) {
            return 0.0;
        }
        
        // 简化的内存效率计算：执行时间越短，内存使用越少，效率越高
        double timeScore = Math.max(0, 1.0 - (metrics.getExecutionTimeMs() / 60000.0)); // 60秒基准
        double memoryScore = Math.max(0, 1.0 - (metrics.getMemoryUsedBytes() / (1024 * 1024 * 1024.0))); // 1GB基准
        
        return (timeScore + memoryScore) / 2.0;
    }
    
    private double calculateResourceUtilization(PerformanceMetrics metrics) {
        // 计算资源利用率（CPU + 内存）
        double cpuUtilization = Math.min(1.0, metrics.getCpuUsagePercent() / 100.0);
        double memoryUtilization = Math.min(1.0, metrics.getMemoryUsedBytes() / (1024.0 * 1024 * 1024)); // 1GB基准
        
        return (cpuUtilization + memoryUtilization) / 2.0;
    }
    
    /**
     * 监控配置类
     */
    public static class MonitorConfiguration {
        private boolean enableRealTimeMonitoring = true;
        private boolean enableAnomalyDetection = true;
        private int maxHistoricalRecords = 10000;
        private long monitoringIntervalMs = 1000; // 1秒
        
        public MonitorConfiguration() {}
        
        public boolean isEnableRealTimeMonitoring() { return enableRealTimeMonitoring; }
        public void setEnableRealTimeMonitoring(boolean enableRealTimeMonitoring) { 
            this.enableRealTimeMonitoring = enableRealTimeMonitoring; 
        }
        
        public boolean isEnableAnomalyDetection() { return enableAnomalyDetection; }
        public void setEnableAnomalyDetection(boolean enableAnomalyDetection) { 
            this.enableAnomalyDetection = enableAnomalyDetection; 
        }
        
        public int getMaxHistoricalRecords() { return maxHistoricalRecords; }
        public void setMaxHistoricalRecords(int maxHistoricalRecords) { 
            this.maxHistoricalRecords = maxHistoricalRecords; 
        }
        
        public long getMonitoringIntervalMs() { return monitoringIntervalMs; }
        public void setMonitoringIntervalMs(long monitoringIntervalMs) { 
            this.monitoringIntervalMs = monitoringIntervalMs; 
        }
    }
}
