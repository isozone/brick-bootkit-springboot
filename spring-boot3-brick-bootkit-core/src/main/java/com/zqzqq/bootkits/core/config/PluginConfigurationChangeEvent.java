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


package com.zqzqq.bootkits.core.config;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

/**
 * 插件配置变更事件
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class PluginConfigurationChangeEvent extends ApplicationEvent {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginConfigurationChangeEvent.class);
    
    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 旧配置
     */
    private final PluginConfiguration oldConfiguration;
    
    /**
     * 新配置
     */
    private final PluginConfiguration newConfiguration;
    
    /**
     * 变更原因
     */
    private final String changeReason;
    
    /**
     * 变更时间戳
     */
    private final long timestamp;
    
    public PluginConfigurationChangeEvent(String pluginId, 
                                        PluginConfiguration oldConfiguration,
                                        PluginConfiguration newConfiguration,
                                        String changeReason) {
        super(pluginId);
        this.pluginId = pluginId;
        this.oldConfiguration = oldConfiguration;
        this.newConfiguration = newConfiguration;
        this.changeReason = changeReason;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 检查是否为新增配置
     */
    public boolean isCreation() {
        return oldConfiguration == null && newConfiguration != null;
    }
    
    /**
     * 检查是否为删除配置
     */
    public boolean isDeletion() {
        return oldConfiguration != null && newConfiguration == null;
    }
    
    /**
     * 检查是否为更新配置
     */
    public boolean isUpdate() {
        return oldConfiguration != null && newConfiguration != null;
    }
    
    /**
     * 获取变更的属性键集合
     */
    public java.util.Set<String> getChangedPropertyKeys() {
        if (!isUpdate()) {
            return java.util.Collections.emptySet();
        }
        
        java.util.Set<String> changedKeys = new java.util.HashSet<>();
        
        // 检查新增和修改的属性
        for (String key : newConfiguration.getProperties().keySet()) {
            Object newValue = newConfiguration.getProperties().get(key);
            Object oldValue = oldConfiguration.getProperties().get(key);
            
            if (!java.util.Objects.equals(newValue, oldValue)) {
                changedKeys.add(key);
            }
        }
        
        // 检查删除的属性
        for (String key : oldConfiguration.getProperties().keySet()) {
            if (!newConfiguration.getProperties().containsKey(key)) {
                changedKeys.add(key);
            }
        }
        
        return changedKeys;
    }
    
    /**
     * 检查指定属性是否发生变更
     */
    public boolean isPropertyChanged(String propertyKey) {
        return getChangedPropertyKeys().contains(propertyKey);
    }
    
    /**
     * 获取属性的旧值
     */
    public Object getOldPropertyValue(String propertyKey) {
        return oldConfiguration != null ? 
                oldConfiguration.getProperties().get(propertyKey) : null;
    }
    
    /**
     * 获取属性的新值
     */
    public Object getNewPropertyValue(String propertyKey) {
        return newConfiguration != null ? 
                newConfiguration.getProperties().get(propertyKey) : null;
    }
    
    /**
     * 配置变更类型枚举
     */
    public enum ChangeType {
        CREATED,
        UPDATED,
        REMOVED
    }
    
    // Getter methods
    public String getPluginId() {
        return pluginId;
    }
    
    public PluginConfiguration getOldConfiguration() {
        return oldConfiguration;
    }
    
    public PluginConfiguration getNewConfiguration() {
        return newConfiguration;
    }
    
    public String getChangeReason() {
        return changeReason;
    }
    
    public long getEventTimestamp() {
        return timestamp;
    }
}
