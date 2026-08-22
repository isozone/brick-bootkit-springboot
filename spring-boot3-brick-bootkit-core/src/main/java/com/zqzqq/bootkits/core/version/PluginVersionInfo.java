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


package com.zqzqq.bootkits.core.version;

import com.zqzqq.bootkits.core.dependency.VersionConstraint;
import java.util.*;

/**
 * 插件版本信息
 * 包含插件的版本信息、依赖关系、约束条件等
 */
public class PluginVersionInfo {
    
    private final String pluginId;
    private final VersionUtils.VersionInfo version;
    private final List<String> requiredPlugins;
    private final List<String> optionalPlugins;
    private final Map<String, VersionConstraint> versionConstraints;
    private final Map<String, String> metadata;
    private final String minimumFrameworkVersion;
    private final String maximumFrameworkVersion;
    private final String targetFrameworkVersion;
    private final List<String> conflicts;
    private final boolean deprecated;
    private final String deprecationMessage;
    
    public PluginVersionInfo(Builder builder) {
        this.pluginId = builder.pluginId;
        this.version = builder.version;
        this.requiredPlugins = new ArrayList<>(builder.requiredPlugins);
        this.optionalPlugins = new ArrayList<>(builder.optionalPlugins);
        this.versionConstraints = new HashMap<>(builder.versionConstraints);
        this.metadata = new HashMap<>(builder.metadata);
        this.minimumFrameworkVersion = builder.minimumFrameworkVersion;
        this.maximumFrameworkVersion = builder.maximumFrameworkVersion;
        this.targetFrameworkVersion = builder.targetFrameworkVersion;
        this.conflicts = new ArrayList<>(builder.conflicts);
        this.deprecated = builder.deprecated;
        this.deprecationMessage = builder.deprecationMessage;
    }
    
    /**
     * 创建构建器
     */
    public static Builder newBuilder(String pluginId, String version) {
        return new Builder(pluginId, version);
    }
    
    /**
     * 验证版本兼容性
     */
    public VersionCompatibilityResult validateCompatibility(PluginVersionInfo otherInfo) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 检查框架版本兼容性
        if (minimumFrameworkVersion != null) {
            if (targetFrameworkVersion != null && 
                !VersionUtils.isCompatible(targetFrameworkVersion, minimumFrameworkVersion)) {
                issues.add("框架版本要求冲突：当前要求 " + minimumFrameworkVersion + "，但目标版本 " + targetFrameworkVersion);
            }
        }
        
        if (maximumFrameworkVersion != null && targetFrameworkVersion != null) {
            if (!VersionUtils.isCompatible(targetFrameworkVersion, maximumFrameworkVersion)) {
                issues.add("框架版本超出上限：当前上限 " + maximumFrameworkVersion + "，但目标版本 " + targetFrameworkVersion);
            }
        }
        
        // 检查依赖冲突
        for (String requiredPlugin : requiredPlugins) {
            if (!otherInfo.pluginId.equals(requiredPlugin)) {
                PluginVersionInfo requiredInfo = getPluginVersionInfo(requiredPlugin);
                if (requiredInfo == null) {
                    issues.add("缺少必需的插件依赖：" + requiredPlugin);
                } else {
                    VersionCompatibilityResult depResult = otherInfo.checkDependencyCompatibility(requiredPlugin, requiredInfo);
                    issues.addAll(depResult.getIssues());
                    warnings.addAll(depResult.getWarnings());
                }
            }
        }
        
        // 检查版本约束
        for (Map.Entry<String, VersionConstraint> entry : versionConstraints.entrySet()) {
            String targetPluginId = entry.getKey();
            VersionConstraint constraint = entry.getValue();
            
            PluginVersionInfo targetInfo = getPluginVersionInfo(targetPluginId);
            if (targetInfo != null) {
                if (!constraint.isSatisfied(targetInfo.getVersion().toString())) {
                    issues.add("版本约束不满足：插件 " + targetPluginId + " 的版本 " + 
                              targetInfo.getVersion() + " 不满足约束 " + constraint);
                }
            }
        }
        
        // 检查冲突
        for (String conflictId : conflicts) {
            if (conflictId.equals(otherInfo.pluginId)) {
                issues.add("插件冲突：与 " + otherInfo.pluginId + " 存在版本冲突");
            }
        }
        
