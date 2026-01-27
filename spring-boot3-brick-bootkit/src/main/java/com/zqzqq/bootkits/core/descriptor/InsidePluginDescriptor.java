/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.descriptor;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * 内部的PluginDescriptor
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface InsidePluginDescriptor extends PluginDescriptor {

    /**
     * 获取需要排除的自动配置
     * @return 排除的自动配置类集合
     */
    Set<String> getExcludeAutoConfigurations();

    /**
     * 设置需要排除的自动配置
     * @param excludeClasses 排除的自动配置类集合
     */
    void setExcludeAutoConfigurations(Set<String> excludeClasses);

    /**
     * 得到插件的Properties 配置
     * @return Properties
     */
    Properties getProperties();

    /**
     * 获取插件配置文件名称
     * 与getConfigFileLocation 配置二选一, 如果都有则优先使用 getConfigFileName
     * @return String
     */
    String getConfigFileName();

    /**
     * 获取插件配置文件路径
     * 与getConfigFileName 配置二选一, 如果都有则优先使用 getConfigFileName
     * @return String
     */
    String getConfigFileLocation();

    /**
     * 得到插件启动时参数
     * @return String
     */
    String getArgs();

    /**
     * 得到内部的插件路径
     * @return Path
     */
    Path getInsidePluginPath();

    /**
     * 获取插件文件名称
     * @return String
     */
    String getPluginFileName();


    /**
     * 获取插件classes path路径
     * @return Path
     */
    String getPluginClassPath();

    /**
     * 获取插件依赖配置的目标
     * @return String
     */
    String getPluginLibDir();

    /**
     * 获取插件依赖的路径
     * @return String
     */
    Set<PluginLibInfo> getPluginLibInfo();

    /**
     * 设置当前插件包含主程序包加载资源的匹配
     * @return Set
     */
    Set<String> getIncludeMainResourcePatterns();

    /**
     * 设置当前插件排除从主程序加载资源的匹配
     * @return Set
     */
    Set<String> getExcludeMainResourcePatterns();

    /**
     * 转换为PluginDescriptor
     * @return PluginDescriptor
     */
    PluginDescriptor toPluginDescriptor();

}

