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

import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.UserInfo;
import com.zqzqq.bootkits.distributed.UserService;
import com.zqzqq.bootkits.distributed.registry.DistributedPluginServiceRegistry;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 跨容器场景的端到端验证（Feign 桥 + 分布式 LOCATOR/gRPC）。
 * <p>
 * 模拟两个独立容器：
 * <ul>
 *   <li>WORKER 容器：真实 gRPC server，承载 {@code UserService} 插件能力，并把它
 *       以 {@link RemoteServiceRegistration} 形式登记到（内存）服务目录；</li>
 *   <li>HOST 容器：本地未注册 {@code UserService}，但通过 {@link DistributedPluginServiceRegistry}
 *       发现目录中的远端节点，从而 {@code registry.getServices(UserService)} 返回跨容器 gRPC 代理。</li>
 * </ul>
 * HOST 侧对 {@code @FeignClient UserService} 的调用，经 {@link PluginFeignBridgeHandler} 透明路由到
 * WORKER 的 gRPC，调用方代码零改动；而未注册为插件能力的接口则回落到原生 Feign HTTP。
 */
class CrossContainerFeignBridgeTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "127.0.0.1";

    /** HOST 侧感知到的「原生 Feign HTTP 回退」哨兵值。 */
    private static final String HTTP_SENTINEL = "HTTP:";

    interface OrderService {
        String order(Long id);
    }

    private GrpcServerBootstrap workerServer;
    private GrpcClientProvider clientProvider;
    private ServiceDirectory directory;
    private DistributedPluginServiceRegistry hostRegistry;
    private final List<GrpcServerBootstrap> started = new ArrayList<>();

    /** WORKER 容器内的本地注册中心：只认识 UserService 这一个能力。 */
    private PluginServiceRegistry workerLocalRegistry() {
        PluginServiceRegistry local = mock(PluginServiceRegistry.class);
        when(local.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenReturn(new WorkerUserServiceImpl());
        return local;
    }

    /** 内存目录：仅包含 WORKER 暴露的 UserService 远端注册。 */
    private ServiceDirectory inMemoryDirectory(int workerPort) {
        RemoteServiceRegistration reg = new RemoteServiceRegistration(
                PLUGIN_ID, UserService.class.getName(), "1.0.0",
                "worker-node", HOST, workerPort, System.currentTimeMillis());
        return new ServiceDirectory() {
            @Override public void register(RemoteServiceRegistration r) { }
            @Override public void registerAll(List<RemoteServiceRegistration> rs) { }
            @Override public void heartbeat(String si, String pid, String nid) { }
            @Override public List<RemoteServiceRegistration> lookup(String si) {
                return UserService.class.getName().equals(si) ? List.of(reg) : new ArrayList<>();
            }
            @Override public RemoteServiceRegistration lookup(String si, String pid) {
                return UserService.class.getName().equals(si) ? reg : null;
            }
            @Override public void unregister(String si, String pid, String nid) { }
            @Override public void unregisterAllByNode(String nid) { }
            @Override public Set<String> allServiceInterfaces() {
                return Set.of(UserService.class.getName());
            }
        };
    }

    /** WORKER 的真实业务实现：返回带 worker 标记的结果，便于断言调用确实落到跨容器节点。 */
    static class WorkerUserServiceImpl implements UserService {
        @Override public String getUserName(Long userId) {
            return "worker-User-" + userId;
        }
        @Override public UserInfo getUserInfo(Long userId) {
            return new UserInfo(userId, "worker-User-" + userId);
        }
        @Override public UserInfo getUserInfo(String name) {
            return new UserInfo(0L, "worker-" + name);
        }
    }

    /** 原生 Feign HTTP 代理（模拟未走桥时的默认行为）。 */
    @SuppressWarnings("unchecked")
    private <T> T httpDelegate(Class<T> iface, String sentinelPrefix) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getUserName".equals(method.getName())) {
                            return sentinelPrefix + args[0];
                        }
                        if ("order".equals(method.getName())) {
                            return sentinelPrefix + args[0];
                        }
                        return sentinelPrefix + method.getName();
                    }
                });
    }

    /** 测试用 BPP：绕过真实 @FeignClient 类型依赖，直接判定为 Feign 客户端。 */
    static class TestBpp extends PluginFeignBridgePostProcessor {
        TestBpp(PluginServiceRegistry registry) {
            super(registry, true);
        }
        @Override boolean isFeignClient(Class<?> iface) {
            return true;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        int port = findFreePort();
        workerServer = new GrpcServerBootstrap(port, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(workerLocalRegistry()));
        workerServer.start();
        started.add(workerServer);

        directory = inMemoryDirectory(port);
        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);

        // HOST 本地注册中心：空（不承载 UserService），只通过目录发现远端。
        DefaultPluginServiceRegistry hostLocal = new DefaultPluginServiceRegistry();
        hostRegistry = new DistributedPluginServiceRegistry(
                hostLocal, directory,
                new com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory(
                        directory, clientProvider));
    }

    @AfterEach
    void tearDown() {
        for (GrpcServerBootstrap s : started) {
            try { s.shutdown(); } catch (Exception ignored) { }
        }
        started.clear();
        if (clientProvider != null) {
            clientProvider.shutdownNow();
        }
    }

    @Test
    void feignClientShouldRouteCrossContainerViaBridgeToWorker() throws Throwable {
        UserService httpClient = httpDelegate(UserService.class, HTTP_SENTINEL);
        UserService bridged = (UserService) new TestBpp(hostRegistry)
                .postProcessAfterInitialization(httpClient, "userClient");

        // 跨容器：应落到 WORKER 的 gRPC 实现，而非原生 HTTP 哨兵。
        String name = bridged.getUserName(7L);
        assertThat(name).isEqualTo("worker-User-7");
        assertThat(name).doesNotStartWith(HTTP_SENTINEL);

        UserInfo info = bridged.getUserInfo(9L);
        assertThat(info.getName()).isEqualTo("worker-User-9");
    }

    @Test
    void nonPluginInterfaceFallsBackToNativeFeignHttp() {
        OrderService httpClient = httpDelegate(OrderService.class, HTTP_SENTINEL);
        OrderService result = (OrderService) new TestBpp(hostRegistry)
                .postProcessAfterInitialization(httpClient, "orderClient");

        // 未注册为插件能力 → 不包装，直接回落原生 Feign HTTP。
        assertThat(result).isSameAs(httpClient);
        assertThat(result.order(5L)).isEqualTo(HTTP_SENTINEL + 5L);
    }

    @Test
    void hostRegistryResolvesRemoteProxyAcrossContainer() {
        List<UserService> services = hostRegistry.getServices(UserService.class);
        assertThat(services).isNotEmpty();

        // 经由目录解析出的远端代理，确实能跨容器调用到 WORKER。
        assertThat(services.get(0).getUserName(3L)).isEqualTo("worker-User-3");
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
