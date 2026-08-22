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


package com.zqzqq.bootkits.integration.monitoring;

import com.zqzqq.bootkits.core.monitoring.PluginMetrics;
import com.zqzqq.bootkits.core.monitoring.PluginMonitoringConfiguration;
import com.zqzqq.bootkits.core.monitoring.PluginPerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 插件监控自动配置
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class})
@ConditionalOnProperty(prefix = "plugin.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PluginMonitoringProperties.class)
public class PluginMonitoringAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(PluginMonitoringAutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean
    public PluginMonitoringConfiguration pluginMonitoringConfiguration(PluginMonitoringProperties properties) {
        PluginMonitoringConfiguration config = new PluginMonitoringConfiguration();
        
        // 从properties复制配置
        config.setEnabled(properties.isEnabled());
        config.setCollectionInterval(properties.getCollectionInterval());
        config.setMemoryMonitoringEnabled(properties.isMemoryMonitoringEnabled());
        config.setCpuMonitoringEnabled(properties.isCpuMonitoringEnabled());
        config.setThreadMonitoringEnabled(properties.isThreadMonitoringEnabled());
        config.setClassLoadingMonitoringEnabled(properties.isClassLoadingMonitoringEnabled());
        config.setGcMonitoringEnabled(properties.isGcMonitoringEnabled());
        config.setMemoryWarningThreshold(properties.getMemoryWarningThreshold());
        config.setMemoryCriticalThreshold(properties.getMemoryCriticalThreshold());
        config.setCpuWarningThreshold(properties.getCpuWarningThreshold());
        config.setCpuCriticalThreshold(properties.getCpuCriticalThreshold());
        config.setThreadWarningThreshold(properties.getThreadWarningThreshold());
        config.setThreadCriticalThreshold(properties.getThreadCriticalThreshold());
        config.setPerformanceReportEnabled(properties.isPerformanceReportEnabled());
        config.setPerformanceReportInterval(properties.getPerformanceReportInterval());
        config.setSlowOperationDetectionEnabled(properties.isSlowOperationDetectionEnabled());
        config.setSlowOperationThreshold(properties.getSlowOperationThreshold());
        config.setHistoryRetentionDays(properties.getHistoryRetentionDays());
        config.setRealTimeMonitoringEnabled(properties.isRealTimeMonitoringEnabled());
        config.setRealTimeUpdateInterval(properties.getRealTimeUpdateInterval());
        
        config.validate();
        
        log.info("Plugin monitoring configuration initialized: enabled={}, interval={}s", 
                config.isEnabled(), config.getCollectionInterval());
        
        return config;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PluginMetrics pluginMetrics(MeterRegistry meterRegistry) {
        log.info("Initializing plugin metrics with MeterRegistry: {}", meterRegistry.getClass().getSimpleName());
        return new PluginMetrics(meterRegistry);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PluginPerformanceMonitor pluginPerformanceMonitor(PluginMetrics pluginMetrics) {
        log.info("Initializing plugin performance monitor");
        return new PluginPerformanceMonitor(pluginMetrics);
    }
}