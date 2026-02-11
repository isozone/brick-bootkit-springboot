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

package com.zqzqq.bootkits.core.communication.exception;

/**
 * Exception thrown when a service is not found.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class ServiceNotFoundException extends ServiceException {

    private final Class<?> serviceInterface;

    public ServiceNotFoundException(String message) {
        super(message);
        this.serviceInterface = null;
    }

    public ServiceNotFoundException(String message, Class<?> serviceInterface) {
        super(message);
        this.serviceInterface = serviceInterface;
    }

    public ServiceNotFoundException(String message, Class<?> serviceInterface, Throwable cause) {
        super(message, cause);
        this.serviceInterface = serviceInterface;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }
}
