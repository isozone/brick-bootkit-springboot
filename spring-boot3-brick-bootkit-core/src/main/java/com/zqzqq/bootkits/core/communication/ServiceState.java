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
 * Plugin service state enumeration.
 * <p>
 * Defines all possible states a plugin service can be in during its lifecycle.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public enum ServiceState {

    /**
     * Registered but not yet initialized.
     */
    REGISTERED,

    /**
     * Currently initializing.
     */
    INITIALIZING,

    /**
     * Ready and active.
     */
    READY,

    /**
     * Currently processing a request.
     */
    ACTIVE,

    /**
     * Paused temporarily (manually or due to load).
     */
    PAUSED,

    /**
     * Stopped (plugin stopped).
     */
    STOPPED,

    /**
     * Unregistered (removed from registry).
     */
    UNREGISTERED,

    /**
     * Health check failed.
     */
    UNHEALTHY,

    /**
     * Circuit breaker open (fast fail).
     */
    CIRCUIT_OPEN,

    /**
     * Overloaded (too many requests).
     */
    OVERLOADED
}
