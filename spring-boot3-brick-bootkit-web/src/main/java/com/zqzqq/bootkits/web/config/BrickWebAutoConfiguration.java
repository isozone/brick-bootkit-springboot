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


package com.zqzqq.bootkits.web.config;

import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationDecision;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Brick Web 自动配置类
 */
@Slf4j
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "plugin.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BrickWebProperties.class)
public class BrickWebAutoConfiguration implements WebMvcConfigurer {

    private static final String DEFAULT_PAGE_PREFIX = "/plugins-web";

    private final BrickWebProperties properties;

    public BrickWebAutoConfiguration(BrickWebProperties properties,
                                     ObjectProvider<IntegrationConfiguration> integrationConfigurationProvider) {
        this.properties = properties;
        initializeProperties(integrationConfigurationProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginWebAuthorizer pluginWebAuthorizer() {
        return new PluginWebAuthorizer() {
            @Override
            public PluginWebAuthorizationDecision authorize(
                    com.zqzqq.bootkits.web.auth.PluginWebAuthorizationContext context) {
                return PluginWebAuthorizationDecision.deny("host authorizer is required");
            }

            @Override
            public boolean isFallback() {
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginWebAuthorizationService pluginWebAuthorizationService(BrickWebProperties properties,
                                                                       PluginWebAuthorizer pluginWebAuthorizer) {
        return new PluginWebAuthorizationService(properties, pluginWebAuthorizer);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/")
                .resourceChain(true);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true);

        if (!properties.isEnableUI()) {
            return;
        }

        for (String pagePrefix : resolvePagePrefixes()) {
            registry.addResourceHandler(pagePrefix + "/**")
                    .addResourceLocations("classpath:/static/plugins-web/")
                    .resourceChain(true);
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (!properties.isEnableUI()) {
            return;
        }
        registry.addRedirectViewController("/", normalizePagePrefix(properties.getPagePrefix()) + "/");
    }

    private void initializeProperties(IntegrationConfiguration integrationConfiguration) {
        if (integrationConfiguration != null) {
            if (properties.getPluginPaths() == null || properties.getPluginPaths().isEmpty()) {
                properties.setPluginPaths(integrationConfiguration.pluginPath());
            }
            if (!StringUtils.hasText(properties.getUploadTempPath())) {
                properties.setUploadTempPath(integrationConfiguration.uploadTempPath());
            }
            if (!StringUtils.hasText(properties.getBackupPath())) {
                properties.setBackupPath(integrationConfiguration.backupPath());
            }
            if (!StringUtils.hasText(properties.getPluginRestPathPrefix())) {
                properties.setPluginRestPathPrefix(integrationConfiguration.pluginRestPathPrefix());
            }
            log.info("Brick Web 配置已从 IntegrationConfiguration 合并缺省值");
        }

        if (properties.getPluginPaths() == null) {
            properties.setPluginPaths(Collections.emptyList());
        }
        if (!StringUtils.hasText(properties.getUploadTempPath())) {
            properties.setUploadTempPath(System.getProperty("java.io.tmpdir"));
        }
        if (!StringUtils.hasText(properties.getBackupPath())) {
            properties.setBackupPath(System.getProperty("java.io.tmpdir") + "/brick-backup");
        }
        if (!StringUtils.hasText(properties.getPluginRestPathPrefix())) {
            properties.setPluginRestPathPrefix("/api/brick");
        }
    }

    private List<String> resolvePagePrefixes() {
        LinkedHashSet<String> prefixes = new LinkedHashSet<>();
        prefixes.add(normalizePagePrefix(DEFAULT_PAGE_PREFIX));
        prefixes.add(normalizePagePrefix(properties.getPagePrefix()));
        return new ArrayList<>(prefixes);
    }

    private String normalizePagePrefix(String pagePrefix) {
        String normalized = StringUtils.hasText(pagePrefix) ? pagePrefix.trim() : DEFAULT_PAGE_PREFIX;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
