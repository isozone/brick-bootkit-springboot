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



package com.zqzqq.bootkits.core.launcher.plugin;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.extract.OpExtractFactory;
import com.zqzqq.bootkits.spring.invoke.InvokeSupperCache;

/**
 * 插件交互接口
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface PluginInteractive {

    /**
     * 获取插件信息
     * @return PluginDescriptor
     */
    InsidePluginDescriptor getPluginDescriptor();

    /**
     * 获取插件内部信息
     * @return PluginInsideInfo
     */
    PluginInsideInfo getPluginInsideInfo();

    /**
     * 获取主程序包的MainApplicationContext
     * @return MainApplicationContext
     */
    MainApplicationContext getMainApplicationContext();

    /**
     * 获取主程序包框架集成配置信息
     * @return IntegrationConfiguration
     */
    IntegrationConfiguration getConfiguration();

    /**
     * 获取远程调用缓存
     * @return InvokeSupperCache
     */
    InvokeSupperCache getInvokeSupperCache();

    /**
     * 获取业务扩展功能的工厂
     * @return OpExtractFactory
     */
    OpExtractFactory getOpExtractFactory();

}