        return new VersionCompatibilityResult(issues, warnings);
    }
    
    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 获取版本信息
     */
    public VersionUtils.VersionInfo getVersion() {
        return version;
    }
    
    /**
     * 获取必需依赖列表
     */
    public List<String> getRequiredPlugins() {
        return new ArrayList<>(requiredPlugins);
    }
    
    /**
     * 获取可选依赖列表
     */
    public List<String> getOptionalPlugins() {
        return new ArrayList<>(optionalPlugins);
    }
    
    /**
     * 获取版本约束
     */
    public Map<String, VersionConstraint> getVersionConstraints() {
        return new HashMap<>(versionConstraints);
    }
    
    /**
     * 获取元数据
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 获取最小框架版本
     */
    public String getMinimumFrameworkVersion() {
        return minimumFrameworkVersion;
    }
    
    /**
     * 获取最大框架版本
     */
    public String getMaximumFrameworkVersion() {
        return maximumFrameworkVersion;
    }
    
    /**
     * 获取目标框架版本
     */
    public String getTargetFrameworkVersion() {
        return targetFrameworkVersion;
    }
    
    /**
     * 获取冲突列表
     */
    public List<String> getConflicts() {
        return new ArrayList<>(conflicts);
    }
    
    /**
     * 是否已弃用
     */
    public boolean isDeprecated() {
        return deprecated;
    }
    
    /**
     * 获取弃用消息
     */
    public String getDeprecationMessage() {
        return deprecationMessage;
    }
    
    /**
     * 检查依赖兼容性
     */
    private VersionCompatibilityResult checkDependencyCompatibility(String dependencyId, PluginVersionInfo dependencyInfo) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        VersionConstraint constraint = versionConstraints.get(dependencyId);
        if (constraint != null && !constraint.isSatisfied(dependencyInfo.getVersion().toString())) {
            issues.add("依赖版本约束不满足：插件 " + dependencyId + " 的版本 " + 
                      dependencyInfo.getVersion() + " 不满足约束 " + constraint);
        }
        
        // 检查依赖的弃用状态
        if (dependencyInfo.isDeprecated()) {
            warnings.add("依赖插件已弃用：" + dependencyId + " - " + dependencyInfo.getDeprecationMessage());
        }
        
        return new VersionCompatibilityResult(issues, warnings);
    }
    
    /**
     * 获取插件版本信息（模拟方法，实际应该从插件仓库获取）
     */
    private PluginVersionInfo getPluginVersionInfo(String pluginId) {
        // 这里应该从插件仓库或配置中获取
        // 为了演示，返回null，实际使用时需要实现真实的获取逻辑
        return null;
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginVersionInfo{pluginId='").append(pluginId)
          .append("', version='").append(version).append("'");
        
        if (!requiredPlugins.isEmpty()) {
            sb.append(", requiredPlugins=").append(requiredPlugins);
        }
        
        if (!optionalPlugins.isEmpty()) {
            sb.append(", optionalPlugins=").append(optionalPlugins);
        }
        
        if (!versionConstraints.isEmpty()) {
            sb.append(", versionConstraints=").append(versionConstraints);
        }
        
        if (deprecated) {
            sb.append(", deprecated=true");
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 插件版本构建器
     */
    public static class Builder {
        private final String pluginId;
        private final VersionUtils.VersionInfo version;
        private final List<String> requiredPlugins = new ArrayList<>();
        private final List<String> optionalPlugins = new ArrayList<>();
        private final Map<String, VersionConstraint> versionConstraints = new HashMap<>();
        private final Map<String, String> metadata = new HashMap<>();
        private String minimumFrameworkVersion;
        private String maximumFrameworkVersion;
        private String targetFrameworkVersion;
        private final List<String> conflicts = new ArrayList<>();
        private boolean deprecated = false;
        private String deprecationMessage;
        
        private Builder(String pluginId, String version) {
            if (pluginId == null || pluginId.trim().isEmpty()) {
                throw new IllegalArgumentException("插件ID不能为空");
            }
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("插件版本不能为空");
            }
            
            this.pluginId = pluginId;
            this.version = VersionUtils.parseVersion(version);
        }
        
        /**
         * 添加必需依赖
         */
        public Builder addRequiredPlugin(String pluginId) {
            if (pluginId != null && !pluginId.trim().isEmpty()) {
                requiredPlugins.add(pluginId);
            }
            return this;
        }
        
        /**
         * 添加可选依赖
         */
        public Builder addOptionalPlugin(String pluginId) {
            if (pluginId != null && !pluginId.trim().isEmpty()) {
                optionalPlugins.add(pluginId);
            }
            return this;
        }
        
        /**
         * 添加版本约束
         */
        public Builder addVersionConstraint(String pluginId, VersionConstraint constraint) {
            if (pluginId != null && constraint != null) {
                versionConstraints.put(pluginId, constraint);
            }
            return this;
        }
        
        /**
         * 添加元数据
         */
        public Builder addMetadata(String key, String value) {
            if (key != null && value != null) {
                metadata.put(key, value);
            }
            return this;
        }
        
        /**
         * 设置最小框架版本
         */
        public Builder setMinimumFrameworkVersion(String version) {
            this.minimumFrameworkVersion = version;
            return this;
        }
        
        /**
         * 设置最大框架版本
         */
        public Builder setMaximumFrameworkVersion(String version) {
            this.maximumFrameworkVersion = version;
            return this;
        }
        
        /**
         * 设置目标框架版本
         */
        public Builder setTargetFrameworkVersion(String version) {
            this.targetFrameworkVersion = version;
            return this;
        }
        
        /**
         * 添加冲突插件
         */
        public Builder addConflict(String pluginId) {
            if (pluginId != null && !pluginId.trim().isEmpty()) {
                conflicts.add(pluginId);
            }
            return this;
        }
        
        /**
         * 标记为已弃用
         */
        public Builder setDeprecated(String deprecationMessage) {
            this.deprecated = true;
            this.deprecationMessage = deprecationMessage;
            return this;
        }
        
        /**
         * 构建PluginVersionInfo实例
         */
        public PluginVersionInfo build() {
            return new PluginVersionInfo(this);
        }
    }
    
    /**
     * 版本兼容性结果
     */
    public static class VersionCompatibilityResult {
        private final List<String> issues;
        private final List<String> warnings;
        
        public VersionCompatibilityResult(List<String> issues, List<String> warnings) {
            this.issues = new ArrayList<>(issues);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public boolean isCompatible() {
            return issues.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("VersionCompatibilityResult{");
            
            if (!issues.isEmpty()) {
                sb.append("issues=").append(issues);
            }
            
            if (!warnings.isEmpty()) {
                if (!issues.isEmpty()) {
                    sb.append(", ");
                }
                sb.append("warnings=").append(warnings);
            }
            
            sb.append(", compatible=").append(isCompatible());
            sb.append('}');
            return sb.toString();
        }
    }
}
