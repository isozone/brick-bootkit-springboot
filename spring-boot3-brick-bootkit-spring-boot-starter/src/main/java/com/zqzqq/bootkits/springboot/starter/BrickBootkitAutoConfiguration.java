package com.zqzqq.bootkits.springboot.starter;

import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventListener;
import com.zqzqq.bootkits.core.plugin.Plugin;
import com.zqzqq.bootkits.core.plugin.PluginManager;
import com.zqzqq.bootkits.springboot.starter.properties.BrickBootkitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brick BootKit Spring Boot Starter
 * 自动初始化插件框架和 EventBus
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@Component
public class BrickBootkitAutoConfiguration implements ApplicationRunner {

    private final BrickBootkitProperties properties;
    private final PluginManager pluginManager;
    private final PluginEventBus eventBus;
    private final Map<String, Plugin> loadedPlugins = new ConcurrentHashMap<>();

    public BrickBootkitAutoConfiguration(
            BrickBootkitProperties properties,
            PluginManager pluginManager,
            PluginEventBus eventBus) {
        this.properties = properties;
        this.pluginManager = pluginManager;
        this.eventBus = eventBus;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            log.info("Brick BootKit 插件框架已禁用");
            return;
        }

        log.info("Brick BootKit Spring Boot Starter 初始化中...");

        // 自动发现并加载插件
        List<File> pluginPaths = discoverPlugins();
        for (File pluginPath : pluginPaths) {
            try {
                log.info("加载插件: {}", pluginPath.getName());
                Plugin plugin = pluginManager.installPlugin(pluginPath);
                if (plugin != null) {
                    loadedPlugins.put(plugin.getId(), plugin);
                    eventBus.publish(eventBus.createEvent(
                            PluginEvent.EventType.PLUGIN_INSTALLED, plugin.getId()));
                    log.info("插件加载成功: {} ({})", plugin.getName(), plugin.getId());
                }
            } catch (Exception e) {
                log.error("插件加载失败: {}", pluginPath.getName(), e);
            }
        }

        // 启动插件
        for (Plugin plugin : loadedPlugins.values()) {
            try {
                pluginManager.startPlugin(plugin.getId());
                log.info("插件启动成功: {}", plugin.getId());
            } catch (Exception e) {
                log.error("插件启动失败: {}", plugin.getId(), e);
            }
        }

        // 注册 Spring 事件监听
        registerSpringEventListeners();

        log.info("Brick BootKit Spring Boot Starter 初始化完成，已加载 {} 个插件", loadedPlugins.size());
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
        for (Plugin plugin : new ArrayList<>(loadedPlugins.values())) {
            try {
                pluginManager.stopPlugin(plugin.getId());
                plugin.uninstall();
            } catch (Exception e) {
                log.warn("插件关闭失败: {}", plugin.getId(), e);
            }
        }
        loadedPlugins.clear();
        if (eventBus != null) {
            eventBus.shutdown();
        }
        if (pluginManager != null) {
            pluginManager.shutdown();
        }
        log.info("Brick BootKit 关闭完成");
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

    /**
     * 获取已加载插件
     */
    public Map<String, Plugin> getLoadedPlugins() {
        return Collections.unmodifiableMap(loadedPlugins);
    }

    /**
     * 获取 EventBus
     */
    public PluginEventBus getEventBus() {
        return eventBus;
    }
}
