package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脚本版本DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptVersionDTO {
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 变更日志
     */
    private String changeLog;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}