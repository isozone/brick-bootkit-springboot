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

package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.SpringBeanFactory;
import lombok.Getter;
import org.springframework.core.io.ResourceLoader;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供插件上下文的工具类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public abstract class PluginContextHolder {


    @Getter
    private static ProcessorContext processorContext;

    @Getter
    private static ConcurrentHashMap<String, ProcessorContext> processorContextMap = new ConcurrentHashMap<>();

    /**
     * 初始化 ProcessorContext
     *
     * @param processorContext 处理器上下文
     */
    public static void initialize(ProcessorContext processorContext) {
        PluginContextHolder.processorContext = processorContext;
    }

    /**
     * 获取插件(classloader
     *
     * @return ClassLoader
     */
    public static ClassLoader getClassLoader() {
        return processorContext.getClassLoader();
    }

    /**
     * 获取插件 ApplicationContext
     *
     * @return GenericApplicationContext
     */
    public static org.springframework.context.ApplicationContext getApplicationContext() {
        return processorContext.getApplicationContext();
    }

    /**
     * 获取插件 SpringBeanFactory
     *
     * @return SpringBeanFactory
     */
    public static SpringBeanFactory getBeanFactory() {
        return processorContext.getMainBeanFactory();
    }

    /**
     * 获取插件 PluginInfo
     *
     * @return PluginInfo
     */
    public static PluginInfo getPluginInfo() {
        return processorContext.getPluginInfo();
    }

    /**
     * 获取插件资源加载器
     *
     * @return ResourceLoader
     */
    public static ResourceLoader getResourceLoader() {
        return processorContext.getResourceLoader();
    }

    /**
     * 获取插件配置
     *
     * @return IntegrationConfiguration
     */
    public static IntegrationConfiguration getConfiguration() {
        return processorContext.getConfiguration();
    }

    /**
     * 获取主程序应用上下文
     *
     * @return MainApplicationContext
     */
    public static MainApplicationContext getMainApplicationContext() {
        return processorContext.getMainApplicationContext();
    }

    /**
     * 获取插件交互接口
     *
     * @return PluginInteractive
     */
    public static PluginInteractive getPluginInteractive() {
        return processorContext.getPluginInteractive();
    }

    /**
     * 获取插件描述符
     *
     * @return InsidePluginDescriptor
     */
    public static InsidePluginDescriptor getPluginDescriptor() {
        return processorContext.getPluginDescriptor();
    }

    /**
     * 获取运行模式
     *
     * @return RunMode
     */
    public static ProcessorContext.RunMode getRunMode() {
        return processorContext.runMode();
    }

}
