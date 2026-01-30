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
