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

import com.zqzqq.bootkits.scripts.core.ScriptType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 脚本信息DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptInfoDTO {
    
    /**
     * 脚本名称
     */
    private String scriptName;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 脚本类型
     */
    private ScriptType scriptType;
    
    /**
     * 脚本内容
     */
    private String content;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 脚本分组
     */
    private String group;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 标签
     */
    private List<String> tags;
    
    /**
     * 依赖列表
     */
    private List<ScriptDependencyDTO> dependencies;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
