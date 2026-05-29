package com.zqzqq.bootkits.web.scripts.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脚本模板实体
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("brick_script_template")
public class ScriptTemplateEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String templateId;
    private String templateName;
    private String displayName;
    private String description;
    private String category;
    private String tags;
    
    private String scriptType;
    private String templateContent;
    
    private String parameters;
    private String defaultValues;
    
    private Long usageCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
