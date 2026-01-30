package com.zqzqq.bootkits.web.scripts.scheduler;

import com.zqzqq.bootkits.scripts.core.ScriptConfiguration;
import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import com.zqzqq.bootkits.scripts.core.ScriptManager;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import com.zqzqq.bootkits.web.scripts.dto.SchedulerTaskDTO;
import com.zqzqq.bootkits.web.scripts.service.ScriptExecutionService;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 定时任务调度器
 * 负责按照cron表达式自动执行定时任务
 * 
 * @author brick-bootkit
 */
@Slf4j
@Component("scriptTaskScheduler")
@RequiredArgsConstructor
public class TaskScheduler {
    
    private final ScriptStorage scriptStorage;
    private final ScriptExecutionService scriptExecutionService;
    private final ScriptManager scriptManager;
    
    /**
     * 每分钟检查一次待执行的任务
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndExecuteTasks() {
        log.debug("Checking scheduler tasks for execution...");
        
        try {
            List<SchedulerTaskDTO> tasks = scriptStorage.getAllSchedulerTasks();
            LocalDateTime now = LocalDateTime.now();
            
            for (SchedulerTaskDTO task : tasks) {
                // 只处理启用状态的任务
                if (!isTaskEnabled(task)) {
                    continue;
                }
                
                // 检查是否到达执行时间
                if (shouldExecuteNow(task, now)) {
                    executeTask(task);
                }
            }
        } catch (StorageException e) {
            log.error("Failed to check scheduler tasks", e);
        }
    }
    
    /**
     * 判断任务是否处于启用状态
     */
    private boolean isTaskEnabled(SchedulerTaskDTO task) {
        String status = task.getTaskStatus();
        return SchedulerTaskDTO.SchedulerStatus.ENABLED.name().equals(status) ||
               SchedulerTaskDTO.SchedulerStatus.RUNNING.name().equals(status);
    }
    
    /**
     * 判断任务是否应该执行
     */
    private boolean shouldExecuteTask(SchedulerTaskDTO task, LocalDateTime now) {
        LocalDateTime nextExecution = task.getNextExecution();
        if (nextExecution == null) {
            return false;
        }
        return !now.isBefore(nextExecution);
    }
    
