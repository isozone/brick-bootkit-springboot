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

package com.zqzqq.bootkits.core.classloader;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.loader.classloader.GeneralUrlClassLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 插件基础 url classLoader
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.4
 */
@Slf4j
public class PluginGeneralUrlClassLoader extends GeneralUrlClassLoader implements PluginResourceLoaderFactory{

    private final PluginResourceLoaderFactory proxy;

    public PluginGeneralUrlClassLoader(String name, GeneralUrlClassLoader parent) {
        super(name, parent);
        this.proxy = new PluginResourceLoaderFactoryProxy(this, parent);
    }

    @Override
    public void addResource(InsidePluginDescriptor descriptor) throws Exception {
        proxy.addResource(descriptor);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
    
    /**
     * 增强与Spring Boot 3.5.x的兼容性
     * Spring Boot 3.5.x中可能使用了更严格的类加载机制
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            // 首先尝试从父类加载器加载Spring相关类
            if (name.startsWith("org.springframework.") || 
                name.startsWith("jakarta.") || 
                name.startsWith("java.") ||
                name.startsWith("javax.")) {
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // 忽略异常，继续尝试其他方式加载
                }
            }
            // 使用标准类加载逻辑
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            // 如果标准加载失败，尝试从线程上下文件类加载器加
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader != null && contextClassLoader != this) {
                    return contextClassLoader.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {
                // 忽略异常
            }
            throw e;
        }
    }
}

