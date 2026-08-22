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

import com.zqzqq.bootkits.core.communication.event.*;
import com.zqzqq.bootkits.core.communication.exception.*;

import java.util.List;
import java.util.Set;

/**
 * Plugin service registry interface.
 * <p>
 * Core responsibilities:
 * <ul>
 *   <li>Manage service registration/unregistration</li>
 *   <li>Provide service discovery capabilities</li>
 *   <li>Support service version management</li>
 *   <li>Handle service dependencies</li>
 * </ul>
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public interface PluginServiceRegistry {

    // ==================== Registration/Unregistration ====================

    /**
     * Register a plugin service.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface class
     * @param serviceInstance service instance
     * @param metadata service metadata
     * @return service descriptor
     * @throws ServiceRegistrationException if registration fails
     */
    ServiceDescriptor registerService(
        String pluginId,
        Class<?> serviceInterface,
        Object serviceInstance,
        ServiceMetadata metadata
    ) throws ServiceRegistrationException;

    /**
     * Register a service with default metadata.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface class
     * @param serviceInstance service instance
     * @return service descriptor
     */
    default ServiceDescriptor registerService(
        String pluginId,
        Class<?> serviceInterface,
        Object serviceInstance
    ) throws ServiceRegistrationException {
        return registerService(
            pluginId,
            serviceInterface,
            serviceInstance,
            ServiceMetadata.builder()
                .version("1.0.0")
                .build()
        );
    }

    /**
     * Batch register services.
     *
     * @param pluginId plugin ID
     * @param registrations service registrations
     */
    void registerServices(String pluginId, List<ServiceRegistration> registrations);

    /**
     * Unregister all services provided by a plugin.
     *
     * @param pluginId plugin ID
     */
    void unregisterAllServices(String pluginId);

    /**
     * Unregister a single service.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface
     */
    void unregisterService(String pluginId, Class<?> serviceInterface);

    // ==================== Service Discovery ====================

    /**
     * Get service by interface (single instance).
     * <p>
     * Lookup strategy:
     * <ol>
     *   <li>Exact version match</li>
     *   <li>If no exact match, return highest compatible version</li>
     *   <li>If multiple compatible versions, return highest priority</li>
     * </ol>
     *
     * @param pluginId target plugin ID (null or empty to search all plugins)
     * @param serviceInterface service interface
     * @param <T> service type
     * @return service proxy
     * @throws ServiceNotFoundException if service not found
     */
    <T> T getService(String pluginId, Class<T> serviceInterface)
        throws ServiceNotFoundException;

    /**
     * Get all services implementing an interface (multiple instances).
     *
     * @param serviceInterface service interface
     * @param <T> service type
     * @return service list (sorted by priority)
     */
    <T> List<T> getServices(Class<T> serviceInterface);

    /**
     * Get services by interface and version range.
     *
     * @param serviceInterface service interface
     * @param versionRange version range, e.g. "[1.0,2.0)"
     * @param <T> service type
     * @return matching services
     */
    <T> List<T> getServicesByVersion(Class<T> serviceInterface, String versionRange);

    // ==================== Dependency Management ====================

    /**
     * Register a service dependency.
     *
     * @param consumerPluginId consumer plugin ID
     * @param requiredService required service interface
     * @param versionRange version range
     * @param optional whether this is optional
     */
    void registerServiceDependency(
        String consumerPluginId,
        Class<?> requiredService,
        String versionRange,
        boolean optional
    );

    /**
     * Check if service dependencies are satisfied.
     *
     * @param pluginId plugin ID
     * @return dependency check result
     */
    ServiceDependencyCheckResult checkDependencies(String pluginId);

    // ==================== Subscription/Publishing (Optional) ====================

    /**
     * Subscribe to service changes.
     *
     * @param serviceInterface service interface
     * @param listener change listener
     * @return subscription
     */
    ServiceSubscription subscribe(
        Class<?> serviceInterface,
        ServiceChangeListener listener
    );

    /**
     * Publish a service event.
     *
     * @param event service event
     */
    void publishEvent(ServiceEvent event);

    // ==================== Query ====================

    /**
     * Get all registered service interfaces.
     *
     * @return set of interface classes
     */
    Set<Class<?>> getRegisteredInterfaces();

    /**
     * Get all service interfaces provided by a plugin.
     *
     * @param pluginId plugin ID
     * @return set of interface classes
     */
    Set<Class<?>> getServicesByPlugin(String pluginId);

    /**
     * Check if service is registered.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface
     * @return true if registered
     */
    boolean isRegistered(String pluginId, Class<?> serviceInterface);

    /**
     * Get service descriptor.
     *
     * @param pluginId plugin ID
     * @param serviceInterface service interface
     * @return service descriptor or null
     */
    ServiceDescriptor getServiceDescriptor(String pluginId, Class<?> serviceInterface);

    /**
     * Get all registered plugins.
     *
     * @return set of plugin IDs
     */
    Set<String> getRegisteredPlugins();

    /**
     * Get statistics about registered services.
     *
     * @return registry statistics
     */
    RegistryStatistics getStatistics();
}
