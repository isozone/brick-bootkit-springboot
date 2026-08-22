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


package com.zqzqq.bootkits.core.plugin;

/**
 * 插件接口
 * 所有插件都必须实现此接口
 */
public interface Plugin {

    /**
     * 获取插件ID
     */
    String getId();

    /**
     * 获取插件名称
     */
    String getName();

    /**
     * 获取插件版本
     */
    String getVersion();

    /**
     * 获取插件描述
     */
    String getDescription();

    /**
     * 启动插件
     */
    void start() throws Exception;

    /**
     * 停止插件
     */
    void stop() throws Exception;

    /**
     * 卸载插件
     */
    void uninstall() throws Exception;

    /**
     * 检查插件是否正在运行
     */
    boolean isRunning();
    
    /**
     * 获取插件的类加载器
     */
    ClassLoader getClassLoader();
}