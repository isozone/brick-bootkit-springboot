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


package com.zqzqq.bootkits.distributed.registry;

import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.core.communication.ServiceMetadata;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证「本地插件服务注册/注销 → 触发调度器立即同步」的热加载推送链路：
 * 远端的门面在 register/unregister 本地服务时回调 collectionChangeHandler。
 */
class FacadeChangeHookTest {

    interface UserService {
        String getName(Long id);
    }

    interface OrderService {
        Long getCount(Long id);
    }

    @Test
    void registerAndUnregisterTriggerCollectionChangeHook() {
        DefaultPluginServiceRegistry local = new DefaultPluginServiceRegistry();
        ServiceDirectory dir = new ServiceDirectory() {
            @Override public void register(RemoteServiceRegistration r) { }
            @Override public void registerAll(java.util.List<RemoteServiceRegistration> l) { }
            @Override public void heartbeat(String s, String p, String n) { }
            @Override public java.util.List<RemoteServiceRegistration> lookup(String s) { return java.util.Collections.emptyList(); }
            @Override public RemoteServiceRegistration lookup(String s, String p) { return null; }
            @Override public void unregister(String s, String p, String n) { }
            @Override public void unregisterAllByNode(String n) { }
            @Override public java.util.Set<String> allServiceInterfaces() { return java.util.Collections.emptySet(); }
        };
        DistributedPluginServiceRegistry facade =
                new DistributedPluginServiceRegistry(local, dir,
                        new RemoteServiceProxyFactory(dir, null));

        AtomicInteger calls = new AtomicInteger();
        facade.setCollectionChangeHandler(calls::incrementAndGet);

        UserService impl = id -> String.valueOf(id);
        OrderService orderImpl = id -> 1L;
        // 注册触发
        ServiceDescriptor d = facade.registerService("plugin-a", UserService.class, impl,
                new ServiceMetadata());
        assertThat(d).isNotNull();
        assertThat(calls.get()).isEqualTo(1);

        // 再注册一个不同服务仍触发
        facade.registerService("plugin-a", OrderService.class, orderImpl, new ServiceMetadata());
        assertThat(calls.get()).isEqualTo(2);

        // 注销触发
        facade.unregisterService("plugin-a", UserService.class);
        assertThat(calls.get()).isEqualTo(3);
    }
}