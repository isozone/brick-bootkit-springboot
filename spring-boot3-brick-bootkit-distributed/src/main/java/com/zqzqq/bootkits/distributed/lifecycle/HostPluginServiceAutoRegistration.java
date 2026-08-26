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

package com.zqzqq.bootkits.distributed.lifecycle;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿主级 {@code @PluginService} / {@code @BrickService} 自动注册。
 * <p>
 * 面向「分离容器」拓扑：每个微服务容器都是一个独立 JVM，无需被改造为 brick 插件、
 * 也无需引入插件打包流程——只要它的业务 {@code @Service} 实现标注了
 * {@code @PluginService}（接口保持不变），本组件在宿主（主应用）Spring 上下文就绪后
 * 自动把这些 bean 注册进本地 {@link PluginServiceRegistry}。
 * <p>
 * 当该节点以 {@code role=WORKER} 启动时，{@code ServiceRegistrationScheduler} 会把它们
 * 作为「本节点可远程提供服务」发布到服务目录（Redis/Nacos），其它容器（HOST）即可经
 * LOCATOR/gRPC 跨容器调用——提供方业务代码仅多一个注解，调用方 {@code @FeignClient}
 * 经桥接零改动。
 * <p>
 * 与 {@code ServiceRegistryLifecycleExtension}（仅扫描<b>插件</b>上下文）互补：本组件扫描
 * <b>宿主主上下文</b>，二者作用域不重叠，不会重复注册。
 * <p>
 * 注解识别按类型全名匹配（ClassLoader 安全），兼容插件 ClassLoader 与主程序不同的情况。
 */
public class HostPluginServiceAutoRegistration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HostPluginServiceAutoRegistration.class);

    private static final String CORE_SERVICE_ANNOTATION =
            "com.zqzqq.bootkits.core.communication.annotation.PluginService";
    private static final String SDK_SERVICE_ANNOTATION =
            "com.zqzqq.bootkits.sdk.annotation.BrickService";

    private final ApplicationContext applicationContext;
    private final PluginServiceRegistry registry;
    private final String pluginId;

    public HostPluginServiceAutoRegistration(ApplicationContext applicationContext,
                                             PluginServiceRegistry registry,
                                             String pluginId) {
        this.applicationContext = applicationContext;
        this.registry = registry;
        this.pluginId = pluginId;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (registry == null || applicationContext == null) {
            return;
        }
        int registered = 0;
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            try {
                Object bean = applicationContext.getBean(beanName);
                if (bean == null) {
                    continue;
                }
                Class<?> userClass = ClassUtils.getUserClass(bean.getClass());
                ServiceAnnotationMeta meta = resolveServiceAnnotation(userClass);
                if (meta == null) {
                    continue;
                }
                Class<?> interfaceClass = resolveInterfaceClass(userClass, meta);
                if (interfaceClass == null) {
                    log.debug("跳过 @PluginService bean {}：无法解析服务接口", beanName);
                    continue;
                }
                registry.registerService(pluginId, interfaceClass, bean, buildMetadata(meta));
                registered++;
                log.debug("宿主服务自动注册：pluginId={}, interface={}, bean={}",
                        pluginId, interfaceClass.getName(), beanName);
            } catch (Exception e) {
                log.debug("宿主服务自动注册跳过 bean {}（原因: {}）", beanName, e.getMessage());
            }
        }
        if (registered > 0) {
            log.info("宿主服务自动注册完成：pluginId={}, services={}", pluginId, registered);
        }
    }

    private ServiceAnnotationMeta resolveServiceAnnotation(Class<?> beanClass) {
        Annotation annotation = findAnnotationByName(beanClass, CORE_SERVICE_ANNOTATION);
        if (annotation != null) {
            return readAnnotation(annotation);
        }
        annotation = findAnnotationByName(beanClass, SDK_SERVICE_ANNOTATION);
        if (annotation != null) {
            return readAnnotation(annotation);
        }
        return null;
    }

    private Annotation findAnnotationByName(Class<?> clazz, String annotationName) {
        // 用户类上的直接注解命中（Spring AOP 代理场景已由 getUserClass 还原为真实类）
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotationName.equals(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    private ServiceAnnotationMeta readAnnotation(Annotation annotation) {
        ServiceAnnotationMeta meta = new ServiceAnnotationMeta();
        Class<?> type = annotation.annotationType();
        meta.interfaceClass = readClassValue(annotation, type, "interfaceClass");
        meta.version = readStringValue(annotation, type, "version", "1.0.0");
        meta.name = readStringValue(annotation, type, "name", "");
        meta.description = readStringValue(annotation, type, "description", "");
        meta.priority = readIntValue(annotation, type, "priority", 0);
        meta.singleton = readBooleanValue(annotation, type, "singleton", true);
        meta.enabled = readBooleanValue(annotation, type, "enabled", true);
        return meta;
    }

    private Class<?> readClassValue(Annotation annotation, Class<?> type, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof Class<?> clazz && clazz != void.class) {
                return clazz;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String readStringValue(Annotation annotation, Class<?> type, String methodName, String defaultValue) {
        try {
            Method method = type.getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return defaultValue;
    }

    private int readIntValue(Annotation annotation, Class<?> type, String methodName, int defaultValue) {
        try {
            Method method = type.getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof Integer i) {
                return i;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return defaultValue;
    }

    private boolean readBooleanValue(Annotation annotation, Class<?> type, String methodName, boolean defaultValue) {
        try {
            Method method = type.getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof Boolean b) {
                return b;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return defaultValue;
    }

    private Class<?> resolveInterfaceClass(Class<?> beanClass, ServiceAnnotationMeta meta) {
        if (meta.interfaceClass != null) {
            return meta.interfaceClass;
        }
        Class<?>[] interfaces = beanClass.getInterfaces();
        return interfaces.length > 0 ? interfaces[0] : null;
    }

    private ServiceMetadata buildMetadata(ServiceAnnotationMeta meta) {
        return ServiceMetadata.builder()
                .name(meta.name)
                .version(meta.version)
                .description(meta.description)
                .priority(meta.priority)
                .singleton(meta.singleton)
                .enabled(meta.enabled)
                .build();
    }

    /**
     * 注解元数据（与具体注解类型解耦，按全名匹配）。
     */
    private static class ServiceAnnotationMeta {
        private Class<?> interfaceClass;
        private String version;
        private String name;
        private String description;
        private int priority;
        private boolean singleton;
        private boolean enabled;
    }
}
