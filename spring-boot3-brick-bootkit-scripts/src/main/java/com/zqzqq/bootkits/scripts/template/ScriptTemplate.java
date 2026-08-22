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


package com.zqzqq.bootkits.scripts.template;

import com.zqzqq.bootkits.scripts.core.ScriptType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本模板
 * 用于定义可复用的脚本模板，支持变量替换和参数化
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptTemplate {
    
    /**
     * 模板变量类型
     */
    public enum VariableType {
        /**
         * 字符串类型
         */
        STRING,
        /**
         * 数字类型
         */
        NUMBER,
        /**
         * 布尔类型
         */
        BOOLEAN,
        /**
         * 列表类型
         */
        LIST,
        /**
         * 对象类型
         */
        OBJECT
    }
    
    /**
     * 模板变量定义
     */
    public static class TemplateVariable {
        private String name;
        private VariableType type;
        private String description;
        private String defaultValue;
        private boolean required;
        private String pattern;
        private List<String> allowedValues;
        
        public TemplateVariable() {
        }
        
        public TemplateVariable(String name, VariableType type, String description, String defaultValue, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
            this.required = required;
        }
        
        public TemplateVariable(String name, VariableType type, String description, String defaultValue, boolean required, 
                              String pattern, List<String> allowedValues) {
            this(name, type, description, defaultValue, required);
            this.pattern = pattern;
            this.allowedValues = allowedValues;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public VariableType getType() { return type; }
        public void setType(VariableType type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public List<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
        
        @Override
        public String toString() {
            return "TemplateVariable{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", description='" + description + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    ", required=" + required +
                    ", pattern='" + pattern + '\'' +
                    ", allowedValues=" + allowedValues +
                    '}';
        }
    }
    
    // 模板属性
    private String templateId;
    private String name;
    private String description;
    private String scriptContent;
    private ScriptType scriptType;
    private Map<String, TemplateVariable> variables;
    private String category;
    private String tags;
    private String author;
    private String version;
    private LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;
    private boolean isActive;
    private long usageCount;
    private Map<String, Object> metadata;
    
    /**
     * 构造函数
     */
    public ScriptTemplate() {
        this.variables = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.createdTime = LocalDateTime.now();
        this.lastModifiedTime = LocalDateTime.now();
        this.isActive = true;
        this.usageCount = 0;
        this.version = "1.0.0";
    }
    
    /**
     * 构造函数
     *
     * @param templateId 模板ID
     * @param name 模板名称
     * @param description 模板描述
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     */
    public ScriptTemplate(String templateId, String name, String description, 
                         String scriptContent, ScriptType scriptType) {
        this();
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.scriptContent = scriptContent;
        this.scriptType = scriptType;
    }
    
    /**
     * 添加变量定义
     *
     * @param variable 变量定义
     */
    public void addVariable(TemplateVariable variable) {
        if (variable != null && variable.getName() != null) {
            variables.put(variable.getName(), variable);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 移除变量定义
     *
     * @param variableName 变量名
     */
    public void removeVariable(String variableName) {
        if (variableName != null) {
            variables.remove(variableName);
            lastModifiedTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取变量定义
     *
     * @param variableName 变量名
     * @return 变量定义
     */
    public TemplateVariable getVariable(String variableName) {
        return variables.get(variableName);
    }
    
    /**
     * 获取所有变量定义
     *
     * @return 变量定义列表
     */
    public List<TemplateVariable> getVariables() {
        return new ArrayList<>(variables.values());
    }
    
    /**
     * 检查是否包含指定变量
     *
     * @param variableName 变量名
     * @return 是否包含
     */
    public boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }
    
    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        this.usageCount++;
    }
    
    /**
     * 设置元数据
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
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
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 验证变量值
     *
     * @param variableName 变量名
     * @param value 变量值
     * @return 验证结果
     */
    public ValidationResult validateVariable(String variableName, Object value) {
        TemplateVariable variable = variables.get(variableName);
        if (variable == null) {
            return ValidationResult.error("变量 '" + variableName + "' 不存在");
        }
        
        // 检查必需字段
        if (variable.isRequired() && (value == null || value.toString().trim().isEmpty())) {
            return ValidationResult.error("变量 '" + variableName + "' 是必需的");
        }
        
        // 检查默认值
        if (value == null || value.toString().trim().isEmpty()) {
            if (variable.getDefaultValue() != null) {
                value = variable.getDefaultValue();
                return ValidationResult.ok("使用默认值: " + variable.getDefaultValue());
            }
        }
        
        String valueStr = value.toString();
        
        // 检查类型
        switch (variable.getType()) {
            case NUMBER:
                try {
                    Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    return ValidationResult.error("变量 '" + variableName + "' 必须是数字");
                }
                break;
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(valueStr) && !"false".equalsIgnoreCase(valueStr)) {
                    return ValidationResult.error("变量 '" + variableName + "' 必须是布尔值 (true/false)");
                }
                break;
            case LIST:
                if (!valueStr.contains(",")) {
                    return ValidationResult.error("变量 '" + variableName + "' 必须是逗号分隔的列表");
                }
                break;
        }
        
        // 检查允许的值
        if (variable.getAllowedValues() != null && !variable.getAllowedValues().isEmpty()) {
            if (!variable.getAllowedValues().contains(valueStr)) {
                return ValidationResult.error("变量 '" + variableName + "' 的值必须是以下之一: " + 
                                             String.join(", ", variable.getAllowedValues()));
            }
        }
        
        // 检查正则表达式
        if (variable.getPattern() != null && !variable.getPattern().isEmpty()) {
            if (!valueStr.matches(variable.getPattern())) {
                return ValidationResult.error("变量 '" + variableName + "' 不匹配要求的格式: " + variable.getPattern());
            }
        }
        
        return ValidationResult.ok("变量验证通过");
    }
    
    /**
     * 验证结果
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
        
        public static ValidationResult ok(String message) {
            return new ValidationResult(true, message, null);
        }
        
        public static ValidationResult ok(String message, Object correctedValue) {
            return new ValidationResult(true, message, correctedValue);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, null);
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public Object getCorrectedValue() { return correctedValue; }
    }
    
    // Getters and Setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; lastModifiedTime = LocalDateTime.now(); }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; lastModifiedTime = LocalDateTime.now(); }
    
    public String getScriptContent() { return scriptContent; }
    public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; lastModifiedTime = LocalDateTime.now(); }
    
    public ScriptType getScriptType() { return scriptType; }
    public void setScriptType(ScriptType scriptType) { this.scriptType = scriptType; lastModifiedTime = LocalDateTime.now(); }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; lastModifiedTime = LocalDateTime.now(); }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; lastModifiedTime = LocalDateTime.now(); }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; lastModifiedTime = LocalDateTime.now(); }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; lastModifiedTime = LocalDateTime.now(); }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; lastModifiedTime = LocalDateTime.now(); }
    
    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; lastModifiedTime = LocalDateTime.now(); }
    
    @Override
    public String toString() {
        return "ScriptTemplate{" +
                "templateId='" + templateId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", scriptType=" + scriptType +
                ", category='" + category + '\'' +
                ", tags='" + tags + '\'' +
                ", author='" + author + '\'' +
                ", version='" + version + '\'' +
                ", isActive=" + isActive +
                ", usageCount=" + usageCount +
                ", variablesCount=" + variables.size() +
                ", createdTime=" + createdTime +
                ", lastModifiedTime=" + lastModifiedTime +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ScriptTemplate that = (ScriptTemplate) obj;
        return java.util.Objects.equals(templateId, that.templateId);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(templateId);
    }
}