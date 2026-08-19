package com.zqzqq.bootkits.distributed.rpc;

import com.zqzqq.bootkits.distributed.metrics.DistributedMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link GrpcClientProvider} / {@link GrpcServerBootstrap} 的 keepAlive 与池化配置：
 * <ul>
 *   <li>默认构造的 keepAlive 与默认值一致；</li>
 *   <li>显式 keepAlive 配置能被读取；</li>
 *   <li>同一 host:port:tls 复用同一 channel（池化），不同节点不同 channel。</li>
 * </ul>
 * 不真实建立 gRPC 连接，只验证配置生效与 channel 复用语义。
 *
 * @since 4.0.9 方向三/四：建连池化 + keepAlive
 */
@DisplayName("GrpcKeepAlive & ChannelPool Test")
class GrpcKeepAliveConfigTest {

    @Test
    @DisplayName("默认构造的 keepAlive 用默认值（60s ping / 20s ack 超时 / 无活跃调用也 ping）")
    void defaultKeepAliveMatchesRecommendedDefaults() {
        GrpcClientProvider provider = new GrpcClientProvider(
                16 * 1024 * 1024, 5000L, false, "", "", new DistributedMetrics());

        assertThat(provider.getKeepAliveTimeMillis()).isEqualTo(60_000L);
        assertThat(provider.getKeepAliveTimeoutMillis()).isEqualTo(20_000L);
        assertThat(provider.isKeepAliveWithoutCalls()).isTrue();
    }

    @Test
    @DisplayName("全参构造能覆盖 keepAlive 配置（用于跨 NAT/公网部署调参）")
    void explicitKeepAliveConfigIsApplied() {
        GrpcClientProvider provider = new GrpcClientProvider(
                16 * 1024 * 1024, 5000L, false, "", "",
                30_000L, 10_000L, false, new DistributedMetrics());

        assertThat(provider.getKeepAliveTimeMillis()).isEqualTo(30_000L);
        assertThat(provider.getKeepAliveTimeoutMillis()).isEqualTo(10_000L);
        assertThat(provider.isKeepAliveWithoutCalls()).isFalse();
    }

    @Test
    @DisplayName("同一 host:port:tls 的多次取用复用同一 ManagedChannel；不同节点不同 channel")
    void channelIsPooledByHostPortTlsKey() {
        GrpcClientProvider provider = new GrpcClientProvider(16 * 1024 * 1024, 5000L);

        // 同一节点（明文）多次取用 → 复用
        io.grpc.ManagedChannel a1 = provider.channel("10.0.0.1", 9090, false);
        io.grpc.ManagedChannel a2 = provider.channel("10.0.0.1", 9090, false);
        assertThat(a1).isSameAs(a2);

        // 同节点 TLS 与明文视为两条连接（缓存键含 TLS 标记，支持混合灰度）
        io.grpc.ManagedChannel a3 = provider.channel("10.0.0.1", 9090, true);
        assertThat(a3).isNotSameAs(a1);

        // 不同节点 → 不同 channel
        io.grpc.ManagedChannel b1 = provider.channel("10.0.0.2", 9090, false);
        assertThat(b1).isNotSameAs(a1);

        provider.close();
    }

    @Test
    @DisplayName("GrpcServerBootstrap 的 keepAlive 与 permit 配置可被读取（不真实启动端口）")
    void serverKeepAliveConfigIsExposed() {
        PluginInvocationServiceImpl service = new PluginInvocationServiceImpl(null);
        GrpcServerBootstrap bootstrap = new GrpcServerBootstrap(
                "127.0.0.1", 0, 16 * 1024 * 1024, service,
                false, "", "", "",
                5L,
                45_000L, 25_000L, true);

        assertThat(bootstrap.getKeepAliveTimeMillis()).isEqualTo(45_000L);
        assertThat(bootstrap.getPermitKeepAliveTimeMillis()).isEqualTo(25_000L);
        assertThat(bootstrap.isPermitKeepAliveWithoutCalls()).isTrue();
        // gracefulShutdownSeconds 至少 1
        assertThat(bootstrap.getPort()).isEqualTo(0);
    }
}
