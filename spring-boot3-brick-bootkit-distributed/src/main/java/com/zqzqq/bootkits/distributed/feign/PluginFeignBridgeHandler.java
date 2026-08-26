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

package com.zqzqq.bootkits.distributed.feign;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Feign 客户端桥接处理器。
 * <p>
 * 被包裹的 Feign 客户端 bean 原本通过 HTTP 调用目标微服务；本处理器把「同时也是插件能力」
 * 的接口调用，改为走 {@link PluginServiceRegistry}（本地优先、远端 gRPC 兜底），
 * 从而让 co-resident 插件调用省去 HTTP 一跳、远程插件调用统一走框架传输层。
 * 若目标接口未被注册为插件能力，则透明回落到原 Feign HTTP 代理，调用方无感。
 */
class PluginFeignBridgeHandler implements InvocationHandler {

    private final Object delegate;
    private final Set<Class<?>> bridgeInterfaces;
    private final PluginServiceRegistry registry;

    PluginFeignBridgeHandler(Object delegate, Set<Class<?>> bridgeInterfaces, PluginServiceRegistry registry) {
        this.delegate = delegate;
        this.bridgeInterfaces = bridgeInterfaces;
        this.registry = registry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("equals".equals(name) && args != null && args.length == 1) {
            return Objects.equals(delegate, args[0]);
        }
        if ("hashCode".equals(name)) {
            return delegate.hashCode();
        }
        if ("toString".equals(name)) {
            return "PluginFeignBridge(" + delegate + ")";
        }
        // 默认方法代理到原 Feign 客户端（其自身处理 default 语义）
        if (method.isDefault()) {
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        Class<?> iface = method.getDeclaringClass();
        if (bridgeInterfaces.contains(iface)) {
            List<?> services = registry.getServices(iface);
            if (services != null && !services.isEmpty()) {
                try {
                    return method.invoke(services.get(0), args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }
        // 回落到原 Feign HTTP 调用
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * 用桥接处理器包裹原 Feign 客户端 bean。
     */
    static Object wrap(Object delegate, Set<Class<?>> bridgeInterfaces, PluginServiceRegistry registry) {
        Class<?>[] interfaces = delegate.getClass().getInterfaces();
        return Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                interfaces, new PluginFeignBridgeHandler(delegate, bridgeInterfaces, registry));
    }
}
