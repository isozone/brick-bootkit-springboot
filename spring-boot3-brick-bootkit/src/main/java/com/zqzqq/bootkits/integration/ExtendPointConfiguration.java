/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.core.DefaultRealizeProvider;
import com.zqzqq.bootkits.core.RealizeProvider;
import com.zqzqq.bootkits.core.classloader.DefaultMainResourceMatcher;
import com.zqzqq.bootkits.core.classloader.MainResourceMatcher;
import com.zqzqq.bootkits.core.descriptor.decrypt.DefaultPluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.descriptor.decrypt.PluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.launcher.plugin.DefaultMainResourcePatternDefiner;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorService;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorStartupReporter;
import com.zqzqq.bootkits.integration.operator.DefaultPluginOperator;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import com.zqzqq.bootkits.integration.operator.PluginOperatorWrapper;
import com.zqzqq.bootkits.integration.user.DefaultPluginUser;
import com.zqzqq.bootkits.integration.user.PluginUser;
import com.zqzqq.bootkits.spring.extract.ExtractFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
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
    public PluginDoctorService pluginDoctorService(ObjectProvider<com.zqzqq.bootkits.core.PluginManager> pluginManagerProvider) {
        return new PluginDoctorService(configuration, pluginManagerProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginDoctorStartupReporter pluginDoctorStartupReporter(PluginDoctorService pluginDoctorService) {
        return new PluginDoctorStartupReporter(pluginDoctorService);
    }

}

