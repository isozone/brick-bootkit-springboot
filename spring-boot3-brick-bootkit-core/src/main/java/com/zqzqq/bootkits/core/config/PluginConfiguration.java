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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 插件配置实体
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class PluginConfiguration {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 配置版本
     */
    private String version;
    
    /**
     * 环境标识
     */
    private String environment;
    
    /**
     * 配置属性
     */
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * 配置元数据
     */
    private PluginConfigurationMetadata metadata;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;

    public PluginConfiguration() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.metadata = new PluginConfigurationMetadata();
    }

    public PluginConfiguration(String pluginId, String version) {
        this();
        this.pluginId = pluginId;
        this.version = version;
    }

    /**
     * 复制配置（创建深度副本）
     */
    public PluginConfiguration copy() {
        PluginConfiguration copy = new PluginConfiguration();
        copy.pluginId = this.pluginId;
        copy.version = this.version;
        copy.environment = this.environment;
        copy.properties = new HashMap<>(this.properties);
        copy.metadata = this.metadata.copy();
        copy.createdAt = this.createdAt;
        copy.updatedAt = LocalDateTime.now(); // 复制时更新时间为现在
        copy.description = this.description;
        copy.enabled = this.enabled;
        return copy;
    }

    /**
     * 获取插件ID（public方法，确保兼容性）
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取版本（public方法，确保兼容性）
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取环境（public方法，确保兼容性）
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * 获取属性集合（public方法，确保兼容性）
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * 获取配置元数据（public方法，确保兼容性）
     */
    public PluginConfigurationMetadata getMetadata() {
        return metadata;
    }

    /**
     * 获取创建时间（public方法，确保兼容性）
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取最后更新时间（public方法，确保兼容性）
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取描述（public方法，确保兼容性）
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否启用（public方法，确保兼容性）
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置插件ID（public方法，确保兼容性）
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置版本（public方法，确保兼容性）
     */
    public void setVersion(String version) {
        this.version = version;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置环境（public方法，确保兼容性）
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置配置元数据（public方法，确保兼容性）
     */
    public void setMetadata(PluginConfigurationMetadata metadata) {
        this.metadata = metadata;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置描述（public方法，确保兼容性）
     */
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置启用状态（public方法，确保兼容性）
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置属性值（public方法，确保兼容性）
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置属性值（String类型）
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置属性值（int类型）
     */
    public void setProperty(String key, int value) {
        properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置属性值（boolean类型）
     */
    public void setProperty(String key, boolean value) {
        properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查属性是否存在
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * 移除属性
     */
    public Object removeProperty(String key) {
        Object removed = properties.remove(key);
        if (removed != null) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    /**
     * 获取String类型属性
     */
    public String getString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取String类型属性（带默认值）
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取Integer类型属性
     */
    public Integer getInteger(String key) {
        Object value = properties.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取Integer类型属性（带默认值）
     */
    public Integer getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取Boolean类型属性
     */
    public Boolean getBoolean(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return null;
    }

    /**
     * 获取Boolean类型属性（带默认值）
     */
    public Boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取属性值（public方法，确保兼容性）
     */
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        try {
            return type.getConstructor(String.class).newInstance(value.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("无法将值 '" + value + "' 转换为类型 " + type.getName(), e);
        }
    }

    /**
     * 获取属性值（带默认值）
     */
    public <T> T getProperty(String key, T defaultValue, Class<T> type) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }

    

    /**
     * 检查配置是否有效（public方法，确保兼容性）
     */
    public boolean isValid() {
        return pluginId != null && !pluginId.trim().isEmpty() &&
               version != null && !version.trim().isEmpty() &&
               metadata != null;
    }

    /**
     * 添加单个参数的构造函数（public方法，确保兼容性）
     */
    public PluginConfiguration(String pluginId) {
        this(pluginId, "1.0.0");
    }
    
    /**
     * 配置元数据
     */
    public static class PluginConfigurationMetadata {
        
        /**
         * 配置来源
         */
        private String source;
        
        /**
         * 配置格式
         */
        private String format;
        
        /**
         * 配置校验规则
         */
        private Map<String, String> validationRules = new HashMap<>();
        
        /**
         * 敏感属性列表
         */
        private Set<String> sensitiveProperties = new HashSet<>();
        
        /**
         * 配置标签
         */
        private Map<String, String> tags = new HashMap<>();
        
        public PluginConfigurationMetadata copy() {
            PluginConfigurationMetadata copy = new PluginConfigurationMetadata();
            copy.source = this.source;
            copy.format = this.format;
            copy.validationRules = new HashMap<>(this.validationRules);
            copy.sensitiveProperties = new HashSet<>(this.sensitiveProperties);
            copy.tags = new HashMap<>(this.tags);
            return copy;
        }
        
        /**
         * 获取配置来源（public方法，确保兼容性）
         */
        public String getSource() {
            return source;
        }

        /**
         * 设置配置来源（public方法，确保兼容性）
         */
        public void setSource(String source) {
            this.source = source;
        }

        /**
         * 获取配置格式（public方法，确保兼容性）
         */
        public String getFormat() {
            return format;
        }

        /**
         * 设置配置格式（public方法，确保兼容性）
         */
        public void setFormat(String format) {
            this.format = format;
        }

        /**
         * 获取配置校验规则（public方法，确保兼容性）
         */
        public Map<String, String> getValidationRules() {
            return validationRules;
        }

        /**
         * 设置配置校验规则（public方法，确保兼容性）
         */
        public void setValidationRules(Map<String, String> validationRules) {
            this.validationRules = validationRules;
        }

        /**
         * 获取敏感属性列表（public方法，确保兼容性）
         */
        public Set<String> getSensitiveProperties() {
            return sensitiveProperties;
        }

        /**
         * 设置敏感属性列表（public方法，确保兼容性）
         */
        public void setSensitiveProperties(Set<String> sensitiveProperties) {
            this.sensitiveProperties = sensitiveProperties;
        }

        /**
         * 获取配置标签（public方法，确保兼容性）
         */
        public Map<String, String> getTags() {
            return tags;
        }

        /**
         * 设置配置标签（public方法，确保兼容性）
         */
        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }
}