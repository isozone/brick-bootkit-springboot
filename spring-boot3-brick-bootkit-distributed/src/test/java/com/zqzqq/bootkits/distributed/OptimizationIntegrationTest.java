package com.zqzqq.bootkits.distributed;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.RedisServiceDirectory;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本轮「彻底优化」新增能力的专项验证：
 * <ul>
 *   <li>远端异常 cause 链精确还原：嵌套异常在宿主被完整重建（异常 + 根因）；</li>
 *   <li>节点健康状态 + 快速失败：宕机节点被标记不健康后，后继调用不再反复撞网络超时；</li>
 *   <li>Redis 目录兜底缓存：Redis 故障时降级返回 last-known-good 快照而非不可用。</li>
 * </ul>
 */
class OptimizationIntegrationTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "127.0.0.1";

    private GrpcServerBootstrap server;
    private GrpcClientProvider clientProvider;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
        }
        if (clientProvider != null) {
            clientProvider.shutdownNow();
        }
    }

    // ==================== 1. 异常 cause 链精确还原 ====================

    @Test
    void shouldRestoreRemoteExceptionCauseChain() throws Exception {
        int port = findFreePort();
        // 远端抛出「业务异常 + 根因」的嵌套异常
        PluginServiceRegistry registry = mock(PluginServiceRegistry.class);
        when(registry.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenThrow(new IllegalStateException("outer failed",
                        new IllegalArgumentException("root cause: null value")));

        server = new GrpcServerBootstrap(port, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(registry));
        server.start();

        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        RemoteServiceProxyFactory factory =
                new RemoteServiceProxyFactory(directoryStub(HOST, port, false), clientProvider);
        UserService proxy = factory.createProxy(PLUGIN_ID, UserService.class);

        assertThatThrownBy(() -> proxy.getUserName(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outer failed")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("root cause: null value");
    }

    // ==================== 2. 节点健康状态 + 快速失败 ====================

    @Test
    void shouldMarkDownNodeUnhealthyAndSkipIt() throws Exception {
        int goodPort = findFreePort();
        PluginServiceRegistry goodRegistry = mock(PluginServiceRegistry.class);
        when(goodRegistry.getService(eq(PLUGIN_ID), eq(UserService.class)))
                .thenReturn(new GrpcInvocationIntegrationTest.UserServiceImpl());
        server = new GrpcServerBootstrap(goodPort, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(goodRegistry));
        server.start();

        clientProvider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);

        // 目录返回两个节点：一个「已宕机」的坏节点 + 一个健康的 gRPC 节点
        int deadPort = findFreePort(); // 该端口无服务监听，连接必然失败
        RemoteServiceRegistration dead =
                new RemoteServiceRegistration(PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        "node-dead", HOST, deadPort, System.currentTimeMillis());
        RemoteServiceRegistration good =
                new RemoteServiceRegistration(PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        "node-good", HOST, goodPort, System.currentTimeMillis());

        // 有状态的目录：默认同时返回死节点与健康节点；forceDead 时只返回死节点。
        ServiceDirectory directory = buildStatefulDirectory(dead, good);

        RemoteServiceProxyFactory factory = new RemoteServiceProxyFactory(directory, clientProvider);
        UserService proxy = factory.createProxy(PLUGIN_ID, UserService.class);

        // 首次调用：目录只返回死节点 → 必触发 failover（无健康节点可转 → 抛错）。
        // 次数的绝对值取决于 gRPC 对该死节点连接失败的呈现（可能一次或多次），
        // 只要 >=1 即证明「不可达节点确实被识别并切换到下一副本/判不可用」。
        assertThatThrownBy(() -> proxy.getUserName(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("所有执行节点均不可用");
        long failedOnce = clientProvider.failoverCount();
        assertThat(failedOnce).isGreaterThanOrEqualTo(1);

        // 死节点此时已被标记不健康。切换目录为 [死, 健康]：
        // 健康节点优先 → 后继调用直接跳过死节点（快速失败），稳定成功且 failover 不再增长。
        ((StatefulDirectory) directory).forceDead = false;
        for (int i = 0; i < 5; i++) {
            assertThat(proxy.getUserName(100L + i)).startsWith("User-");
        }
        assertThat(clientProvider.failoverCount()).isEqualTo(failedOnce);
    }

    // ==================== 3. Redis 目录兜底缓存 ====================

    @SuppressWarnings("unchecked")
    @Test
    void shouldServeLastKnownGoodSnapshotWhenRedisDown() {
        RemoteServiceRegistration reg = new RemoteServiceRegistration(
                PLUGIN_ID, UserService.class.getName(), "1.0.0",
                "node-cached", HOST, findFreePort(), System.currentTimeMillis());

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        // 阶段 1：Redis 正常，返回一条注册 → 用于填充兜底缓存
        List<Object> healthyValues = new ArrayList<>();
        healthyValues.add("{\"pluginId\":\"" + PLUGIN_ID
                + "\",\"serviceInterface\":\"" + UserService.class.getName()
                + "\",\"version\":\"1.0.0\",\"nodeId\":\"node-cached\",\"host\":\"" + HOST
                + "\",\"port\":" + reg.getPort() + ",\"registeredAt\":" + reg.getRegisteredAt()
                + ",\"tlsEnabled\":false}");
        when(hashOps.values("pfx:svc:" + UserService.class.getName())).thenReturn(healthyValues);

        RedisServiceDirectory directory =
                new RedisServiceDirectory(redis, new ObjectMapper(), "pfx", 30L, 30_000L);

        // 阶段 1 成功查询，填充快照
        assertThat(directory.lookup(UserService.class.getName())).hasSize(1);

        // 阶段 2：Redis 故障（抛异常）→ 应降级返回上次快照，而非置空
        when(hashOps.values(eq("pfx:svc:" + UserService.class.getName())))
                .thenThrow(new RuntimeException("connection refused"));
        List<RemoteServiceRegistration> fallback = directory.lookup(UserService.class.getName());
        assertThat(fallback).hasSize(1);
        assertThat(fallback.get(0).getNodeId()).isEqualTo("node-cached");
    }

    private static ServiceDirectory directoryStub(String host, int port, boolean tlsEnabled) {
        return new ServiceDirectory() {
            private RemoteServiceRegistration reg() {
                return new RemoteServiceRegistration(
                        PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        "node-t", host, port, System.currentTimeMillis(), tlsEnabled);
            }
            @Override public void register(RemoteServiceRegistration r) { }
            @Override public void registerAll(List<RemoteServiceRegistration> rs) { }
            @Override public void heartbeat(String a, String b, String c) { }
            @Override public void unregister(String a, String b, String c) { }
            @Override public void unregisterAllByNode(String n) { }
            @Override public Set<String> allServiceInterfaces() { return Set.of(UserService.class.getName()); }
            @Override public RemoteServiceRegistration lookup(String iface, String pluginId) {
                return UserService.class.getName().equals(iface) ? reg() : null;
            }
            @Override public List<RemoteServiceRegistration> lookup(String iface) {
                if (!UserService.class.getName().equals(iface)) {
                    return new ArrayList<>();
                }
                List<RemoteServiceRegistration> list = new ArrayList<>();
                list.add(reg());
                return list;
            }
        };
    }

    private static final class StatefulDirectory implements ServiceDirectory {
        RemoteServiceRegistration dead;
        RemoteServiceRegistration good;
        boolean forceDead = true;

        StatefulDirectory(RemoteServiceRegistration dead, RemoteServiceRegistration good) {
            this.dead = dead;
            this.good = good;
        }
        @Override public void register(RemoteServiceRegistration r) { }
        @Override public void registerAll(List<RemoteServiceRegistration> rs) { }
        @Override public void heartbeat(String a, String b, String c) { }
        @Override public void unregister(String a, String b, String c) { }
        @Override public void unregisterAllByNode(String n) { }
        @Override public Set<String> allServiceInterfaces() { return Set.of(UserService.class.getName()); }
        @Override public RemoteServiceRegistration lookup(String iface, String pluginId) { return null; }
        @Override public List<RemoteServiceRegistration> lookup(String iface) {
            List<RemoteServiceRegistration> list = new ArrayList<>();
            if (forceDead) {
                list.add(dead); // 仅死节点 → 首次调用必撞死节点触发 failover
            } else {
                list.add(dead);
                list.add(good);
            }
            return list;
        }
    }

    private static ServiceDirectory buildStatefulDirectory(RemoteServiceRegistration dead,
                                                           RemoteServiceRegistration good) {
        return new StatefulDirectory(dead, good);
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}