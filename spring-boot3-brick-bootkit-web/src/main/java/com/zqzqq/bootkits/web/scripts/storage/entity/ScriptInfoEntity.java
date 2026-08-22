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
