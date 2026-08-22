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


package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批处理作业DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobDTO {
    
    /**
     * 作业ID
     */
    private String jobId;
    
    /**
     * 作业名称
     */
    private String jobName;
    
    /**
     * 作业描述
     */
    private String description;
    
    /**
     * 脚本列表
     */
    private List<String> scriptNames;
    
    /**
     * 执行参数
     */
    private String parameters;
    
    /**
     * 执行模式
     */
    private ExecutionMode executionMode;
    
    /**
     * 并发数
     */
    private Integer maxConcurrency;
    
    /**
     * 出错时是否停止
     */
    private Boolean stopOnError;
    
    /**
     * 作业状态
     */
    private BatchStatus jobStatus;
    
    /**
     * 总项目数
     */
    private Integer totalItems;
    
    /**
     * 已处理项目数
     */
    private Integer processedItems;
    
    /**
     * 成功项目数
     */
    private Integer successItems;
    
    /**
     * 失败项目数
     */
    private Integer failedItems;
    
    /**
     * 进度百分比
     */
    private Integer progress;
    
    /**
     * 已处理的脚本列表
     */
    private List<String> processedScripts;
    
    /**
     * 执行日志
     */
    private List<String> executionLog;
    
    /**
     * 最后处理的脚本
     */
    private String lastProcessedScript;
    
    /**
     * 最后处理时间
     */
    private LocalDateTime lastProcessedTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 调度时间
     */
    private LocalDateTime scheduledTime;
    
    /**
     * 提交人
     */
    private String submittedBy;
    
    /**
     * 批处理状态枚举
     */
    public enum BatchStatus {
        PENDING,
        CREATED,
        RUNNING,
        PAUSED,
        STOPPED,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }
    
    /**
     * 执行模式枚举
     */
    public enum ExecutionMode {
        SEQUENTIAL,
        PARALLEL,
        QUEUE
    }
}