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
 * 插件依赖信息
 * 定义插件的依赖关系、版本约束和冲突信息
 */
public class PluginDependency {
    
    private final String pluginId;
    private final String version;
    private final Set<String> requiredDependencies;
    private final Set<String> optionalDependencies;
    private final Set<String> conflicts;
    private final Map<String, VersionConstraint> versionConstraints;
    private final PluginDependencyType dependencyType;
    private final String description;
    private final boolean deprecated;
    private final String deprecationMessage;
    private final Map<String, Object> metadata;
    
    public PluginDependency(Builder builder) {
        this.pluginId = builder.pluginId;
        this.version = builder.version;
        this.requiredDependencies = new HashSet<>(builder.requiredDependencies);
        this.optionalDependencies = new HashSet<>(builder.optionalDependencies);
        this.conflicts = new HashSet<>(builder.conflicts);
        this.versionConstraints = new HashMap<>(builder.versionConstraints);
        this.dependencyType = builder.dependencyType;
        this.description = builder.description;
        this.deprecated = builder.deprecated;
        this.deprecationMessage = builder.deprecationMessage;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    /**
     * 创建构建器
     */
    public static Builder newBuilder(String pluginId, String version) {
        return new Builder(pluginId, version);
    }
    
    /**
     * 创建构建器（简化版本）
     */
    public static Builder newBuilder(String pluginId) {
        return new Builder(pluginId, "1.0.0");
    }
    
    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 获取版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取必需依赖列表
     */
    public Set<String> getRequiredDependencies() {
        return new HashSet<>(requiredDependencies);
    }
    
    /**
     * 获取可选依赖列表
     */
    public Set<String> getOptionalDependencies() {
        return new HashSet<>(optionalDependencies);
    }
    
    /**
     * 获取冲突列表
     */
    public Set<String> getConflicts() {
        return new HashSet<>(conflicts);
    }
    
    /**
     * 获取版本约束
     */
    public Map<String, VersionConstraint> getVersionConstraints() {
        return new HashMap<>(versionConstraints);
    }
    
    /**
     * 获取依赖类型
     */
    public PluginDependencyType getDependencyType() {
        return dependencyType;
    }
    
    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
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
     * 获取元数据
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 获取元数据项
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 检查是否依赖指定插件
     */
    public boolean hasDependency(String pluginId) {
        return requiredDependencies.contains(pluginId) || 
               optionalDependencies.contains(pluginId);
    }
    
    /**
     * 检查是否为冲突插件
     */
    public boolean hasConflict(String pluginId) {
        return conflicts.contains(pluginId);
    }
    
    /**
     * 获取依赖总数
     */
    public int getDependencyCount() {
        return requiredDependencies.size() + optionalDependencies.size();
    }
    
    /**
     * 获取必需依赖总数
     */
    public int getRequiredDependencyCount() {
        return requiredDependencies.size();
    }
    
    /**
     * 获取可选依赖总数
     */
    public int getOptionalDependencyCount() {
        return optionalDependencies.size();
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginDependency{pluginId='").append(pluginId)
          .append("', version='").append(version).append("'");
        
        if (!requiredDependencies.isEmpty()) {
            sb.append(", required=").append(requiredDependencies);
        }
        
        if (!optionalDependencies.isEmpty()) {
            sb.append(", optional=").append(optionalDependencies);
        }
        
        if (!conflicts.isEmpty()) {
            sb.append(", conflicts=").append(conflicts);
        }
        
        if (!versionConstraints.isEmpty()) {
            sb.append(", constraints=").append(versionConstraints);
        }
        
        sb.append(", type=").append(dependencyType);
        
        if (deprecated) {
            sb.append(", deprecated=true");
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 插件依赖构建器
     */
    public static class Builder {
        private final String pluginId;
        private final String version;
        private final Set<String> requiredDependencies = new HashSet<>();
        private final Set<String> optionalDependencies = new HashSet<>();
        private final Set<String> conflicts = new HashSet<>();
        private final Map<String, VersionConstraint> versionConstraints = new HashMap<>();
        private PluginDependencyType dependencyType = PluginDependencyType.INTERNAL;
        private String description;
        private boolean deprecated = false;
        private String deprecationMessage;
        private final Map<String, Object> metadata = new HashMap<>();
        
        private Builder(String pluginId, String version) {
            if (pluginId == null || pluginId.trim().isEmpty()) {
                throw new IllegalArgumentException("插件ID不能为空");
            }
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("插件版本不能为空");
            }
            
            this.pluginId = pluginId;
            this.version = version;
        }
        
        /**
         * 添加必需依赖
         */
        public Builder addRequiredDependency(String pluginId) {
            if (pluginId != null && !pluginId.trim().isEmpty()) {
                requiredDependencies.add(pluginId);
            }
            return this;
        }
        
        /**
         * 添加多个必需依赖
         */
        public Builder addRequiredDependencies(String... pluginIds) {
            for (String pluginId : pluginIds) {
                addRequiredDependency(pluginId);
            }
            return this;
        }
        
        /**
         * 添加可选依赖
         */
        public Builder addOptionalDependency(String pluginId) {
            if (pluginId != null && !pluginId.trim().isEmpty()) {
                optionalDependencies.add(pluginId);
            }
            return this;
        }
        
        /**
         * 添加多个可选依赖
         */
        public Builder addOptionalDependencies(String... pluginIds) {
            for (String pluginId : pluginIds) {
                addOptionalDependency(pluginId);
            }
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
         * 添加多个冲突插件
         */
        public Builder addConflicts(String... pluginIds) {
            for (String pluginId : pluginIds) {
                addConflict(pluginId);
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
         * 设置依赖类型
         */
        public Builder setDependencyType(PluginDependencyType type) {
            this.dependencyType = type;
            return this;
        }
        
        /**
         * 设置描述
         */
        public Builder setDescription(String description) {
            this.description = description;
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
         * 添加元数据
         */
        public Builder addMetadata(String key, Object value) {
            if (key != null) {
                metadata.put(key, value);
            }
            return this;
        }
        
        /**
         * 构建PluginDependency实例
         */
        public PluginDependency build() {
            return new PluginDependency(this);
        }
    }
}