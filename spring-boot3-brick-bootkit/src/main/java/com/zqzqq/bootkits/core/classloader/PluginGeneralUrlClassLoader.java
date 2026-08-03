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
import com.zqzqq.bootkits.loader.classloader.GenericClassLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.DefaultResourceLoaderFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 插件基础 classLoader。
 * <p>
 * 共存模式下插件包通常是嵌套包结构：外层插件 jar 内包含 classes/ 与 lib/*.jar。
 * 标准 URLClassLoader 不能直接加载 jar 内部的 jar，因此这里必须使用
 * GenericClassLoader + ResourceLoaderFactory 的资源存储体系，由
 * NestedPluginJarResourceLoader 解析外层插件包并把内嵌依赖 class 缓存到本地资源中。
 *
 * @author starBlues
 * @version 4.0.8
 * @since 3.0.4
 */
@Slf4j
public class PluginGeneralUrlClassLoader extends GenericClassLoader implements PluginResourceLoaderFactory {

    private final PluginResourceLoaderFactory proxy;

    public PluginGeneralUrlClassLoader(String name, GeneralUrlClassLoader parent) {
        super(name, parent, new DefaultResourceLoaderFactory(name));
        this.proxy = new PluginResourceLoaderFactoryProxy(this, parent);
    }

    @Override
    public void addResource(InsidePluginDescriptor descriptor) throws Exception {
        proxy.addResource(descriptor);
    }

    /**
     * 增强与 Spring Boot 3.5.x 的兼容性：Spring/Jakarta/JDK 相关类优先交给父加载器，
     * 业务插件类和插件内嵌依赖仍由 GenericClassLoader 的资源存储体系加载。
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            if (name.startsWith("org.springframework.")
                    || name.startsWith("jakarta.")
                    || name.startsWith("java.")
                    || name.startsWith("javax.")) {
                try {
                    Class<?> clazz = getParent().loadClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (ClassNotFoundException ignored) {
                    // 忽略异常，继续走插件本地加载逻辑
                }
            }
            Class<?> clazz = super.loadClass(name);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null && contextClassLoader != this) {
                try {
                    Class<?> clazz = contextClassLoader.loadClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (ClassNotFoundException ignored) {
                    // 忽略异常，抛出原始异常
                }
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}

