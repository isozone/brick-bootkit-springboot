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


package com.zqzqq.bootkits.distributed.config;

import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.DistributedServiceLocator;
import com.zqzqq.bootkits.distributed.registry.DistributedPluginServiceRegistry;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 分布式模块自动装配测试。
 * <p>
 * 重点验证「彻底透明替换」的核心不变量：
 * <ol>
 *   <li>当主框架的默认注册中心用 {@code @ConditionalOnMissingBean(PluginServiceRegistry.class)}
 *       提供时，启用分布式模块后全局<b>恰好只有一个</b> {@link PluginServiceRegistry} Bean，
 *       且它是分布式门面 {@link DistributedPluginServiceRegistry}（本地优先/远端兜底）；</li>
 *   <li>HOST 角色不启动 gRPC 服务端、不启动注册调度器；</li>
 *   <li>WORKER 角色额外装配 gRPC 服务端与注册调度器。</li>
 * </ol>
 */
class DistributedAutoConfigurationTest {

    /**
     * 模拟主框架 {@code ExtendPointConfiguration}：以自动配置形式、且在分布式自动配置
     * <b>之后</b>求值（对应真实环境 {@link DistributedPluginAutoConfiguration} 声明了
     * {@code @AutoConfigureBefore(SpringBootPluginStarter)}），用 {@code @ConditionalOnMissingBean}
     * 提供默认本地注册中心。启用分布式后应被门面取代，从而验证只有一个注册中心。
     */
    @AutoConfiguration
    @org.springframework.boot.autoconfigure.AutoConfigureAfter(
            com.zqzqq.bootkits.distributed.config.DistributedPluginAutoConfiguration.class)
    static class SimulatedMainFrameworkConfig {
        @Bean
        @ConditionalOnMissingBean(PluginServiceRegistry.class)
        public PluginServiceRegistry pluginServiceRegistry() {
            return new DefaultPluginServiceRegistry();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DistributedPluginAutoConfiguration.class,
                    SimulatedMainFrameworkConfig.class));

    /** 提供 StringRedisTemplate，否则 @ConditionalOnBean 不会激活本配置。 */
    private ApplicationContextRunner withRedisAndSimulatedMain(String role, String nodeId) {
        return runner
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withPropertyValues(
                        "plugin.distributed.enabled=true",
                        "plugin.distributed.role=" + role,
                        "plugin.distributed.node-id=" + nodeId,
                        "plugin.distributed.port=19090");
    }

    @Test
    void shouldExposeExactlyOneDistributedPluginServiceRegistry() {
        withRedisAndSimulatedMain("HOST", "test-host")
                .run(context -> {
                    // 全局只有一个 PluginServiceRegistry Bean
                    String[] names = context.getBeanNamesForType(PluginServiceRegistry.class);
                    assertThat(names).hasSize(1);

                    PluginServiceRegistry registry = context.getBean(PluginServiceRegistry.class);
                    assertThat(registry).isInstanceOf(DistributedPluginServiceRegistry.class);
                });
    }

    @Test
    void shouldNotRegisterWorkerComponentsForHostRole() {
        withRedisAndSimulatedMain("HOST", "test-host")
                .run(context -> {
                    assertThat(context.getBeanNamesForType(GrpcServerBootstrap.class)).isEmpty();
                    assertThat(context.getBeanNamesForType(PluginInvocationServiceImpl.class)).isEmpty();
                    // 宿主仍具备调用方能力
                    assertThat(context.getBeanNamesForType(GrpcClientProvider.class)).hasSize(1);
                    assertThat(context.getBeanNamesForType(DistributedServiceLocator.class)).hasSize(1);
                });
    }

    @Test
    void workerRoleShouldStartGrpcServerAndRegistryScheduler() {
        withRedisAndSimulatedMain("WORKER", "test-worker")
                .run(context -> {
                    assertThat(context.getBeanNamesForType(GrpcServerBootstrap.class)).hasSize(1);
                    assertThat(context.getBeanNamesForType(PluginInvocationServiceImpl.class)).hasSize(1);
                    assertThat(context.getBean(GrpcServerBootstrap.class)).isNotNull();
                });
    }

    @Test
    void shouldBeInactiveWithoutEnabledFlag() {
        runner
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withPropertyValues("plugin.distributed.role=HOST")
                .run(context -> {
                    assertThat(context.getBeanNamesForType(GrpcClientProvider.class)).isEmpty();
                    assertThat(context.getBeanNamesForType(DistributedServiceLocator.class)).isEmpty();
                });
    }
}