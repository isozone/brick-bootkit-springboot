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

package com.zqzqq.bootkits.core.communication.event;

import com.zqzqq.bootkits.core.communication.ServiceDescriptor;

import java.time.Instant;

/**
 * Base class for service events.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public abstract class ServiceEvent {

    private final ServiceDescriptor descriptor;
    private final Instant timestamp;

    protected ServiceEvent(ServiceDescriptor descriptor) {
        this.descriptor = descriptor;
        this.timestamp = Instant.now();
    }

    public ServiceDescriptor getDescriptor() {
        return descriptor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Get event type.
     */
    public abstract EventType getType();

    public enum EventType {
        REGISTERED,
        UNREGISTERED,
        STATE_CHANGED,
        HEALTH_CHANGED,
        ERROR
    }
}
