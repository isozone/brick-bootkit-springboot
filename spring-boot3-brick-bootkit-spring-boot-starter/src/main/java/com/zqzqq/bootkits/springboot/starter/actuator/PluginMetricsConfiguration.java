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


package com.zqzqq.bootkits.springboot.starter.actuator;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.monitoring.PluginMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 插件 Metrics 配置
 * 注册插件相关的 Prometheus 指标
 * 基于主框架真实运行时（com.zqzqq.bootkits.core.PluginManager）
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@Component
public class PluginMetricsConfiguration {

    private final PluginMetrics pluginMetrics;
    private final PluginManager pluginManager;
    private final MeterRegistry meterRegistry;

    public PluginMetricsConfiguration(
            PluginMetrics pluginMetrics,
            PluginManager pluginManager,
            MeterRegistry meterRegistry) {
        this.pluginMetrics = pluginMetrics;
        this.pluginManager = pluginManager;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerPluginMetrics() {
        // 注册插件数量指标（基于真实运行时已启动插件数）
        io.micrometer.core.instrument.Gauge.builder(
                "plugin.active.count",
                () -> {
                    List<PluginInfo> startedPlugins = pluginManager == null ? null : pluginManager.getStartedPlugins();
                    return startedPlugins == null ? 0.0 : startedPlugins.size();
                }
        ).description("当前活跃插件数量").register(meterRegistry);

        // 注册插件内存使用指标
        io.micrometer.core.instrument.Gauge.builder(
                "plugin.total.memory.usage",
                pluginMetrics,
                PluginMetrics::getTotalMemoryUsage
        ).description("插件总内存使用（字节）").register(meterRegistry);

        log.info("插件 Prometheus Metrics 已注册");
    }
}
