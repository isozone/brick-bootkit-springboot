package com.zqzqq.bootkits.integration.registry;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceMetadata;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginApplicationContextGetter;
import com.zqzqq.bootkits.integration.spi.PluginLifecycleExtension;
import com.zqzqq.bootkits.spring.ApplicationContext;
import com.zqzqq.bootkits.spring.SpringBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 插件服务注册中心自动接线扩展。
 * <p>
 * 实现 {@link PluginLifecycleExtension} SPI，在插件启动（afterStart）时
 * 自动扫描插件 Spring 容器中标注 {@code @PluginService}（core 注解）或
 * {@code @BrickService}（SDK 注解）的 Bean，注册到 {@link PluginServiceRegistry}；
 * 插件停止（afterStop）时注销该插件全部服务。
 * <p>
 * 注解识别通过类型全名匹配，避免插件 ClassLoader 与主程序 ClassLoader
 * 不同导致注解类型不一致的问题。
 */
public class ServiceRegistryLifecycleExtension implements PluginLifecycleExtension {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryLifecycleExtension.class);

    /** core 模块 @PluginService 注解类型全名 */
    private static final String CORE_SERVICE_ANNOTATION =
            "com.zqzqq.bootkits.core.communication.annotation.PluginService";

    /** SDK 模块 @BrickService 注解类型全名 */
    private static final String SDK_SERVICE_ANNOTATION =
            "com.zqzqq.bootkits.sdk.annotation.BrickService";

    private final ObjectProvider<PluginServiceRegistry> registryProvider;
    private final BrickServiceReferenceInjector serviceReferenceInjector;

    public ServiceRegistryLifecycleExtension(ObjectProvider<PluginServiceRegistry> registryProvider) {
        this.registryProvider = registryProvider;
        this.serviceReferenceInjector = new BrickServiceReferenceInjector(registryProvider.getIfAvailable());
    }

    @Override
    public String getExtensionId() {
        return "service-registry-sync";
    }

    @Override
    public void afterStart(PluginInsideInfo pluginInsideInfo) {
        PluginServiceRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        String pluginId = pluginInsideInfo.getPluginId();
        ApplicationContext pluginContext = PluginApplicationContextGetter.get(pluginId);
        if (pluginContext == null) {
            log.debug("插件上下文不可用，跳过服务注册: pluginId={}", pluginId);
            return;
        }
        SpringBeanFactory beanFactory = pluginContext.getSpringBeanFactory();
        if (beanFactory == null) {
            log.debug("插件 BeanFactory 不可用，跳过服务注册: pluginId={}", pluginId);
            return;
        }

        int registered = 0;
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            try {
                Object bean = beanFactory.getBean(beanName);
                // 1. 注册本插件提供的服务（@PluginService / @BrickService）
                ServiceAnnotationMeta meta = resolveServiceAnnotation(bean.getClass());
                if (meta != null) {
                    Class<?> interfaceClass = resolveInterfaceClass(bean.getClass(), meta);
                    if (interfaceClass != null) {
                        registry.registerService(pluginId, interfaceClass, bean, buildMetadata(meta));
                        registered++;
                    }
                }
                // 2. 注入跨插件服务引用（@BrickServiceReference）
                if (serviceReferenceInjector != null) {
                    serviceReferenceInjector.injectReferences(bean, pluginId);
                }
            } catch (Exception e) {
                log.debug("插件服务注册/注入跳过 bean: {}（原因: {}）", beanName, e.getMessage());
            }
        }

        if (registered > 0) {
            log.info("插件服务注册完成: pluginId={}, services={}", pluginId, registered);
        }
    }

    @Override
    public void afterStop(PluginInsideInfo pluginInsideInfo) {
        PluginServiceRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        registry.unregisterAllServices(pluginInsideInfo.getPluginId());
        log.info("插件服务已注销: pluginId={}", pluginInsideInfo.getPluginId());
    }

    /**
     * 解析 Bean 类上的服务注解（按类型全名匹配，兼容 ClassLoader 差异）
     */
    private ServiceAnnotationMeta resolveServiceAnnotation(Class<?> beanClass) {
        Annotation annotation = findAnnotationByName(beanClass, CORE_SERVICE_ANNOTATION);
        if (annotation != null) {
            return readAnnotation(annotation, CORE_SERVICE_ANNOTATION);
        }
        annotation = findAnnotationByName(beanClass, SDK_SERVICE_ANNOTATION);
        if (annotation != null) {
            return readAnnotation(annotation, SDK_SERVICE_ANNOTATION);
        }
        return null;
    }

    private Annotation findAnnotationByName(Class<?> clazz, String annotationName) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotationName.equals(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 反射读取注解属性（兼容不同 ClassLoader 加载的注解类型）
     */
    private ServiceAnnotationMeta readAnnotation(Annotation annotation, String annotationName) {
        ServiceAnnotationMeta meta = new ServiceAnnotationMeta();
        Class<?> annotationType = annotation.annotationType();
        meta.interfaceClass = readClassValue(annotation, annotationType, "interfaceClass");
        meta.version = readStringValue(annotation, annotationType, "version", "1.0.0");
        meta.name = readStringValue(annotation, annotationType, "name", "");
        meta.description = readStringValue(annotation, annotationType, "description", "");
        meta.priority = readIntValue(annotation, annotationType, "priority", 0);
        meta.singleton = readBooleanValue(annotation, annotationType, "singleton", true);
        meta.enabled = readBooleanValue(annotation, annotationType, "enabled", true);
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

    /**
     * 解析服务接口：优先使用注解显式声明，否则取实现的第一个接口
     */
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
     * 注解元数据（与具体注解类型解耦）
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
