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


package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.state.EnhancedPluginState;

/**
 * 插件内部信息接口
 * 定义了插件内部管理所需的方法
 */
public interface PluginInsideInfo extends PluginInfo {

    /**
     * 获取插件类加载器
     *
     * @return 插件的类加载器
     */
    ClassLoader getClassLoader();

    /**
     * 设置插件类加载器
     *
     * @param classLoader 插件的类加载器
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * 设置插件状态
     *
     * @param state 新的插件状态
     */
    void setPluginState(EnhancedPluginState state);

    /**
     * 获取插件状态
     *
     * @return 当前插件状态
     */
    EnhancedPluginState getPluginState();

    /**
     * 检查插件是否跟随系统
     *
     * @return 如果跟随系统返回true，否则返回false
     */
    boolean isFollowSystem();

    /**
     * 设置插件是否跟随系统
     *
     * @param follow 是否跟随系统
     */
    void setFollowSystem(boolean follow);

}
