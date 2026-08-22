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


package com.zqzqq.bootkits.distributed;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * gRPC 泛化调用的完整闭环测试：
 * 证明「宿主通过服务目录发现远端节点 → gRPC 转发到执行节点 → 执行节点反射执行 → 回传结果」链路可用。
 * <p>本测试不依赖真实 Redis，用内存 stub 的 ServiceDirectory 模拟目录。</p>
 */
class GrpcInvocationIntegrationTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "127.0.0.1";

    private final int serverPort = findFreePort();

    private GrpcServerBootstrap server;
    private GrpcClientProvider clientProvider;
    private RemoteServiceProxyFactory proxyFactory;

    static final class UserServiceImpl implements UserService {
        private final java.util.concurrent.atomic.AtomicInteger invokeCount =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public String getUserName(Long userId) {
            invokeCount.incrementAndGet();
            return "User-" + userId;
        }

        @Override
        public UserInfo getUserInfo(Long userId) {
            invokeCount.incrementAndGet();
            return new UserInfo(userId, "User-" + userId);
        }

        @Override
        public UserInfo getUserInfo(String name) {
            invokeCount.incrementAndGet();
            return new UserInfo(0L, name);
        }

        int count() {
            return invokeCount.get();
        }
    }

    final UserServiceImpl serviceImpl = new UserServiceImpl();

    @BeforeEach
    void setUp() throws Exception {
        // 1. 模拟执行节点的本地注册中心：getService 返回真实插件实例
        PluginServiceRegistry localRegistry = mock(PluginServiceRegistry.class);
        when(localRegistry.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenReturn(serviceImpl);

        // 2. 启动 gRPC 服务端（执行节点侧）
        server = new GrpcServerBootstrap(serverPort, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(localRegistry));
        server.start();

        // 3. 宿主侧连接池 + 远程代理工厂
        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        proxyFactory = new RemoteServiceProxyFactory(
                directoryStub(HOST, serverPort), clientProvider);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
        }
        if (clientProvider != null) {
            clientProvider.shutdownNow();
        }
    }

    private static ServiceDirectory directoryStub(String host, int port) {
        return new ServiceDirectory() {
            @Override
            public void register(RemoteServiceRegistration registration) {
            }

            @Override
            public void registerAll(List<RemoteServiceRegistration> registrations) {
            }

            @Override
            public void heartbeat(String serviceInterface, String pluginId, String nodeId) {
            }

            private RemoteServiceRegistration reg() {
                return new RemoteServiceRegistration(
                        PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        "node-test", host, port, System.currentTimeMillis());
            }

            @Override
            public List<RemoteServiceRegistration> lookup(String serviceInterface) {
                if (!UserService.class.getName().equals(serviceInterface)) {
                    return new ArrayList<>();
                }
                List<RemoteServiceRegistration> list = new ArrayList<>();
                list.add(reg());
                return list;
            }

            @Override
            public RemoteServiceRegistration lookup(String serviceInterface, String pluginId) {
                return UserService.class.getName().equals(serviceInterface) ? reg() : null;
            }

            @Override
            public void unregister(String serviceInterface, String pluginId, String nodeId) {
            }

            @Override
            public void unregisterAllByNode(String nodeId) {
            }

            @Override
            public Set<String> allServiceInterfaces() {
                return Set.of(UserService.class.getName());
            }
        };
    }

    @Test
    void shouldInvokeRemotePluginServiceTransparently() {
        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);

        // 1. 基本类型入参 + String 返回值
        String name = proxy.getUserName(1L);
        assertThat(name).isEqualTo("User-1");

        // 2. 自定义 DTO 往返
        UserInfo info = proxy.getUserInfo(99L);
        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(99L);
        assertThat(info.getName()).isEqualTo("User-99");

        assertThat(serviceImpl.count()).isEqualTo(2);
    }

    @Test
    void shouldPropagateRemoteExceptionWithMessage() {
        int failPort = findFreePort();
        PluginServiceRegistry failing = mock(PluginServiceRegistry.class);
        when(failing.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenThrow(new IllegalStateException("remote blew up"));

        GrpcServerBootstrap failingServer = new GrpcServerBootstrap(
                failPort, 16 * 1024 * 1024, new PluginInvocationServiceImpl(failing));
        try {
            failingServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        GrpcClientProvider cp = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        RemoteServiceProxyFactory rpf = new RemoteServiceProxyFactory(directoryStub(HOST, failPort), cp);

        try {
            UserService proxy = rpf.createProxy(PLUGIN_ID, UserService.class);
            proxy.getUserName(5L);
            throw new AssertionError("应抛出远端异常");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("remote blew up");
        } finally {
            cp.shutdownNow();
            failingServer.shutdown();
        }
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}