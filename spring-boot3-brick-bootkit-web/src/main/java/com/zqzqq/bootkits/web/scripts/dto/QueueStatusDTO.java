package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 队列状态DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusDTO {
    
    /**
     * 队列名称
     */
    private String queueName;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 队列状态
     */
    private QueueStatus status;
    
    /**
     * 最大容量
     */
    private Integer maxSize;
    
    /**
     * 当前容量
     */
    private Integer currentSize;
    
    /**
     * 总入队数
     */
    private Long totalEnqueued;
    
    /**
     * 总出队数
     */
    private Long totalDequeued;
    
    /**
     * 总成功数
     */
    private Long totalSuccess;
    
    /**
     * 总失败数
     */
    private Long totalFailed;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 队列状态枚举
     */
    public enum QueueStatus {
        ACTIVE,
        PAUSED
    }
}