    /**
     * 判断任务是否应该执行（带并发控制）
     */
    private boolean shouldExecuteNow(SchedulerTaskDTO task, LocalDateTime now) {
        LocalDateTime nextExecution = task.getNextExecution();
        if (nextExecution == null) {
            return false;
        }
        
        // 如果还没到执行时间，不执行
        if (now.isBefore(nextExecution)) {
            return false;
        }
        
        // 检查是否正在运行（避免并发执行）
        if (SchedulerTaskDTO.SchedulerStatus.RUNNING.name().equals(task.getTaskStatus())) {
            log.debug("Task {} is already running, skipping", task.getTaskId());
            return false;
        }
        
        // 检查最大并发数
        Integer maxConcurrent = task.getMaxConcurrentRuns();
        if (maxConcurrent != null && maxConcurrent > 0) {
            int runningCount = countRunningExecutions(task.getTaskId());
            if (runningCount >= maxConcurrent) {
                log.debug("Task {} has {} running executions (max: {}), skipping", 
                        task.getTaskId(), runningCount, maxConcurrent);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 统计正在运行的任务数
     */
    private int countRunningExecutions(String taskId) {
        // 简化实现：返回0表示不限制并发
        // 实际项目中可以从执行记录表中查询
        return 0;
    }
    
    /**
     * 执行定时任务
     */
    private void executeTask(SchedulerTaskDTO task) {
        String taskId = task.getTaskId();
        String scriptName = task.getScriptName();
        String params = task.getParameters();
        String executionId = UUID.randomUUID().toString();
        
        log.info("Scheduler executing task: {} - {} - cron: {}", 
                taskId, task.getTaskName(), task.getCronExpression());
        
        try {
            // 获取脚本内容
            Optional<String> scriptContentOpt = scriptStorage.getScriptContent(scriptName);
            if (scriptContentOpt.isEmpty()) {
                log.error("Script not found for scheduler task: {}", scriptName);
                recordTaskFailure(task, executionId, "Script not found: " + scriptName);
                return;
            }
            String scriptContent = scriptContentOpt.get();
            
            // 记录执行开始
            scriptExecutionService.recordExecutionStart(
                scriptName, 
                "SHELL",
                executionId, 
                "scheduler", 
                params
            );
            
            // 更新任务状态为运行中
            updateTaskStatus(task, SchedulerTaskDTO.SchedulerStatus.RUNNING.name());
            
            // 执行配置
            ScriptConfiguration config = new ScriptConfiguration();
            int timeoutMinutes = task.getTimeoutMinutes() != null ? task.getTimeoutMinutes() : 30;
            config.setTimeoutMs(timeoutMinutes * 60 * 1000L);
            config.setWorkingDirectory(null);
            
            // 执行脚本
            String[] arguments = params != null ? params.split("\\s+") : new String[0];
            ScriptExecutionResult execResult = scriptManager.executeScript(
                    ScriptType.SHELL, scriptContent, arguments, config);
            
            long durationMs = execResult.getExecutionTimeMs();
            String output = execResult.getMergedOutputString();
            
            // 记录执行结果
            if (execResult.isSuccess()) {
                scriptExecutionService.recordExecutionSuccess(
                    scriptName, executionId, output, durationMs);
                updateTaskSuccess(task, "SUCCESS");
            } else {
                scriptExecutionService.recordExecutionFailure(
                    scriptName, executionId, execResult.getErrorMessage(), durationMs);
                updateTaskFailure(task, "FAILED: " + execResult.getErrorMessage());
            }
            
            log.info("Scheduler task executed: {} - result: {}", taskId, execResult.isSuccess() ? "SUCCESS" : "FAILED");
            
        } catch (Exception e) {
            log.error("Failed to execute scheduler task: {}", taskId, e);
            scriptExecutionService.recordExecutionFailure(scriptName, executionId, e.getMessage(), 0L);
            updateTaskFailure(task, "ERROR: " + e.getMessage());
        }
    }
    
    /**
     * 记录任务执行失败
     */
    private void recordTaskFailure(SchedulerTaskDTO task, String executionId, String errorMsg) {
        try {
            task.setLastExecutionStartTime(LocalDateTime.now());
            task.setLastExecutionResult("ERROR: " + errorMsg);
            task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.FAILED.name());
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            scriptStorage.saveSchedulerTask(task);
        } catch (StorageException e) {
            log.error("Failed to record task failure", e);
        }
    }
    
    /**
     * 更新任务状态为运行中
     */
    private void updateTaskStatus(SchedulerTaskDTO task, String status) {
        try {
            task.setTaskStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            scriptStorage.saveSchedulerTask(task);
        } catch (StorageException e) {
            log.error("Failed to update task status", e);
        }
    }
    
    /**
     * 更新任务为成功状态
     */
    private void updateTaskSuccess(SchedulerTaskDTO task, String result) {
        try {
            task.setLastExecutionStartTime(LocalDateTime.now());
            task.setLastExecutionResult(result);
            task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.SUCCESS.name());
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            task.setNextExecution(calculateNextExecution(task.getCronExpression()));
            task.setUpdatedAt(LocalDateTime.now());
            scriptStorage.saveSchedulerTask(task);
        } catch (StorageException e) {
            log.error("Failed to update task success status", e);
        }
    }
    
    /**
     * 更新任务为失败状态
     */
    private void updateTaskFailure(SchedulerTaskDTO task, String result) {
        try {
            task.setLastExecutionStartTime(LocalDateTime.now());
            task.setLastExecutionResult(result);
            task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.FAILED.name());
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            task.setNextExecution(calculateNextExecution(task.getCronExpression()));
            task.setUpdatedAt(LocalDateTime.now());
            scriptStorage.saveSchedulerTask(task);
        } catch (StorageException e) {
            log.error("Failed to update task failure status", e);
        }
    }
    
    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextExecution(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length < 5) {
                return LocalDateTime.now().plusMinutes(1);
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            // 解析 cron 各字段
            int targetMinute = parseCronField(parts[0], -1, 0, 59);
            int targetHour = parseCronField(parts[1], -1, 0, 23);
            int targetDayOfMonth = parseCronField(parts[2], -1, 1, 31);
            int targetMonth = parseCronField(parts[3], -1, 1, 12);
            // parts[4] 是周几，暂时忽略
            
            // 从当前时间开始，最多查找1年内的下一个执行时间
            LocalDateTime candidate = now.withSecond(0).withNano(0);
            
            for (int i = 0; i < 365 * 24 * 60; i++) {
                candidate = candidate.plusMinutes(1);
                
                int m = candidate.getMinute();
                int h = candidate.getHour();
                int d = candidate.getDayOfMonth();
                int mon = candidate.getMonthValue();
                
                // 检查所有字段是否匹配
                boolean minuteMatch = (targetMinute == -1 || targetMinute == m);
                boolean hourMatch = (targetHour == -1 || targetHour == h);
                boolean dayMatch = (targetDayOfMonth == -1 || targetDayOfMonth == d);
                boolean monthMatch = (targetMonth == -1 || targetMonth == mon);
                
                if (minuteMatch && hourMatch && dayMatch && monthMatch) {
                    return candidate;
                }
            }
            
            // 如果找不到，返回1分钟后（理论上不应该发生）
            return now.plusMinutes(1);
            
        } catch (Exception e) {
            log.warn("Failed to parse cron expression: {}", cronExpression);
            return LocalDateTime.now().plusMinutes(1);
        }
    }
    
    /**
     * 解析Cron字段
     * @param field cron字段值
     * @param defaultValue 默认值，-1表示需要动态计算
     * @param min 最小值
     * @param max 最大值
     * @return 解析后的值，-1表示任意值（*）
     */
    private int parseCronField(String field, int defaultValue, int min, int max) {
        if (field == null || field.equals("*") || field.equals("?")) {
            return -1;  // -1 表示任意值
        }
        
        try {
            int value = Integer.parseInt(field);
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}