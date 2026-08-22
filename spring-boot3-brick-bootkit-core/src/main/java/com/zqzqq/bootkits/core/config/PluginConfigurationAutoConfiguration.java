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


package com.zqzqq.bootkits.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 插件配置管理自动配置
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PluginConfigurationProperties.class)
@ConditionalOnProperty(prefix = "plugin.configuration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PluginConfigurationAutoConfiguration {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginConfigurationAutoConfiguration.class);
    
    @Bean
    public PluginConfigurationManager pluginConfigurationManager(
            ApplicationEventPublisher eventPublisher,
            PluginConfigurationProperties properties) {
        
        log.info("Initializing Plugin Configuration Manager with properties: {}", properties);
        return new PluginConfigurationManager(eventPublisher, properties);
    }
}