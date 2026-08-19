package com.zqzqq.bootkits.distributed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.DistributedServiceLocator;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import com.zqzqq.bootkits.distributed.registry.DistributedPluginServiceRegistry;
import com.zqzqq.bootkits.distributed.registry.RedisServiceDirectory;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.registry.ServiceRegistrationScheduler;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * 分布式插件自动配置。
 * <p>
 * 启用条件：{@code plugin.distributed.enabled=true} 且存在 {@link StringRedisTemplate}。
 *
 * <p><b>设计目标：完全透明，且只有一个 {code PluginServiceRegistry} Bean。</b></p>
 * 主框架在 {@code ExtendPointConfiguration} 中通过
 * {@code @ConditionalOnMissingBean(PluginServiceRegistry.class)} 提供「默认本地注册中心」。
 * 本配置用 {@link DistributedPluginServiceRegistry} 以同名 {@code pluginServiceRegistry}
 * Bean 并抢占该条件，从而<b>彻底替换</b>主框架默认实现：
 * <ul>
 *   <li>全局仅存在一个 {@code PluginServiceRegistry} Bean（门面），无 {@code @Primary} 歧义、
 *       无重复 Bean、无自引用循环；</li>
 *   <li>原有 {@code @Autowired PluginServiceRegistry} / {@code ObjectProvider<PluginServiceRegistry>}
 *       的代码自动获得「本地优先、远端兜底」能力，调用方零改动；</li>
 *   <li>门面内部持有 {@link DefaultPluginServiceRegistry} 作为本地底座（执行节点的本地插件用其执行）。</li>
 * </ul>
 * 为保证门面在 {@code SpringBootPluginStarter} 的 {@code @ConditionalOnMissingBean} 求值前完成定义，
 * 本配置声明 {@code @AutoConfigureBefore(SpringBootPluginStarter.class)}。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DistributedPluginProperties.class)
