package com.zqzqq.bootkits.web.scripts.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 队列项实体
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("brick_queue_item")
public class QueueItemEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String itemId;
    private String queueName;
    private String scriptName;
    private String parameters;
    private Integer priority;
    private String queueStatus;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String result;
    private String errorMessage;
    private String submittedBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}