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

/**
 * 事件监听器接口
 * 插件通过此接口订阅事件
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public interface PluginEventListener {

    /**
     * 处理事件
     * 
     * @param event 事件对象
     */
    void onEvent(PluginEvent event);

    /**
     * 获取监听器优先级（数值越小优先级越高）
     */
    default int priority() {
        return 0;
    }

    /**
     * 是否异步处理（默认同步）
     */
    default boolean async() {
        return false;
    }

    /**
     * 是否支持的事件类型
     * 返回 false 表示支持所有类型
     */
    default boolean supportsType(PluginEvent.EventType type) {
        return true;
    }
}
