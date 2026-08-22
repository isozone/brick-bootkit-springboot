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


package com.zqzqq.bootkits.sdk;

import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.eventbus.PluginEventListener;
import com.zqzqq.bootkits.sdk.annotation.BrickEventListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件监听器自动装配器
 * 扫描插件类中的 @BrickEventListener 注解方法，自动注册为事件监听器
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class EventBusAutoConfigure {

    /**
     * 自动装配事件监听器
     * 扫描 plugin 实例中的所有方法，查找 @BrickEventListener 注解
     * 
     * @param pluginId 插件 ID
     * @param pluginInstance 插件实例
     * @param eventBus 事件总线
     */
    public static void autoConfigure(String pluginId, Object pluginInstance, PluginEventBus eventBus) {
        if (pluginInstance == null || eventBus == null) {
            return;
        }

        Class<?> clazz = pluginInstance.getClass();
        
        // 扫描所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            BrickEventListener annotation = method.getAnnotation(BrickEventListener.class);
            if (annotation != null) {
                // 创建代理监听器
                EventListenerProxy proxy = new EventListenerProxy(pluginInstance, method, annotation);
                eventBus.registerListener(pluginId, proxy);
            }
        }
    }

    /**
     * 事件监听器代理
     * 将带注解的方法包装为 PluginEventListener
     */
    private static class EventListenerProxy implements PluginEventListener {

        private final Object target;
        private final Method method;
        private final BrickEventListener annotation;

        public EventListenerProxy(Object target, Method method, BrickEventListener annotation) {
            this.target = target;
            this.method = method;
            this.method.setAccessible(true);
            this.annotation = annotation;
        }

        @Override
        public void onEvent(PluginEvent event) {
            try {
                // 检查事件类型是否匹配
                boolean typeMatch = false;
                for (PluginEvent.EventType eventType : annotation.value()) {
                    if (eventType == event.getType()) {
                        typeMatch = true;
                        break;
                    }
                }
                if (!typeMatch) {
                    return;
                }

                // 检查目标插件匹配（如果指定了目标）
                if (event.getTargetPluginId() != null 
                        && !event.getTargetPluginId().equals(event.getSourcePluginId())) {
                    return;
                }

                // 调用方法（无参数）
                method.invoke(target);
                event.markHandled();
            } catch (Exception e) {
                throw new RuntimeException("事件处理方法异常: " + e.getMessage(), e);
            }
        }

        @Override
        public int priority() {
            return annotation.priority();
        }

        @Override
        public boolean async() {
            return annotation.async();
        }

        @Override
        public boolean supportsType(PluginEvent.EventType type) {
            for (PluginEvent.EventType eventType : annotation.value()) {
                if (eventType == type) {
                    return true;
                }
            }
            return false;
        }
    }
}
