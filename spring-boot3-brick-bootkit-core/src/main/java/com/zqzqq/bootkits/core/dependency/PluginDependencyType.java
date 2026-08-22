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

/**
 * 插件依赖类型枚举
 * 定义不同类型的插件依赖关系
 */
public enum PluginDependencyType {
    
    /**
     * 内部依赖 - 同一插件系统内的插件
     */
    INTERNAL("internal", "内部依赖"),
    
    /**
     * 外部依赖 - 来自外部系统的插件
     */
    EXTERNAL("external", "外部依赖"),
    
    /**
     * 系统依赖 - 系统级插件或服务
     */
    SYSTEM("system", "系统依赖"),
    
    /**
     * 运行时依赖 - 运行时必需的依赖
     */
    RUNTIME("runtime", "运行时依赖"),
    
    /**
     * 编译时依赖 - 编译时必需的依赖
     */
    COMPILE("compile", "编译时依赖"),
    
    /**
     * 可选依赖 - 插件功能扩展依赖
     */
    OPTIONAL("optional", "可选依赖"),
    
    /**
     * 测试依赖 - 仅在测试时需要的依赖
     */
    TEST("test", "测试依赖"),
    
    /**
     * 构建依赖 - 构建过程中需要的依赖
     */
    PROVIDED("provided", "构建依赖");
    
    private final String code;
    private final String description;
    
    PluginDependencyType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 获取依赖类型代码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取依赖类型描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取依赖类型
     */
    public static PluginDependencyType fromCode(String code) {
        for (PluginDependencyType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的依赖类型代码: " + code);
    }
    
    /**
     * 检查是否为必需依赖类型
     */
    public boolean isRequired() {
        return this == INTERNAL || this == EXTERNAL || this == SYSTEM || 
               this == RUNTIME || this == COMPILE || this == PROVIDED;
    }
    
    /**
     * 检查是否为可选依赖类型
     */
    public boolean isOptional() {
        return this == OPTIONAL || this == TEST;
    }
    
    /**
     * 检查是否为构建时依赖
     */
    public boolean isBuildTime() {
        return this == COMPILE || this == PROVIDED;
    }
    
    /**
     * 检查是否为运行时依赖
     */
    public boolean isRuntime() {
        return this == RUNTIME || this == INTERNAL || this == EXTERNAL || this == SYSTEM;
    }
    
    /**
     * 转换为字符串
     */
    @Override
    public String toString() {
        return name() + "(" + code + " - " + description + ")";
    }
}