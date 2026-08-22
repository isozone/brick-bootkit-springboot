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



package com.zqzqq.bootkits.core.exception;

import java.util.function.Supplier;

/**
 * 插件异常基类
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginException extends RuntimeException {

    private final String pluginId;

    public PluginException() {
        super();
        this.pluginId = null;
    }

    public PluginException(String message) {
        super(message);
        this.pluginId = null;
    }

    public PluginException(Throwable cause) {
        super(cause);
        this.pluginId = null;
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
        this.pluginId = null;
    }

    public PluginException(String pluginId, String message) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginException(Object pluginDescriptor, String message) {
        this(resolvePluginId(pluginDescriptor), messageFor(resolvePluginId(pluginDescriptor), message));
    }

    public PluginException(String pluginId, String message, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }

    public PluginException(Object pluginDescriptor, String opType, Throwable cause) {
        this(resolvePluginId(pluginDescriptor), messageFor(resolvePluginId(pluginDescriptor), opType), cause);
    }

    public String getPluginId() {
        return pluginId;
    }

    public static PluginException getPluginException(Throwable throwable, Supplier<PluginException> getException) {
        if (throwable instanceof PluginException) {
            return (PluginException) throwable;
        }
        return getException.get();
    }

    private static String resolvePluginId(Object pluginDescriptor) {
        if (pluginDescriptor == null) {
            return null;
        }
        try {
            Object value = pluginDescriptor.getClass().getMethod("getPluginId").invoke(pluginDescriptor);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ex) {
            return String.valueOf(pluginDescriptor);
        }
    }

    private static String messageFor(String pluginId, String message) {
        if (pluginId == null || pluginId.isBlank()) {
            return message;
        }
        return "Plugin[" + pluginId + "] " + message;
    }

    @Override
    public String toString() {
        return String.format("PluginException{pluginId='%s', message='%s'}", pluginId, getMessage());
    }
}
