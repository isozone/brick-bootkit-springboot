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

/**
 * 插件权限类型枚举
 *
 * @author starBlues
 * @since 4.0.0
 */
public enum PluginPermissionType {

    /**
     * 文件系统权限
     */
    FILE_SYSTEM("file_system", "文件系统访问权限"),

    /**
     * 网络权限
     */
    NETWORK("network", "网络访问权限"),

    /**
     * 系统属性权限
     */
    SYSTEM_PROPERTY("system_property", "系统属性访问权限"),

    /**
     * 反射权限
     */
    REFLECTION("reflection", "反射访问权限"),

    /**
     * 运行时权限
     */
    RUNTIME("runtime", "运行时权限"),

    /**
     * 数据库权限
     */
    DATABASE("database", "数据库访问权限"),

    /**
     * JMX权限
     */
    JMX("jmx", "JMX管理权限"),

    /**
     * 安全权限
     */
    SECURITY("security", "安全管理权限"),

    /**
     * 线程权限
     */
    THREAD("thread", "线程管理权限"),

    /**
     * 类加载器权限
     */
    CLASSLOADER("classloader", "类加载器权限"),

    /**
     * 全部权限
     */
    ALL("all", "全部权限");

    private final String code;
    private final String description;

    PluginPermissionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取权限类型
     *
     * @param code 权限代码
     * @return 权限类型
     */
    public static PluginPermissionType fromCode(String code) {
        for (PluginPermissionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的权限类型代码: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}