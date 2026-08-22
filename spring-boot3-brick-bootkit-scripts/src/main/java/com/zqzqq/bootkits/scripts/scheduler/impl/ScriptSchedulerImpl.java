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


package com.zqzqq.bootkits.scripts.scheduler.impl;

import com.zqzqq.bootkits.scripts.core.*;
import com.zqzqq.bootkits.scripts.scheduler.ScriptScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 脚本调度器默认实现
 * 提供完整的定时任务管理、脚本执行队列和批处理作业调度功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptSchedulerImpl implements ScriptScheduler {
    
    private final ScriptManager scriptManager;
    private final ScheduledExecutorService schedulerExecutor;
    private final ExecutorService scriptExecutor;
    private final Queue<QueueItem> scriptQueue;
    private final Map<String, TaskStatus> scheduledTasks;
    private final Map<String, JobStatus> batchJobs;
    private final AtomicInteger queueCounter;
    private final AtomicInteger taskCounter;
    private final AtomicInteger jobCounter;
    private volatile boolean isRunning;
    
    private final QueueProcessor queueProcessor;
    
    /**
     * 构造函数
     *
     * @param scriptManager 脚本管理器
     */
    public ScriptSchedulerImpl(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        this.schedulerExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Script-Scheduler-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.scriptExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "Script-Worker-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.scriptQueue = new PriorityQueue<>(Comparator.comparing(QueueItem::getScheduledAt));
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.batchJobs = new ConcurrentHashMap<>();
        this.queueCounter = new AtomicInteger(0);
        this.taskCounter = new AtomicInteger(0);
        this.jobCounter = new AtomicInteger(0);
        this.isRunning = false;
        this.queueProcessor = new QueueProcessor();
        
        // 启动队列处理器
        schedulerExecutor.scheduleWithFixedDelay(queueProcessor, 1, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public SchedulerResult start() {
        if (isRunning) {
            return SchedulerResult.success("调度器已在运行", "start");
        }
        
        isRunning = true;
        return SchedulerResult.success("调度器启动成功", "start");
    }
    
    @Override
    public SchedulerResult stop() {
        if (!isRunning) {
            return SchedulerResult.success("调度器已停止", "stop");
        }
        
        try {
            isRunning = false;
            
            // 取消所有定时任务
            scheduledTasks.values().forEach(status -> {
                if (status.getScheduledFuture() != null) {
                    status.getScheduledFuture().cancel(true);
                }
            });
            
            return SchedulerResult.success("调度器停止成功", "stop");
            
        } catch (Exception e) {
            return SchedulerResult.failure("调度器停止失败: " + e.getMessage(), e, "stop");
        }
    }
    
    @Override
    public SchedulerResult scheduleTask(ScheduledTask task) {
        if (!isRunning) {
            return SchedulerResult.failure("调度器未运行", null, "scheduleTask");
        }
        
        if (task == null) {
            return SchedulerResult.failure("任务信息不能为空", null, "scheduleTask");
        }
        
        if (scheduledTasks.containsKey(task.getId())) {
            return SchedulerResult.failure("任务ID已存在: " + task.getId(), null, "scheduleTask");
        }
        
        try {
            ScheduledFuture<?> scheduledFuture = null;
            TaskState state = TaskState.SCHEDULED;
            LocalDateTime nextExecution = null;
            
            // 根据调度类型创建调度
            switch (task.getScheduleType()) {
                case ONCE:
                    scheduledFuture = scheduleOnce(task);
                    nextExecution = parseScheduleTime(task.getScheduleExpression());
                    break;
                case CRON:
                    scheduledFuture = scheduleCron(task);
                    nextExecution = LocalDateTime.now().plusMinutes(1); // 简化处理
                    break;
                case INTERVAL:
                    scheduledFuture = scheduleInterval(task);
                    nextExecution = LocalDateTime.now().plusSeconds(parseInterval(task.getScheduleExpression()));
                    break;
                case DAILY:
                    scheduledFuture = scheduleDaily(task);
                    nextExecution = parseDailyTime(task.getScheduleExpression());
                    break;
                default:
                    return SchedulerResult.failure("不支持的调度类型: " + task.getScheduleType(), null, "scheduleTask");
            }
            
            // 创建任务状态
            TaskStatus status = new TaskStatus(
                task.getId(), task.getScriptName(), task.getScheduleType(),
                state, nextExecution, null, 0, task.getMaxExecutions(),
                null, scheduledFuture
            );
            
            scheduledTasks.put(task.getId(), status);
            
            return SchedulerResult.success("任务调度成功: " + task.getId(), "scheduleTask", status);
            
        } catch (Exception e) {
            return SchedulerResult.failure("任务调度失败: " + e.getMessage(), e, "scheduleTask");
        }
    }
    
    @Override
    public SchedulerResult cancelTask(String taskId) {
        TaskStatus status = scheduledTasks.get(taskId);
        if (status == null) {
            return SchedulerResult.failure("任务不存在: " + taskId, null, "cancelTask");
        }
        
        try {
            if (status.getScheduledFuture() != null) {
                status.getScheduledFuture().cancel(true);
            }
            
            // 更新任务状态
            TaskStatus updatedStatus = new TaskStatus(
                status.getTaskId(), status.getScriptName(), status.getScheduleType(),
                TaskState.CANCELLED, status.getNextExecution(), status.getLastExecution(),
                status.getExecutionCount(), status.getMaxExecutions(),
                "任务已取消", null
            );
            
            scheduledTasks.put(taskId, updatedStatus);
            
            return SchedulerResult.success("任务取消成功: " + taskId, "cancelTask");
            
        } catch (Exception e) {
            return SchedulerResult.failure("任务取消失败: " + e.getMessage(), e, "cancelTask");
        }
    }
    
    @Override
    public Optional<TaskStatus> getTaskStatus(String taskId) {
        return Optional.ofNullable(scheduledTasks.get(taskId));
    }
    
    @Override
    public List<TaskStatus> getAllTasks() {
        return new ArrayList<>(scheduledTasks.values());
    }
    
    @Override
    public SchedulerResult enqueueScript(QueueItem queueItem) {
        if (queueItem == null) {
            return SchedulerResult.failure("队列项不能为空", null, "enqueueScript");
        }
        
        synchronized (scriptQueue) {
            scriptQueue.offer(queueItem);
            scriptQueue.notify();
        }
        
        return SchedulerResult.success("脚本已添加到队列: " + queueItem.getId(), "enqueueScript", queueItem);
    }
    
    @Override
    public Optional<QueueItem> dequeueScript() {
        synchronized (scriptQueue) {
            return Optional.ofNullable(scriptQueue.poll());
        }
    }
    
    @Override
    public QueueStatus getQueueStatus() {
        int total = scriptQueue.size();
        int processing = queueProcessor.getProcessingCount();
        int completed = queueProcessor.getCompletedCount();
        int failed = queueProcessor.getFailedCount();
        int pending = total - processing;
        
        double avgWaitTime = queueProcessor.getAverageWaitTime();
        
        return new QueueStatus(total, pending, processing, completed, failed, avgWaitTime);
    }
    
    @Override
    public SchedulerResult addBatchJob(BatchJob job) {
        if (job == null) {
            return SchedulerResult.failure("作业信息不能为空", null, "addBatchJob");
        }
        
        if (batchJobs.containsKey(job.getId())) {
            return SchedulerResult.failure("作业ID已存在: " + job.getId(), null, "addBatchJob");
        }
        
        try {
            // 创建作业状态
            JobStatus status = new JobStatus(
                job.getId(), job.getName(), JobState.PENDING,
                null, null, job.getScriptNames().size(), 0, 0, 0, List.of()
            );
            
            batchJobs.put(job.getId(), status);
            
            // 提交作业执行
            executeBatchJob(job);
            
            return SchedulerResult.success("批处理作业已添加: " + job.getId(), "addBatchJob", status);
            
        } catch (Exception e) {
            return SchedulerResult.failure("批处理作业添加失败: " + e.getMessage(), e, "addBatchJob");
        }
    }
    
    @Override
    public Optional<JobStatus> getJobStatus(String jobId) {
        return Optional.ofNullable(batchJobs.get(jobId));
    }
    
    @Override
    public List<JobStatus> getAllJobs() {
        return new ArrayList<>(batchJobs.values());
    }
    
    /**
     * 一次性任务调度
     */
    private ScheduledFuture<?> scheduleOnce(ScheduledTask task) {
        LocalDateTime executeTime = parseScheduleTime(task.getScheduleExpression());
        long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), executeTime);
        
        return schedulerExecutor.schedule(() -> executeTask(task), delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Cron表达式调度
     */
    private ScheduledFuture<?> scheduleCron(ScheduledTask task) {
        // 简化实现：每分钟执行一次
        return schedulerExecutor.scheduleWithFixedDelay(() -> executeTask(task), 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 间隔任务调度
     */
    private ScheduledFuture<?> scheduleInterval(ScheduledTask task) {
        long interval = parseInterval(task.getScheduleExpression());
        return schedulerExecutor.scheduleWithFixedDelay(() -> executeTask(task), interval, interval, TimeUnit.SECONDS);
    }
    
    /**
     * 每日任务调度
     */
    private ScheduledFuture<?> scheduleDaily(ScheduledTask task) {
        LocalDateTime executeTime = parseDailyTime(task.getScheduleExpression());
        long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), executeTime);
        
        return schedulerExecutor.scheduleWithFixedDelay(() -> executeTask(task), delay, 24, TimeUnit.HOURS);
    }
    
    /**
     * 执行任务
     */
    private void executeTask(ScheduledTask task) {
        try {
            // 更新任务状态为运行中
            TaskStatus currentStatus = scheduledTasks.get(task.getId());
            if (currentStatus != null) {
                TaskStatus runningStatus = new TaskStatus(
                    currentStatus.getTaskId(), currentStatus.getScriptName(), currentStatus.getScheduleType(),
                    TaskState.RUNNING, currentStatus.getNextExecution(), LocalDateTime.now(),
                    currentStatus.getExecutionCount() + 1, currentStatus.getMaxExecutions(),
                    null, currentStatus.getScheduledFuture()
                );
                scheduledTasks.put(task.getId(), runningStatus);
            }
            
            // 执行脚本
            ScriptExecutionResult result = scriptManager.executeScript(
                task.getScriptName(), task.getArguments(), task.getConfiguration()
            );
            
            // 更新任务状态为完成或失败
            TaskState newState = result.isSuccess() ? TaskState.COMPLETED : TaskState.FAILED;
            TaskStatus newStatus = new TaskStatus(
                task.getId(), task.getScriptName(), task.getScheduleType(),
                newState, calculateNextExecution(task), LocalDateTime.now(),
                currentStatus.getExecutionCount() + 1, task.getMaxExecutions(),
                result.isSuccess() ? null : result.getErrorMessage(), currentStatus.getScheduledFuture()
            );
            
            scheduledTasks.put(task.getId(), newStatus);
            
            // 检查是否需要停止（达到最大执行次数或任务禁用）
            if (task.getMaxExecutions() > 0 && newStatus.getExecutionCount() >= task.getMaxExecutions() ||
                !task.isEnabled()) {
                if (currentStatus.getScheduledFuture() != null) {
                    currentStatus.getScheduledFuture().cancel(false);
                }
            }
            
        } catch (Exception e) {
            // 更新任务状态为失败
            TaskStatus currentStatus = scheduledTasks.get(task.getId());
            if (currentStatus != null) {
                TaskStatus failedStatus = new TaskStatus(
                    currentStatus.getTaskId(), currentStatus.getScriptName(), currentStatus.getScheduleType(),
                    TaskState.FAILED, currentStatus.getNextExecution(), LocalDateTime.now(),
                    currentStatus.getExecutionCount() + 1, task.getMaxExecutions(),
                    e.getMessage(), currentStatus.getScheduledFuture()
                );
                scheduledTasks.put(task.getId(), failedStatus);
            }
        }
    }
    
    /**
     * 执行批处理作业
     */
    private void executeBatchJob(BatchJob job) {
        scriptExecutor.submit(() -> {
            try {
                // 更新作业状态为运行中
                updateJobStatus(job.getId(), JobState.RUNNING, LocalDateTime.now());
                
                List<String> scripts = job.getScriptNames();
                CountDownLatch latch = new CountDownLatch(scripts.size());
                AtomicInteger completedCount = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);
                List<String> errors = Collections.synchronizedList(new ArrayList<>());
                
                // 根据执行模式处理脚本
                switch (job.getExecutionMode()) {
                    case SEQUENTIAL:
                        executeScriptsSequentially(job, scripts, latch, completedCount, failedCount, errors);
                        break;
                    case PARALLEL:
                        executeScriptsInParallel(job, scripts, latch, completedCount, failedCount, errors);
                        break;
                    case PRIORITY:
                        executeScriptsByPriority(job, scripts, latch, completedCount, failedCount, errors);
                        break;
                }
                
                // 等待所有脚本执行完成
                latch.await();
                
                // 更新作业状态为完成
                JobState finalState = failedCount.get() > 0 && job.isStopOnError() ? JobState.FAILED : JobState.COMPLETED;
                updateJobStatus(job.getId(), finalState, LocalDateTime.now());
                
            } catch (Exception e) {
                updateJobStatus(job.getId(), JobState.FAILED, LocalDateTime.now());
            }
        });
    }
    
    /**
     * 顺序执行脚本
     */
    private void executeScriptsSequentially(BatchJob job, List<String> scripts, CountDownLatch latch,
                                          AtomicInteger completedCount, AtomicInteger failedCount,
                                          List<String> errors) {
        for (String scriptName : scripts) {
            try {
                ScriptExecutionResult result = scriptManager.executeScript(scriptName);
                if (result.isSuccess()) {
                    completedCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                    errors.add("脚本执行失败: " + scriptName + " - " + result.getErrorMessage());
                    if (job.isStopOnError()) {
                        break;
                    }
                }
            } catch (Exception e) {
                failedCount.incrementAndGet();
                errors.add("脚本执行异常: " + scriptName + " - " + e.getMessage());
                if (job.isStopOnError()) {
                    break;
                }
            } finally {
                latch.countDown();
            }
        }
    }
    
    /**
     * 并行执行脚本
     */
    private void executeScriptsInParallel(BatchJob job, List<String> scripts, CountDownLatch latch,
                                        AtomicInteger completedCount, AtomicInteger failedCount,
                                        List<String> errors) {
        Semaphore semaphore = new Semaphore(job.getMaxConcurrency());
        
        scripts.forEach(scriptName -> {
            scriptExecutor.submit(() -> {
                try {
                    semaphore.acquire();
                    ScriptExecutionResult result = scriptManager.executeScript(scriptName);
                    if (result.isSuccess()) {
                        completedCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                        errors.add("脚本执行失败: " + scriptName + " - " + result.getErrorMessage());
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    errors.add("脚本执行异常: " + scriptName + " - " + e.getMessage());
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            });
        });
    }
    
    /**
     * 按优先级执行脚本
     */
    private void executeScriptsByPriority(BatchJob job, List<String> scripts, CountDownLatch latch,
                                        AtomicInteger completedCount, AtomicInteger failedCount,
                                        List<String> errors) {
        // 简化实现：按顺序执行
        executeScriptsSequentially(job, scripts, latch, completedCount, failedCount, errors);
    }
    
    /**
     * 更新作业状态
     */
    private void updateJobStatus(String jobId, JobState state, LocalDateTime time) {
        JobStatus currentStatus = batchJobs.get(jobId);
        if (currentStatus != null) {
            JobStatus newStatus = new JobStatus(
                currentStatus.getJobId(), currentStatus.getJobName(), state,
                time, time, currentStatus.getTotalScripts(),
                currentStatus.getCompletedScripts(), currentStatus.getFailedScripts(),
                currentStatus.getRunningScripts(), currentStatus.getErrorMessages()
            );
            batchJobs.put(jobId, newStatus);
        }
    }
    
    /**
     * 解析调度时间
     */
    private LocalDateTime parseScheduleTime(String timeStr) {
        try {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now().plusMinutes(1); // 默认1分钟后
        }
    }
    
    /**
     * 解析间隔时间
     */
    private long parseInterval(String intervalStr) {
        try {
            if (intervalStr.endsWith("s")) {
                return Long.parseLong(intervalStr.substring(0, intervalStr.length() - 1));
            } else if (intervalStr.endsWith("m")) {
                return Long.parseLong(intervalStr.substring(0, intervalStr.length() - 1)) * 60;
            } else if (intervalStr.endsWith("h")) {
                return Long.parseLong(intervalStr.substring(0, intervalStr.length() - 1)) * 3600;
            }
            return Long.parseLong(intervalStr);
        } catch (Exception e) {
            return 60; // 默认60秒
        }
    }
    
    /**
     * 解析每日执行时间
     */
    private LocalDateTime parseDailyTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime executeTime = now.withHour(hour).withMinute(minute).withSecond(second);
            
            // 如果时间已过，设置为明天
            if (executeTime.isBefore(now)) {
                executeTime = executeTime.plusDays(1);
            }
            
            return executeTime;
        } catch (Exception e) {
            return LocalDateTime.now().plusHours(1); // 默认1小时后
        }
    }
    
    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextExecution(ScheduledTask task) {
        switch (task.getScheduleType()) {
            case ONCE:
                return null; // 一次性任务没有下次执行
            case CRON:
                return LocalDateTime.now().plusMinutes(1); // 简化处理
            case INTERVAL:
                long interval = parseInterval(task.getScheduleExpression());
                return LocalDateTime.now().plusSeconds(interval);
            case DAILY:
                return parseDailyTime(task.getScheduleExpression());
            default:
                return null;
        }
    }
    
    /**
     * 队列处理器
     */
    private class QueueProcessor implements Runnable {
        private final AtomicInteger processingCount = new AtomicInteger(0);
        private final AtomicInteger completedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final AtomicLong totalWaitTime = new AtomicLong(0);
        
        @Override
        public void run() {
            if (!isRunning) {
                return;
            }
            
            Optional<QueueItem> itemOption = dequeueScript();
            QueueItem item = itemOption.orElse(null);
            if (item != null) {
                processingCount.incrementAndGet();
                
                scriptExecutor.submit(() -> {
                    try {
                        long waitTime = ChronoUnit.MILLIS.between(item.getCreatedAt(), LocalDateTime.now());
                        totalWaitTime.addAndGet(waitTime);
                        
                        ScriptExecutionResult result = scriptManager.executeScript(
                            item.getScriptName(), item.getArguments(), item.getConfiguration()
                        );
                        
                        if (result.isSuccess()) {
                            completedCount.incrementAndGet();
                        } else {
                            failedCount.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                    } finally {
                        processingCount.decrementAndGet();
                    }
                });
            }
        }
        
        public int getProcessingCount() { return processingCount.get(); }
        public int getCompletedCount() { return completedCount.get(); }
        public int getFailedCount() { return failedCount.get(); }
        public double getAverageWaitTime() {
            long total = completedCount.get() + failedCount.get();
            return total > 0 ? (double) totalWaitTime.get() / total : 0.0;
        }
    }
}