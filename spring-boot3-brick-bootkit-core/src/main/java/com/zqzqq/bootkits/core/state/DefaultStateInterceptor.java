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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 默认状态变更拦截器（审计日志）
 * @since 3.5.5
 */
public class DefaultStateInterceptor implements PluginStateInterceptor {
    private static final Logger log = LoggerFactory.getLogger(DefaultStateInterceptor.class);
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preStateChange(String pluginId,
            EnhancedPluginState currentState,
            EnhancedPluginState newState) {
        log.info("[{}] 插件 {} 准备从 {} 状态转换到 {} 状态",
            FORMATTER.format(LocalDateTime.now()),
            pluginId,
            currentState,
            newState);
        return true;
    }

    @Override
    public void postStateChange(String pluginId,
            EnhancedPluginState previousState,
            EnhancedPluginState newState) {
        log.info("[{}] 插件 {} 已从 {} 状态成功转换到 {} 状态",
            FORMATTER.format(LocalDateTime.now()),
            pluginId,
            previousState,
            newState);
    }

    @Override
    public void onStateChangeFailure(String pluginId,
            EnhancedPluginState currentState,
            EnhancedPluginState attemptedState,
            Throwable cause) {
        log.error("[{}] 插件 {} 状态转换失败: {} -> {}, 原因: {}",
            FORMATTER.format(LocalDateTime.now()),
            pluginId,
            currentState,
            attemptedState,
            cause.getMessage());
    }
}