@ConditionalOnProperty(prefix = "plugin.distributed", name = "enabled", havingValue = "true")
@ConditionalOnBean(StringRedisTemplate.class)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@AutoConfigureBefore(name = "com.zqzqq.bootkits.integration.SpringBootPluginStarter")
public class DistributedPluginAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DistributedPluginAutoConfiguration.class);

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationContext applicationContext;

    private GrpcServerBootstrap serverBootstrap;
    private ServiceRegistrationScheduler registrationScheduler;

    // ==================== 服务目录 ====================

    @Bean
    public ServiceDirectory distributedServiceDirectory(
            StringRedisTemplate redis,
            ObjectProvider<ObjectMapper> mapperProvider,
            DistributedPluginProperties properties) {
        ObjectMapper mapper = mapperProvider.getIfAvailable(ObjectMapper::new);
        return new RedisServiceDirectory(redis, mapper,
                properties.getRegistryPrefix(),
                properties.getHeartbeatTtlSeconds());
    }

    // ==================== 调用方（宿主/HOST 与 WORKER 共用） ====================

    @Bean
    public GrpcClientProvider distributedGrpcClientProvider(DistributedPluginProperties properties) {
        return new GrpcClientProvider(
                properties.getMaxInboundMessageSize(),
                properties.getCallTimeoutMillis(),
                properties.isTlsEnabled(),
                properties.getTlsCaCertPath(),
                properties.getAuthToken());
    }

    @Bean
    public RemoteServiceProxyFactory distributedRemoteServiceProxyFactory(
            ServiceDirectory directory,
            GrpcClientProvider clientProvider) {
        return new RemoteServiceProxyFactory(directory, clientProvider);
    }

    /**
     * 分布式门面：作为全局<b>唯一</b>的 {@link PluginServiceRegistry} Bean。
     * <p>
     * 返回类型是 {@link PluginServiceRegistry} 的子类型，配合
     * {@code @AutoConfigureBefore(SpringBootPluginStarter)}，主框架的
     * {@code @ConditionalOnMissingBean(PluginServiceRegistry.class)} 会识别到本 Bean
     * 从而跳过创建默认实现——于是<b>全局只有一个 PluginServiceRegistry</b>，
     * 既无重复 Bean/歧义，又无需 {@code @Primary}。
     * <p>
     * 内部自行创建 {@link DefaultPluginServiceRegistry} 作为本地底座
     * （执行节点的本地插件用它执行；门面对外统一做「本地优先、远端兜底」）。
     */
    @Bean
    public DistributedPluginServiceRegistry distributedPluginServiceRegistry(
            ServiceDirectory directory,
            RemoteServiceProxyFactory remoteServiceProxyFactory) {
        DefaultPluginServiceRegistry local = new DefaultPluginServiceRegistry();
        return new DistributedPluginServiceRegistry(local, directory, remoteServiceProxyFactory);
    }

    /**
     * 面向调用方的一站式入口 Bean。只要模块启用即可 {@code @Autowired} 或
     * 通过静态方法 {@link DistributedServiceLocator#service} 使用，不依赖组件扫描。
     */
    @Bean
    public DistributedServiceLocator distributedServiceLocator() {
        return new DistributedServiceLocator();
    }

    // ==================== 执行节点（仅 WORKER） ====================
    // 注意：这里注入的 PluginServiceRegistry 就是全局唯一的分布式门面（本地优先），
    // 因此本地插件执行与注册收集都走门面的「本地」分支，无需额外原始注册中心。

    @Bean
    @ConditionalOnProperty(prefix = "plugin.distributed", name = "role", havingValue = "WORKER")
    public PluginInvocationServiceImpl distributedPluginInvocationService(
            PluginServiceRegistry registry) {
        return new PluginInvocationServiceImpl(registry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "plugin.distributed", name = "role", havingValue = "WORKER")
    public GrpcServerBootstrap distributedGrpcServerBootstrap(
            DistributedPluginProperties properties,
            PluginInvocationServiceImpl invocationService) {
        GrpcServerBootstrap bootstrap = new GrpcServerBootstrap(
                properties.getHost(),
                properties.getPort(),
                properties.getMaxInboundMessageSize(),
                invocationService,
                properties.isTlsEnabled(),
                properties.getTlsCertChainPath(),
                properties.getTlsPrivateKeyPath(),
                properties.getAuthToken());
        try {
            bootstrap.start();
        } catch (IOException e) {
            throw new IllegalStateException("分布式插件 gRPC 服务启动失败，端口: " + properties.getPort(), e);
        }
        this.serverBootstrap = bootstrap;
        log.info("分布式插件 WORKER 节点 gRPC 服务已启动，端口: {}", properties.getPort());
        return bootstrap;
    }

    @Bean
    @ConditionalOnProperty(prefix = "plugin.distributed", name = "role", havingValue = "WORKER")
    public ServiceRegistrationScheduler distributedServiceRegistrationScheduler(
            PluginServiceRegistry registry,
            ServiceDirectory directory,
            DistributedPluginProperties properties) {
        // 对外可达/监听地址：显式配置 host 则使用（多网卡场景建议如此），否则自动解析局域网 IPv4。
        String host = (properties.getHost() != null && !properties.getHost().isEmpty())
                ? properties.getHost()
                : ServiceRegistrationScheduler.resolveHost();
        String nodeId = resolveNodeId(properties, host);
        ServiceRegistrationScheduler scheduler = new ServiceRegistrationScheduler(
                registry,
                directory,
                nodeId,
                host,
                properties.getPort(),
                properties.getHeartbeatIntervalSeconds());
        scheduler.start();
        this.registrationScheduler = scheduler;
        return scheduler;
    }

    /**
     * 解析稳定的节点 ID：优先取配置的 {@code plugin.distributed.node-id}，
     * 其次尝试复用框架 {@code ClusterNodeRegistry} 的节点 ID（若已启用集群），
     * 最后回退「host:port」，保证跨重启稳定、不残留陈旧注册。
     */
    private String resolveNodeId(DistributedPluginProperties properties, String host) {
        if (properties.getNodeId() != null && !properties.getNodeId().isEmpty()) {
            return properties.getNodeId();
        }
        try {
            Object clusterNode = applicationContext.getBean("clusterNodeRegistry");
            java.lang.reflect.Method get = clusterNode.getClass().getMethod("getNodeId");
            Object id = get.invoke(clusterNode);
            if (id != null && !id.toString().isEmpty()) {
                return id.toString();
            }
        } catch (Exception ignored) {
            // 未启用集群，走 host:port 回退
        }
        return host + ":" + properties.getPort();
    }

    @PreDestroy
    public void destroy() {
        if (registrationScheduler != null) {
            registrationScheduler.shutdown();
        }
        if (serverBootstrap != null) {
            serverBootstrap.shutdown();
        }
    }
}