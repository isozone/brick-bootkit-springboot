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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PluginFeignBridgeTest {

    interface BridgeSvc {
        String hi(String x);
    }

    /** 被 @FeignClient 接口继承的「真正的服务接口」。 */
    interface Base {
        String hi(String x);
    }

    /** 模拟典型的 @FeignClient 接口：extends 真正的服务接口。 */
    interface Ext extends Base {
    }

    private BridgeSvc remoteStandIn() {
        return x -> "REMOTE:" + x;
    }

    private BridgeSvc httpDelegate() {
        return x -> "HTTP:" + x;
    }

    private Base remoteStandInBase() {
        return x -> "REMOTE:" + x;
    }

    private Base httpDelegateBase() {
        return x -> "HTTP:" + x;
    }

    private Ext proxyDelegateExt() {
        return (Ext) Proxy.newProxyInstance(Ext.class.getClassLoader(),
                new Class<?>[]{Ext.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return method.invoke(httpDelegateBase(), args);
                    }
                });
    }

    private BridgeSvc proxyDelegate(BridgeSvc delegate) {
        return (BridgeSvc) Proxy.newProxyInstance(
                BridgeSvc.class.getClassLoader(),
                new Class<?>[]{BridgeSvc.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return method.invoke(delegate, args);
                    }
                });
    }

    @Test
    void handlerRoutesToRegistryWhenServiceRegistered() throws Throwable {
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(BridgeSvc.class)).thenReturn(List.of(remoteStandIn()));

        BridgeSvc bridged = (BridgeSvc) PluginFeignBridgeHandler.wrap(
                httpDelegate(), Collections.singleton(BridgeSvc.class), registry);

        assertEquals("REMOTE:a", bridged.hi("a"));
    }

    @Test
    void handlerFallsBackToHttpWhenNoPluginService() throws Throwable {
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(BridgeSvc.class)).thenReturn(Collections.emptyList());

        BridgeSvc bridged = (BridgeSvc) PluginFeignBridgeHandler.wrap(
                httpDelegate(), Collections.singleton(BridgeSvc.class), registry);

        assertEquals("HTTP:a", bridged.hi("a"));
    }

    @Test
    void handlerPreservesEqualsHashCodeToString() {
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(any())).thenReturn(Collections.emptyList());

        BridgeSvc http = httpDelegate();
        BridgeSvc bridged = (BridgeSvc) PluginFeignBridgeHandler.wrap(
                http, Collections.singleton(BridgeSvc.class), registry);

        assertTrue(bridged.equals(http));
        assertEquals(http.hashCode(), bridged.hashCode());
        org.assertj.core.api.Assertions.assertThat(bridged.toString()).contains("PluginFeignBridge");
    }

    @Test
    void postProcessorWrapsFeignClientProxyWhenRegistered() {
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(BridgeSvc.class)).thenReturn(List.of(remoteStandIn()));

        PluginFeignBridgePostProcessor bpp = new TestBpp(registry);
        BridgeSvc http = proxyDelegate(httpDelegate());
        Object result = bpp.postProcessAfterInitialization(http, "userClient");

        BridgeSvc bridged = (BridgeSvc) result;
        assertEquals("REMOTE:a", bridged.hi("a"));
    }

    @Test
    void postProcessorDoesNotWrapWhenNoPluginService() {
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(BridgeSvc.class)).thenReturn(Collections.emptyList());

        PluginFeignBridgePostProcessor bpp = new TestBpp(registry);
        BridgeSvc http = proxyDelegate(httpDelegate());
        Object result = bpp.postProcessAfterInitialization(http, "userClient");

        // 未注册为插件能力：原 bean 原样返回（仍是 http delegate）
        assertSame(http, result);
    }

    @Test
    void handlerRoutesWhenFeignClientExtendsRegisteredBaseInterface() throws Throwable {
        // 真实场景：@FeignClient 接口 Ext extends Base，能力以 Base 注册，而非 Ext。
        PluginServiceRegistry registry = Mockito.mock(PluginServiceRegistry.class);
        when(registry.getServices(Base.class)).thenReturn(java.util.List.of(remoteStandInBase()));

        Ext bridged = (Ext) new TestBpp(registry)
                .postProcessAfterInitialization(proxyDelegateExt(), "userClient");

        // 经父接口 Base 命中插件能力，而非回落到原 Feign HTTP
        assertEquals("REMOTE:a", bridged.hi("a"));
    }

    /** 测试用子类：绕过 @FeignClient 注解的真实存在性，直接判定为 Feign 客户端。 */
    static class TestBpp extends PluginFeignBridgePostProcessor {
        TestBpp(PluginServiceRegistry registry) {
            super(registry, true);
        }

        @Override
        boolean isFeignClient(Class<?> iface) {
            return true;
        }
    }
}
