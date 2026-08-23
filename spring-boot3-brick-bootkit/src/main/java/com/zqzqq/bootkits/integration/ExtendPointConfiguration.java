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



package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.core.DefaultRealizeProvider;
import com.zqzqq.bootkits.core.RealizeProvider;
import com.zqzqq.bootkits.core.classloader.DefaultMainResourceMatcher;
import com.zqzqq.bootkits.core.classloader.MainResourceMatcher;
import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.config.PluginConfigurationManager;
import com.zqzqq.bootkits.core.descriptor.decrypt.DefaultPluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.descriptor.decrypt.PluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.dependency.PluginDependencyManager;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.isolation.PluginResourceIsolation;
import com.zqzqq.bootkits.core.isolation.PluginResourceMonitor;
import com.zqzqq.bootkits.core.isolation.QuotaManager;
import com.zqzqq.bootkits.core.launcher.plugin.DefaultMainResourcePatternDefiner;
import com.zqzqq.bootkits.core.performance.PerformanceThresholds;
import com.zqzqq.bootkits.core.performance.PluginPerformanceAnalyzer;
import com.zqzqq.bootkits.core.sandbox.PluginSandbox;
import com.zqzqq.bootkits.core.security.PluginSecurityConfiguration;
import com.zqzqq.bootkits.core.security.PluginSecurityManager;
import com.zqzqq.bootkits.integration.cluster.ClusterLifecycleExtension;
import com.zqzqq.bootkits.integration.cluster.ClusterNodeRegistry;
import com.zqzqq.bootkits.integration.cluster.PluginClusterStateSync;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorService;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorStartupReporter;
import com.zqzqq.bootkits.integration.operator.DefaultPluginOperator;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import com.zqzqq.bootkits.integration.operator.PluginOperatorWrapper;
import com.zqzqq.bootkits.integration.registry.ServiceRegistryLifecycleExtension;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.integration.rollout.RolloutModeCanaryRoutingResolver;
import com.zqzqq.bootkits.integration.security.PluginSecurityAdmissionCheck;
import com.zqzqq.bootkits.integration.user.DefaultPluginUser;
import com.zqzqq.bootkits.integration.user.PluginUser;
import com.zqzqq.bootkits.spring.extract.ExtractFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统 Bean 配置
 * @author starBlues
 * @version 3.0.3
 */
@Configuration
public class ExtendPointConfiguration {

    private final GenericApplicationContext applicationContext;
    private final IntegrationConfiguration configuration;

