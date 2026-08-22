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


package com.zqzqq.bootkits.core.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 插件性能指标收集器
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginMetrics {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginMetrics.class);
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> pluginCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pluginMemoryUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> pluginTimers = new ConcurrentHashMap<>();
    
    // 计数器
    private final Counter pluginInstallCounter;
    private final Counter pluginUninstallCounter;
    private final Counter pluginStartCounter;
    private final Counter pluginStopCounter;
    private final Counter pluginErrorCounter;
    
    // 定时器
    private final Timer pluginLoadTimer;
    private final Timer pluginStartTimer;
    private final Timer pluginStopTimer;
    
    public PluginMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化计数器
        this.pluginInstallCounter = Counter.builder("plugin.install.total")
                .description("Total number of plugin installations")
                .register(meterRegistry);
                
        this.pluginUninstallCounter = Counter.builder("plugin.uninstall.total")
                .description("Total number of plugin uninstallations")
                .register(meterRegistry);
                
        this.pluginStartCounter = Counter.builder("plugin.start.total")
                .description("Total number of plugin starts")
                .register(meterRegistry);
                
        this.pluginStopCounter = Counter.builder("plugin.stop.total")
                .description("Total number of plugin stops")
                .register(meterRegistry);
                
        this.pluginErrorCounter = Counter.builder("plugin.error.total")
                .description("Total number of plugin errors")
                .register(meterRegistry);
        
        // 初始化定时器
        this.pluginLoadTimer = Timer.builder("plugin.load.duration")
                .description("Plugin loading duration")
                .register(meterRegistry);
                
        this.pluginStartTimer = Timer.builder("plugin.start.duration")
                .description("Plugin startup duration")
                .register(meterRegistry);
                
        this.pluginStopTimer = Timer.builder("plugin.stop.duration")
                .description("Plugin stop duration")
                .register(meterRegistry);
        
        // 注册插件数量指标
        Gauge.builder("plugin.active.count", () -> getActivePluginCount())
                .description("Number of active plugins")
                .register(meterRegistry);
                
        Gauge.builder("plugin.total.memory.usage", () -> getTotalMemoryUsage())
                .description("Total memory usage of all plugins in bytes")
                .register(meterRegistry);
    }
    
    /**
     * 记录插件安装
     */
    public void recordPluginInstall(String pluginId) {
        pluginInstallCounter.increment();
        pluginCounts.put(pluginId, new AtomicInteger(1));
        log.debug("Recorded plugin install: {}", pluginId);
    }
    
    /**
     * 记录插件卸载
     */
    public void recordPluginUninstall(String pluginId) {
        pluginUninstallCounter.increment();
        pluginCounts.remove(pluginId);
        pluginMemoryUsage.remove(pluginId);
        pluginTimers.remove(pluginId);
        log.debug("Recorded plugin uninstall: {}", pluginId);
    }
    
    /**
     * 记录插件启动
     */
    public void recordPluginStart(String pluginId) {
        pluginStartCounter.increment();
        log.debug("Recorded plugin start: {}", pluginId);
    }
    
    /**
     * 记录插件停止
     */
    public void recordPluginStop(String pluginId) {
        pluginStopCounter.increment();
        log.debug("Recorded plugin stop: {}", pluginId);
    }
    
    /**
     * 记录插件错误
     */
    public void recordPluginError(String pluginId, String errorType) {
        pluginErrorCounter.increment();
        log.warn("Recorded plugin error: {} - {}", pluginId, errorType);
    }
    
    /**
     * 记录插件加载时间
     */
    public Timer.Sample startLoadTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 完成插件加载时间记录
     */
    public void recordLoadTime(Timer.Sample sample) {
        sample.stop(pluginLoadTimer);
    }
    
    /**
     * 记录插件启动时间
     */
    public Timer.Sample startStartupTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 完成插件启动时间记录
     */
    public void recordStartupTime(Timer.Sample sample) {
        sample.stop(pluginStartTimer);
    }
    
    /**
     * 记录插件停止时间
     */
    public Timer.Sample startStopTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 完成插件停止时间记录
     */
    public void recordStopTime(Timer.Sample sample) {
        sample.stop(pluginStopTimer);
    }
    
    /**
     * 更新插件内存使用量
     */
    public void updatePluginMemoryUsage(String pluginId, long memoryBytes) {
        pluginMemoryUsage.put(pluginId, new AtomicLong(memoryBytes));
        
        // 为特定插件注册内存使用量指标
        Gauge.builder("plugin.memory.usage", () -> (double) getPluginMemoryUsage(pluginId))
                .description("Memory usage of specific plugin in bytes")
                .tag("plugin.id", pluginId)
                .register(meterRegistry);
    }
    
    /**
     * 获取活跃插件数量
     */
    public int getActivePluginCount() {
        return pluginCounts.size();
    }
    
    /**
     * 获取总内存使用量
     */
    public long getTotalMemoryUsage() {
        return pluginMemoryUsage.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
    
    /**
     * 获取特定插件的内存使用量
     */
    public long getPluginMemoryUsage(String pluginId) {
        AtomicLong usage = pluginMemoryUsage.get(pluginId);
        return usage != null ? usage.get() : 0L;
    }
    
    /**
     * 创建插件特定的定时器
     */
    public Timer getPluginTimer(String pluginId, String operation) {
        String key = pluginId + "." + operation;
        return pluginTimers.computeIfAbsent(key, k -> 
            Timer.builder("plugin.operation.duration")
                    .description("Plugin operation duration")
                    .tag("plugin.id", pluginId)
                    .tag("operation", operation)
                    .register(meterRegistry));
    }
    
    /**
     * 清理所有指标
     */
    public void clear() {
        pluginCounts.clear();
        pluginMemoryUsage.clear();
        pluginTimers.clear();
        log.info("Plugin metrics cleared");
    }
}