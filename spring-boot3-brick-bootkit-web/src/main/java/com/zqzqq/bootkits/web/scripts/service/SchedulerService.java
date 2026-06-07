package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.scripts.core.ScriptConfiguration;
import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import com.zqzqq.bootkits.scripts.core.ScriptManager;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import com.zqzqq.bootkits.web.scripts.dto.SchedulerTaskDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 定时任务服务层
 * 提供定时任务的CRUD操作和调度管理
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    
    private final ScriptStorage scriptStorage;
    private final ScriptExecutionService scriptExecutionService;
    private final ScriptManager scriptManager;
    
    /**
     * 获取所有定时任务
     */
    public List<SchedulerTaskDTO> getAllTasks() throws StorageException {
        return scriptStorage.getAllSchedulerTasks();
    }
    
    /**
     * 获取单个定时任务
     */
    public Optional<SchedulerTaskDTO> getTask(String taskId) throws StorageException {
        return scriptStorage.getSchedulerTask(taskId);
    }
    
    /**
     * 创建定时任务
     */
    public SchedulerTaskDTO createTask(SchedulerTaskDTO taskDTO) throws StorageException {
        // 生成任务ID
        if (taskDTO.getTaskId() == null || taskDTO.getTaskId().isEmpty()) {
            taskDTO.setTaskId(UUID.randomUUID().toString());
        }
        
        LocalDateTime now = LocalDateTime.now();
        taskDTO.setCreatedAt(now);
        taskDTO.setUpdatedAt(now);
        
        // 默认状态
        if (taskDTO.getTaskStatus() == null) {
            taskDTO.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.CREATED.name());
        }
        
        // 设置默认重试次数
        if (taskDTO.getRetryCount() == null) {
            taskDTO.setRetryCount(0);
        }
        
        // 计算下次执行时间
        taskDTO.setNextExecution(calculateNextExecution(taskDTO.getCronExpression()));
        
        // 保存到存储
        scriptStorage.saveSchedulerTask(taskDTO);
        
        log.info("Created scheduler task: {} - {}", taskDTO.getTaskId(), taskDTO.getTaskName());
        return taskDTO;
    }
    
    /**
     * 更新定时任务
     */
    public SchedulerTaskDTO updateTask(String taskId, SchedulerTaskDTO taskDTO) throws StorageException {
        Optional<SchedulerTaskDTO> existingTaskOpt = scriptStorage.getSchedulerTask(taskId);
        if (existingTaskOpt.isEmpty()) {
            throw new StorageException("Task not found: " + taskId);
        }
        
        SchedulerTaskDTO existingTask = existingTaskOpt.get();
        
        // 更新字段
        if (taskDTO.getTaskName() != null) {
            existingTask.setTaskName(taskDTO.getTaskName());
        }
        if (taskDTO.getDescription() != null) {
            existingTask.setDescription(taskDTO.getDescription());
        }
        if (taskDTO.getCronExpression() != null) {
            existingTask.setCronExpression(taskDTO.getCronExpression());
            existingTask.setNextExecution(calculateNextExecution(taskDTO.getCronExpression()));
        }
        if (taskDTO.getScriptName() != null) {
            existingTask.setScriptName(taskDTO.getScriptName());
        }
        if (taskDTO.getParameters() != null) {
            existingTask.setParameters(taskDTO.getParameters());
        }
        if (taskDTO.getMaxConcurrentRuns() != null) {
            existingTask.setMaxConcurrentRuns(taskDTO.getMaxConcurrentRuns());
        }
        if (taskDTO.getTimeoutMinutes() != null) {
            existingTask.setTimeoutMinutes(taskDTO.getTimeoutMinutes());
        }
        if (taskDTO.getRetryCount() != null) {
            existingTask.setRetryCount(taskDTO.getRetryCount());
        }
        if (taskDTO.getRetryIntervalMinutes() != null) {
            existingTask.setRetryIntervalMinutes(taskDTO.getRetryIntervalMinutes());
        }
        if (taskDTO.getNotifyOnSuccess() != null) {
            existingTask.setNotifyOnSuccess(taskDTO.getNotifyOnSuccess());
        }
        if (taskDTO.getNotifyOnFailure() != null) {
            existingTask.setNotifyOnFailure(taskDTO.getNotifyOnFailure());
        }
        if (taskDTO.getNotificationTarget() != null) {
            existingTask.setNotificationTarget(taskDTO.getNotificationTarget());
        }
        
        existingTask.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新
        scriptStorage.saveSchedulerTask(existingTask);
        
        log.info("Updated scheduler task: {}", taskId);
        return existingTask;
    }
    
    /**
     * 删除定时任务
     */
    public boolean deleteTask(String taskId) throws StorageException {
        Optional<SchedulerTaskDTO> taskOpt = scriptStorage.getSchedulerTask(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }
        
        // 删除存储
        scriptStorage.deleteSchedulerTask(taskId);
        
        log.info("Deleted scheduler task: {}", taskId);
        return true;
    }
    
    /**
     * 启用定时任务
     */
    public SchedulerTaskDTO enableTask(String taskId) throws StorageException {
        return updateTaskStatus(taskId, SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
    }
    
    /**
     * 禁用定时任务
     */
    public SchedulerTaskDTO disableTask(String taskId) throws StorageException {
        return updateTaskStatus(taskId, SchedulerTaskDTO.SchedulerStatus.DISABLED.name());
    }
    
    /**
     * 立即执行定时任务
     * 执行脚本并记录执行结果
     */
    public void executeTask(String taskId) throws StorageException {
        Optional<SchedulerTaskDTO> taskOpt = scriptStorage.getSchedulerTask(taskId);
        if (taskOpt.isEmpty()) {
            throw new StorageException("Task not found: " + taskId);
        }
        
        SchedulerTaskDTO task = taskOpt.get();
        String scriptName = task.getScriptName();
        String params = task.getParameters();
        String executionId = UUID.randomUUID().toString();
        
        try {
            // 获取脚本内容
            Optional<String> scriptContentOpt = scriptStorage.getScriptContent(scriptName);
            if (scriptContentOpt.isEmpty()) {
                throw new StorageException("Script not found: " + scriptName);
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
            
            log.info("Executing scheduler task: {} - script: {}", taskId, scriptName);
            
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
                    scriptName, 
                    executionId, 
                    output, 
                    durationMs
                );
                task.setLastExecutionResult("SUCCESS");
                task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.SUCCESS.name());
            } else {
                scriptExecutionService.recordExecutionFailure(
                    scriptName, 
                    executionId, 
                    execResult.getErrorMessage(), 
                    durationMs
                );
                task.setLastExecutionResult("FAILED: " + execResult.getErrorMessage());
                task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.FAILED.name());
            }
            
            // 更新任务状态
            task.setLastExecutionStartTime(LocalDateTime.now());
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            task.setNextExecution(calculateNextExecution(task.getCronExpression()));
            
            scriptStorage.saveSchedulerTask(task);
            
            log.info("Scheduler task executed: {} - result: {}", taskId, execResult.isSuccess() ? "SUCCESS" : "FAILED");
            
        } catch (Exception e) {
            log.error("Failed to execute scheduler task: {}", taskId, e);
            
            // 记录执行失败
            scriptExecutionService.recordExecutionFailure(
                scriptName, 
                executionId, 
                e.getMessage(), 
                0L
            );
            
            task.setLastExecutionStartTime(LocalDateTime.now());
            task.setLastExecutionResult("ERROR: " + e.getMessage());
            task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.FAILED.name());
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            
            try {
                scriptStorage.saveSchedulerTask(task);
            } catch (StorageException se) {
                log.error("Failed to save task after execution failure", se);
            }
            
            throw new StorageException("Failed to execute task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成任务执行
     */
    public void completeTaskExecution(String taskId, boolean success, String output) throws StorageException {
        Optional<SchedulerTaskDTO> taskOpt = scriptStorage.getSchedulerTask(taskId);
        if (taskOpt.isPresent()) {
            SchedulerTaskDTO task = taskOpt.get();
            
            task.setTaskStatus(SchedulerTaskDTO.SchedulerStatus.ENABLED.name());
            
            if (success) {
                task.setLastExecutionResult("SUCCESS: " + (output != null ? output : ""));
                task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.SUCCESS.name());
            } else {
                task.setLastExecutionResult("FAILED: " + (output != null ? output : ""));
                task.setLastExecutionStatus(SchedulerTaskDTO.ExecutionResultStatus.FAILED.name());
            }
            
            // 更新下次执行时间
            task.setNextExecution(calculateNextExecution(task.getCronExpression()));
            
            scriptStorage.saveSchedulerTask(task);
        }
    }
    
    /**
     * 更新任务状态
     */
    private SchedulerTaskDTO updateTaskStatus(String taskId, String status) throws StorageException {
        scriptStorage.updateTaskStatus(taskId, status);
        
        Optional<SchedulerTaskDTO> taskOpt = scriptStorage.getSchedulerTask(taskId);
        return taskOpt.orElse(null);
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