    public ExtendPointConfiguration(GenericApplicationContext applicationContext,
                                    IntegrationConfiguration configuration) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        autoDetectMainPackageIfPossible();
        this.configuration.checkConfig();
    }

    private void autoDetectMainPackageIfPossible() {
        if (StringUtils.hasText(configuration.mainPackage())) {
            return;
        }
        if (!(configuration instanceof AutoIntegrationConfiguration autoConfiguration)) {
            return;
        }
        try {
            List<String> packages = AutoConfigurationPackages.get(applicationContext.getDefaultListableBeanFactory());
            if (!packages.isEmpty() && StringUtils.hasText(packages.get(0))) {
                autoConfiguration.setMainPackage(packages.get(0));
            }
        } catch (Exception ignored) {
            // Fall back to explicit configuration validation below.
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginUser createPluginUser() {
        return new DefaultPluginUser(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginOperator createPluginOperator(RealizeProvider realizeProvider) {
        PluginOperator pluginOperator = new DefaultPluginOperator(
                applicationContext,
                realizeProvider,
                configuration
        );
        return new PluginOperatorWrapper(pluginOperator, configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    public RealizeProvider realizeProvider() {
        DefaultRealizeProvider defaultRealizeProvider = new DefaultRealizeProvider(configuration, applicationContext);
        defaultRealizeProvider.init();
        return defaultRealizeProvider;
    }

    @Bean
    public ExtractFactory extractFactory(){
        return ExtractFactory.getInstant();
    }

    @Bean
    public MainResourceMatcher mainResourceMatcher(){
        return new DefaultMainResourceMatcher(new DefaultMainResourcePatternDefiner(
                configuration,
                applicationContext
        ));
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginDescriptorDecrypt pluginDescriptorDecrypt(){
        return new DefaultPluginDescriptorDecrypt(applicationContext, configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.zqzqq.bootkits.core.PluginManager pluginManager(PluginOperator pluginOperator) {
        if (pluginOperator instanceof PluginOperatorWrapper) {
            return ((PluginOperatorWrapper) pluginOperator).getPluginManager();
        } else if (pluginOperator instanceof DefaultPluginOperator) {
            return ((DefaultPluginOperator) pluginOperator).getPluginManager();
        }
        return null;
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginDoctorService pluginDoctorService(ObjectProvider<com.zqzqq.bootkits.core.PluginManager> pluginManagerProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.security.PluginSecurityManager> securityManagerProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.communication.PluginServiceRegistry> serviceRegistryProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.config.PluginConfigurationManager> configurationManagerProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.eventbus.PluginEventBus> eventBusProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.dependency.PluginDependencyManager> dependencyManagerProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.core.performance.PluginPerformanceAnalyzer> performanceAnalyzerProvider,
                                                   ObjectProvider<com.zqzqq.bootkits.integration.cluster.ClusterNodeRegistry> clusterNodeRegistryProvider,
                                                   List<PluginRolloutProbe> rolloutProbes) {
        return new PluginDoctorService(configuration,
                pluginManagerProvider,
                securityManagerProvider,
                serviceRegistryProvider,
                configurationManagerProvider,
                eventBusProvider,
                dependencyManagerProvider,
                performanceAnalyzerProvider,
                clusterNodeRegistryProvider,
                rolloutProbes);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginDoctorStartupReporter pluginDoctorStartupReporter(PluginDoctorService pluginDoctorService) {
        return new PluginDoctorStartupReporter(pluginDoctorService);
    }

    // ==================== 插件安全中心 ====================

    @Bean
    @ConditionalOnMissingBean
    public PluginSecurityConfiguration pluginSecurityConfiguration() {
        return new PluginSecurityConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginSecurityManager pluginSecurityManager(PluginSecurityConfiguration securityConfiguration) {
        return new PluginSecurityManager(securityConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginSandbox pluginSandbox() {
        return new PluginSandbox();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginSecurityAdmissionCheck pluginSecurityAdmissionCheck(PluginSecurityManager securityManager) {
        return new PluginSecurityAdmissionCheck(securityManager);
    }

    // ==================== 插件服务注册中心 ====================

    @Bean
    @ConditionalOnMissingBean
    public PluginServiceRegistry pluginServiceRegistry(IntegrationConfiguration integrationConfiguration) {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        registry.setCanaryRoutingResolver(new RolloutModeCanaryRoutingResolver(integrationConfiguration));
        return registry;
    }

    /**
     * 服务注册自动接线扩展：插件启动时自动扫描 @PluginService / @BrickService 注解的 Bean
     * 并注册到服务注册中心；插件停止时自动注销。
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistryLifecycleExtension serviceRegistryLifecycleExtension(
            ObjectProvider<PluginServiceRegistry> pluginServiceRegistryProvider) {
        return new ServiceRegistryLifecycleExtension(pluginServiceRegistryProvider);
    }

    // ==================== 插件配置热更新 ====================
    // 说明：PluginConfigurationManager 由 core 模块的 PluginConfigurationAutoConfiguration 注册，
    // 此处仅保留配置属性绑定（@EnableConfigurationProperties），不重复注册 Bean，
    // 避免 BeanDefinitionOverrideException。

    // ==================== 插件性能分析与资源隔离 ====================

    @Bean
    @ConditionalOnMissingBean
    public QuotaManager quotaManager() {
        return new QuotaManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginResourceMonitor pluginResourceMonitor() {
        return new PluginResourceMonitor();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginResourceIsolation pluginResourceIsolation(QuotaManager quotaManager,
                                                           PluginResourceMonitor resourceMonitor) {
        return new PluginResourceIsolation(quotaManager, resourceMonitor);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginPerformanceAnalyzer pluginPerformanceAnalyzer() {
        return new PluginPerformanceAnalyzer(PerformanceThresholds.defaultThresholds());
    }

    // ==================== 插件依赖分析 ====================

    @Bean
    @ConditionalOnMissingBean
    public PluginDependencyManager pluginDependencyManager() {
        return new PluginDependencyManager();
    }

    // ==================== 插件事件总线 ====================

    @Bean
    @ConditionalOnMissingBean
    public PluginEventBus pluginEventBus() {
        return new PluginEventBus();
    }

    // ==================== 集群节点注册与插件状态同步 ====================

    @Bean
    @ConditionalOnMissingBean
    public ClusterNodeRegistry clusterNodeRegistry() {
        return new ClusterNodeRegistry(resolveClusterSharedRoot(), configuration.clusterWebBaseUrl());
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginClusterStateSync pluginClusterStateSync() {
        return new PluginClusterStateSync(resolveClusterSharedRoot());
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterLifecycleExtension clusterLifecycleExtension(PluginClusterStateSync pluginClusterStateSync,
                                                               ClusterNodeRegistry clusterNodeRegistry) {
        return new ClusterLifecycleExtension(pluginClusterStateSync, clusterNodeRegistry);
    }

    /**
     * 解析集群共享根目录：优先使用 cluster.shared-path 配置，否则取第一个插件根目录。
     */
    private java.nio.file.Path resolveClusterSharedRoot() {
        String sharedPath = configuration.clusterSharedPath();
        if (StringUtils.hasText(sharedPath)) {
            return java.nio.file.Paths.get(sharedPath).toAbsolutePath().normalize();
        }
        List<String> pluginPaths = configuration.pluginPath();
        if (pluginPaths != null && !pluginPaths.isEmpty() && StringUtils.hasText(pluginPaths.get(0))) {
            return java.nio.file.Paths.get(pluginPaths.get(0)).toAbsolutePath().normalize();
        }
        return java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

}

