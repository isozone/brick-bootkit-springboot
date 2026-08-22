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

/**
 * Service descriptor interface.
 * <p>
 * Stores metadata and runtime information about a registered service.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public interface ServiceDescriptor {

    /**
     * Get unique service ID.
     * Format: pluginId:interfaceClassName:version
     *
     * @return service ID
     */
    String getServiceId();

    /**
     * Get service interface class.
     *
     * @return service interface
     */
    Class<?> getServiceInterface();

    /**
     * Get service implementation class.
     *
     * @return implementation class
     */
    Class<?> getServiceImplementation();

    /**
     * Get plugin ID that provides this service.
     *
     * @return plugin ID
     */
    String getPluginId();

    /**
     * Get service version.
     *
     * @return semantic version
     */
    String getVersion();

    /**
     * Get actual service instance.
     *
     * @return service instance
     */
    Object getServiceInstance();

    /**
     * Get service metadata.
     *
     * @return metadata
     */
    ServiceMetadata getMetadata();

    /**
     * Get current service state.
     *
     * @return state
     */
    ServiceState getState();

    /**
     * Get registration timestamp.
     *
     * @return timestamp in milliseconds
     */
    long getRegisteredAt();

    /**
     * Get last invocation timestamp.
     *
     * @return timestamp in milliseconds, 0 if never called
     */
    long getLastCalledAt();

    /**
     * Get invocation count.
     *
     * @return number of invocations
     */
    long getCallCount();

    /**
     * Get priority (lower number = higher priority).
     *
     * @return priority value
     */
    int getPriority();

    /**
     * Check if service is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Check if service is running.
     *
     * @return true if in READY or ACTIVE state
     */
    default boolean isRunning() {
        ServiceState state = getState();
        return state == ServiceState.READY || state == ServiceState.ACTIVE;
    }

    /**
     * Record a service invocation.
     *
     * @param success whether invocation was successful
     * @param durationMs invocation duration in milliseconds
     */
    void recordInvocation(boolean success, long durationMs);

    /**
     * Update service state.
     *
     * @param newState new state
     */
    void setState(ServiceState newState);

    /**
     * Update health status.
     *
     * @param healthy whether service is healthy
     */
    void setHealthy(boolean healthy);
}
