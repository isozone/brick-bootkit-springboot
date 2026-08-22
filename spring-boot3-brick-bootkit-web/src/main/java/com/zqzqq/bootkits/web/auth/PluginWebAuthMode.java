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


package com.zqzqq.bootkits.web.auth;

/**
 * Plugin web authorization mode.
 */
public enum PluginWebAuthMode {
    /**
     * Disable authorization checks in web module.
     */
    DISABLED,
    /**
     * Delegate authorization to host application's authorizer.
     * If no custom authorizer exists, fallback authorizer is used.
     */
    DELEGATE,
    /**
     * Delegate authorization to host application and require custom authorizer.
     * Startup fails if fallback authorizer is used.
     */
    STRICT;

    public static PluginWebAuthMode from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DELEGATE;
        }
        String normalized = value.trim().toUpperCase();
        for (PluginWebAuthMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return DELEGATE;
    }
}

