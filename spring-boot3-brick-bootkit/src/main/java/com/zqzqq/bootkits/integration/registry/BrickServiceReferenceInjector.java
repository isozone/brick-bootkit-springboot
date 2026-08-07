package com.zqzqq.bootkits.integration.registry;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@code @BrickServiceReference} 跨插件服务字段注入器。
 * <p>
 * 扫描 Bean 中标注 SDK {@code @BrickServiceReference} 的字段，
 * 基于字段接口类型创建 JDK 动态代理并注入；代理方法调用时
 * 从 {@link PluginServiceRegistry} 获取真实服务并委托执行。
 * <p>
 * 通过动态代理实现跨插件/跨 ClassLoader 注入：
 * 代理使用调用方插件 ClassLoader 的接口类型，因此字段类型与代理类型一致，
 * 避免"提供方 ClassLoader 的接口"与"调用方 ClassLoader 的字段类型"不匹配。
 */
public class BrickServiceReferenceInjector {

    private static final Logger log = LoggerFactory.getLogger(BrickServiceReferenceInjector.class);

    /** SDK @BrickServiceReference 注解类型全名（避免依赖 sdk 模块导致循环依赖） */
    private static final String SERVICE_REFERENCE_ANNOTATION =
            "com.zqzqq.bootkits.sdk.annotation.BrickServiceReference";

    private final PluginServiceRegistry serviceRegistry;

    public BrickServiceReferenceInjector(PluginServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 扫描 Bean 的所有字段并注入 @BrickServiceReference 代理
     *
     * @param bean           目标 Bean
     * @param currentPluginId 当前插件 ID（用于日志，可不传）
     */
    public void injectReferences(Object bean, String currentPluginId) {
        if (bean == null || serviceRegistry == null) {
            return;
        }
        Class<?> beanClass = bean.getClass();
        for (Field field : beanClass.getDeclaredFields()) {
            Annotation reference = findAnnotationByName(field, SERVICE_REFERENCE_ANNOTATION);
            if (reference == null) {
                continue;
            }
            try {
                boolean optional = readBooleanValue(reference, "optional", false);
                Class<?> interfaceType = resolveInterfaceType(reference, field);
                if (interfaceType == null) {
                    if (!optional) {
                        log.warn("@BrickServiceReference 字段无法解析接口类型: {}.{}",
                                beanClass.getName(), field.getName());
                    }
                    continue;
                }
                Object proxy = createServiceProxy(interfaceType);
                field.setAccessible(true);
                field.set(bean, proxy);
                log.debug("已注入 @BrickServiceReference: {}.{} -> {}", beanClass.getName(),
                        field.getName(), interfaceType.getName());
            } catch (Exception e) {
                log.warn("@BrickServiceReference 注入失败: {}.{} - {}", beanClass.getName(),
                        field.getName(), e.getMessage());
            }
        }
    }

    /**
     * 创建服务动态代理：方法调用时从注册中心获取真实服务并委托
     */
    private Object createServiceProxy(Class<?> interfaceType) {
        return Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, args);
                    }
                    Object target = serviceRegistry.getService(null, interfaceType);
                    if (target == null) {
                        throw new IllegalStateException("未找到跨插件服务实现: " + interfaceType.getName());
                    }
                    return method.invoke(target, args);
                }
        );
    }

    /**
     * 解析注入目标接口：优先注解 value，否则取字段类型
     */
    private Class<?> resolveInterfaceType(Annotation annotation, Field field) {
        Class<?> declared = readClassValue(annotation, "value");
        if (declared != null && declared != void.class) {
            return declared;
        }
        if (field.getType() != null && field.getType().isInterface()) {
            return field.getType();
        }
        return null;
    }

    private Annotation findAnnotationByName(Field field, String annotationName) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (annotationName.equals(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    private Class<?> readClassValue(Annotation annotation, String methodName) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            return value instanceof Class<?> clazz ? clazz : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean readBooleanValue(Annotation annotation, String methodName, boolean defaultValue) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            return value instanceof Boolean b ? b : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
