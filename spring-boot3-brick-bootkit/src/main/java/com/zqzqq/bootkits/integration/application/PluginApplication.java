/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.integration.application;

import com.zqzqq.bootkits.integration.listener.PluginInitializerListener;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import com.zqzqq.bootkits.integration.user.PluginUser;
import org.springframework.context.ApplicationContext;

/**
 * 插件搴旂敤銆?
 * @author starBlues
 * @version 3.0.0
 */
public interface PluginApplication{

    /**
     * 鍒濆鍖?
     * @param applicationContext Spring上下?
     * @param listener 插件初始化监听器?
     */
    void initialize(ApplicationContext applicationContext, PluginInitializerListener listener);


    /**
     * 获取插件操作权?
     * @return 插件操作鑰?
     */
    PluginOperator getPluginOperator();

    /**
     * 获取插件操作权?
     * @return 插件操作鑰?
     */
    PluginUser getPluginUser();
}

