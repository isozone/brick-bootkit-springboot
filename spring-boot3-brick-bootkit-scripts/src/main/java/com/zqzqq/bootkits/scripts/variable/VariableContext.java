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


package com.zqzqq.bootkits.scripts.variable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 变量上下文
 * 提供脚本执行时的变量存储和访问
 *
 * @author starBlues
 * @since 4.0.1
 */
public class VariableContext {
    
    // 变量存储
    private final Map<String, Object> variables;
    private final Map<String, String> metadata;
    
    // 上下文信息
    private String contextId;
    private final LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;
    private final Map<String, Object> executionContext;
    
    // 变量定义
    private final Map<String, VariableDefinition> variableDefinitions;
    
    /**
     * 变量定义
     */
    public static class VariableDefinition {
        private String name;
        private Class<?> type;
        private Object defaultValue;
        private boolean required;
        private String description;
        private Set<String> allowedValues;
        private String pattern;
        private VariableScope scope;
        
        public VariableDefinition() {}
        
        public VariableDefinition(String name, Class<?> type, Object defaultValue, boolean required, String description) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.required = required;
            this.description = description;
            this.scope = VariableScope.CONTEXT;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Class<?> getType() { return type; }
        public void setType(Class<?> type) { this.type = type; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Set<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(Set<String> allowedValues) { this.allowedValues = allowedValues; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public VariableScope getScope() { return scope; }
        public void setScope(VariableScope scope) { this.scope = scope; }
        
        @Override
        public String toString() {
            return "VariableDefinition{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", defaultValue=" + defaultValue +
                    ", required=" + required +
                    ", scope=" + scope +
                    '}';
        }
    }
    
    /**
     * 变量作用域
     */
    public enum VariableScope {
        /**
         * 上下文作用域
         */
        CONTEXT,
        /**
         * 系统作用域
         */
        SYSTEM,
        /**
         * 环境作用域
         */
        ENVIRONMENT,
        /**
         * 配置作用域
         */
        CONFIGURATION,
        /**
         * 全局作用域
         */
        GLOBAL
    }
    
    /**
     * 变量验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private Object correctedValue;
        
        private ValidationResult(boolean valid, String message, Object correctedValue) {
            this.valid = valid;
            this.message = message;
            this.correctedValue = correctedValue;
        }
        
        public static ValidationResult success(String message) {
            return new ValidationResult(true, message, null);
        }
        
        public static ValidationResult success(String message, Object correctedValue) {
            return new ValidationResult(true, message, correctedValue);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public Object getCorrectedValue() { return correctedValue; }
    }
    
    /**
     * 构造函数
     */
    public VariableContext() {
        this.variables = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.variableDefinitions = new ConcurrentHashMap<>();
        this.executionContext = new ConcurrentHashMap<>();
        this.contextId = UUID.randomUUID().toString();
        this.createdTime = LocalDateTime.now();
        this.lastModifiedTime = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     *
     * @param contextId 上下文ID
     */
    public VariableContext(String contextId) {
        this();
        this.contextId = contextId;
    }
    
    /**
     * 设置变量
     *
     * @param name 变量名
     * @param value 变量值
     */
    public void setVariable(String name, Object value) {
        if (name != null && !name.trim().isEmpty()) {
            variables.put(name, value);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 设置变量
     *
     * @param name 变量名
     * @param value 变量值
     * @param scope 变量作用域
     */
    public void setVariable(String name, Object value, VariableScope scope) {
        setVariable(name, value);
        VariableDefinition def = variableDefinitions.get(name);
        if (def != null) {
            def.setScope(scope);
        }
    }
    
    /**
     * 获取变量
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getVariable(String name) {
        return variables.get(name);
    }
    
    /**
     * 获取变量
     *
     * @param name 变量名
     * @param defaultValue 默认值
     * @return 变量值或默认值
     */
    public Object getVariable(String name, Object defaultValue) {
        Object value = variables.get(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取字符串变量
     *
     * @param name 变量名
     * @return 字符串值
     */
    public String getString(String name) {
        Object value = getVariable(name);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取字符串变量
     *
     * @param name 变量名
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public String getString(String name, String defaultValue) {
        String value = getString(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取数字变量
     *
     * @param name 变量名
     * @return 数字值
     */
    public Number getNumber(String name) {
        Object value = getVariable(name);
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取布尔变量
     *
     * @param name 变量名
     * @return 布尔值
     */
    public Boolean getBoolean(String name) {
        Object value = getVariable(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return "true".equalsIgnoreCase(value.toString()) || 
                   "1".equals(value.toString()) ||
                   "yes".equalsIgnoreCase(value.toString());
        }
        return null;
    }
    
    /**
     * 检查变量是否存在
     *
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }
    
    /**
     * 移除变量
     *
     * @param name 变量名
     * @return 被移除的变量值
     */
    public Object removeVariable(String name) {
        lastModifiedTime = LocalDateTime.now();
        return variables.remove(name);
    }
    
    /**
     * 清空所有变量
     */
    public void clearVariables() {
        variables.clear();
        lastModifiedTime = LocalDateTime.now();
    }
    
    /**
     * 获取所有变量名
     *
     * @return 变量名集合
     */
    public Set<String> getVariableNames() {
        return new HashSet<>(variables.keySet());
    }
    
    /**
     * 获取所有变量
     *
     * @return 变量映射
     */
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(variables);
    }
    
    /**
     * 设置变量定义
     *
     * @param definition 变量定义
     */
    public void setVariableDefinition(VariableDefinition definition) {
        if (definition != null && definition.getName() != null) {
            variableDefinitions.put(definition.getName(), definition);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取变量定义
     *
     * @param name 变量名
     * @return 变量定义
     */
    public VariableDefinition getVariableDefinition(String name) {
        return variableDefinitions.get(name);
    }
    
    /**
     * 获取所有变量定义
     *
     * @return 变量定义映射
     */
    public Map<String, VariableDefinition> getAllVariableDefinitions() {
        return new HashMap<>(variableDefinitions);
    }
    
    /**
     * 验证变量
     *
     * @param name 变量名
     * @param value 变量值
     * @return 验证结果
     */
    public ValidationResult validateVariable(String name, Object value) {
        VariableDefinition definition = variableDefinitions.get(name);
        if (definition == null) {
            return ValidationResult.success("变量未定义，使用默认值");
        }
        
        // 检查必需字段
        if (definition.isRequired() && (value == null || value.toString().trim().isEmpty())) {
            if (definition.getDefaultValue() != null) {
                return ValidationResult.success("使用默认值: " + definition.getDefaultValue(), definition.getDefaultValue());
            }
            return ValidationResult.failure("必需变量 '" + name + "' 不能为空");
        }
        
        // 检查默认值
        if (value == null || value.toString().trim().isEmpty()) {
            if (definition.getDefaultValue() != null) {
                return ValidationResult.success("使用默认值: " + definition.getDefaultValue(), definition.getDefaultValue());
            }
            return ValidationResult.success("变量为空，使用空值");
        }
        
        String valueStr = value.toString();
        
        // 检查类型
        if (definition.getType() != null) {
            if (definition.getType() == Number.class) {
                try {
                    Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    return ValidationResult.failure("变量 '" + name + "' 必须是数字");
                }
            } else if (definition.getType() == Boolean.class) {
                if (!"true".equalsIgnoreCase(valueStr) && !"false".equalsIgnoreCase(valueStr)) {
                    return ValidationResult.failure("变量 '" + name + "' 必须是布尔值 (true/false)");
                }
            }
        }
        
        // 检查允许的值
        if (definition.getAllowedValues() != null && !definition.getAllowedValues().isEmpty()) {
            if (!definition.getAllowedValues().contains(valueStr)) {
                return ValidationResult.failure("变量 '" + name + "' 的值必须是以下之一: " + 
                                             String.join(", ", definition.getAllowedValues()));
            }
        }
        
        // 检查正则表达式
        if (definition.getPattern() != null && !definition.getPattern().isEmpty()) {
            if (!valueStr.matches(definition.getPattern())) {
                return ValidationResult.failure("变量 '" + name + "' 不匹配要求的格式: " + definition.getPattern());
            }
        }
        
        return ValidationResult.success("变量验证通过");
    }
    
    /**
     * 设置元数据
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, String value) {
        if (key != null) {
            metadata.put(key, value);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取元数据
     *
     * @param key 键
     * @return 值
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 获取所有元数据
     *
     * @return 元数据映射
     */
    public Map<String, String> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 设置执行上下文
     *
     * @param key 键
     * @param value 值
     */
    public void setExecutionContext(String key, Object value) {
        if (key != null) {
            executionContext.put(key, value);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取执行上下文
     *
     * @param key 键
     * @return 值
     */
    public Object getExecutionContext(String key) {
        return executionContext.get(key);
    }
    
    /**
     * 获取所有执行上下文
     *
     * @return 执行上下文映射
     */
    public Map<String, Object> getAllExecutionContext() {
        return new HashMap<>(executionContext);
    }
    
    /**
     * 复制上下文
     *
     * @return 新的上下文实例
     */
    public VariableContext copy() {
        VariableContext copy = new VariableContext();
        copy.variables.putAll(this.variables);
        copy.metadata.putAll(this.metadata);
        copy.variableDefinitions.putAll(this.variableDefinitions);
        copy.executionContext.putAll(this.executionContext);
        return copy;
    }
    
    /**
     * 合并上下文
     *
     * @param other 其他上下文
     * @param overwrite 是否覆盖已存在的变量
     */
    public void merge(VariableContext other, boolean overwrite) {
        if (other == null) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : other.variables.entrySet()) {
            if (!overwrite && hasVariable(entry.getKey())) {
                continue;
            }
            setVariable(entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<String, VariableDefinition> entry : other.variableDefinitions.entrySet()) {
            if (!overwrite && variableDefinitions.containsKey(entry.getKey())) {
                continue;
            }
            setVariableDefinition(entry.getValue());
        }
        
        for (Map.Entry<String, Object> entry : other.executionContext.entrySet()) {
            if (!overwrite && executionContext.containsKey(entry.getKey())) {
                continue;
            }
            setExecutionContext(entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<String, String> entry : other.metadata.entrySet()) {
            if (!overwrite && metadata.containsKey(entry.getKey())) {
                continue;
            }
            setMetadata(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 获取上下文统计信息
     *
     * @return 统计信息
     */
    public ContextStatistics getStatistics() {
        return new ContextStatistics(
            variables.size(),
            variableDefinitions.size(),
            metadata.size(),
            executionContext.size(),
            createdTime,
            lastModifiedTime
        );
    }
    
    /**
     * 上下文统计信息
     */
    public static class ContextStatistics {
        private final int variableCount;
        private final int definitionCount;
        private final int metadataCount;
        private final int executionContextCount;
        private final LocalDateTime createdTime;
        private final LocalDateTime lastModifiedTime;
        
        public ContextStatistics(int variableCount, int definitionCount, int metadataCount, 
                               int executionContextCount, LocalDateTime createdTime, LocalDateTime lastModifiedTime) {
            this.variableCount = variableCount;
            this.definitionCount = definitionCount;
            this.metadataCount = metadataCount;
            this.executionContextCount = executionContextCount;
            this.createdTime = createdTime;
            this.lastModifiedTime = lastModifiedTime;
        }
        
        public int getVariableCount() { return variableCount; }
        public int getDefinitionCount() { return definitionCount; }
        public int getMetadataCount() { return metadataCount; }
        public int getExecutionContextCount() { return executionContextCount; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    }
    
    // Getters
    public String getContextId() { return contextId; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    
    @Override
    public String toString() {
        return "VariableContext{" +
                "contextId='" + contextId + '\'' +
                ", variableCount=" + variables.size() +
                ", definitionCount=" + variableDefinitions.size() +
                ", createdTime=" + createdTime +
                ", lastModifiedTime=" + lastModifiedTime +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VariableContext that = (VariableContext) obj;
        return contextId.equals(that.contextId);
    }
    
    @Override
    public int hashCode() {
        return contextId.hashCode();
    }
}