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

package com.zqzqq.bootkits.spring;


import com.zqzqq.bootkits.core.PluginCloseType;
import com.zqzqq.bootkits.core.exception.PluginProhibitStopException;
import com.zqzqq.bootkits.spring.web.thymeleaf.ThymeleafConfig;

/**
 * 插件钩子接口
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public interface SpringPluginHook {

    /**
     * 停止前校验。如果抛出 PluginProhibitStopException 异常, 表示当前插件不可停止
     * @throws PluginProhibitStopException 插件禁止停止
     */
    void stopVerify() throws PluginProhibitStopException;

    /**
     * 返回插件 ApplicationContext
     * @return ApplicationContext
     */
    ApplicationContext getApplicationContext();

    /**
     * 得到插件中 web 的配置
     * @return WebConfig
     */
    WebConfig getWebConfig();

    /**
     * 获取插件中 Thymeleaf 的配置
     * @return ThymeleafConfig
     */
    ThymeleafConfig getThymeleafConfig();

    /**
     * 关闭调用
     * @param closeType 关闭类型
     * @since 3.1.0
     * @throws Exception 关闭异常
     */
    void close(PluginCloseType closeType) throws Exception;

}

