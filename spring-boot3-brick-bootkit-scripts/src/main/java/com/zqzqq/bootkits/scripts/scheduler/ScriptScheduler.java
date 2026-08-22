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


package com.zqzqq.bootkits.scripts.scheduler;

import com.zqzqq.bootkits.scripts.core.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * 脚本调度器接口
 * 提供定时任务管理、脚本执行队列和批处理作业调度功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptScheduler {
    
    /**
     * 启动调度器
     *
     * @return 启动结果
     */
    SchedulerResult start();
    
    /**
     * 停止调度器
     *
     * @return 停止结果
     */
    SchedulerResult stop();
    
    /**
     * 添加定时任务
     *
     * @param task 任务信息
     * @return 调度结果
     */
    SchedulerResult scheduleTask(ScheduledTask task);
    
    /**
     * 取消定时任务
     *
     * @param taskId 任务ID
     * @return 取消结果
     */
    SchedulerResult cancelTask(String taskId);
    
    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    Optional<TaskStatus> getTaskStatus(String taskId);
    
    /**
     * 获取所有任务
     *
     * @return 任务列表
     */
    List<TaskStatus> getAllTasks();
    
    /**
     * 添加脚本到执行队列
     *
     * @param queueItem 队列项
     * @return 添加结果
     */
    SchedulerResult enqueueScript(QueueItem queueItem);
    
    /**
     * 从执行队列获取下一个脚本
     *
     * @return 队列项
     */
    Optional<QueueItem> dequeueScript();
    
    /**
     * 获取队列状态
     *
     * @return 队列状态
     */
    QueueStatus getQueueStatus();
    
    /**
     * 添加批处理作业
     *
     * @param job 作业信息
     * @return 添加结果
     */
    SchedulerResult addBatchJob(BatchJob job);
    
    /**
     * 获取批处理作业状态
     *
     * @param jobId 作业ID
     * @return 作业状态
     */
    Optional<JobStatus> getJobStatus(String jobId);
    
    /**
     * 获取所有批处理作业
     *
     * @return 作业列表
     */
    List<JobStatus> getAllJobs();
    
    /**
     * 定时任务信息
     */
    class ScheduledTask {
        private final String id;
        private final String scriptName;
        private final String[] arguments;
        private final ScriptConfiguration configuration;
        private final ScheduleType scheduleType;
        private final String scheduleExpression;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int maxExecutions;
        private final boolean enabled;
        private final String description;
        
        public ScheduledTask(String id, String scriptName, String[] arguments,
                           ScriptConfiguration configuration, ScheduleType scheduleType,
                           String scheduleExpression, LocalDateTime startTime,
                           LocalDateTime endTime, int maxExecutions, boolean enabled, String description) {
            this.id = id;
            this.scriptName = scriptName;
            this.arguments = arguments;
            this.configuration = configuration;
            this.scheduleType = scheduleType;
            this.scheduleExpression = scheduleExpression;
            this.startTime = startTime;
            this.endTime = endTime;
            this.maxExecutions = maxExecutions;
            this.enabled = enabled;
            this.description = description;
        }
        
        // Factory methods
        public static ScheduledTask once(String id, String scriptName, String[] arguments,
                                       ScriptConfiguration config, LocalDateTime executeTime) {
            return new ScheduledTask(id, scriptName, arguments, config, 
                                   ScheduleType.ONCE, executeTime.toString(), 
                                   executeTime, null, 1, true, "一次性任务");
        }
        
        public static ScheduledTask cron(String id, String scriptName, String[] arguments,
                                       ScriptConfiguration config, String cronExpression) {
            return new ScheduledTask(id, scriptName, arguments, config,
                                   ScheduleType.CRON, cronExpression,
                                   LocalDateTime.now(), null, -1, true, "Cron任务");
        }
        
        public static ScheduledTask interval(String id, String scriptName, String[] arguments,
                                           ScriptConfiguration config, long intervalSeconds) {
            return new ScheduledTask(id, scriptName, arguments, config,
                                   ScheduleType.INTERVAL, intervalSeconds + "s",
                                   LocalDateTime.now(), null, -1, true, "间隔任务");
        }
        
        // Getters
        public String getId() { return id; }
        public String getScriptName() { return scriptName; }
        public String[] getArguments() { return arguments; }
        public ScriptConfiguration getConfiguration() { return configuration; }
        public ScheduleType getScheduleType() { return scheduleType; }
        public String getScheduleExpression() { return scheduleExpression; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getMaxExecutions() { return maxExecutions; }
        public boolean isEnabled() { return enabled; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("ScheduledTask{id='%s', scriptName='%s', type=%s, enabled=%s}",
                    id, scriptName, scheduleType, enabled);
        }
    }
    
    /**
     * 调度类型
     */
    enum ScheduleType {
        ONCE,      // 一次性执行
        CRON,      // Cron表达式
        INTERVAL,  // 固定间隔
        DAILY,     // 每日
        WEEKLY,    // 每周
        MONTHLY    // 每月
    }
    
    /**
     * 任务状态
     */
    class TaskStatus {
        private final String taskId;
        private final String scriptName;
        private final ScheduleType scheduleType;
        private final TaskState state;
        private final LocalDateTime nextExecution;
        private final LocalDateTime lastExecution;
        private final int executionCount;
        private final int maxExecutions;
        private final String errorMessage;
        private final ScheduledFuture<?> scheduledFuture;
        
        public TaskStatus(String taskId, String scriptName, ScheduleType scheduleType,
                        TaskState state, LocalDateTime nextExecution,
                        LocalDateTime lastExecution, int executionCount,
                        int maxExecutions, String errorMessage, ScheduledFuture<?> scheduledFuture) {
            this.taskId = taskId;
            this.scriptName = scriptName;
            this.scheduleType = scheduleType;
            this.state = state;
            this.nextExecution = nextExecution;
            this.lastExecution = lastExecution;
            this.executionCount = executionCount;
            this.maxExecutions = maxExecutions;
            this.errorMessage = errorMessage;
            this.scheduledFuture = scheduledFuture;
        }
        
        // Getters
        public String getTaskId() { return taskId; }
        public String getScriptName() { return scriptName; }
        public ScheduleType getScheduleType() { return scheduleType; }
        public TaskState getState() { return state; }
        public LocalDateTime getNextExecution() { return nextExecution; }
        public LocalDateTime getLastExecution() { return lastExecution; }
        public int getExecutionCount() { return executionCount; }
        public int getMaxExecutions() { return maxExecutions; }
        public String getErrorMessage() { return errorMessage; }
        public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        
        @Override
        public String toString() {
            return String.format("TaskStatus{id='%s', script='%s', state=%s, next=%s}",
                    taskId, scriptName, state, nextExecution);
        }
    }
    
    /**
     * 任务状态枚举
     */
    enum TaskState {
        SCHEDULED,    // 已调度
        RUNNING,      // 运行中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        CANCELLED,    // 已取消
        DISABLED      // 已禁用
    }
    
    /**
     * 队列项
     */
    class QueueItem {
        private final String id;
        private final String scriptName;
        private final String[] arguments;
        private final ScriptConfiguration configuration;
        private final QueuePriority priority;
        private final LocalDateTime createdAt;
        private final LocalDateTime scheduledAt;
        private final String submittedBy;
        
        public QueueItem(String id, String scriptName, String[] arguments,
                        ScriptConfiguration configuration, QueuePriority priority,
                        LocalDateTime scheduledAt, String submittedBy) {
            this.id = id;
            this.scriptName = scriptName;
            this.arguments = arguments;
            this.configuration = configuration;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
            this.scheduledAt = scheduledAt;
            this.submittedBy = submittedBy;
        }
        
        // Factory methods
        public static QueueItem normal(String id, String scriptName, String[] arguments,
                                     ScriptConfiguration config, String submittedBy) {
            return new QueueItem(id, scriptName, arguments, config, QueuePriority.NORMAL,
                               LocalDateTime.now(), submittedBy);
        }
        
        public static QueueItem high(String id, String scriptName, String[] arguments,
                                   ScriptConfiguration config, String submittedBy) {
            return new QueueItem(id, scriptName, arguments, config, QueuePriority.HIGH,
                               LocalDateTime.now(), submittedBy);
        }
        
        public static QueueItem low(String id, String scriptName, String[] arguments,
                                  ScriptConfiguration config, String submittedBy) {
            return new QueueItem(id, scriptName, arguments, config, QueuePriority.LOW,
                               LocalDateTime.now(), submittedBy);
        }
        
        // Getters
        public String getId() { return id; }
        public String getScriptName() { return scriptName; }
        public String[] getArguments() { return arguments; }
        public ScriptConfiguration getConfiguration() { return configuration; }
        public QueuePriority getPriority() { return priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getScheduledAt() { return scheduledAt; }
        public String getSubmittedBy() { return submittedBy; }
        
        @Override
        public String toString() {
            return String.format("QueueItem{id='%s', script='%s', priority=%s, submittedBy='%s'}",
                    id, scriptName, priority, submittedBy);
        }
    }
    
    /**
     * 队列优先级
     */
    enum QueuePriority {
        LOW(1), NORMAL(2), HIGH(3), URGENT(4);
        
        private final int level;
        
        QueuePriority(int level) {
            this.level = level;
        }
        
        public int getLevel() { return level; }
    }
    
    /**
     * 队列状态
     */
    class QueueStatus {
        private final int totalItems;
        private final int pendingItems;
        private final int processingItems;
        private final int completedItems;
        private final int failedItems;
        private final double averageWaitTime;
        
        public QueueStatus(int totalItems, int pendingItems, int processingItems,
                         int completedItems, int failedItems, double averageWaitTime) {
            this.totalItems = totalItems;
            this.pendingItems = pendingItems;
            this.processingItems = processingItems;
            this.completedItems = completedItems;
            this.failedItems = failedItems;
            this.averageWaitTime = averageWaitTime;
        }
        
        // Getters
        public int getTotalItems() { return totalItems; }
        public int getPendingItems() { return pendingItems; }
        public int getProcessingItems() { return processingItems; }
        public int getCompletedItems() { return completedItems; }
        public int getFailedItems() { return failedItems; }
        public double getAverageWaitTime() { return averageWaitTime; }
        
        @Override
        public String toString() {
            return String.format("QueueStatus{total=%d, pending=%d, processing=%d, completed=%d, failed=%d}",
                    totalItems, pendingItems, processingItems, completedItems, failedItems);
        }
    }
    
    /**
     * 批处理作业
     */
    class BatchJob {
        private final String id;
        private final String name;
        private final List<String> scriptNames;
        private final BatchExecutionMode executionMode;
        private final boolean stopOnError;
        private final int maxConcurrency;
        private final LocalDateTime scheduledTime;
        private final String submittedBy;
        private final String description;
        
        public BatchJob(String id, String name, List<String> scriptNames,
                       BatchExecutionMode executionMode, boolean stopOnError,
                       int maxConcurrency, LocalDateTime scheduledTime,
                       String submittedBy, String description) {
            this.id = id;
            this.name = name;
            this.scriptNames = scriptNames;
            this.executionMode = executionMode;
            this.stopOnError = stopOnError;
            this.maxConcurrency = maxConcurrency;
            this.scheduledTime = scheduledTime;
            this.submittedBy = submittedBy;
            this.description = description;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public List<String> getScriptNames() { return scriptNames; }
        public BatchExecutionMode getExecutionMode() { return executionMode; }
        public boolean isStopOnError() { return stopOnError; }
        public int getMaxConcurrency() { return maxConcurrency; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public String getSubmittedBy() { return submittedBy; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("BatchJob{id='%s', name='%s', scripts=%d, mode=%s}",
                    id, name, scriptNames.size(), executionMode);
        }
    }
    
    /**
     * 批处理执行模式
     */
    enum BatchExecutionMode {
        SEQUENTIAL,    // 顺序执行
        PARALLEL,      // 并行执行
        PRIORITY       // 按优先级执行
    }
    
    /**
     * 作业状态
     */
    class JobStatus {
        private final String jobId;
        private final String jobName;
        private final JobState state;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int totalScripts;
        private final int completedScripts;
        private final int failedScripts;
        private final int runningScripts;
        private final List<String> errorMessages;
        
        public JobStatus(String jobId, String jobName, JobState state,
                        LocalDateTime startTime, LocalDateTime endTime,
                        int totalScripts, int completedScripts, int failedScripts,
                        int runningScripts, List<String> errorMessages) {
            this.jobId = jobId;
            this.jobName = jobName;
            this.state = state;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalScripts = totalScripts;
            this.completedScripts = completedScripts;
            this.failedScripts = failedScripts;
            this.runningScripts = runningScripts;
            this.errorMessages = errorMessages != null ? errorMessages : List.of();
        }
        
        // Getters
        public String getJobId() { return jobId; }
        public String getJobName() { return jobName; }
        public JobState getState() { return state; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalScripts() { return totalScripts; }
        public int getCompletedScripts() { return completedScripts; }
        public int getFailedScripts() { return failedScripts; }
        public int getRunningScripts() { return runningScripts; }
        public List<String> getErrorMessages() { return errorMessages; }
        
        @Override
        public String toString() {
            return String.format("JobStatus{id='%s', name='%s', state=%s, progress=%d/%d}",
                    jobId, jobName, state, completedScripts, totalScripts);
        }
    }
    
    /**
     * 作业状态枚举
     */
    enum JobState {
        PENDING,     // 等待执行
        RUNNING,     // 运行中
        COMPLETED,   // 已完成
        FAILED,      // 失败
        CANCELLED,   // 已取消
        PAUSED       // 已暂停
    }
    
    /**
     * 调度器操作结果
     */
    class SchedulerResult {
        private final boolean success;
        private final String message;
        private final Exception exception;
        private final String operation;
        private final Object data;
        
        public SchedulerResult(boolean success, String message, Exception exception,
                             String operation, Object data) {
            this.success = success;
            this.message = message;
            this.exception = exception;
            this.operation = operation;
            this.data = data;
        }
        
        // Factory methods
        public static SchedulerResult success(String message, String operation, Object data) {
            return new SchedulerResult(true, message, null, operation, data);
        }
        
        public static SchedulerResult success(String message, String operation) {
            return success(message, operation, null);
        }
        
        public static SchedulerResult failure(String message, Exception exception, String operation) {
            return new SchedulerResult(false, message, exception, operation, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
        public String getOperation() { return operation; }
        public Object getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("SchedulerResult{success=%s, operation='%s', message='%s'}",
                    success, operation, message);
        }
    }
}