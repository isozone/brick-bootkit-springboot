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
 * 插件兼容性检查结果
 * 用于检查插件之间的兼容性
 */
public class PluginCompatibilityResult {
    
    private boolean compatible;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> conflicts;
    private final List<String> missingDependencies;
    private final Map<String, CompatibilityIssue> issueMap;
    
    public PluginCompatibilityResult(boolean compatible, List<String> errors) {
        this.compatible = compatible;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>();
        this.conflicts = new ArrayList<>();
        this.missingDependencies = new ArrayList<>();
        this.issueMap = new HashMap<>();
    }
    
    public PluginCompatibilityResult(boolean compatible, List<String> errors, 
                                   List<String> warnings) {
        this.compatible = compatible;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.conflicts = new ArrayList<>();
        this.missingDependencies = new ArrayList<>();
        this.issueMap = new HashMap<>();
    }
    
    /**
     * 创建兼容的结果
     */
    public static PluginCompatibilityResult compatible() {
        return new PluginCompatibilityResult(true, new ArrayList<>());
    }
    
    /**
     * 创建不兼容的结果
     */
    public static PluginCompatibilityResult incompatible(List<String> errors) {
        return new PluginCompatibilityResult(false, new ArrayList<>(errors));
    }
    
    /**
     * 创建部分兼容的结果（有警告）
     */
    public static PluginCompatibilityResult compatibleWithWarnings(List<String> warnings) {
        return new PluginCompatibilityResult(true, new ArrayList<>(), 
                                            new ArrayList<>(warnings));
    }
    
    /**
     * 是否兼容
     */
    public boolean isCompatible() {
        return compatible;
    }
    
    /**
     * 是否不兼容
     */
    public boolean isIncompatible() {
        return !compatible;
    }
    
    /**
     * 是否有警告
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
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
     * 获取冲突列表
     */
    public List<String> getConflicts() {
        return new ArrayList<>(conflicts);
    }
    
    /**
     * 获取缺失依赖列表
     */
    public List<String> getMissingDependencies() {
        return new ArrayList<>(missingDependencies);
    }
    
    /**
     * 添加错误信息
     */
    public void addError(String error) {
        if (error != null) {
            errors.add(error);
            compatible = false;
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
     * 添加冲突信息
     */
    public void addConflict(String conflict) {
        if (conflict != null) {
            conflicts.add(conflict);
            errors.add("冲突: " + conflict);
            compatible = false;
        }
    }
    
    /**
     * 添加缺失依赖信息
     */
    public void addMissingDependency(String dependency) {
        if (dependency != null) {
            missingDependencies.add(dependency);
            errors.add("缺失依赖: " + dependency);
            compatible = false;
        }
    }
    
    /**
     * 添加兼容性问题
     */
    public void addIssue(String pluginId, CompatibilityIssue issue) {
        if (pluginId != null && issue != null) {
            issueMap.put(pluginId, issue);
            
            switch (issue.getSeverity()) {
                case ERROR:
                    errors.add("插件 " + pluginId + ": " + issue.getDescription());
                    compatible = false;
                    break;
                case WARNING:
                    warnings.add("插件 " + pluginId + ": " + issue.getDescription());
                    break;
            }
        }
    }
    
    /**
     * 获取兼容性问题
     */
    public CompatibilityIssue getIssue(String pluginId) {
        return issueMap.get(pluginId);
    }
    
    /**
     * 获取所有兼容性问题
     */
    public Map<String, CompatibilityIssue> getAllIssues() {
        return new HashMap<>(issueMap);
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
     * 获取冲突总数
     */
    public int getConflictCount() {
        return conflicts.size();
    }
    
    /**
     * 获取缺失依赖总数
     */
    public int getMissingDependencyCount() {
        return missingDependencies.size();
    }
    
    /**
     * 获取总问题数
     */
    public int getTotalIssueCount() {
        return errors.size() + warnings.size();
    }
    
    /**
     * 是否有指定类型的兼容性问题
     */
    public boolean hasIssue(String pluginId) {
        return issueMap.containsKey(pluginId);
    }
    
    /**
     * 检查是否包含特定类型的错误
     */
    public boolean hasConflictErrors() {
        return conflicts.stream().anyMatch(error -> 
            error.toLowerCase().contains("冲突") || error.toLowerCase().contains("conflict"));
    }
    
    /**
     * 检查是否包含缺失依赖错误
     */
    public boolean hasMissingDependencyErrors() {
        return missingDependencies.stream().anyMatch(dep -> 
            dep.toLowerCase().contains("缺失") || dep.toLowerCase().contains("missing"));
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginCompatibilityResult{");
        sb.append("compatible=").append(compatible);
        
        if (!errors.isEmpty()) {
            sb.append(", errors=").append(errors);
        }
        
        if (!warnings.isEmpty()) {
            sb.append(", warnings=").append(warnings);
        }
        
        if (!conflicts.isEmpty()) {
            sb.append(", conflicts=").append(conflicts);
        }
        
        if (!missingDependencies.isEmpty()) {
            sb.append(", missingDependencies=").append(missingDependencies);
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 兼容性问题
     */
    public static class CompatibilityIssue {
        private final String pluginId;
        private final String description;
        private final IssueSeverity severity;
        private final String suggestion;
        
        public CompatibilityIssue(String pluginId, String description, 
                                IssueSeverity severity, String suggestion) {
            this.pluginId = pluginId;
            this.description = description;
            this.severity = severity;
            this.suggestion = suggestion;
        }
        
        public CompatibilityIssue(String pluginId, String description, IssueSeverity severity) {
            this(pluginId, description, severity, null);
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public String getDescription() {
            return description;
        }
        
        public IssueSeverity getSeverity() {
            return severity;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(pluginId).append(": ").append(description);
            if (suggestion != null) {
                sb.append(" (建议: ").append(suggestion).append(")");
            }
            return sb.toString();
        }
    }
    
    /**
     * 问题严重程度
     */
    public enum IssueSeverity {
        /**
         * 错误 - 阻止兼容
         */
        ERROR("错误"),
        
        /**
         * 警告 - 提示性信息
         */
        WARNING("警告"),
        
        /**
         * 信息 - 一般性提示
         */
        INFO("信息");
        
        private final String description;
        
        IssueSeverity(String description) {
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