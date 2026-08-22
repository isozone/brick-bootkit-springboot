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


package com.zqzqq.bootkits.core.dependency;

import java.util.*;

/**
 * 插件依赖解析结果
 * 包含依赖解析是否成功、错误信息、警告信息和依赖列表
 */
public class PluginDependencyResolution {
    
    private final boolean successful;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> dependencies;
    private final Map<String, DependencyStatus> dependencyStatus;
    
    public PluginDependencyResolution(boolean successful, List<String> errors, 
                                    List<String> warnings, List<String> dependencies) {
        this.successful = successful;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.dependencies = new ArrayList<>(dependencies);
        this.dependencyStatus = new HashMap<>();
    }
    
    /**
     * 创建成功的结果
     */
    public static PluginDependencyResolution success(List<String> dependencies) {
        return new PluginDependencyResolution(true, new ArrayList<>(), 
                                            new ArrayList<>(), dependencies);
    }
    
    /**
     * 创建失败的结果
     */
    public static PluginDependencyResolution failure(List<String> errors) {
        return new PluginDependencyResolution(false, new ArrayList<>(errors), 
                                            new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * 创建部分成功的结果（有警告）
     */
    public static PluginDependencyResolution partialSuccess(List<String> warnings, 
                                                           List<String> dependencies) {
        return new PluginDependencyResolution(true, new ArrayList<>(), 
                                            new ArrayList<>(warnings), dependencies);
    }
    
    /**
     * 是否解析成功
     */
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * 是否部分成功（有警告但无错误）
     */
    public boolean isPartialSuccess() {
        return successful && !warnings.isEmpty();
    }
    
    /**
     * 获取错误信息
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * 获取警告信息
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * 获取依赖列表
     */
    public List<String> getDependencies() {
        return new ArrayList<>(dependencies);
    }
    
    /**
     * 添加错误信息
     */
    public void addError(String error) {
        if (error != null) {
            errors.add(error);
        }
    }
    
    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        if (warning != null) {
            warnings.add(warning);
        }
    }
    
    /**
     * 添加依赖状态
     */
    public void addDependencyStatus(String pluginId, DependencyStatus status) {
        if (pluginId != null && status != null) {
            dependencyStatus.put(pluginId, status);
        }
    }
    
    /**
     * 获取依赖状态
     */
    public DependencyStatus getDependencyStatus(String pluginId) {
        return dependencyStatus.get(pluginId);
    }
    
    /**
     * 获取所有依赖状态
     */
    public Map<String, DependencyStatus> getAllDependencyStatus() {
        return new HashMap<>(dependencyStatus);
    }
    
    /**
     * 获取依赖总数
     */
    public int getDependencyCount() {
        return dependencies.size();
    }
    
    /**
     * 获取错误总数
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * 获取警告总数
     */
    public int getWarningCount() {
        return warnings.size();
    }
    
    /**
     * 是否包含指定插件依赖
     */
    public boolean hasDependency(String pluginId) {
        return dependencies.contains(pluginId);
    }
    
    /**
     * 检查依赖状态
     */
    public boolean hasDependencyStatus(String pluginId) {
        return dependencyStatus.containsKey(pluginId);
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginDependencyResolution{");
        sb.append("successful=").append(successful);
        
        if (!dependencies.isEmpty()) {
            sb.append(", dependencies=").append(dependencies);
        }
        
        if (!errors.isEmpty()) {
            sb.append(", errors=").append(errors);
        }
        
        if (!warnings.isEmpty()) {
            sb.append(", warnings=").append(warnings);
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 依赖状态
     */
    public enum DependencyStatus {
        /**
         * 正常状态
         */
        RESOLVED("已解析"),
        
        /**
         * 未找到
         */
        NOT_FOUND("未找到"),
        
        /**
         * 版本冲突
         */
        VERSION_CONFLICT("版本冲突"),
        
        /**
         * 已弃用
         */
        DEPRECATED("已弃用"),
        
        /**
         * 循环依赖
         */
        CIRCULAR_DEPENDENCY("循环依赖");
        
        private final String description;
        
        DependencyStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return name() + "(" + description + ")";
        }
    }
}