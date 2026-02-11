/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication;

import com.zqzqq.bootkits.core.communication.annotation.PluginService;
import com.zqzqq.bootkits.core.communication.annotation.ServiceDependency;
import com.zqzqq.bootkits.core.communication.event.ServiceRegisteredEvent;
import com.zqzqq.bootkits.core.communication.event.ServiceUnregisteredEvent;
import com.zqzqq.bootkits.core.plugin.Plugin;

import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Plugin service registry manager.
 * <p>
 * Integrates service registry with plugin lifecycle management.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class PluginServiceRegistryManager {

    private static final Logger log = LoggerFactory.getLogger(PluginServiceRegistryManager.class);

    private final PluginServiceRegistry serviceRegistry;
    private final Plugin plugin;

    public PluginServiceRegistryManager(PluginServiceRegistry serviceRegistry, Plugin plugin) {
        this.serviceRegistry = serviceRegistry;
        this.plugin = plugin;
    }

    /**
     * Register services from a plugin.
     * <p>
     * Scans for classes annotated with @PluginService and registers them.
     *
     * @param plugin the plugin
     * @param serviceClasses classes to register as services
     * @return number of services registered
     */
    public int registerPluginServices(Plugin plugin, List<Class<?>> serviceClasses) {
        int count = 0;
        
        for (Class<?> serviceClass : serviceClasses) {
            PluginService annotation = serviceClass.getAnnotation(PluginService.class);
            if (annotation == null) {
                continue;
            }
            
            try {
                // Determine interface class
                Class<?> interfaceClass = annotation.interfaceClass();
                if (interfaceClass == void.class) {
                    interfaceClass = findServiceInterface(serviceClass);
                    if (interfaceClass == null) {
                        continue;
                    }
                }
                
                // Create instance
                Object instance = instantiateService(serviceClass, plugin);
                if (instance == null) {
                    continue;
                }
                
                // Build metadata
                ServiceMetadata metadata = buildMetadata(annotation);
                
                // Register
                serviceRegistry.registerService(
                    plugin.getId(),
                    interfaceClass,
                    instance,
                    metadata
                );
                
                count++;
                
            } catch (Exception e) {
                log.error("Failed to register service: {}", serviceClass.getName(), e);
            }
        }
        
        return count;
    }

    /**
     * Unregister all services from a plugin.
     */
    public void unregisterPluginServices(String pluginId) {
        serviceRegistry.unregisterAllServices(pluginId);
    }

    /**
     * Check service dependencies for a plugin.
     *
     * @param pluginId plugin ID
     * @return dependency check result
     */
    public ServiceDependencyCheckResult checkServiceDependencies(String pluginId) {
        return serviceRegistry.checkDependencies(pluginId);
    }

    /**
     * Get a service from another plugin.
     *
     * @param targetPluginId target plugin ID
     * @param serviceInterface service interface class
     * @param <T> service type
     * @return service proxy
     */
    public <T> T getService(String targetPluginId, Class<T> serviceInterface) {
        return serviceRegistry.getService(targetPluginId, serviceInterface);
    }

    /**
     * Get a service from any plugin.
     *
     * @param serviceInterface service interface class
     * @param <T> service type
     * @return service proxy
     */
    public <T> T getAnyService(Class<T> serviceInterface) {
        return serviceRegistry.getService(null, serviceInterface);
    }

    /**
     * Get all services implementing an interface.
     *
     * @param serviceInterface service interface class
     * @param <T> service type
     * @return list of service proxies
     */
    public <T> List<T> getAllServices(Class<T> serviceInterface) {
        return serviceRegistry.getServices(serviceInterface);
    }

    /**
     * Subscribe to service changes.
     *
     * @param serviceInterface service interface
     * @param listener change listener
     * @return subscription
     */
    public ServiceSubscription subscribe(Class<?> serviceInterface, ServiceChangeListener listener) {
        return serviceRegistry.subscribe(serviceInterface, listener);
    }

    /**
     * Get all services provided by a plugin.
     *
     * @param pluginId plugin ID
     * @return set of service interface classes
     */
    public Set<Class<?>> getPluginServices(String pluginId) {
        return serviceRegistry.getServicesByPlugin(pluginId);
    }

    /**
     * Check if a plugin provides a specific service.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface class
     * @return true if registered
     */
    public boolean hasService(String pluginId, Class<?> serviceInterface) {
        return serviceRegistry.isRegistered(pluginId, serviceInterface);
    }

    /**
     * Get the service registry.
     *
     * @return service registry
     */
    public PluginServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Find the service interface implemented by a class.
     */
    private Class<?> findServiceInterface(Class<?> implementationClass) {
        for (Class<?> iface : implementationClass.getInterfaces()) {
            // Skip common framework interfaces
            if (iface.getName().startsWith("java.") || iface.getName().startsWith("javax.")) {
                continue;
            }
            return iface;
        }
        return null;
    }

    /**
     * Instantiate a service class.
     */
    private Object instantiateService(Class<?> serviceClass, Plugin plugin) {
        try {
            // Try to get from plugin instance if it has the field
            for (Field field : plugin.getClass().getDeclaredFields()) {
                if (field.getType().equals(serviceClass)) {
                    field.setAccessible(true);
                    return field.get(plugin);
                }
            }
            
            // Fallback: create new instance
            return serviceClass.getDeclaredConstructor().newInstance();
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build service metadata from annotation.
     */
    private ServiceMetadata buildMetadata(PluginService annotation) {
        ServiceMetadata.Builder builder = ServiceMetadata.builder()
            .version(annotation.version())
            .name(annotation.name())
            .description(annotation.description())
            .priority(annotation.priority())
            .singleton(annotation.singleton())
            .enabled(annotation.enabled());
        
        for (String tag : annotation.tags()) {
            builder.name(tag);
        }
        
        return builder.build();
    }

    /**
     * Utility method to get service from any available provider.
     *
     * @param <T> service type
     * @param serviceInterface service interface class
     *
     * @return service proxy or null
     */
    public static <T> T findService(
        Class<T> serviceInterface,
        PluginServiceRegistry serviceRegistry
    ) {
        try {
            return serviceRegistry.getService(null, serviceInterface);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Utility method to get service from specific provider.
     *
     * @param <T> service type
     * @param serviceInterface service interface class
     * @param pluginId target plugin ID
     * @param serviceRegistry service registry
     * @return service proxy or null
     */
    public static <T> T findService(
        Class<T> serviceInterface,
        PluginServiceRegistry serviceRegistry,
        String pluginId
    ) {
        try {
            return serviceRegistry.getService(pluginId, serviceInterface);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Utility method to get all available services.
     *
     * @param <T> service type
     * @param serviceInterface service interface class
     * @param serviceRegistry service registry
     * @return list of service proxies
     */
    public static <T> List<T> findAllServices(
        Class<T> serviceInterface,
        PluginServiceRegistry serviceRegistry
    ) {
        return serviceRegistry.getServices(serviceInterface);
    }
}
