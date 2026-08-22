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



package com.zqzqq.bootkits.integration.application;

import com.zqzqq.bootkits.integration.listener.PluginInitializerListener;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import com.zqzqq.bootkits.integration.user.PluginUser;
import org.springframework.context.ApplicationContext;

/**
 * 插件应用
 * @author starBlues
 * @version 3.0.0
 */
public interface PluginApplication{

    /**
     * 初始化
     * @param applicationContext Spring 上下文
     * @param listener 插件初始化监听器
     */
    void initialize(ApplicationContext applicationContext, PluginInitializerListener listener);


    /**
     * 获取插件操作器
     * @return 插件操作器
     */
    PluginOperator getPluginOperator();

    /**
     * 获取插件用户能力
     * @return 插件用户能力
     */
    PluginUser getPluginUser();
}

