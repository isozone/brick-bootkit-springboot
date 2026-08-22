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

import com.zqzqq.bootkits.core.lifecycle.PluginLifecycleEvent;
import com.zqzqq.bootkits.core.lifecycle.PluginLifecycleListener;
import com.zqzqq.bootkits.core.lifecycle.PluginLifecycleState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

/**
 * 插件生命周期监控监听器
 * 监听插件生命周期事件并收集相关指标
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginLifecycleMonitoringListener implements PluginLifecycleListener, Ordered {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginLifecycleMonitoringListener.class);
    
    private final PluginMetrics pluginMetrics;
    private final PluginPerformanceMonitor performanceMonitor;
    
    public PluginLifecycleMonitoringListener(PluginMetrics pluginMetrics, 
                                           PluginPerformanceMonitor performanceMonitor) {
        this.pluginMetrics = pluginMetrics;
        this.performanceMonitor = performanceMonitor;
    }
    
    @Override
    public void onEvent(PluginLifecycleEvent event) {
        String pluginId = event.getPluginId();
        PluginLifecycleState newState = event.getNewState();
        PluginLifecycleState oldState = event.getOldState();
        
        log.debug("Plugin {} state changed from {} to {}", pluginId, oldState, newState);
        
        switch (newState) {
            case INSTALLED:
                handlePluginInstalled(pluginId, event);
                break;
            case STARTING:
            case STARTED:
                handlePluginStarted(pluginId, event);
                break;
            case STOPPING:
            case STOPPED:
                handlePluginStopped(pluginId, event);
                break;
            case UNINSTALLED:
                handlePluginUninstalled(pluginId, event);
                break;
            case ERROR:
                handlePluginError(pluginId, event);
                break;
            default:
                log.debug("Unhandled plugin state: {}", newState);
        }
    }
    
    private void handlePluginInstalled(String pluginId, PluginLifecycleEvent event) {
        pluginMetrics.recordPluginInstall(pluginId);
        
        // 开始监控插件性能
        if (event.getPluginClassLoader() != null) {
            performanceMonitor.startMonitoringPlugin(pluginId, event.getPluginClassLoader());
        }
        
        log.info("Started monitoring for installed plugin: {}", pluginId);
    }
    
    private void handlePluginStarted(String pluginId, PluginLifecycleEvent event) {
        pluginMetrics.recordPluginStart(pluginId);
        log.info("Recorded start event for plugin: {}", pluginId);
    }
    
    private void handlePluginStopped(String pluginId, PluginLifecycleEvent event) {
        pluginMetrics.recordPluginStop(pluginId);
        log.info("Recorded stop event for plugin: {}", pluginId);
    }
    
    private void handlePluginUninstalled(String pluginId, PluginLifecycleEvent event) {
        pluginMetrics.recordPluginUninstall(pluginId);
        
        // 停止监控插件性能
        performanceMonitor.stopMonitoringPlugin(pluginId);
        
        log.info("Stopped monitoring for uninstalled plugin: {}", pluginId);
    }
    
    private void handlePluginError(String pluginId, PluginLifecycleEvent event) {
        String errorType = event.getErrorType() != null ? event.getErrorType() : "unknown";
        pluginMetrics.recordPluginError(pluginId, errorType);
        
        log.warn("Recorded error event for plugin: {} - {}", pluginId, errorType);
    }
    
    @Override
    public int getPriority() {
        return Ordered.HIGHEST_PRECEDENCE + 100; // 高优先级，确保监控数据及时收集
    }

    @Override
    public int getOrder() {
        return getPriority(); // 兼容Ordered接口
    }
}