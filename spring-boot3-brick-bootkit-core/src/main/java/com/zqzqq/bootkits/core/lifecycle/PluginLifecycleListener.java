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



package com.zqzqq.bootkits.core.lifecycle;

/**
 * 插件生命周期监听器接口
 *
 * @author starBlues
 * @since 4.0.0
 */
public interface PluginLifecycleListener {

    /**
     * 插件生命周期事件回调
     *
     * @param event 生命周期事件
     */
    void onEvent(PluginLifecycleEvent event);

    /**
     * 获取监听器优先级
     * 数值越小优先级越高
     *
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 是否支持异步处理
     *
     * @return true表示支持异步处理
     */
    default boolean isAsync() {
        return false;
    }
}