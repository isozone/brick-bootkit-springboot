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

package com.zqzqq.bootkits.distributed.feign;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Feign → 插件服务桥接后置处理器。
 * <p>
 * 在 bean 初始化后扫描：若某 bean 是 JDK 动态代理、且其接口带有 {@code @FeignClient}
 * 注解，同时该接口已被注册为插件能力（{@code PluginServiceRegistry.getServices} 非空），
 * 则用一个走 {@link PluginServiceRegistry} 的代理包裹它。调用方保持 {@code @Autowired}
 * 注入的同一个接口类型，代码完全不变。
 * <p>
 * 为避免对 Spring Cloud OpenFeign 的编译期依赖，{@code @FeignClient} 仅按注解全限定名
 * 字符串比对，不导入其类型。
 */
public class PluginFeignBridgePostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PluginFeignBridgePostProcessor.class);

    private static final String FEIGN_CLIENT_ANNOTATION =
            "org.springframework.cloud.openfeign.FeignClient";

    private final PluginServiceRegistry registry;
    private final boolean enabled;

    public PluginFeignBridgePostProcessor(PluginServiceRegistry registry, boolean enabled) {
        this.registry = registry;
        this.enabled = enabled;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!enabled || bean == null) {
            return bean;
        }
        Class<?> beanClass = bean.getClass();
        if (!Proxy.isProxyClass(beanClass)) {
            return bean;
        }
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            return bean;
        }
        Set<Class<?>> bridgeInterfaces = new LinkedHashSet<>();
        for (Class<?> iface : interfaces) {
            if (!isFeignClient(iface)) {
                continue;
            }
            // 典型用法：@FeignClient 接口 extends 真正的服务接口（如 UserClient extends UserService）。
            // 代理只直接实现 UserClient，方法 getDeclaringClass() 返回的是 UserService，
            // 因此必须把「@FeignClient 接口 + 其全部父接口」中已注册为插件能力的接口都纳入桥接集合。
            Set<Class<?>> hierarchy = new LinkedHashSet<>();
            collectInterfaceHierarchy(iface, hierarchy);
            for (Class<?> candidate : hierarchy) {
                if (isRegisteredPluginService(candidate)) {
                    bridgeInterfaces.add(candidate);
                }
            }
        }
        if (bridgeInterfaces.isEmpty()) {
            return bean;
        }
        log.info("Feign 桥接生效：将 {} 的接口 {} 改为走插件服务注册中心（本地优先/远端 gRPC）",
                beanName, bridgeInterfaces);
        return PluginFeignBridgeHandler.wrap(bean, bridgeInterfaces, registry);
    }

    boolean isFeignClient(Class<?> iface) {
        for (Annotation annotation : iface.getAnnotations()) {
            if (FEIGN_CLIENT_ANNOTATION.equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /** 收集接口及其全部父接口（含间接父接口），去重。 */
    private void collectInterfaceHierarchy(Class<?> iface, Set<Class<?>> out) {
        if (iface == null || !iface.isInterface() || out.contains(iface)) {
            return;
        }
        out.add(iface);
        for (Class<?> parent : iface.getInterfaces()) {
            collectInterfaceHierarchy(parent, out);
        }
    }

    private boolean isRegisteredPluginService(Class<?> iface) {
        try {
            List<?> services = registry.getServices(iface);
            return services != null && !services.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
