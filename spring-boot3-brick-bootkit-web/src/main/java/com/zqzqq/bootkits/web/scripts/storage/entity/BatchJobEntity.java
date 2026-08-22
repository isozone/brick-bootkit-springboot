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
 * 批处理任务实体
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("brick_batch_job")
public class BatchJobEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String jobId;
    private String jobName;
    private String description;
    private String scriptNames;
    private String parameters;
    private String executionMode;
    private Integer maxConcurrency;
    private Boolean stopOnError;
    
    private String jobStatus;
    private Integer totalItems;
    private Integer processedItems;
    private Integer successItems;
    private Integer failedItems;
    private Integer progress;
    
    private String processedScripts;
    private String executionLog;
    private String lastProcessedScript;
    private LocalDateTime lastProcessedTime;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private LocalDateTime scheduledTime;
    private String submittedBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
