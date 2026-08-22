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