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


package com.zqzqq.bootkits.core.eventbus;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件事件总线事件基类
 * 支持插件间异步通信，替代直接依赖
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class PluginEvent {

    /** 事件类型 */
    private final EventType type;
    
    /** 触发插件 ID */
    private final String sourcePluginId;
    
    /** 目标插件 ID（null 表示广播） */
    private final String targetPluginId;
    
    /** 事件时间 */
    private final Instant timestamp;
    
    /** 事件数据 */
    private final Map<String, Object> payload;
    
    /** 是否已被处理 */
    private volatile boolean handled = false;

    public PluginEvent(EventType type, String sourcePluginId) {
        this(type, sourcePluginId, null);
    }

    public PluginEvent(EventType type, String sourcePluginId, String targetPluginId) {
        this.type = type;
        this.sourcePluginId = sourcePluginId;
        this.targetPluginId = targetPluginId;
        this.timestamp = Instant.now();
        this.payload = new HashMap<>();
    }

    /**
     * 添加事件数据
     */
    public PluginEvent put(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }

    /**
     * 获取事件数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) payload.get(key);
    }

    /**
     * 获取所有事件数据
     */
    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    /**
     * 标记事件已处理
     */
    public void markHandled() {
        this.handled = true;
    }

    /**
     * 是否已处理
     */
    public boolean isHandled() {
        return handled;
    }

    public EventType getType() {
        return type;
    }

    public String getSourcePluginId() {
        return sourcePluginId;
    }

    public String getTargetPluginId() {
        return targetPluginId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isBroadcast() {
        return targetPluginId == null;
    }

    @Override
    public String toString() {
        return String.format("PluginEvent{type=%s, source='%s', target='%s', timestamp=%s}",
                type, sourcePluginId, targetPluginId, timestamp);
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 插件生命周期事件 */
        PLUGIN_INSTALLED,
        PLUGIN_UNINSTALLED,
        PLUGIN_STARTING,
        PLUGIN_STARTED,
        PLUGIN_STOPPING,
        PLUGIN_STOPPED,
        
        /** 服务相关事件 */
        SERVICE_REGISTERED,
        SERVICE_UNREGISTERED,
        SERVICE_CHANGED,
        
        /** 配置相关事件 */
        CONFIG_CHANGED,
        CONFIG_RELOADED,
        
        /** 错误相关事件 */
        PLUGIN_ERROR,
        PLUGIN_RECOVERED,
        
        /** 自定义业务事件 */
        CUSTOM
    }
}
