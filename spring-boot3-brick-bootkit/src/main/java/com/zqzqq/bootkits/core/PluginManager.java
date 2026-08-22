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

import com.zqzqq.bootkits.core.exception.PluginException;

import java.nio.file.Path;
import java.util.List;

/**
 * 插件管理器
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public interface PluginManager {

    /**
     * 得到插件root目录
     * @return List
     */
    List<String> getPluginsRoots();

    /**
     * 得到插件默认的根路径
     * @return String
     */
    String getDefaultPluginRoot();

    /**
     * 加载配置目录中所有插件
     * @return 加载的插件信息
     */
    List<PluginInfo> loadPlugins();

    /**
     * 安装插件
     * @param path 插件路径
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    PluginInfo install(Path path) throws PluginException;

    /**
     * 启动插件
     * @param pluginId 插件id
     * @throws PluginException 插件异常
     */
    void start(String pluginId) throws PluginException;

    /**
     * 停止插件
     * @param pluginId 插件id
     * @throws PluginException 插件异常
     */
    void stop(String pluginId) throws PluginException;

    /**
     * 卸载插件
     * @param pluginId 插件id
     * @throws PluginException 插件异常
     */
    void uninstall(String pluginId) throws PluginException;

    /**
     * 获取插件信息
     * @param pluginId 插件id
     * @return 插件信息
     */
    PluginInfo getPlugin(String pluginId);

    /**
     * 获取所有插件信息
     * @return 插件信息列表
     */
    List<PluginInfo> getPlugins();

    /**
     * 获取已启动的插件信息
     * @return 插件信息列表
     */
    List<PluginInfo> getStartedPlugins();

    /**
     * 获取已解析的插件信息
     * @return 插件信息列表
     */
    List<PluginInfo> getResolvedPlugins();

    /**
     * 重新加载插件
     * @param pluginId 插件id
     * @throws PluginException 插件异常
     */
    void reload(String pluginId) throws PluginException;

    /**
     * 验证插件
     * @param jarPath 插件jar路径
     * @return 验证结果
     * @throws PluginException 插件异常
     */
    boolean verify(Path jarPath) throws PluginException;

    /**
     * 解析插件
     * @param pluginPath 插件路径
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    PluginInfo parse(Path pluginPath) throws PluginException;

    /**
     * 关闭插件管理器
     */
    void close();
}