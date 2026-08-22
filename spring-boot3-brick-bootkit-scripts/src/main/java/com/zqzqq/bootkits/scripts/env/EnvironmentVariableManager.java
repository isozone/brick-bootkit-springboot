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


package com.zqzqq.bootkits.scripts.env;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 环境变量管理器接口
 * 提供完整的环境变量管理功能，包括作用域管理、验证转换、导入导出等
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface EnvironmentVariableManager {
    
    /**
     * 环境变量作用域枚举
     */
    enum Scope {
        /** 系统级作用域 - 对所有进程和用户可见 */
        SYSTEM,
        /** 用户级作用域 - 对当前用户的所有进程可见 */
        USER,
        /** 项目级作用域 - 仅对当前项目/应用可见 */
        PROJECT,
        /** 进程级作用域 - 仅对当前进程可见 */
        PROCESS
    }
    
    /**
     * 环境变量类型枚举
     */
    enum VariableType {
        STRING, INTEGER, LONG, DOUBLE, BOOLEAN, LIST, MAP
    }
    
    /**
     * 环境变量定义
     */
    class EnvironmentVariable {
        private final String key;
        private final Object value;
        private final VariableType type;
        private final Scope scope;
        private final boolean readOnly;
        private final String description;
        private final LocalDateTime createdTime;
        private final LocalDateTime lastModifiedTime;
        private final String source;
        
        public EnvironmentVariable(String key, Object value, VariableType type, Scope scope, 
                                boolean readOnly, String description, String source) {
            this.key = key;
            this.value = value;
            this.type = type;
            this.scope = scope;
            this.readOnly = readOnly;
            this.description = description;
            this.createdTime = LocalDateTime.now();
            this.lastModifiedTime = this.createdTime;
            this.source = source;
        }
        
        public EnvironmentVariable(String key, Object value, VariableType type, Scope scope, 
                                boolean readOnly, String description, LocalDateTime createdTime,
                                LocalDateTime lastModifiedTime, String source) {
            this.key = key;
            this.value = value;
            this.type = type;
            this.scope = scope;
            this.readOnly = readOnly;
            this.description = description;
            this.createdTime = createdTime;
            this.lastModifiedTime = lastModifiedTime;
            this.source = source;
        }
        
        public String getKey() { return key; }
        public Object getValue() { return value; }
        public VariableType getType() { return type; }
        public Scope getScope() { return scope; }
        public boolean isReadOnly() { return readOnly; }
        public String getDescription() { return description; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
        public String getSource() { return source; }
        
        public void updateLastModifiedTime() {
            // 注意：这里无法直接修改final字段，需要创建新对象
        }
        
        @Override
        public String toString() {
            return String.format("EnvironmentVariable{key='%s', value=%s, type=%s, scope=%s, readOnly=%s}", 
                    key, value, type, scope, readOnly);
        }
    }
    
    /**
     * 环境变量查询条件
     */
    class EnvironmentVariableQuery {
        private final Scope scope;
        private final VariableType type;
        private final String keyPattern;
        private final String descriptionPattern;
        private final Boolean readOnly;
        private final String sourcePattern;
        private final LocalDateTime createdAfter;
        private final LocalDateTime createdBefore;
        
        private EnvironmentVariableQuery(Builder builder) {
            this.scope = builder.scope;
            this.type = builder.type;
            this.keyPattern = builder.keyPattern;
            this.descriptionPattern = builder.descriptionPattern;
            this.readOnly = builder.readOnly;
            this.sourcePattern = builder.sourcePattern;
            this.createdAfter = builder.createdAfter;
            this.createdBefore = builder.createdBefore;
        }
        
        public static class Builder {
            private Scope scope;
            private VariableType type;
            private String keyPattern;
            private String descriptionPattern;
            private Boolean readOnly;
            private String sourcePattern;
            private LocalDateTime createdAfter;
            private LocalDateTime createdBefore;
            
            public Builder scope(Scope scope) {
                this.scope = scope;
                return this;
            }
            
            public Builder type(VariableType type) {
                this.type = type;
                return this;
            }
            
            public Builder keyPattern(String keyPattern) {
                this.keyPattern = keyPattern;
                return this;
            }
            
            public Builder descriptionPattern(String descriptionPattern) {
                this.descriptionPattern = descriptionPattern;
                return this;
            }
            
            public Builder readOnly(Boolean readOnly) {
                this.readOnly = readOnly;
                return this;
            }
            
            public Builder sourcePattern(String sourcePattern) {
                this.sourcePattern = sourcePattern;
                return this;
            }
            
            public Builder createdAfter(LocalDateTime createdAfter) {
                this.createdAfter = createdAfter;
                return this;
            }
            
            public Builder createdBefore(LocalDateTime createdBefore) {
                this.createdBefore = createdBefore;
                return this;
            }
            
            public EnvironmentVariableQuery build() {
                return new EnvironmentVariableQuery(this);
            }
        }
        
        public static Builder newBuilder() {
            return new Builder();
        }
        
        public Scope getScope() { return scope; }
        public VariableType getType() { return type; }
        public String getKeyPattern() { return keyPattern; }
        public String getDescriptionPattern() { return descriptionPattern; }
        public Boolean getReadOnly() { return readOnly; }
        public String getSourcePattern() { return sourcePattern; }
        public LocalDateTime getCreatedAfter() { return createdAfter; }
        public LocalDateTime getCreatedBefore() { return createdBefore; }
    }
    
    /**
     * 环境变量统计信息
     */
    class EnvironmentVariableStatistics {
        private final long totalVariables;
        private final long systemScopeCount;
        private final long userScopeCount;
        private final long projectScopeCount;
        private final long processScopeCount;
        private final long readOnlyCount;
        private final long writableCount;
        private final Map<VariableType, Long> typeDistribution;
        private final Map<String, Long> sourceDistribution;
        private final LocalDateTime lastModifiedTime;
        
        public EnvironmentVariableStatistics(long totalVariables, long systemScopeCount, 
                                           long userScopeCount, long projectScopeCount, 
                                           long processScopeCount, long readOnlyCount, 
                                           long writableCount, Map<VariableType, Long> typeDistribution,
                                           Map<String, Long> sourceDistribution, LocalDateTime lastModifiedTime) {
            this.totalVariables = totalVariables;
            this.systemScopeCount = systemScopeCount;
            this.userScopeCount = userScopeCount;
            this.projectScopeCount = projectScopeCount;
            this.processScopeCount = processScopeCount;
            this.readOnlyCount = readOnlyCount;
            this.writableCount = writableCount;
            this.typeDistribution = typeDistribution;
            this.sourceDistribution = sourceDistribution;
            this.lastModifiedTime = lastModifiedTime;
        }
        
        public long getTotalVariables() { return totalVariables; }
        public long getSystemScopeCount() { return systemScopeCount; }
        public long getUserScopeCount() { return userScopeCount; }
        public long getProjectScopeCount() { return projectScopeCount; }
        public long getProcessScopeCount() { return processScopeCount; }
        public long getReadOnlyCount() { return readOnlyCount; }
        public long getWritableCount() { return writableCount; }
        public Map<VariableType, Long> getTypeDistribution() { return typeDistribution; }
        public Map<String, Long> getSourceDistribution() { return sourceDistribution; }
        public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
        
        @Override
        public String toString() {
            return String.format("EnvironmentVariableStatistics{total=%d, system=%d, user=%d, project=%d, process=%d, readOnly=%d, writable=%d}",
                    totalVariables, systemScopeCount, userScopeCount, projectScopeCount, 
                    processScopeCount, readOnlyCount, writableCount);
        }
    }
    
    // ==================== 基础操作 ====================
    
    /**
     * 设置环境变量
     *
     * @param key 环境变量键
     * @param value 环境变量值
     * @param type 环境变量类型
     * @param scope 作用域
     * @param description 描述
     * @param source 来源
     * @return 设置结果
     */
    boolean setVariable(String key, Object value, VariableType type, Scope scope, 
                       String description, String source);
    
    /**
     * 设置环境变量（使用默认作用域和描述）
     *
     * @param key 环境变量键
     * @param value 环境变量值
     * @param type 环境变量类型
     * @return 设置结果
     */
    boolean setVariable(String key, Object value, VariableType type);
    
    /**
     * 获取环境变量
     *
     * @param key 环境变量键
     * @return 环境变量
     */
    Optional<EnvironmentVariable> getVariable(String key);
    
    /**
     * 获取环境变量值
     *
     * @param key 环境变量键
     * @return 环境变量值
     */
    Optional<Object> getValue(String key);
    
    /**
     * 删除环境变量
     *
     * @param key 环境变量键
     * @return 删除结果
     */
    boolean removeVariable(String key);
    
    /**
     * 检查环境变量是否存在
     *
     * @param key 环境变量键
     * @return 是否存在
     */
    boolean hasVariable(String key);
    
    /**
     * 获取所有环境变量
     *
     * @return 所有环境变量
     */
    List<EnvironmentVariable> getAllVariables();
    
    /**
     * 获取指定作用域的环境变量
     *
     * @param scope 作用域
     * @return 指定作用域的环境变量
     */
    List<EnvironmentVariable> getVariablesByScope(Scope scope);
    
    // ==================== 查询和过滤 ====================
    
    /**
     * 根据条件查询环境变量
     *
     * @param query 查询条件
     * @return 查询结果
     */
    List<EnvironmentVariable> queryVariables(EnvironmentVariableQuery query);
    
    /**
     * 根据键模式查找环境变量
     *
     * @param keyPattern 键模式（支持通配符*）
     * @return 匹配的环境变量
     */
    List<EnvironmentVariable> findVariablesByKeyPattern(String keyPattern);
    
    /**
     * 根据类型查找环境变量
     *
     * @param type 变量类型
     * @return 指定类型的变量
     */
    List<EnvironmentVariable> findVariablesByType(VariableType type);
    
    /**
     * 获取环境变量键列表
     *
     * @return 环境变量键列表
     */
    List<String> getVariableKeys();
    
    /**
     * 获取环境变量值列表
     *
     * @return 环境变量值列表
     */
    List<Object> getVariableValues();
    
    // ==================== 类型转换 ====================
    
    /**
     * 获取字符串类型的值
     *
     * @param key 环境变量键
     * @return 字符串值
     */
    Optional<String> getStringValue(String key);
    
    /**
     * 获取整数类型的值
     *
     * @param key 环境变量键
     * @return 整数值
     */
    Optional<Integer> getIntegerValue(String key);
    
    /**
     * 获取长整型类型的值
     *
     * @param key 环境变量键
     * @return 长整型值
     */
    Optional<Long> getLongValue(String key);
    
    /**
     * 获取双精度浮点数类型的值
     *
     * @param key 环境变量键
     * @return 双精度浮点数值
     */
    Optional<Double> getDoubleValue(String key);
    
    /**
     * 获取布尔类型的值
     *
     * @param key 环境变量键
     * @return 布尔值
     */
    Optional<Boolean> getBooleanValue(String key);
    
    /**
     * 获取列表类型的值
     *
     * @param key 环境变量键
     * @return 列表值
     */
    Optional<List<?>> getListValue(String key);
    
    /**
     * 获取映射类型的值
     *
     * @param key 环境变量键
     * @return 映射值
     */
    Optional<Map<?, ?>> getMapValue(String key);
    
    // ==================== 导入导出 ====================
    
    /**
     * 导出环境变量为Map
     *
     * @param scope 作用域（null表示所有作用域）
     * @return 环境变量Map
     */
    Map<String, String> exportToMap(Scope scope);
    
    /**
     * 从Map导入环境变量
     *
     * @param variables Map形式的环境变量
     * @param scope 作用域
     * @param overwrite 是否覆盖已存在的变量
     * @param source 来源
     * @return 导入结果
     */
    int importFromMap(Map<String, ?> variables, Scope scope, boolean overwrite, String source);
    
    /**
     * 从系统环境变量导入
     *
     * @param prefix 键前缀（可选）
     * @param scope 作用域
     * @param overwrite 是否覆盖已存在的变量
     * @return 导入的变量数量
     */
    int importFromSystemEnvironment(String prefix, Scope scope, boolean overwrite);
    
    /**
     * 导出为JSON格式字符串
     *
     * @param scope 作用域（null表示所有作用域）
     * @return JSON格式的环境变量字符串
     */
    String exportToJson(Scope scope);
    
    /**
     * 从JSON格式字符串导入
     *
     * @param json JSON格式的环境变量字符串
     * @param overwrite 是否覆盖已存在的变量
     * @return 导入结果
     */
    boolean importFromJson(String json, boolean overwrite);
    
    // ==================== 作用域操作 ====================
    
    /**
     * 清理指定作用域的所有环境变量
     *
     * @param scope 作用域
     * @return 清理的变量数量
     */
    int clearScope(Scope scope);
    
    /**
     * 清理所有作用域的环境变量
     *
     * @return 清理的变量数量
     */
    int clearAll();
    
    /**
     * 复制作用域变量到另一个作用域
     *
     * @param sourceScope 源作用域
     * @param targetScope 目标作用域
     * @param overwrite 是否覆盖
     * @return 复制的变量数量
     */
    int copyScope(Scope sourceScope, Scope targetScope, boolean overwrite);
    
    /**
     * 验证变量是否可以在指定作用域修改
     *
     * @param key 变量键
     * @param targetScope 目标作用域
     * @return 验证结果
     */
    boolean canModifyInScope(String key, Scope targetScope);
    
    // ==================== 统计和监控 ====================
    
    /**
     * 获取环境变量统计信息
     *
     * @return 统计信息
     */
    EnvironmentVariableStatistics getStatistics();
    
    /**
     * 获取作用域分布统计
     *
     * @return 作用域分布
     */
    Map<Scope, Long> getScopeDistribution();
    
    /**
     * 获取类型分布统计
     *
     * @return 类型分布
     */
    Map<VariableType, Long> getTypeDistribution();
    
    /**
     * 检查环境变量完整性
     *
     * @return 检查结果
     */
    boolean validateIntegrity();
    
    /**
     * 修复环境变量完整性问题
     *
     * @return 修复结果
     */
    boolean repairIntegrity();
    
    // ==================== 验证和工具 ====================
    
    /**
     * 验证环境变量键
     *
     * @param key 环境变量键
     * @return 验证结果
     */
    boolean validateKey(String key);
    
    /**
     * 验证环境变量值
     *
     * @param value 环境变量值
     * @param type 环境变量类型
     * @return 验证结果
     */
    boolean validateValue(Object value, VariableType type);
    
    /**
     * 转换环境变量值类型
     *
     * @param value 原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    Object convertValue(Object value, VariableType targetType);
    
    /**
     * 格式化环境变量值用于显示
     *
     * @param variable 环境变量
     * @return 格式化后的字符串
     */
    String formatValue(EnvironmentVariable variable);
    
    /**
     * 获取环境变量名称（用于显示）
     *
     * @param variable 环境变量
     * @return 显示名称
     */
    String getDisplayName(EnvironmentVariable variable);
    
    /**
     * 搜索环境变量
     *
     * @param keyword 关键词
     * @return 匹配的环境变量
     */
    List<EnvironmentVariable> searchVariables(String keyword);
    
    /**
     * 批量设置环境变量
     *
     * @param variables 变量映射
     * @param scope 作用域
     * @param source 来源
     * @return 设置结果
     */
    boolean setVariables(Map<String, Object> variables, Scope scope, String source);
    
    /**
     * 批量删除环境变量
     *
     * @param keys 变量键列表
     * @return 删除的变量数量
     */
    int removeVariables(List<String> keys);
    
    /**
     * 获取环境变量变更历史（如果有的话）
     *
     * @param key 变量键
     * @return 变更历史
     */
    List<EnvironmentVariable> getVariableHistory(String key);
    
    /**
     * 重置环境变量到系统默认值
     *
     * @param key 变量键
     * @return 重置结果
     */
    boolean resetToSystemDefault(String key);
    
    /**
     * 备份当前环境变量配置
     *
     * @param scope 作用域（null表示所有作用域）
     * @return 备份标识
     */
    String backupVariables(Scope scope);
    
    /**
     * 恢复环境变量配置
     *
     * @param backupId 备份标识
     * @param overwrite 是否覆盖现有配置
     * @return 恢复结果
     */
    boolean restoreVariables(String backupId, boolean overwrite);
    
    /**
     * 列出所有备份
     *
     * @return 备份列表
     */
    List<String> listBackups();
    
    /**
     * 删除备份
     *
     * @param backupId 备份标识
     * @return 删除结果
     */
    boolean deleteBackup(String backupId);
    
    /**
     * 获取环境变量管理器信息
     *
     * @return 管理器信息
     */
    String getManagerInfo();
    
    /**
     * 检查管理器是否支持指定功能
     *
     * @param feature 功能名称
     * @return 是否支持
     */
    boolean supportsFeature(String feature);
    
    /**
     * 获取管理器能力列表
     *
     * @return 能力列表
     */
    List<String> getCapabilities();
}