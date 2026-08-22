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



package com.zqzqq.bootkits.core.security;

import java.util.Objects;

/**
 * 插件权限定义
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginPermission {

    private final PluginPermissionType type;
    private final String target;
    private final String action;
    private final String description;

    public PluginPermission(PluginPermissionType type, String target, String action) {
        this(type, target, action, null);
    }

    public PluginPermission(PluginPermissionType type, String target, String action, String description) {
        this.type = Objects.requireNonNull(type, "权限类型不能为空");
        this.target = target;
        this.action = action;
        this.description = description;
    }

    /**
     * 创建文件系统权限
     *
     * @param path 文件路径
     * @param action 操作类型 (read, write, delete, execute)
     * @return 文件系统权限
     */
    public static PluginPermission fileSystem(String path, String action) {
        return new PluginPermission(PluginPermissionType.FILE_SYSTEM, path, action, 
                "文件系统权限: " + action + " " + path);
    }

    /**
     * 创建网络权限
     *
     * @param host 主机地址
     * @param action 操作类型 (connect, listen, resolve)
     * @return 网络权限
     */
    public static PluginPermission network(String host, String action) {
        return new PluginPermission(PluginPermissionType.NETWORK, host, action,
                "网络权限: " + action + " " + host);
    }

    /**
     * 创建系统属性权限
     *
     * @param property 属性名
     * @param action 操作类型 (read, write)
     * @return 系统属性权限
     */
    public static PluginPermission systemProperty(String property, String action) {
        return new PluginPermission(PluginPermissionType.SYSTEM_PROPERTY, property, action,
                "系统属性权限: " + action + " " + property);
    }

    /**
     * 创建反射权限
     *
     * @param className 类名
     * @param action 操作类型 (access, modify)
     * @return 反射权限
     */
    public static PluginPermission reflection(String className, String action) {
        return new PluginPermission(PluginPermissionType.REFLECTION, className, action,
                "反射权限: " + action + " " + className);
    }

    /**
     * 创建运行时权限
     *
     * @param name 权限名称
     * @return 运行时权限
     */
    public static PluginPermission runtime(String name) {
        return new PluginPermission(PluginPermissionType.RUNTIME, name, "execute",
                "运行时权限: " + name);
    }

    /**
     * 创建数据库权限
     *
     * @param database 数据库名
     * @param action 操作类型 (read, write, admin)
     * @return 数据库权限
     */
    public static PluginPermission database(String database, String action) {
        return new PluginPermission(PluginPermissionType.DATABASE, database, action,
                "数据库权限: " + action + " " + database);
    }

    /**
     * 创建全部权限
     *
     * @return 全部权限
     */
    public static PluginPermission all() {
        return new PluginPermission(PluginPermissionType.ALL, "*", "*", "全部权限");
    }

    /**
     * 检查权限是否匹配
     *
     * @param other 其他权限
     * @return 是否匹配
     */
    public boolean matches(PluginPermission other) {
        if (this.type == PluginPermissionType.ALL) {
            return true;
        }
        
        if (!this.type.equals(other.type)) {
            return false;
        }
        
        return matchesTarget(other.target) && matchesAction(other.action);
    }

    private boolean matchesTarget(String otherTarget) {
        if ("*".equals(this.target)) {
            return true;
        }
        return Objects.equals(this.target, otherTarget);
    }

    private boolean matchesAction(String otherAction) {
        if ("*".equals(this.action)) {
            return true;
        }
        return Objects.equals(this.action, otherAction);
    }

    public PluginPermissionType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginPermission that = (PluginPermission) o;
        return type == that.type &&
                Objects.equals(target, that.target) &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, target, action);
    }

    @Override
    public String toString() {
        return String.format("PluginPermission{type=%s, target='%s', action='%s'}", 
                type, target, action);
    }
}