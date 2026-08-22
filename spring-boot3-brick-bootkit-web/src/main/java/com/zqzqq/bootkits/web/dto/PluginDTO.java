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

import com.zqzqq.bootkits.core.PluginInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 插件数据传输对象
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDTO implements Serializable {
    
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
    private LocalDateTime startTime;
    
    /**
     * 停止时间
     */
    private LocalDateTime stopTime;
    
    /**
     * 插件主类
     */
    private String mainClass;
    
    /**
     * 插件类型
     */
    private String pluginType;
    
    /**
     * 扩展信息
     */
    private Map<String, Object> extensionInfo;
    
    /**
     * 是否跟随系统启动
     */
    private Boolean isFollowSystem;
    
    /**
     * 从 PluginInfo 转换
     */
    public static PluginDTO from(PluginInfo pluginInfo) {
        if (pluginInfo == null) {
            return null;
        }
        
        return PluginDTO.builder()
                .pluginId(pluginInfo.getPluginId())
                .name(pluginInfo.getPluginDescriptor() != null ? 
                      pluginInfo.getPluginDescriptor().getName() : pluginInfo.getPluginId())
                .version(pluginInfo.getPluginDescriptor() != null ? 
                         pluginInfo.getPluginDescriptor().getPluginVersion() : null)
                .description(pluginInfo.getPluginDescriptor() != null ? 
                             pluginInfo.getPluginDescriptor().getDescription() : null)
                .state(pluginInfo.getPluginState() != null ? pluginInfo.getPluginState().name() : null)
                .stateDescription(pluginInfo.getPluginState() != null ? 
                                  pluginInfo.getPluginState().getDescription() : null)
                .pluginPath(pluginInfo.getPluginPath())
                .startTime(pluginInfo.getStartTime() > 0 ? 
                           LocalDateTime.ofEpochSecond(pluginInfo.getStartTime() / 1000, 0, java.time.ZoneOffset.UTC) : null)
                .stopTime(pluginInfo.getStopTime() > 0 ? 
                          LocalDateTime.ofEpochSecond(pluginInfo.getStopTime() / 1000, 0, java.time.ZoneOffset.UTC) : null)
                .extensionInfo(pluginInfo.getExtensionInfo())
                .isFollowSystem(pluginInfo.isFollowSystem())
                .build();
    }
}
