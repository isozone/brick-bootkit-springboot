package com.zqzqq.bootkits.distributed.proxy;

import com.zqzqq.bootkits.distributed.UserService;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校正后回归：整组重试（wholeGroupAttempts）仅在「本轮真发了网络调用且有节点 UNAVAILABLE」时触发，
 * 不会因为「全部节点都在熔断窗口里、被短路跳过」而进入无谓退避。
 * <p>历史问题：旧实现里 {@code groupAllUnavailable} 声明后从未改成 false，
 * 导致 {@code if (!groupAllUnavailable) break;} 是死代码，整组重试逻辑等价于「永远走完所有轮次」。
 * 修复后改为按本轮是否真发生 UNAVAILABLE 来决定是否继续重试。
 *
 * @since 4.0.9 方向二/三：整组重试语义修正回归
 */
class RetryRoundSemanticsTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "10.0.0.1";
    private static final int PORT = 9090;

    @Test
    void shouldNotRetryWhenAllNodesShortCircuitedByBreaker() {
        // 场景：唯一节点已连续失败 3 次，熔断 OPEN → 本轮所有候选都会被 allowAttempt()==false 短路跳过。
        // 期望：不会进入「整组不可达」退避重试（任何重试都不会改善），直接抛出最终的不可用错误。
        GrpcClientProvider clients = new GrpcClientProvider(16 * 1024 * 1024, 5000L);
        // 把节点打到 OPEN
        clients.markFailure(HOST, PORT, false);
        clients.markFailure(HOST, PORT, false);
        clients.markFailure(HOST, PORT, false);

        // 同一目录每次都返回这个已被熔断的节点
        RemoteServiceRegistration reg = new RemoteServiceRegistration(
                PLUGIN_ID, UserService.class.getName(), "1.0.0",
                "node-x", HOST, PORT, System.currentTimeMillis(), false);
        ServiceDirectory directory = new SingleRegistrationDirectory(reg);

        // maxFailoverRetries=2：旧死代码逻辑下会退避重试 3 轮（每轮都全被短路），耗时被无谓拉长；
        // 修复后应只跑 1 轮就判定「没真发生过 UNAVAILABLE」，直接抛错，无退避。
        RemoteInvocationHandler handler = new RemoteInvocationHandler(
                PLUGIN_ID, UserService.class, directory, clients, null,
                java.util.Collections.emptyMap(), 2);

        long begin = System.nanoTime();
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> handler.invoke(java.lang.reflect.Proxy.newProxyInstance(
                        UserService.class.getClassLoader(),
                        new Class<?>[]{UserService.class},
                        handler),
                        UserService.class.getDeclaredMethods()[0],
                        new Object[]{1L}));
        long elapsedMillis = (System.nanoTime() - begin) / 1_000_000L;

        // 仍应抛出「所有节点不可用」错误
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("所有执行节点均不可用");
        // 关键断言：不应发生退避（旧死代码会至少退避 20ms+40ms）；给 15ms 余量防止抖动误报。
        // 修复后耗时主要来自熔断短路判断，应在毫秒级内完成。
        assertThat(elapsedMillis)
                .as("整轮全短路时不应进入退避重试（耗时=%dms）", elapsedMillis)
                .isLessThan(15L);

        clients.shutdownNow();
    }

    /**
     * 只返回一条固定注册的有状态目录（每次 lookup 都返回同一节点）。
     */
    private static final class SingleRegistrationDirectory implements ServiceDirectory {
        private final RemoteServiceRegistration reg;
        SingleRegistrationDirectory(RemoteServiceRegistration reg) { this.reg = reg; }
        @Override public void register(RemoteServiceRegistration r) { }
        @Override public void registerAll(List<RemoteServiceRegistration> rs) { }
        @Override public void heartbeat(String a, String b, String c) { }
        @Override public void unregister(String a, String b, String c) { }
        @Override public void unregisterAllByNode(String n) { }
        @Override public Set<String> allServiceInterfaces() { return Set.of(UserService.class.getName()); }
        @Override public RemoteServiceRegistration lookup(String iface, String pluginId) { return reg; }
        @Override public List<RemoteServiceRegistration> lookup(String iface) {
            List<RemoteServiceRegistration> list = new ArrayList<>();
            list.add(reg);
            return list;
        }
    }
}
