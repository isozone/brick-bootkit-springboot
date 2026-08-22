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


package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 插件详情 DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDetailDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件状态
     */
    private String state;
    
    /**
     * 状态描述
     */
    private String stateDescription;
    
    /**
     * 插件路径
     */
    private String pluginPath;
    
    /**
     * 启动时间
     */
    private String startTime;
    
    /**
     * 停止时间
     */
    private String stopTime;
    
    /**
     * 插件主类
     */
    private String mainClass;
    
    /**
     * 插件类型
     */
    private String pluginType;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 依赖插件列表
     */
    private List<DependencyPluginDTO> dependentPlugins;
    
    /**
     * 扩展信息
     */
    private Map<String, Object> extensionInfo;
    
    /**
     * 依赖插件 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyPluginDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String pluginId;
        private String pluginName;
        private String version;
    }
}
