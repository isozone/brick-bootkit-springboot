/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.lifecycle;

import java.time.LocalDateTime;

/**
 * 插件生命周期事件
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginLifecycleEvent {

    private final String pluginId;
    private final PluginLifecycleState oldState;
    private final PluginLifecycleState newState;
    private final LocalDateTime timestamp;
    private final String errorMessage;
    private final Throwable exception;
    private final ClassLoader pluginClassLoader;

    public PluginLifecycleEvent(String pluginId, PluginLifecycleState oldState, 
                               PluginLifecycleState newState) {
        this(pluginId, oldState, newState, null, null, null);
    }

    public PluginLifecycleEvent(String pluginId, PluginLifecycleState oldState, 
                               PluginLifecycleState newState, String errorMessage, 
                               Throwable exception, ClassLoader pluginClassLoader) {
        this.pluginId = pluginId;
        this.oldState = oldState;
        this.newState = newState;
        this.timestamp = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.pluginClassLoader = pluginClassLoader;
    }

    public String getPluginId() {
        return pluginId;
    }

    public PluginLifecycleState getOldState() {
        return oldState;
    }

    public PluginLifecycleState getNewState() {
        return newState;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getException() {
        return exception;
    }

    public ClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }

    public boolean hasError() {
        return errorMessage != null || exception != null;
    }

    public String getErrorType() {
        if (exception != null) {
            return exception.getClass().getSimpleName();
        }
        return "UNKNOWN";
    }

    @Override
    public String toString() {
        return String.format("PluginLifecycleEvent{pluginId='%s', oldState=%s, newState=%s, timestamp=%s, hasError=%s}",
                pluginId, oldState, newState, timestamp, hasError());
    }
}