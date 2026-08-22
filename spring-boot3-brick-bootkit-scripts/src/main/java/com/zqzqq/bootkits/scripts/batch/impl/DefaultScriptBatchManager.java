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


package com.zqzqq.bootkits.scripts.batch.impl;

import com.zqzqq.bootkits.scripts.batch.*;
import com.zqzqq.bootkits.scripts.core.*;
import com.zqzqq.bootkits.scripts.core.impl.DefaultScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 脚本批量操作管理器默认实现
 * 提供高效的批量脚本执行和管理功能，支持多种执行策略和错误处理
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptBatchManager implements ScriptBatchManager, AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptBatchManager.class);
    
    /**
     * 线程池关闭超时时间（秒）
     */
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    
    // 存储运行中的批量操作
    private final Map<String, BatchOperation> runningOperations = new ConcurrentHashMap<>();
    private final Map<String, BatchOperation> completedOperations = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong totalBatchOperations = new AtomicLong(0);
    private final AtomicLong completedBatchOperations = new AtomicLong(0);
    private final AtomicLong failedBatchOperations = new AtomicLong(0);
    private final AtomicLong totalScriptsProcessed = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // 组件
    private final ScriptManager scriptManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 配置
    private BatchOperationConfiguration configuration;
    
    // 锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 构造函数
     */
    public DefaultScriptBatchManager() {
        this(null);
    }
    
    /**
     * 构造函数
     *
     * @param scriptManager 脚本管理器
     */
    public DefaultScriptBatchManager(ScriptManager scriptManager) {
        this.scriptManager = scriptManager != null ? scriptManager : new DefaultScriptManager();
        this.configuration = new BatchOperationConfiguration();
        
        // 创建执行器
        this.executorService = Executors.newFixedThreadPool(
            configuration.getDefaultMaxConcurrency(),
            r -> {
                Thread t = new Thread(r, "batch-executor");
                t.setDaemon(true);
                return t;
            }
        );
        
        // 创建清理执行器
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "batch-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // 启动清理任务
        startCleanupTask();
        
        logger.info("脚本批量操作管理器已初始化");
    }
    
    /**
     * 构造函数
     *
     * @param scriptManager 脚本管理器
     * @param configuration 配置
     */
    public DefaultScriptBatchManager(ScriptManager scriptManager, BatchOperationConfiguration configuration) {
        this.scriptManager = scriptManager != null ? scriptManager : new DefaultScriptManager();
        this.configuration = configuration != null ? configuration : new BatchOperationConfiguration();
        
        this.executorService = Executors.newFixedThreadPool(
            configuration.getDefaultMaxConcurrency(),
            r -> {
                Thread t = new Thread(r, "batch-executor");
                t.setDaemon(true);
                return t;
            }
        );
        
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "batch-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
    }
    
    @Override
    public BatchOperationResult executeBatch(List<ScriptConfiguration> scripts, Map<String, Object> parameters,
                                           BatchOperationConfiguration configuration) {
        if (scripts == null || scripts.isEmpty()) {
            return BatchOperationResult.failure("脚本列表不能为空", null);
        }
        
        // 使用提供的配置或默认配置
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        // 验证配置
        if (scripts.size() > config.getMaxOperationsPerBatch()) {
            return BatchOperationResult.failure(
                String.format("批量操作数量超过限制: %d > %d", scripts.size(), config.getMaxOperationsPerBatch()), 
                null);
        }
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        // 创建批量操作对象
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.EXECUTE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            scripts.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("parameters", parameters), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        // 存储操作
        runningOperations.put(operationId, operation);
        
        try {
            // 执行批量操作
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performExecuteBatch(operation, scripts, parameters, config);
                } catch (Exception e) {
                    logger.error("批量执行异常", e);
                    return BatchOperationResult.failure("批量执行异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            // 等待完成或超时
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量执行超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量执行超时", operation);
            } catch (Exception e) {
                logger.error("批量执行失败", e);
                return BatchOperationResult.failure("批量执行失败: " + e.getMessage(), operation);
            }
            
            // 更新统计
            updateStatistics(result);
            
            return result;
            
        } finally {
            // 移动到完成操作列表
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult executeFromTemplates(List<String> templateIds, List<Map<String, Object>> variableMappings,
                                                    BatchOperationConfiguration configuration) {
        if (templateIds == null || templateIds.isEmpty()) {
            return BatchOperationResult.failure("模板ID列表不能为空", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        // 验证输入
        if (variableMappings != null && variableMappings.size() != templateIds.size()) {
            return BatchOperationResult.failure("变量映射列表大小与模板ID列表不匹配", null);
        }
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.EXECUTE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            templateIds.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("templateIds", templateIds), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performExecuteFromTemplates(operation, templateIds, variableMappings, config);
                } catch (Exception e) {
                    logger.error("模板批量执行异常", e);
                    return BatchOperationResult.failure("模板批量执行异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("模板批量执行超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("模板批量执行超时", operation);
            } catch (Exception e) {
                logger.error("模板批量执行失败", e);
                return BatchOperationResult.failure("模板批量执行失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult copyBatch(List<String> sourcePaths, List<String> targetPaths,
                                        BatchOperationConfiguration configuration) {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            return BatchOperationResult.failure("源路径列表不能为空", null);
        }
        
        if (targetPaths == null || targetPaths.size() != sourcePaths.size()) {
            return BatchOperationResult.failure("目标路径列表无效", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.COPY, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            sourcePaths.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("sourcePaths", sourcePaths, "targetPaths", targetPaths), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performCopyBatch(operation, sourcePaths, targetPaths, config);
                } catch (Exception e) {
                    logger.error("批量复制异常", e);
                    return BatchOperationResult.failure("批量复制异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量复制超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量复制超时", operation);
            } catch (Exception e) {
                logger.error("批量复制失败", e);
                return BatchOperationResult.failure("批量复制失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult deleteBatch(List<String> scriptPaths, BatchOperationConfiguration configuration) {
        if (scriptPaths == null || scriptPaths.isEmpty()) {
            return BatchOperationResult.failure("脚本路径列表不能为空", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.DELETE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            scriptPaths.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("scriptPaths", scriptPaths), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performDeleteBatch(operation, scriptPaths, config);
                } catch (Exception e) {
                    logger.error("批量删除异常", e);
                    return BatchOperationResult.failure("批量删除异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量删除超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量删除超时", operation);
            } catch (Exception e) {
                logger.error("批量删除失败", e);
                return BatchOperationResult.failure("批量删除失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult moveBatch(List<String> sourcePaths, List<String> targetPaths,
                                        BatchOperationConfiguration configuration) {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            return BatchOperationResult.failure("源路径列表不能为空", null);
        }
        
        if (targetPaths == null || targetPaths.size() != sourcePaths.size()) {
            return BatchOperationResult.failure("目标路径列表无效", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.MOVE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            sourcePaths.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("sourcePaths", sourcePaths, "targetPaths", targetPaths), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performMoveBatch(operation, sourcePaths, targetPaths, config);
                } catch (Exception e) {
                    logger.error("批量移动异常", e);
                    return BatchOperationResult.failure("批量移动异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量移动超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量移动超时", operation);
            } catch (Exception e) {
                logger.error("批量移动失败", e);
                return BatchOperationResult.failure("批量移动失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult updateBatch(List<ScriptUpdate> updates, BatchOperationConfiguration configuration) {
        if (updates == null || updates.isEmpty()) {
            return BatchOperationResult.failure("更新列表不能为空", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.UPDATE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            updates.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("updates", updates), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performUpdateBatch(operation, updates, config);
                } catch (Exception e) {
                    logger.error("批量更新异常", e);
                    return BatchOperationResult.failure("批量更新异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量更新超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量更新超时", operation);
            } catch (Exception e) {
                logger.error("批量更新失败", e);
                return BatchOperationResult.failure("批量更新失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public BatchOperationResult validateBatch(List<String> scriptPaths, BatchOperationConfiguration configuration) {
        if (scriptPaths == null || scriptPaths.isEmpty()) {
            return BatchOperationResult.failure("脚本路径列表不能为空", null);
        }
        
        BatchOperationConfiguration config = configuration != null ? configuration : this.configuration;
        
        String operationId = generateOperationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchOperation operation = new BatchOperation(
            operationId, BatchOperationType.VALIDATE, config.getDefaultExecutionStrategy(),
            startTime, startTime, null, BatchOperationStatus.RUNNING,
            scriptPaths.size(), 0, 0, 0, 0, List.of(), List.of(),
            Map.of("scriptPaths", scriptPaths), executorService,
            config.isDefaultAllowPartialSuccess(), config.getDefaultMaxConcurrency(),
            config.getDefaultTimeoutMs()
        );
        
        runningOperations.put(operationId, operation);
        
        try {
            CompletableFuture<BatchOperationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performValidateBatch(operation, scriptPaths, config);
                } catch (Exception e) {
                    logger.error("批量验证异常", e);
                    return BatchOperationResult.failure("批量验证异常: " + e.getMessage(), operation);
                }
            }, executorService);
            
            BatchOperationResult result;
            try {
                if (config.getDefaultTimeoutMs() > 0) {
                    result = future.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
                } else {
                    result = future.get();
                }
            } catch (TimeoutException e) {
                logger.warn("批量验证超时: {}", operationId);
                cancelBatchOperation(operationId);
                return BatchOperationResult.failure("批量验证超时", operation);
            } catch (Exception e) {
                logger.error("批量验证失败", e);
                return BatchOperationResult.failure("批量验证失败: " + e.getMessage(), operation);
            }
            
            updateStatistics(result);
            return result;
            
        } finally {
            lock.writeLock().lock();
            try {
                runningOperations.remove(operationId);
                completedOperations.put(operationId, operation);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public boolean cancelBatchOperation(String operationId) {
        if (operationId == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            BatchOperation operation = runningOperations.get(operationId);
            if (operation != null && operation.getStatus() == BatchOperationStatus.RUNNING) {
                // 标记为取消状态（实际取消需要通过线程中断实现）
                logger.info("取消批量操作: {}", operationId);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean pauseBatchOperation(String operationId) {
        if (operationId == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            BatchOperation operation = runningOperations.get(operationId);
            if (operation != null && operation.getStatus() == BatchOperationStatus.RUNNING) {
                // 标记为暂停状态
                logger.info("暂停批量操作: {}", operationId);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean resumeBatchOperation(String operationId) {
        if (operationId == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            BatchOperation operation = runningOperations.get(operationId);
            if (operation != null && operation.getStatus() == BatchOperationStatus.PAUSED) {
                // 恢复执行
                logger.info("恢复批量操作: {}", operationId);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public BatchOperation getBatchOperation(String operationId) {
        if (operationId == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            BatchOperation operation = runningOperations.get(operationId);
            if (operation != null) {
                return operation;
            }
            return completedOperations.get(operationId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<BatchOperation> getAllBatchOperations() {
        lock.readLock().lock();
        try {
            List<BatchOperation> allOperations = new ArrayList<>();
            allOperations.addAll(runningOperations.values());
            allOperations.addAll(completedOperations.values());
            return allOperations;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<BatchOperation> getRunningBatchOperations() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(runningOperations.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<BatchOperation> getBatchOperationHistory(int limit) {
        lock.readLock().lock();
        try {
            return completedOperations.values().stream()
                .sorted(Comparator.comparing(BatchOperation::getEndTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public int cleanupCompletedOperations(int olderThanMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(olderThanMinutes);
        
        lock.writeLock().lock();
        try {
            int cleanedCount = 0;
            Iterator<Map.Entry<String, BatchOperation>> iterator = completedOperations.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<String, BatchOperation> entry = iterator.next();
                BatchOperation operation = entry.getValue();
                
                if (operation.getEndTime() != null && operation.getEndTime().isBefore(cutoffTime)) {
                    iterator.remove();
                    cleanedCount++;
                }
            }
            
            logger.info("清理完成的批量操作: {} 个", cleanedCount);
            return cleanedCount;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public BatchOperationStatistics getStatistics() {
        lock.readLock().lock();
        try {
            long totalOps = totalBatchOperations.get();
            long completedOps = completedBatchOperations.get();
            long failedOps = failedBatchOperations.get();
            long runningOps = runningOperations.size();
            
            double successRate = totalOps > 0 ? (double) completedOps / totalOps : 0.0;
            
            // 按类型统计
            Map<BatchOperationType, Long> operationsByType = getAllBatchOperations().stream()
                .collect(Collectors.groupingBy(BatchOperation::getOperationType, Collectors.counting()));
            
            // 按策略统计
            Map<ExecutionStrategy, Long> operationsByStrategy = getAllBatchOperations().stream()
                .collect(Collectors.groupingBy(BatchOperation::getExecutionStrategy, Collectors.counting()));
            
            // 按状态统计
            Map<String, Long> operationsByStatus = getAllBatchOperations().stream()
                .collect(Collectors.groupingBy(op -> op.getStatus().name(), Collectors.counting()));
            
            LocalDateTime lastOperationTime = getAllBatchOperations().stream()
                .map(BatchOperation::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            
            return new BatchOperationStatistics(
                totalOps, completedOps, failedOps, runningOps,
                totalScriptsProcessed.get(), totalExecutionTime.get(),
                successRate, operationsByType, operationsByStrategy,
                operationsByStatus, lastOperationTime
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void setConfiguration(BatchOperationConfiguration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
            // 重启清理任务
            stopCleanupTask();
            startCleanupTask();
        }
    }
    
    @Override
    public BatchOperationConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public String getOperationReport(String operationId) {
        BatchOperation operation = getBatchOperation(operationId);
        if (operation == null) {
            return "批量操作不存在: " + operationId;
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== 批量操作报告 ===\n\n");
        
        report.append("基本信息:\n");
        report.append(String.format("  操作ID: %s\n", operation.getOperationId()));
        report.append(String.format("  操作类型: %s\n", operation.getOperationType()));
        report.append(String.format("  执行策略: %s\n", operation.getExecutionStrategy()));
        report.append(String.format("  状态: %s\n", operation.getStatus()));
        report.append(String.format("  创建时间: %s\n", operation.getCreatedTime()));
        report.append(String.format("  开始时间: %s\n", operation.getStartTime()));
        if (operation.getEndTime() != null) {
            report.append(String.format("  结束时间: %s\n", operation.getEndTime()));
        }
        report.append(String.format("  总操作数: %d\n", operation.getTotalOperations()));
        report.append(String.format("  完成数: %d\n", operation.getCompletedOperations()));
        report.append(String.format("  失败数: %d\n", operation.getFailedOperations()));
        report.append(String.format("  取消数: %d\n", operation.getCancelledOperations()));
        report.append(String.format("  成功率: %.2f%%\n", operation.getSuccessRate() * 100));
        report.append(String.format("  失败率: %.2f%%\n", operation.getFailureRate() * 100));
        if (operation.getTotalExecutionTimeMs() > 0) {
            report.append(String.format("  总执行时间: %d ms\n", operation.getTotalExecutionTimeMs()));
            report.append(String.format("  平均执行时间: %d ms\n", operation.getAverageExecutionTime()));
        }
        report.append("\n");
        
        if (!operation.getResults().isEmpty()) {
            report.append("执行结果摘要:\n");
            long successCount = operation.getResults().stream()
                .filter(r -> r.isSuccess())
                .count();
            report.append(String.format("  成功: %d\n", successCount));
            report.append(String.format("  失败: %d\n", operation.getTotalOperations() - successCount));
            report.append("\n");
        }
        
        if (!operation.getErrors().isEmpty()) {
            report.append("错误摘要:\n");
            for (int i = 0; i < Math.min(operation.getErrors().size(), 10); i++) {
                Exception error = operation.getErrors().get(i);
                report.append(String.format("  错误%d: %s\n", i + 1, error.getMessage()));
            }
            if (operation.getErrors().size() > 10) {
                report.append(String.format("  ... 还有 %d 个错误\n", operation.getErrors().size() - 10));
            }
            report.append("\n");
        }
        
        return report.toString();
    }
    
    // 私有方法
    
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
            this::performCleanup,
            configuration.getCleanupIntervalMs(),
            configuration.getCleanupIntervalMs(),
            TimeUnit.MILLISECONDS
        );
    }
    
    private void stopCleanupTask() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void performCleanup() {
        try {
            cleanupCompletedOperations(60); // 清理1小时前的操作
        } catch (Exception e) {
            logger.error("清理任务异常", e);
        }
    }
    
    private String generateOperationId() {
        return "batch-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    private BatchOperationResult performExecuteBatch(BatchOperation operation, List<ScriptConfiguration> scripts,
                                                   Map<String, Object> parameters, BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        LocalDateTime startTime = LocalDateTime.now();
        long totalTime = 0;
        
        if (config.getDefaultExecutionStrategy() == ExecutionStrategy.SEQUENTIAL) {
            // 顺序执行
            for (ScriptConfiguration script : scripts) {
                try {
                    long scriptStartTime = System.currentTimeMillis();

                    String[] arguments = convertParametersToArguments(parameters);
                    // 使用正确的executeScript方法签名
                    ScriptExecutionResult result = scriptManager.executeScript(
                            ScriptType.SHELL,
                            script.getScriptContent(),
                            arguments,
                            script
                    );
                    results.add(result);
                    totalScriptsProcessed.incrementAndGet();
                    totalTime += System.currentTimeMillis() - scriptStartTime;
                } catch (Exception e) {
                    errors.add(e);
                    logger.warn("脚本执行失败: {}", script, e);
                }
            }
        } else {
            // 并行执行
            List<CompletableFuture<ScriptExecutionResult>> futures = scripts.stream()
                .map(script -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // 将参数Map转换为String数组
                        String[] arguments = convertParametersToArguments(parameters);
                        // 使用正确的executeScript方法签名
                        return  scriptManager.executeScript(
                                ScriptType.SHELL,
                                script.getScriptContent(),
                                arguments,
                                script
                        );
                    } catch (Exception e) {
                        errors.add(e);
                        logger.warn("脚本执行失败: {}", script, e);
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());
            
            // 等待所有完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            
            try {
                allOf.get(config.getDefaultTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.warn("批量执行超时");
            } catch (Exception e) {
                logger.error("批量执行异常", e);
            }
            
            // 收集结果
            for (CompletableFuture<ScriptExecutionResult> future : futures) {
                try {
                    ScriptExecutionResult result = future.get();
                    if (result != null) {
                        results.add(result);
                        totalScriptsProcessed.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.add(e);
                }
            }
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
        
        // 更新操作状态
        BatchOperationStatus status;
        if (errors.isEmpty()) {
            status = BatchOperationStatus.COMPLETED;
        } else if (errors.size() == results.size()) {
            status = BatchOperationStatus.FAILED;
        } else {
            status = BatchOperationStatus.PARTIALLY_COMPLETED;
        }
        
        BatchOperation updatedOperation = new BatchOperation(
            operation.getOperationId(), operation.getOperationType(), operation.getExecutionStrategy(),
            operation.getCreatedTime(), operation.getStartTime(), endTime,
            status, operation.getTotalOperations(), results.size(), errors.size(), 0,
            executionTime, results, errors, operation.getMetadata(), operation.getExecutorService(),
            operation.isAllowPartialSuccess(), operation.getMaxConcurrency(), operation.getTimeoutMs()
        );
        
        // 更新操作
        lock.writeLock().lock();
        try {
            runningOperations.put(operation.getOperationId(), updatedOperation);
        } finally {
            lock.writeLock().unlock();
        }
        
        Map<String, Object> summary = Map.of(
            "totalTimeMs", executionTime,
            "averageTimePerScript", results.size() > 0 ? executionTime / results.size() : 0,
            "successRate", (double) results.size() / operation.getTotalOperations()
        );
        
        return BatchOperationResult.success("批量执行完成", updatedOperation, results, errors, summary);
    }
    
    private BatchOperationResult performExecuteFromTemplates(BatchOperation operation, List<String> templateIds,
                                                            List<Map<String, Object>> variableMappings,
                                                            BatchOperationConfiguration config) {
        // 简化实现，实际应该使用模板管理器
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < templateIds.size(); i++) {
            try {
                // 模拟从模板创建脚本并执行
                Map<String, Object> parameters = variableMappings != null ? variableMappings.get(i) : Map.of();
                
                ScriptConfiguration scriptConfig = new ScriptConfiguration();
                scriptConfig.setScriptPath("/generated/script-" + i + ".sh");
                scriptConfig.setScriptContent("# Generated from template " + templateIds.get(i));
                
                // 将参数Map转换为String数组
                String[] arguments = convertParametersToArguments(parameters);
                
                // 使用正确的executeScript方法签名
                ScriptExecutionResult result = scriptManager.executeScript(
                    ScriptType.SHELL,
                    scriptConfig.getScriptContent(),
                    arguments,
                    scriptConfig
                );
                results.add(result);
                totalScriptsProcessed.incrementAndGet();
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("模板执行失败: {}", templateIds.get(i), e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "processedTemplates", templateIds.size(),
            "successfulExecutions", results.size(),
            "failedExecutions", errors.size()
        );
        
        return BatchOperationResult.success("模板批量执行完成", operation, results, errors, summary);
    }
    
    private BatchOperationResult performCopyBatch(BatchOperation operation, List<String> sourcePaths,
                                                List<String> targetPaths, BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < sourcePaths.size(); i++) {
            try {
                String sourcePath = sourcePaths.get(i);
                String targetPath = targetPaths.get(i);
                
                Path source = Paths.get(sourcePath);
                Path target = Paths.get(targetPath);
                
                // 确保目标目录存在
                Files.createDirectories(target.getParent());
                
                // 复制文件
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                
                // 创建成功结果
                ScriptExecutionResult result = ScriptExecutionResult.success(0, 
                    Arrays.asList("复制成功: " + sourcePath + " -> " + targetPath),
                    Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                    null, targetPath, null, null);
                results.add(result);
                
                totalScriptsProcessed.incrementAndGet();
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("复制失败: {}", sourcePaths.get(i), e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "copiedFiles", results.size(),
            "failedCopies", errors.size()
        );
        
        return BatchOperationResult.success("批量复制完成", operation, results, errors, summary);
    }
    
    private BatchOperationResult performDeleteBatch(BatchOperation operation, List<String> scriptPaths,
                                                  BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (String scriptPath : scriptPaths) {
            try {
                Path path = Paths.get(scriptPath);
                
                if (Files.exists(path)) {
                    Files.delete(path);
                    ScriptExecutionResult result = ScriptExecutionResult.success(0,
                        Arrays.asList("删除成功: " + scriptPath),
                        Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                        null, scriptPath, null, null);
                    results.add(result);
                    totalScriptsProcessed.incrementAndGet();
                } else {
                    ScriptExecutionResult result = ScriptExecutionResult.success(0,
                        Arrays.asList("文件不存在: " + scriptPath),
                        Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                        null, scriptPath, null, null);
                    results.add(result);
                }
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("删除失败: {}", scriptPath, e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "deletedFiles", results.size(),
            "failedDeletions", errors.size()
        );
        
        return BatchOperationResult.success("批量删除完成", operation, results, errors, summary);
    }
    
    private BatchOperationResult performMoveBatch(BatchOperation operation, List<String> sourcePaths,
                                                List<String> targetPaths, BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < sourcePaths.size(); i++) {
            try {
                String sourcePath = sourcePaths.get(i);
                String targetPath = targetPaths.get(i);
                
                Path source = Paths.get(sourcePath);
                Path target = Paths.get(targetPath);
                
                // 确保目标目录存在
                Files.createDirectories(target.getParent());
                
                // 移动文件
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                
                ScriptExecutionResult result = ScriptExecutionResult.success(0,
                    Arrays.asList("移动成功: " + sourcePath + " -> " + targetPath),
                    Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                    null, targetPath, null, null);
                results.add(result);
                totalScriptsProcessed.incrementAndGet();
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("移动失败: {}", sourcePaths.get(i), e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "movedFiles", results.size(),
            "failedMoves", errors.size()
        );
        
        return BatchOperationResult.success("批量移动完成", operation, results, errors, summary);
    }
    
    private BatchOperationResult performUpdateBatch(BatchOperation operation, List<ScriptUpdate> updates,
                                                  BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (ScriptUpdate update : updates) {
            try {
                Path path = Paths.get(update.getScriptPath());
                
                // 写入新内容
                Files.write(path, update.getNewContent().getBytes(StandardCharsets.UTF_8));
                
                ScriptExecutionResult result = ScriptExecutionResult.success(0,
                    Arrays.asList("更新成功: " + update.getScriptPath()),
                    Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                    null, update.getScriptPath(), null, null);
                results.add(result);
                totalScriptsProcessed.incrementAndGet();
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("更新失败: {}", update.getScriptPath(), e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "updatedFiles", results.size(),
            "failedUpdates", errors.size()
        );
        
        return BatchOperationResult.success("批量更新完成", operation, results, errors, summary);
    }
    
    private BatchOperationResult performValidateBatch(BatchOperation operation, List<String> scriptPaths,
                                                    BatchOperationConfiguration config) {
        List<ScriptExecutionResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        
        for (String scriptPath : scriptPaths) {
            try {
                Path path = Paths.get(scriptPath);
                
                boolean exists = Files.exists(path);
                boolean readable = Files.isReadable(path);
                long size = exists ? Files.size(path) : 0;
                
                ScriptExecutionResult result = ScriptExecutionResult.success(0,
                    Arrays.asList(
                        "验证成功: " + scriptPath,
                        "存在: " + exists,
                        "可读: " + readable,
                        "大小: " + size + " bytes"
                    ),
                    Arrays.asList(), LocalDateTime.now(), LocalDateTime.now(),
                    null, scriptPath, null, null);
                results.add(result);
                totalScriptsProcessed.incrementAndGet();
                
            } catch (Exception e) {
                errors.add(e);
                logger.warn("验证失败: {}", scriptPath, e);
            }
        }
        
        Map<String, Object> summary = Map.of(
            "validatedFiles", results.size(),
            "failedValidations", errors.size()
        );
        
        return BatchOperationResult.success("批量验证完成", operation, results, errors, summary);
    }
    
    private void updateStatistics(BatchOperationResult result) {
        totalBatchOperations.incrementAndGet();
        
        if (result.isSuccess()) {
            completedBatchOperations.incrementAndGet();
        } else {
            failedBatchOperations.incrementAndGet();
        }
        
        if (result.getOperationTimeMs() > 0) {
            totalExecutionTime.addAndGet(result.getOperationTimeMs());
        }
    }
    
    /**
     * 将参数Map转换为String数组
     *
     * @param parameters 参数Map
     * @return String数组
     */
    private String[] convertParametersToArguments(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return new String[0];
        }
        
        List<String> arguments = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                arguments.add(key + "=" + value.toString());
            } else {
                arguments.add(key + "=");
            }
        }
        
        return arguments.toArray(new String[0]);
    }
    
    /**
     * 关闭管理器，释放资源
     */
    @Override
    public void close() {
        shutdown();
    }
    
    /**
     * 关闭管理器并释放资源
     */
    public void shutdown() {
        logger.info("正在关闭脚本批量操作管理器...");
        
        // 停止清理任务
        stopCleanupTask();
        
        // 关闭主执行器
        shutdownExecutor(executorService, "batch-executor");
        
        // 关闭清理执行器
        shutdownExecutor(cleanupExecutor, "batch-cleanup");
        
        // 清理操作记录
        lock.writeLock().lock();
        try {
            runningOperations.clear();
            completedOperations.clear();
        } finally {
            lock.writeLock().unlock();
        }
        
        logger.info("脚本批量操作管理器已关闭");
    }
    
    /**
     * 关闭单个执行器
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        logger.warn("执行器 [{}] 未能在规定时间内终止", name);
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                logger.warn("关闭执行器 [{}] 时被中断", name);
            }
        }
    }
}
