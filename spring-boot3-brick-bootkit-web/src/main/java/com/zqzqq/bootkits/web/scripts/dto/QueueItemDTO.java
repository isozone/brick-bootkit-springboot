package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 队列项DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueItemDTO {
    
    /**
     * 队列项ID
     */
    private String itemId;
    
    /**
     * 队列名称
     */
    private String queueName;
    
    /**
     * 脚本名称
     */
    private String scriptName;
    
    /**
     * 执行参数
     */
    private String parameters;
    
    /**
     * 优先级（1-低，2-中，3-高，4-紧急）
     */
    private Integer priority;
    
    /**
     * 队列状态
     */
    private QueueItemStatus queueStatus;
    
    /**
     * 预定执行时间
     */
    private LocalDateTime scheduledAt;
    
    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 提交人
     */
    private String submittedBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 队列项状态枚举
     */
    public enum QueueItemStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
}
