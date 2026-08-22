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


package com.zqzqq.bootkits.core.admission;

/**
 * Admission mode for plugin lifecycle validation.
 */
public enum PluginAdmissionMode {

    OFF,
    WARN,
    ENFORCE;

    public static PluginAdmissionMode fromText(String value, PluginAdmissionMode defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        for (PluginAdmissionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return defaultValue;
    }
}
