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


package com.zqzqq.bootkits.springboot.starter;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.springboot.starter.properties.BrickBootkitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brick BootKit Spring Boot Starter
 * 统一基于主框架（spring-boot3-brick-bootkit）的真实运行时：
 * 注入真实 {@link com.zqzqq.bootkits.core.PluginManager}，不再使用 core 模块弃用的简化版。
 * 插件生命周期（加载/启动/停止）由主框架的自动装配（SpringBootPluginStarter）负责，
 * 本类只做事件桥接与旧配置（brick-bootkit.plugin-path）的兼容兜底。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BrickBootkitProperties.class)
public class BrickBootkitAutoConfiguration implements ApplicationRunner {

    private final BrickBootkitProperties properties;
    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final PluginEventBus eventBus;
    private final Map<String, PluginInfo> loadedPlugins = new ConcurrentHashMap<>();

    public BrickBootkitAutoConfiguration(BrickBootkitProperties properties,
                                         ObjectProvider<PluginManager> pluginManagerProvider,
                                         ObjectProvider<PluginEventBus> eventBusProvider) {
        this.properties = properties;
        this.pluginManagerProvider = pluginManagerProvider;
        this.eventBus = eventBusProvider.getIfAvailable(PluginEventBus::new);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginEventBus pluginEventBus() {
        return new PluginEventBus();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Brick BootKit 插件框架已禁用");
            return;
        }

        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            log.warn("未装配主框架 PluginManager，请确认已引入 spring-boot3-brick-bootkit 且 plugin.enable=true");
            return;
        }
        log.info("Brick BootKit Spring Boot Starter 初始化中（主框架真实运行时）...");

        // 主框架（SpringBootPluginStarter）已在 ApplicationStartedEvent 时自动加载插件，直接同步状态
        List<PluginInfo> existing = pluginManager.getPlugins();
        if (!existing.isEmpty()) {
            for (PluginInfo pluginInfo : existing) {
                loadedPlugins.put(pluginInfo.getPluginId(), pluginInfo);
                eventBus.publish(eventBus.createEvent(
                        PluginEvent.EventType.PLUGIN_INSTALLED, pluginInfo.getPluginId()));
            }
            log.info("Brick BootKit 已同步 {} 个插件", existing.size());
            return;
        }

        // 兼容旧配置 brick-bootkit.plugin-path：主框架未加载时手动兜底
        if (properties.isAutoDiscover()) {
            List<File> pluginPaths = discoverPlugins();
            for (File pluginPath : pluginPaths) {
                try {
                    PluginInfo pluginInfo = pluginManager.install(pluginPath.toPath());
                    if (pluginInfo != null) {
                        loadedPlugins.put(pluginInfo.getPluginId(), pluginInfo);
                        eventBus.publish(eventBus.createEvent(
                                PluginEvent.EventType.PLUGIN_INSTALLED, pluginInfo.getPluginId()));
                        log.info("插件加载成功: {} ({})", pluginInfo.getPluginDescriptor().getName(), pluginInfo.getPluginId());
                    }
                } catch (Exception e) {
                    log.error("插件加载失败: {}", pluginPath.getName(), e);
                }
            }
            for (PluginInfo pluginInfo : loadedPlugins.values()) {
                try {
                    pluginManager.start(pluginInfo.getPluginId());
                    log.info("插件启动成功: {}", pluginInfo.getPluginId());
                } catch (Exception e) {
                    log.error("插件启动失败: {}", pluginInfo.getPluginId(), e);
                }
            }
        }
        log.info("Brick BootKit Spring Boot Starter 初始化完成，共加载 {} 个插件", loadedPlugins.size());
    }

    /**
     * 插件关闭时清理资源
     */
    @EventListener(ApplicationFailedEvent.class)
    public void onApplicationFailed() {
        shutdown();
    }

    /**
     * 关闭插件管理器
     */
    public void shutdown() {
        log.info("Brick BootKit 正在关闭...");
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager != null) {
            for (PluginInfo pluginInfo : new ArrayList<>(loadedPlugins.values())) {
                try {
                    pluginManager.stop(pluginInfo.getPluginId());
                } catch (Exception e) {
                    log.warn("插件关闭失败: {}", pluginInfo.getPluginId(), e);
                }
            }
        }
        loadedPlugins.clear();
        if (eventBus != null) {
            eventBus.shutdown();
        }
        log.info("Brick BootKit 关闭完成");
    }

    /**
     * 获取已加载插件
     */
    public Map<String, PluginInfo> getLoadedPlugins() {
        return Collections.unmodifiableMap(loadedPlugins);
    }

    /**
     * 获取 EventBus
     */
    public PluginEventBus getEventBus() {
        return eventBus;
    }

    /**
     * 发现插件文件
     */
    private List<File> discoverPlugins() {
        List<File> plugins = new ArrayList<>();
        String pluginPath = properties.getPluginPath();
        if (pluginPath == null || pluginPath.isEmpty()) {
            log.warn("未配置插件路径，跳过自动发现");
            return plugins;
        }

        File dir = new File(pluginPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("插件路径不存在: {}", pluginPath);
            return plugins;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files != null) {
            plugins.addAll(Arrays.asList(files));
        }

        return plugins;
    }
}
