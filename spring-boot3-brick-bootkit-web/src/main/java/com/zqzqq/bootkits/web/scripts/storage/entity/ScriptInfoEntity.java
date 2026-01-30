package com.zqzqq.bootkits.web.scripts.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脚本信息实体
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("brick_script_info")
public class ScriptInfoEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("script_name")
    private String scriptName;
    
    @TableField("display_name")
    private String displayName;
    
    @TableField("description")
    private String description;
    
    @TableField("script_type")
    private String scriptType;
    
    @TableField("script_content")
    private String scriptContent;
    
    @TableField("version")
    private String version;
    
    @TableField("script_group")
    private String group;
    
    @TableField("author")
    private String author;
    
    @TableField("file_path")
    private String filePath;
    
    @TableField("file_size")
    private Long fileSize;
    
    @TableField("enabled")
    private Boolean enabled;
    
    @TableField("tags")
    private String tags;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField("dependencies")
    private String dependencies;
}
