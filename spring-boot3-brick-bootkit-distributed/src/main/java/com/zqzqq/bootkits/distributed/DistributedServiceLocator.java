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


package com.zqzqq.bootkits.distributed;

import com.zqzqq.bootkits.distributed.registry.DistributedPluginServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

/**
 * 分布式服务定位器（面向调用方的一站式入口）。
 * <p>
 * 用法：
 * <pre>
 *   UserService userService = DistributedServiceLocator.service("user-plugin", UserService.class);
 *   userService.getUserName(1L);
 * </pre>
 * <p>
 * 本地能解析则返回本地服务，远端插件则自动创建 gRPC 远程代理，调用方无感知。
 * <p>
 * 该 Bean 由 {@code DistributedPluginAutoConfiguration} 注册，不依赖调用方
 * 的组件扫描范围，只要启用分布式模块即可直接使用。
 */
public class DistributedServiceLocator implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    private static DistributedPluginServiceRegistry registry() {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring ApplicationContext 尚未初始化，或分布式模块未启用。");
        }
        return applicationContext.getBean(DistributedPluginServiceRegistry.class);
    }

    /**
     * 获取指定插件提供的服务（本地优先，远端兜底）。
     */
    public static <T> T service(String pluginId, Class<T> serviceInterface) {
        return registry().getService(pluginId, serviceInterface);
    }

    /**
     * 获取某接口的全部实现（本地 + 远端聚合）。
     */
    public static <T> java.util.List<T> services(Class<T> serviceInterface) {
        return registry().getServices(serviceInterface);
    }

    /**
     * 判断服务是否可用（本地或远端任一注册即可）。
     */
    public static boolean available(String pluginId, Class<?> serviceInterface) {
        return registry().isRegistered(pluginId, serviceInterface);
    }
}