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



package com.zqzqq.bootkits.core.lifecycle;

/**
 * 插件生命周期状态枚举
 *
 * @author starBlues
 * @since 4.0.0
 */
public enum PluginLifecycleState {

    /**
     * 未安装
     */
    UNINSTALLED("未安装"),

    /**
     * 已安装
     */
    INSTALLED("已安装"),

    /**
     * 启动中
     */
    STARTING("启动中"),

    /**
     * 已启动
     */
    STARTED("已启动"),

    /**
     * 停止中
     */
    STOPPING("停止中"),

    /**
     * 已停止
     */
    STOPPED("已停止"),

    /**
     * 错误状态
     */
    ERROR("错误状态"),

    /**
     * 禁用状态
     */
    DISABLED("禁用状态");

    private final String description;

    PluginLifecycleState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否为活跃状态
     */
    public boolean isActive() {
        return this == STARTED;
    }

    /**
     * 是否为终止状态
     */
    public boolean isTerminated() {
        return this == UNINSTALLED || this == ERROR;
    }

    /**
     * 是否可以启动
     */
    public boolean canStart() {
        return this == INSTALLED || this == STOPPED;
    }

    /**
     * 是否可以停止
     */
    public boolean canStop() {
        return this == STARTED;
    }

    /**
     * 是否可以卸载
     */
    public boolean canUninstall() {
        return this == INSTALLED || this == STOPPED || this == ERROR;
    }
}