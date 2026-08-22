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


package com.zqzqq.bootkits.scripts.security;

/**
 * 脚本权限类型枚举
 * 定义了脚本执行的各种权限级别
 *
 * @author starBlues
 * @since 4.0.1
 */
public enum ScriptPermission {
    
    /**
     * 只读权限 - 脚本只能读取文件，不能修改或执行
     */
    READ_ONLY("READ_ONLY", "只读权限"),
    
    /**
     * 基本执行权限 - 脚本可以执行基本操作，但有限制
     */
    BASIC_EXECUTION("BASIC_EXECUTION", "基本执行权限"),
    
    /**
     * 标准执行权限 - 脚本可以执行大多数操作
     */
    STANDARD_EXECUTION("STANDARD_EXECUTION", "标准执行权限"),
    
    /**
     * 高级执行权限 - 脚本可以执行高级操作，如网络访问
     */
    ADVANCED_EXECUTION("ADVANCED_EXECUTION", "高级执行权限"),
    
    /**
     * 完整权限 - 脚本拥有所有权限，包括系统级操作
     */
    FULL_ACCESS("FULL_ACCESS", "完整权限"),
    
    /**
     * 管理员权限 - 脚本可以执行管理员级别的操作
     */
    ADMINISTRATOR("ADMINISTRATOR", "管理员权限"),
    
    /**
     * 自定义权限 - 根据特定规则定义的权限
     */
    CUSTOM("CUSTOM", "自定义权限");
    
    private final String permission;
    private final String description;
    
    ScriptPermission(String permission, String description) {
        this.permission = permission;
        this.description = description;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据权限名称获取权限枚举
     *
     * @param permission 权限名称
     * @return 权限枚举
     */
    public static ScriptPermission fromPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return READ_ONLY;
        }
        
        for (ScriptPermission p : values()) {
            if (p.permission.equalsIgnoreCase(permission.trim())) {
                return p;
            }
        }
        return READ_ONLY;
    }
}