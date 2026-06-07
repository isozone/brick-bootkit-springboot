package com.zqzqq.bootkits.scripts.batch;

import com.zqzqq.bootkits.scripts.core.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 脚本批量操作管理器接口
 * 提供批量脚本执行、管理和监控功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptBatchManager {
    
    /**
     * 批量操作类型
     */
    enum BatchOperationType {
        /**
         * 批量执行脚本
         */
        EXECUTE,
        /**
         * 批量复制脚本
         */
        COPY,
        /**
         * 批量删除脚本
         */
        DELETE,
        /**
         * 批量移动脚本
         */
        MOVE,
        /**
         * 批量更新脚本
         */
        UPDATE,
        /**
         * 批量验证脚本
         */
        VALIDATE,
        /**
         * 批量编译脚本
         */
        COMPILE,
        /**
         * 批量打包脚本
         */
        PACKAGE,
        /**
         * 批量部署脚本
         */
        DEPLOY
    }
    
    /**
     * 批量操作状态
     */
    enum BatchOperationStatus {
        /**
         * 等待执行
         */
        PENDING,
        /**
         * 正在执行
         */
        RUNNING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 执行失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED,
        /**
         * 暂停
         */
        PAUSED,
        /**
         * 部分完成
         */
        PARTIALLY_COMPLETED
    }
    
    /**
     * 批量操作执行策略
     */
    enum ExecutionStrategy {
        /**
         * 顺序执行
         */
        SEQUENTIAL,
        /**
         * 并行执行
         */
        PARALLEL,
        /**
         * 优先级执行
         */
        PRIORITY,
        /**
         * 负载均衡执行
         */
        LOAD_BALANCED
    }
    
    /**
     * 批量操作
     */
    class BatchOperation {
        private final String operationId;
        private final BatchOperationType operationType;
        private final ExecutionStrategy executionStrategy;
        private final LocalDateTime createdTime;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final BatchOperationStatus status;
        private final int totalOperations;
        private final int completedOperations;
        private final int failedOperations;
        private final int cancelledOperations;
        private final long totalExecutionTimeMs;
        private final List<ScriptExecutionResult> results;
        private final List<Exception> errors;
        private final Map<String, Object> metadata;
        private final ExecutorService executorService;
        private final boolean allowPartialSuccess;
        private final int maxConcurrency;
        private final long timeoutMs;
        
        public BatchOperation(String operationId, BatchOperationType operationType, ExecutionStrategy executionStrategy,
                            LocalDateTime createdTime, LocalDateTime startTime, LocalDateTime endTime,
                            BatchOperationStatus status, int totalOperations, int completedOperations,
                            int failedOperations, int cancelledOperations, long totalExecutionTimeMs,
                            List<ScriptExecutionResult> results, List<Exception> errors,
                            Map<String, Object> metadata, ExecutorService executorService,
                            boolean allowPartialSuccess, int maxConcurrency, long timeoutMs) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.executionStrategy = executionStrategy;
            this.createdTime = createdTime;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.totalOperations = totalOperations;
            this.completedOperations = completedOperations;
            this.failedOperations = failedOperations;
            this.cancelledOperations = cancelledOperations;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
            this.results = results;
            this.errors = errors;
            this.metadata = metadata;
            this.executorService = executorService;
            this.allowPartialSuccess = allowPartialSuccess;
            this.maxConcurrency = maxConcurrency;
            this.timeoutMs = timeoutMs;
        }
        
        // Getters
        public String getOperationId() { return operationId; }
        public BatchOperationType getOperationType() { return operationType; }
        public ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public BatchOperationStatus getStatus() { return status; }
        public int getTotalOperations() { return totalOperations; }
        public int getCompletedOperations() { return completedOperations; }
        public int getFailedOperations() { return failedOperations; }
        public int getCancelledOperations() { return cancelledOperations; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public List<ScriptExecutionResult> getResults() { return results; }
        public List<Exception> getErrors() { return errors; }
        public Map<String, Object> getMetadata() { return metadata; }
        public ExecutorService getExecutorService() { return executorService; }
        public boolean isAllowPartialSuccess() { return allowPartialSuccess; }
        public int getMaxConcurrency() { return maxConcurrency; }
        public long getTimeoutMs() { return timeoutMs; }
        
        // 计算方法
        public double getSuccessRate() {
            return totalOperations > 0 ? (double) completedOperations / totalOperations : 0.0;
        }
        
        public double getFailureRate() {
            return totalOperations > 0 ? (double) failedOperations / totalOperations : 0.0;
        }
        
        public boolean isCompleted() {
            return status == BatchOperationStatus.COMPLETED || 
                   status == BatchOperationStatus.FAILED ||
                   status == BatchOperationStatus.CANCELLED ||
                   status == BatchOperationStatus.PARTIALLY_COMPLETED;
        }
        
        public boolean isSuccessful() {
            return status == BatchOperationStatus.COMPLETED || 
                   (status == BatchOperationStatus.PARTIALLY_COMPLETED && allowPartialSuccess);
        }
        
        public long getAverageExecutionTime() {
            return completedOperations > 0 ? totalExecutionTimeMs / completedOperations : 0;
        }
        
        @Override
        public String toString() {
            return String.format("BatchOperation{id='%s', type=%s, status=%s, progress=%d/%d, successRate=%.2f%%}",
                operationId, operationType, status, completedOperations, totalOperations, getSuccessRate() * 100);
        }
    }
    
    /**
     * 批量操作结果
     */
    class BatchOperationResult {
        private final boolean success;
        private final String message;
        private final BatchOperation operation;
        private final long operationTimeMs;
        private final List<ScriptExecutionResult> successfulResults;
        private final List<Exception> failures;
        private final Map<String, Object> summary;
        
        public BatchOperationResult(boolean success, String message, BatchOperation operation,
                                  long operationTimeMs, List<ScriptExecutionResult> successfulResults,
                                  List<Exception> failures, Map<String, Object> summary) {
            this.success = success;
            this.message = message;
            this.operation = operation;
            this.operationTimeMs = operationTimeMs;
            this.successfulResults = successfulResults;
            this.failures = failures;
            this.summary = summary;
        }
        
        public static BatchOperationResult success(String message, BatchOperation operation,
                                                 List<ScriptExecutionResult> successfulResults,
                                                 List<Exception> failures, Map<String, Object> summary) {
            return new BatchOperationResult(true, message, operation, 0, successfulResults, failures, summary);
        }
        
        public static BatchOperationResult failure(String message, BatchOperation operation) {
            return new BatchOperationResult(false, message, operation, 0, List.of(), List.of(), Map.of());
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BatchOperation getOperation() { return operation; }
        public long getOperationTimeMs() { return operationTimeMs; }
        public List<ScriptExecutionResult> getSuccessfulResults() { return successfulResults; }
        public List<Exception> getFailures() { return failures; }
        public Map<String, Object> getSummary() { return summary; }
    }
    
    /**
     * 批量操作配置
     */
    class BatchOperationConfiguration {
        private int defaultMaxConcurrency = 10;
        private long defaultTimeoutMs = 300000; // 5分钟
        private boolean defaultAllowPartialSuccess = true;
        private ExecutionStrategy defaultExecutionStrategy = ExecutionStrategy.PARALLEL;
        private boolean enableProgressMonitoring = true;
        private boolean enableErrorRecovery = false;
        private int retryAttempts = 3;
        private long retryDelayMs = 1000;
        private boolean enableResourceOptimization = true;
        private long cleanupIntervalMs = 60000; // 1分钟
        private int maxOperationsPerBatch = 1000;
        private boolean enableDetailedLogging = true;
        
        // Getters and Setters
        public int getDefaultMaxConcurrency() { return defaultMaxConcurrency; }
        public void setDefaultMaxConcurrency(int defaultMaxConcurrency) { this.defaultMaxConcurrency = defaultMaxConcurrency; }
        
        public long getDefaultTimeoutMs() { return defaultTimeoutMs; }
        public void setDefaultTimeoutMs(long defaultTimeoutMs) { this.defaultTimeoutMs = defaultTimeoutMs; }
        
        public boolean isDefaultAllowPartialSuccess() { return defaultAllowPartialSuccess; }
        public void setDefaultAllowPartialSuccess(boolean defaultAllowPartialSuccess) { this.defaultAllowPartialSuccess = defaultAllowPartialSuccess; }
        
        public ExecutionStrategy getDefaultExecutionStrategy() { return defaultExecutionStrategy; }
        public void setDefaultExecutionStrategy(ExecutionStrategy defaultExecutionStrategy) { this.defaultExecutionStrategy = defaultExecutionStrategy; }
        
        public boolean isEnableProgressMonitoring() { return enableProgressMonitoring; }
        public void setEnableProgressMonitoring(boolean enableProgressMonitoring) { this.enableProgressMonitoring = enableProgressMonitoring; }
        
        public boolean isEnableErrorRecovery() { return enableErrorRecovery; }
        public void setEnableErrorRecovery(boolean enableErrorRecovery) { this.enableErrorRecovery = enableErrorRecovery; }
        
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
        
        public boolean isEnableResourceOptimization() { return enableResourceOptimization; }
        public void setEnableResourceOptimization(boolean enableResourceOptimization) { this.enableResourceOptimization = enableResourceOptimization; }
        
        public long getCleanupIntervalMs() { return cleanupIntervalMs; }
        public void setCleanupIntervalMs(long cleanupIntervalMs) { this.cleanupIntervalMs = cleanupIntervalMs; }
        
        public int getMaxOperationsPerBatch() { return maxOperationsPerBatch; }
        public void setMaxOperationsPerBatch(int maxOperationsPerBatch) { this.maxOperationsPerBatch = maxOperationsPerBatch; }
        
        public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
        public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
    }
    
    /**
     * 批量执行脚本
     *
     * @param scripts 脚本列表
     * @param parameters 参数映射
     * @param configuration 执行配置
     * @return 批量操作结果
     */
    BatchOperationResult executeBatch(List<ScriptConfiguration> scripts, Map<String, Object> parameters, 
                                    BatchOperationConfiguration configuration);
    
    /**
     * 从模板批量创建和执行脚本
     *
     * @param templateIds 模板ID列表
     * @param variableMappings 变量映射列表
     * @param configuration 执行配置
     * @return 批量操作结果
     */
    BatchOperationResult executeFromTemplates(List<String> templateIds, List<Map<String, Object>> variableMappings,
                                            BatchOperationConfiguration configuration);
    
    /**
     * 批量复制脚本
     *
     * @param sourcePaths 源路径列表
     * @param targetPaths 目标路径列表
     * @param configuration 操作配置
     * @return 批量操作结果
     */
    BatchOperationResult copyBatch(List<String> sourcePaths, List<String> targetPaths,
                                 BatchOperationConfiguration configuration);
    
    /**
     * 批量删除脚本
     *
     * @param scriptPaths 脚本路径列表
     * @param configuration 操作配置
     * @return 批量操作结果
     */
    BatchOperationResult deleteBatch(List<String> scriptPaths, BatchOperationConfiguration configuration);
    
    /**
     * 批量移动脚本
     *
     * @param sourcePaths 源路径列表
     * @param targetPaths 目标路径列表
     * @param configuration 操作配置
     * @return 批量操作结果
     */
    BatchOperationResult moveBatch(List<String> sourcePaths, List<String> targetPaths,
                                 BatchOperationConfiguration configuration);
    
    /**
     * 批量更新脚本
     *
     * @param updates 更新列表
     * @param configuration 操作配置
     * @return 批量操作结果
     */
    BatchOperationResult updateBatch(List<ScriptUpdate> updates, BatchOperationConfiguration configuration);
    
    /**
     * 批量验证脚本
     *
     * @param scriptPaths 脚本路径列表
     * @param configuration 验证配置
     * @return 批量操作结果
     */
    BatchOperationResult validateBatch(List<String> scriptPaths, BatchOperationConfiguration configuration);
    
    /**
     * 取消批量操作
     *
     * @param operationId 操作ID
     * @return 是否成功取消
     */
    boolean cancelBatchOperation(String operationId);
    
    /**
     * 暂停批量操作
     *
     * @param operationId 操作ID
     * @return 是否成功暂停
     */
    boolean pauseBatchOperation(String operationId);
    
    /**
     * 恢复批量操作
     *
     * @param operationId 操作ID
     * @return 是否成功恢复
     */
    boolean resumeBatchOperation(String operationId);
    
    /**
     * 获取批量操作状态
     *
     * @param operationId 操作ID
     * @return 批量操作对象
     */
    BatchOperation getBatchOperation(String operationId);
    
    /**
     * 获取所有批量操作
     *
     * @return 批量操作列表
     */
    List<BatchOperation> getAllBatchOperations();
    
    /**
     * 获取运行中的批量操作
     *
     * @return 运行中的批量操作列表
     */
    List<BatchOperation> getRunningBatchOperations();
    
    /**
     * 获取批量操作历史
     *
     * @param limit 限制数量
     * @return 历史操作列表
     */
    List<BatchOperation> getBatchOperationHistory(int limit);
    
    /**
     * 清理完成的批量操作
     *
     * @param olderThanMinutes 清理多少分钟前的操作
     * @return 清理的数量
     */
    int cleanupCompletedOperations(int olderThanMinutes);
    
    /**
     * 获取批量操作统计信息
     *
     * @return 统计信息
     */
    BatchOperationStatistics getStatistics();
    
    /**
     * 设置批量操作配置
     *
     * @param configuration 配置
     */
    void setConfiguration(BatchOperationConfiguration configuration);
    
    /**
     * 获取批量操作配置
     *
     * @return 配置
     */
    BatchOperationConfiguration getConfiguration();
    
    /**
     * 获取批量操作报告
     *
     * @param operationId 操作ID
     * @return 操作报告
     */
    String getOperationReport(String operationId);
    
    /**
     * 批量操作统计信息
     */
    class BatchOperationStatistics {
        private final long totalBatchOperations;
        private final long completedOperations;
        private final long failedOperations;
        private final long runningOperations;
        private final long totalScriptsProcessed;
        private final long totalExecutionTimeMs;
        private final double averageSuccessRate;
        private final Map<BatchOperationType, Long> operationsByType;
        private final Map<ExecutionStrategy, Long> operationsByStrategy;
        private final Map<String, Long> operationsByStatus;
        private final LocalDateTime lastOperationTime;
        
        public BatchOperationStatistics(long totalBatchOperations, long completedOperations, long failedOperations,
                                      long runningOperations, long totalScriptsProcessed, long totalExecutionTimeMs,
                                      double averageSuccessRate, Map<BatchOperationType, Long> operationsByType,
                                      Map<ExecutionStrategy, Long> operationsByStrategy,
                                      Map<String, Long> operationsByStatus, LocalDateTime lastOperationTime) {
            this.totalBatchOperations = totalBatchOperations;
            this.completedOperations = completedOperations;
            this.failedOperations = failedOperations;
            this.runningOperations = runningOperations;
            this.totalScriptsProcessed = totalScriptsProcessed;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
            this.averageSuccessRate = averageSuccessRate;
            this.operationsByType = operationsByType;
            this.operationsByStrategy = operationsByStrategy;
            this.operationsByStatus = operationsByStatus;
            this.lastOperationTime = lastOperationTime;
        }
        
        // Getters
        public long getTotalBatchOperations() { return totalBatchOperations; }
        public long getCompletedOperations() { return completedOperations; }
        public long getFailedOperations() { return failedOperations; }
        public long getRunningOperations() { return runningOperations; }
        public long getTotalScriptsProcessed() { return totalScriptsProcessed; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public double getAverageSuccessRate() { return averageSuccessRate; }
        public Map<BatchOperationType, Long> getOperationsByType() { return operationsByType; }
        public Map<ExecutionStrategy, Long> getOperationsByStrategy() { return operationsByStrategy; }
        public Map<String, Long> getOperationsByStatus() { return operationsByStatus; }
        public LocalDateTime getLastOperationTime() { return lastOperationTime; }
        
        public long getAverageExecutionTime() {
            return totalBatchOperations > 0 ? totalExecutionTimeMs / totalBatchOperations : 0;
        }
        
        @Override
        public String toString() {
            return String.format("BatchOperationStatistics{total=%d, completed=%d, failed=%d, running=%d, successRate=%.2f%%}",
                totalBatchOperations, completedOperations, failedOperations, runningOperations, averageSuccessRate * 100);
        }
    }
    
    /**
     * 脚本更新信息
     */
    class ScriptUpdate {
        private final String scriptPath;
        private final String newContent;
        private final ScriptType newType;
        private final Map<String, Object> newMetadata;
        
        public ScriptUpdate(String scriptPath, String newContent, ScriptType newType, Map<String, Object> newMetadata) {
            this.scriptPath = scriptPath;
            this.newContent = newContent;
            this.newType = newType;
            this.newMetadata = newMetadata;
        }
        
        public String getScriptPath() { return scriptPath; }
        public String getNewContent() { return newContent; }
        public ScriptType getNewType() { return newType; }
        public Map<String, Object> getNewMetadata() { return newMetadata; }
    }
}
