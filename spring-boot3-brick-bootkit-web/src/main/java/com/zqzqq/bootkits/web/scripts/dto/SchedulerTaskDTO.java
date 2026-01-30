package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerTaskDTO {
    
    private String taskId;
    private String taskName;
    private String description;
    private String cronExpression;
    private String scriptName;
    private String parameters;
    private String taskStatus;
    private Integer maxConcurrentRuns;
    private Integer timeoutMinutes;
    private Integer retryCount;
    private Integer retryIntervalMinutes;
    private Boolean notifyOnSuccess;
    private Boolean notifyOnFailure;
    private String notificationTarget;
    private LocalDateTime lastStartTime;
    private LocalDateTime lastTriggerTime;
    private LocalDateTime lastExecutionStartTime;
    private LocalDateTime lastExecutionEndTime;
    private String lastExecutionId;
    private String lastExecutionResult;
    private String lastExecutionStatus;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastFailureTime;
    private Integer manualTriggerCount;
    private LocalDateTime nextExecution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 调度状态枚举
     */
    public enum SchedulerStatus {
        CREATED,
        RUNNING,
        ENABLED,
        DISABLED,
        ERROR
    }
    
    /**
     * 执行结果状态枚举
     */
    public enum ExecutionResultStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
    
    /**
     * 执行历史记录项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionHistoryItem {
        private String executionId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String result;
        private String status;
    }
}
