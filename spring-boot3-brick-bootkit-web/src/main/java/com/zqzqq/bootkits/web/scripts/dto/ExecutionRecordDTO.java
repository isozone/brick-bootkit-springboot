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
 * 执行记录DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecordDTO {
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 脚本名称
     */
    private String scriptName;
    
    /**
     * 脚本类型
     */
    private String scriptType;
    
    /**
     * 执行状态
     */
    private ExecutionStatus status;
    
    /**
     * 退出码
     */
    private Integer exitCode;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;
    
    /**
     * 持续时间（毫秒）- 兼容字段
     */
    private Long durationMs;
    
    /**
     * 执行输出
     */
    private String output;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行参数
     */
    private List<String> arguments;
    
    /**
     * 参数字符串 - 兼容字段
     */
    private String parameters;
    
    /**
     * 提交人
     */
    private String submittedBy;
    
    /**
     * 执行人 - 兼容字段
     */
    private String executedBy;
    
    /**
     * 进度
     */
    private Integer progress;
    
    /**
     * 状态消息
     */
    private String statusMessage;
    
    /**
     * 日志文件路径
     */
    private String logFile;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
}