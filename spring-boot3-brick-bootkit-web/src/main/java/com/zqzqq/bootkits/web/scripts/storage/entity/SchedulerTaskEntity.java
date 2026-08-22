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


package com.zqzqq.bootkits.web.scripts.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 调度任务实体
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("brick_scheduler_task")
public class SchedulerTaskEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
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
    private LocalDateTime lastExecution;
    private LocalDateTime lastExecutionStartTime;
    private LocalDateTime lastExecutionEndTime;
    private String lastExecutionId;
    private String lastExecutionResult;
    private String lastExecutionStatus;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastFailureTime;
    private Integer manualTriggerCount;
    private LocalDateTime nextExecution;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}