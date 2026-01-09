package com.zqzqq.bootkits.core.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 插件生命周期管理器
 */
public class PluginLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleManager.class);
    private final Map<String, PluginLifecycleState> pluginStates = new HashMap<>();
    private final List<PluginLifecycleListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 注册监听器
     */
    public void addListener(PluginLifecycleListener listener) {
        listeners.add(listener);
        listeners.sort(Comparator.comparingInt(PluginLifecycleListener::getPriority));
    }

    /**
     * 更新插件状态
     */
    public void updateState(String pluginId, PluginLifecycleState newState) {
        PluginLifecycleState oldState = pluginStates.getOrDefault(
            pluginId, PluginLifecycleState.UNINSTALLED);
        
        pluginStates.put(pluginId, newState);
        
        PluginLifecycleEvent event = new PluginLifecycleEvent(
            pluginId, oldState, newState);
        
        // 通知所有监听器
        listeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Lifecycle listener error for plugin: {}", pluginId, e);
            }
        });
    }

    /**
     * 获取当前状态
     */
    public PluginLifecycleState getCurrentState(String pluginId) {
        return pluginStates.getOrDefault(pluginId, PluginLifecycleState.UNINSTALLED);
    }
    
    /**
     * 关闭生命周期管理器，清理所有资源
     * 
     * 注意：这是一个破坏性操作，将清理所有插件状态和监听器
     */
    public void shutdown() {
        pluginStates.clear();
        listeners.clear();
        log.info("Plugin lifecycle manager shutdown completed");
    }
}
