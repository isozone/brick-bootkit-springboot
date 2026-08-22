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
 * Service registration information.
 * <p>
 * Used for batch service registration.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class ServiceRegistration {

    private Class<?> serviceInterface;
    private Object serviceInstance;
    private ServiceMetadata metadata;

    /**
     * Default constructor.
     */
    public ServiceRegistration() {
    }

    /**
     * Constructor with required fields.
     */
    public ServiceRegistration(Class<?> serviceInterface, Object serviceInstance) {
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Constructor with all fields.
     */
    public ServiceRegistration(Class<?> serviceInterface, Object serviceInstance, ServiceMetadata metadata) {
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
        this.metadata = metadata;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Object serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ServiceMetadata metadata) {
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ServiceRegistration registration = new ServiceRegistration();

        public Builder interfaceClass(Class<?> serviceInterface) {
            registration.setServiceInterface(serviceInterface);
            return this;
        }

        public Builder instance(Object serviceInstance) {
            registration.setServiceInstance(serviceInstance);
            return this;
        }

        public Builder metadata(ServiceMetadata metadata) {
            registration.setMetadata(metadata);
            return this;
        }

        public ServiceRegistration build() {
            return registration;
        }
    }
}
