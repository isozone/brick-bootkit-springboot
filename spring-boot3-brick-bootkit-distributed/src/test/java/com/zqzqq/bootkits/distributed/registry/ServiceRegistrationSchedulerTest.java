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

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link ServiceRegistrationScheduler} 的 diff 式注册：
 * 单个长期运行的调度器每周期对在服务做就地 upsert，仅对消失的服务做定点注销，
 * 不再每周期触发全量 {@code unregisterAllByNode}（SCAN）。
 */
class ServiceRegistrationSchedulerTest {

    private static final String NODE = "node-1";
    private static final String HOST = "192.168.1.10";
    private static final int PORT = 9090;

    private static final class RecordingDirectory implements ServiceDirectory {
        final List<String> registered = new ArrayList<>();    // serviceInterface 序列
        final List<String> unregistered = new ArrayList<>();  // serviceInterface 序列
        final List<String> unregisterAllNodes = new ArrayList<>();

        @Override public void register(RemoteServiceRegistration r) {
            registered.add(r.getServiceInterface());
        }
        @Override public void registerAll(List<RemoteServiceRegistration> rs) {
            for (RemoteServiceRegistration r : rs) {
                registered.add(r.getServiceInterface());
            }
        }
        @Override public void unregister(String serviceInterface, String pluginId, String nodeId) {
            unregistered.add(serviceInterface);
        }
        @Override public void unregisterAllByNode(String nodeId) {
            unregisterAllNodes.add(nodeId);
        }
        @Override public void heartbeat(String a, String b, String c) { }
        @Override public java.util.List<RemoteServiceRegistration> lookup(String i) { return java.util.List.of(); }
        @Override public RemoteServiceRegistration lookup(String i, String p) { return null; }
        @Override public Set<String> allServiceInterfaces() { return Set.of(); }
    }

    /** 让 mock 认为 plugin-a 只提供指定接口。 */
    private static void stubPluginProviding(PluginServiceRegistry reg, Class<?> iface) {
        reset(reg);
        Set<String> plugins = new LinkedHashSet<>();
        plugins.add("plugin-a");
        when(reg.getRegisteredPlugins()).thenReturn(plugins);
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(iface);
        when(reg.getServicesByPlugin("plugin-a")).thenReturn(classes);
        ServiceDescriptor descriptor = mock(ServiceDescriptor.class);
        when(descriptor.getVersion()).thenReturn("1.0.0");
        when(reg.getServiceDescriptor("plugin-a", iface)).thenReturn(descriptor);
    }

    @Test
    void shouldUpsertExistingAndOnlyUnregisterVanishedServices() {
        RecordingDirectory dir = new RecordingDirectory();
        PluginServiceRegistry reg = mock(PluginServiceRegistry.class);

        // 单个长期运行的调度器（生产模型），跨多次 sync 保内部分 diff 状态
        ServiceRegistrationScheduler scheduler = new ServiceRegistrationScheduler(
                reg, dir, NODE, HOST, PORT, 60L);

        // 第一轮：插件只提供 A
        stubPluginProviding(reg, A.class);
        scheduler.syncAllServices();
        assertThat(dir.registered).containsExactly(A.class.getName());
        assertThat(dir.unregisterAllNodes).isEmpty();
        assertThat(dir.unregistered).isEmpty();

        // 第二轮：插件卸载 A、改提供 B → 只对 A 定点注销，同时 upsert B，绝不走全量注销
        stubPluginProviding(reg, B.class);
        scheduler.syncAllServices();
        assertThat(dir.unregistered).containsExactly(A.class.getName());
        assertThat(dir.unregisterAllNodes).isEmpty();
        assertThat(dir.registered).contains(B.class.getName());
    }

    interface A { }
    interface B { }
}