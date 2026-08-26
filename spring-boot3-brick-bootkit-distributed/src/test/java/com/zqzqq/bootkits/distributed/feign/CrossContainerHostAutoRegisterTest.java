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
import com.zqzqq.bootkits.core.communication.annotation.PluginService;
import com.zqzqq.bootkits.distributed.registry.DistributedPluginServiceRegistry;
import com.zqzqq.bootkits.distributed.UserInfo;
import com.zqzqq.bootkits.distributed.UserService;
import com.zqzqq.bootkits.distributed.lifecycle.HostPluginServiceAutoRegistration;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨容器端到端：provider 用<b>真实 Spring 上下文 + 真实 @PluginService bean</b>
 * （而非 mock 注册中心）经 {@link HostPluginServiceAutoRegistration} 注册，并作为 WORKER
 * 经 gRPC 对外提供；HOST 侧对 @FeignClient 接口经桥接透明路由到该 WORKER。
 * <p>证明分离容器拓扑下，提供方只需给 @Service 加 @PluginService 即可被跨容器调用。
 */
class CrossContainerHostAutoRegisterTest {

    private static final String HOST = "127.0.0.1";
    private static final String PLUGIN_ID = "host";

    /** 提供方真实业务实现：仅标注 @Component + @PluginService，接口保持不变。 */
    @Component
    @PluginService(version = "1.0.0")
    public static class WorkerUserServiceImpl implements UserService {
        @Override
        public String getUserName(Long userId) {
            return "worker-User-" + userId;
        }
        @Override
        public UserInfo getUserInfo(Long userId) {
            return new UserInfo(userId, "worker-User-" + userId);
        }
        @Override
        public UserInfo getUserInfo(String name) {
            return new UserInfo(0L, "worker-" + name);
        }
    }

    private GrpcServerBootstrap workerServer;
    private GrpcClientProvider clientProvider;
    private ServiceDirectory directory;
    private DistributedPluginServiceRegistry hostRegistry;
    private AnnotationConfigApplicationContext workerContext;
    private final List<GrpcServerBootstrap> started = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        int port = findFreePort();

        // —— WORKER 容器：真实 Spring 上下文承载 @PluginService bean ——
        workerContext = new AnnotationConfigApplicationContext();
        workerContext.register(WorkerUserServiceImpl.class);
        workerContext.refresh();

        PluginServiceRegistry workerLocal = new DefaultPluginServiceRegistry();
        // 模拟「宿主级自动注册」在 WORKER 节点把业务 bean 登记进本地注册中心
        HostPluginServiceAutoRegistration autoReg =
                new HostPluginServiceAutoRegistration(workerContext, workerLocal, PLUGIN_ID);
        autoReg.run(null);
        assertThat(workerLocal.getServicesByPlugin(PLUGIN_ID)).contains(UserService.class);

        workerServer = new GrpcServerBootstrap(port, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(workerLocal));
        workerServer.start();
        started.add(workerServer);

        RemoteServiceRegistration reg = new RemoteServiceRegistration(
                PLUGIN_ID, UserService.class.getName(), "1.0.0",
                "worker-node", HOST, port, System.currentTimeMillis());
        directory = new ServiceDirectory() {
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

        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        hostRegistry = new DistributedPluginServiceRegistry(
                new DefaultPluginServiceRegistry(), directory,
                new com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory(directory, clientProvider));
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
        if (workerContext != null) {
            workerContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    private UserService httpDelegate() {
        return (UserService) Proxy.newProxyInstance(UserService.class.getClassLoader(),
                new Class<?>[]{UserService.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return "HTTP:" + method.getName();
                    }
                });
    }

    @Test
    void feignClientRoutesToRealPluginServiceBeanAcrossContainer() {
        UserService httpClient = httpDelegate();
        UserService bridged = (UserService) new PluginFeignBridgeTest.TestBpp(hostRegistry)
                .postProcessAfterInitialization(httpClient, "userClient");

        // 跨容器落到 WORKER 的真实 @PluginService bean（非 mock）
        assertThat(bridged.getUserName(7L)).isEqualTo("worker-User-7");
        assertThat(bridged.getUserInfo(9L).getName()).isEqualTo("worker-User-9");
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
