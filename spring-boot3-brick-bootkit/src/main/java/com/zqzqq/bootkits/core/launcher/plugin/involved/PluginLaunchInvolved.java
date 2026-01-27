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

package com.zqzqq.bootkits.core.launcher.plugin.involved;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.utils.OrderPriority;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 插件启动前后参与
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public interface PluginLaunchInvolved {

    /**
     * 初始化。仅调用一次
     * @param applicationContext 主程序上下文GenericApplicationContext
     * @param configuration 集成配置
     */
    default void initialize(GenericApplicationContext applicationContext, IntegrationConfiguration configuration){}

    /**
     * 启动之前
     * @param pluginInsideInfo 插件信息
     * @param classLoader 插件classloader
     * @throws Exception 执行异常
     */
    default void before(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception{}

    /**
     * 启动之后
     * @param pluginInsideInfo 插件信息
     * @param classLoader 插件classloader
     * @param pluginHook 鍚姩鎴愬姛鍚庢彃浠惰繑鍥炵殑閽╁瓙
     * @throws Exception 执行异常
     */
    default void after(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader,
                       SpringPluginHook pluginHook) throws Exception{}

    /**
     * 启动失败
     * @param pluginInsideInfo 插件信息
     * @param classLoader 插件classloader
     * @param throwable 异常信息
     * @throws Exception 执行异常
     */
    default void failure(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, Throwable throwable) throws Exception{}

    /**
     * 关闭时的值
     * @param pluginInsideInfo 插件信息
     * @param classLoader 插件classloader
     * @throws Exception 执行异常
     */
    default void close(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception{}

    /**
     * 执行顺序
     * @return OrderPriority
     */
    default OrderPriority order(){
        return OrderPriority.getMiddlePriority();
    }



}

