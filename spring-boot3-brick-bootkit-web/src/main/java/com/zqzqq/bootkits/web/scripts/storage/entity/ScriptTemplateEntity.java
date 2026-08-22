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
