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


package com.zqzqq.bootkits.core.state;

/**
 * 插件状态变更拦截器
 * @since 3.5.5
 */
public interface PluginStateInterceptor {
    
    /**
     * 状态变更前触发
     * @param pluginId 插件ID
     * @param currentState 当前状态
     * @param newState 新状态
     * @return 是否允许继续执行状态变更
     */
    default boolean preStateChange(String pluginId,
            EnhancedPluginState currentState,
            EnhancedPluginState newState) {
        return true;
    }

    /**
     * 状态变更后触发
     * @param pluginId 插件ID
     * @param previousState 之前状态
     * @param newState 新状态
     */
    default void postStateChange(String pluginId,
            EnhancedPluginState previousState,
            EnhancedPluginState newState) {
    }

    /**
     * 状态变更异常时触发
     * @param pluginId 插件ID
     * @param currentState 当前状态
     * @param attemptedState 尝试转换的状态
     * @param cause 异常原因
     */
    default void onStateChangeFailure(String pluginId,
            EnhancedPluginState currentState,
            EnhancedPluginState attemptedState,
            Throwable cause) {
    }
}