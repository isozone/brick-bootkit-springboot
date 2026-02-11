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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Service proxy factory for cross-classloader invocation.
 * <p>
 * Creates proxies that handle classloader switching when invoking
 * services from different plugins.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public final class ServiceProxyFactory {

    private ServiceProxyFactory() {
        // Utility class
    }

    /**
     * Create a service proxy.
     * <p>
     * The proxy handles classloader switching during invocation,
     * allowing services from different plugins to call each other.
     *
     * @param target the actual service instance
     * @param interfaceClass the service interface
     * @param targetClassLoader the classloader of the target (implementation class)
     * @param <T> service type
     * @return proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(
        Object target,
        Class<T> interfaceClass,
        ClassLoader targetClassLoader
    ) {
        // Validate interface
        validateInterface(interfaceClass, target.getClass());

        // Get main classloader for loading the interface
        ClassLoader mainClassLoader = getMainClassLoader(interfaceClass);

        // Create invocation handler
        ServiceInvocationHandler handler = new ServiceInvocationHandler(
            target,
            targetClassLoader,
            interfaceClass
        );

        // Create proxy using main classloader for interface
        return (T) Proxy.newProxyInstance(
            mainClassLoader,
            new Class<?>[] { interfaceClass },
            handler
        );
    }

    /**
     * Validate that the implementation class implements the interface.
     */
    private static void validateInterface(Class<?> interfaceClass, Class<?> implementationClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(
                "Class is not an interface: " + interfaceClass.getName()
            );
        }

        boolean implemented = false;
        for (Class<?> iface : implementationClass.getInterfaces()) {
            if (iface.equals(interfaceClass)) {
                implemented = true;
                break;
            }
        }

        if (!implemented) {
            throw new IllegalArgumentException(
                "Implementation does not implement interface: " +
                implementationClass.getName() + " does not implement " + interfaceClass.getName()
            );
        }
    }

    /**
     * Get the main classloader for loading the interface.
     * Uses the interface class's classloader as it should be shared.
     */
    private static ClassLoader getMainClassLoader(Class<?> interfaceClass) {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        // Fallback to system classloader
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Service invocation handler.
     * <p>
     * Handles the actual method invocation with classloader switching.
     */
    private static class ServiceInvocationHandler implements InvocationHandler {

        private final Object target;
        private final ClassLoader targetClassLoader;
        private final Class<?> interfaceClass;

        public ServiceInvocationHandler(
            Object target,
            ClassLoader targetClassLoader,
            Class<?> interfaceClass
        ) {
            this.target = target;
            this.targetClassLoader = targetClassLoader;
            this.interfaceClass = interfaceClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Handle Object methods specially
            if (methodName.equals("toString")) {
                return target.toString();
            }
            if (methodName.equals("hashCode")) {
                return System.identityHashCode(target);
            }
            if (methodName.equals("equals")) {
                return proxy == args[0];
            }

            // Save original classloader
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                // Switch to target classloader
                Thread.currentThread().setContextClassLoader(targetClassLoader);

                // Invoke the method
                long startTime = System.currentTimeMillis();
                try {
                    Object result = method.invoke(target, args);
                    long duration = System.currentTimeMillis() - startTime;

                    // Record metrics if available
                    recordMetrics(methodName, duration, null);

                    return result;
                } catch (InvocationTargetException e) {
                    long duration = System.currentTimeMillis() - startTime;
                    recordMetrics(methodName, duration, e.getCause());

                    // Unwrap and rethrow
                    throw unwrapException(e.getCause());
                }
            } finally {
                // Restore original classloader
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }

        /**
         * Unwrap InvocationTargetException.
         */
        private Throwable unwrapException(Throwable throwable) {
            if (throwable instanceof InvocationTargetException) {
                return throwable.getCause();
            }
            return throwable;
        }

        /**
         * Record invocation metrics.
         * Can be extended to use actual metrics framework.
         */
        private void recordMetrics(String methodName, long durationMs, Throwable error) {
            // Placeholder for metrics recording
            // In production, integrate with your metrics system (Micrometer, etc.)
        }
    }
}
