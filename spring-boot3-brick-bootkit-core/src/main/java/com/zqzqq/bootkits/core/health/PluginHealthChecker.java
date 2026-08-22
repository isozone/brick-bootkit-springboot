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


package com.zqzqq.bootkits.core.health;

import com.zqzqq.bootkits.core.plugin.Plugin;

/**
 * 插件健康检查器接口
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public interface PluginHealthChecker {
    
    /**
     * 执行插件健康检查
     * 
     * @param plugin 要检查的插件
     * @return 健康检查报告
     */
    PluginHealthReport checkHealth(Plugin plugin);
    
    /**
     * 执行快速健康检查（轻量级）
     * 
     * @param plugin 要检查的插件
     * @return 健康状态
     */
    PluginHealthStatus quickHealthCheck(Plugin plugin);
    
    /**
     * 获取检查器名称
     * 
     * @return 检查器名称
     */
    String getName();
    
    /**
     * 获取检查器描述
     * 
     * @return 检查器描述
     */
    String getDescription();
    
    /**
     * 检查器是否启用
     * 
     * @return 是否启用
     */
    boolean isEnabled();
    
    /**
     * 设置检查器启用状态
     * 
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);
    
    /**
     * 获取检查超时时间（毫秒）
     * 
     * @return 超时时间
     */
    long getTimeoutMillis();
    
    /**
     * 设置检查超时时间
     * 
     * @param timeoutMillis 超时时间
     */
    void setTimeoutMillis(long timeoutMillis);
}