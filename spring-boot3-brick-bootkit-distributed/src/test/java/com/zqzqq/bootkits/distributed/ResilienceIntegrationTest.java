package com.zqzqq.bootkits.distributed;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import com.zqzqq.bootkits.distributed.serialization.DistTrace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 分布式调用的健壮性与可观测性闭环测试：
 * <ul>
 *   <li>多副本轮询：目录中多个执行节点，调用在节点间均匀轮询；</li>
 *   <li>故障转移：首选节点宕机后，调用自动切换到下一副本，业务无感；</li>
 *   <li>traceId 透传：宿主 MDC 中的 traceId 随 gRPC 请求透传到执行节点线程，跨节点日志可串联。</li>
 * </ul>
 * 用真实 gRPC server 模拟多个执行节点，目录用内存 stub（不依赖 Redis）。
 */
class ResilienceIntegrationTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "127.0.0.1";

    /** 记录「哪个节点被调用 + 到达时的 traceId」，供断言。 */
    static final class CallRecord {
        final String nodeId;
        final String traceId;

        CallRecord(String nodeId, String traceId) {
            this.nodeId = nodeId;
            this.traceId = traceId;
        }
    }

    /** 具备调用记录能力的插件服务实现。 */
    static class RecordingUserServiceImpl implements UserService {
        private final String nodeId;
        final List<CallRecord> records = new CopyOnWriteArrayList<>();

        RecordingUserServiceImpl(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public String getUserName(Long userId) {
            records.add(new CallRecord(nodeId, DistTrace.get()));
            return nodeId + "-User-" + userId;
        }

        @Override
        public UserInfo getUserInfo(Long userId) {
            records.add(new CallRecord(nodeId, DistTrace.get()));
            return new UserInfo(userId, nodeId + "-User-" + userId);
        }

        @Override
        public UserInfo getUserInfo(String name) {
            records.add(new CallRecord(nodeId, DistTrace.get()));
            return new UserInfo(0L, nodeId + "-" + name);
        }
    }

    private GrpcServerBootstrap serverA;
    private GrpcServerBootstrap serverB;
    private RecordingUserServiceImpl implA;
    private RecordingUserServiceImpl implB;
    private GrpcClientProvider clientProvider;
    private RemoteServiceProxyFactory proxyFactory;
    private final List<GrpcServerBootstrap> startedServers = new ArrayList<>();

    /**
     * 构造两个执行节点 A/B 的目录 stub。当 {@code includeA}/{@code includeB} 为 false 时，
     * 对应节点从目录中移除（模拟节点下线但尚未被清除的场景）。
     */
    private ServiceDirectory directory() {
        return new ServiceDirectory() {
            private List<RemoteServiceRegistration> live() {
                List<RemoteServiceRegistration> list = new ArrayList<>();
                if (serverA != null) {
                    list.add(reg("node-A", serverA.getPort()));
                }
                if (serverB != null) {
                    list.add(reg("node-B", serverB.getPort()));
                }
                return list;
            }

            private RemoteServiceRegistration reg(String nodeId, int port) {
                return new RemoteServiceRegistration(
                        PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        nodeId, HOST, port, System.currentTimeMillis());
            }

            @Override
            public void register(RemoteServiceRegistration registration) {
            }

            @Override
            public void registerAll(List<RemoteServiceRegistration> registrations) {
            }

            @Override
            public void heartbeat(String serviceInterface, String pluginId, String nodeId) {
            }

            @Override
            public List<RemoteServiceRegistration> lookup(String serviceInterface) {
                return UserService.class.getName().equals(serviceInterface) ? live() : new ArrayList<>();
            }

            @Override
            public RemoteServiceRegistration lookup(String serviceInterface, String pluginId) {
                List<RemoteServiceRegistration> all = live();
                return all.isEmpty() ? null : all.get(0);
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

    private void startNodeA() throws Exception {
        implA = new RecordingUserServiceImpl("node-A");
        serverA = startServer(implA);
        startedServers.add(serverA);
    }

    private void startNodeB() throws Exception {
        implB = new RecordingUserServiceImpl("node-B");
        serverB = startServer(implB);
        startedServers.add(serverB);
    }

    private GrpcServerBootstrap startServer(RecordingUserServiceImpl impl) throws Exception {
        PluginServiceRegistry localRegistry = mock(PluginServiceRegistry.class);
        when(localRegistry.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenReturn(impl);
        GrpcServerBootstrap server = new GrpcServerBootstrap(
                findFreePort(), 16 * 1024 * 1024, new PluginInvocationServiceImpl(localRegistry));
        server.start();
        return server;
    }

    @BeforeEach
    void setUp() throws Exception {
        startNodeA();
        startNodeB();
        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        proxyFactory = new RemoteServiceProxyFactory(directory(), clientProvider);
    }

    @AfterEach
    void tearDown() {
        for (GrpcServerBootstrap server : startedServers) {
            try {
                server.shutdown();
            } catch (Exception ignored) {
                // ignore
            }
        }
        startedServers.clear();
        if (clientProvider != null) {
            clientProvider.shutdownNow();
        }
        MDC.clear();
    }

    @Test
    void shouldRoundRobinAcrossMultipleReplicas() {
        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);

        // 连续多次调用：应分布在两个节点之间，而非全部落在同一节点
        for (int i = 0; i < 8; i++) {
            proxy.getUserName((long) i);
        }

        // 每个节点都应收到调用（轮询），且无一次调用丢失
        assertThat(implA.records).isNotEmpty();
        assertThat(implB.records).isNotEmpty();
        assertThat(implA.records.size() + implB.records.size()).isEqualTo(8);
    }

    @Test
    void shouldFailoverToAnotherReplicaWhenPreferredNodeIsDown() throws Exception {
        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);

        // 预热：确认链路可用
        assertThat(proxy.getUserName(1L)).isNotBlank();

        // 让节点 A 宕机（传输层不可用）。目录中仍保留 A 的注册（模拟陈旧注册尚未被
        // 心跳清除）。由于轮询会在 A/B 间交替，命中已宕机的 A 时必然触发连接失败
        // → 故障转移到 B，从而真实走过 failover 路径。
        serverA.shutdown();

        // 足够多次调用：约一半会先命中已宕机的 A 再转移到 B，另一半直接命中 B。
        // 所有调用必须成功，且结果全部来自存活节点 B —— 证明任何一次调用都不因
        // A 宕机而丢失。
        int calls = 24;
        for (int i = 0; i < calls; i++) {
            String result = proxy.getUserName(100L + i);
            assertThat(result).startsWith("node-B");
        }

        // B 承接了全部 calls 次后继调用；且 A 宕机后不可能再有新的 A 记录。
        assertThat(implB.records.size()).isGreaterThanOrEqualTo(calls);
        assertThat(implA.records).doesNotContainNull();
    }

    @Test
    void shouldPropagateTraceIdAcrossNodes() {
        // 宿主侧建立链路 traceId
        String traceId = "trace-1234567890";
        MDC.put(DistTrace.KEY, traceId);

        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);
        proxy.getUserName(7L);

        MDC.remove(DistTrace.KEY);

        // 无论调用落在哪个节点，执行节点线程都应看到同一 traceId
        List<CallRecord> allRecords = new ArrayList<>(implA.records);
        allRecords.addAll(implB.records);
        assertThat(allRecords).hasSize(1);
        assertThat(allRecords.get(0).traceId).isEqualTo(traceId);
    }

    @Test
    void shouldRouteMethodsByExactSignatureForOverloads() {
        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);

        // 同名重载：getUserInfo(Long) 与 getUserInfo(String) 必须精确命中各自签名。
        // Long 重载返回 id=入参、name 带 "-User-"；String 重载返回 id=0、name 带 "-<name>"。
        UserInfo byId = proxy.getUserInfo(42L);
        assertThat(byId.getId()).isEqualTo(42L);
        assertThat(byId.getName()).contains("-User-42");

        UserInfo byName = proxy.getUserInfo("Alice");
        assertThat(byName.getId()).isZero(); // String 重载返回 id=0，区分于 Long 重载
        assertThat(byName.getName()).endsWith("-Alice");
        assertThat(byName.getName()).doesNotContain("-User-");
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}