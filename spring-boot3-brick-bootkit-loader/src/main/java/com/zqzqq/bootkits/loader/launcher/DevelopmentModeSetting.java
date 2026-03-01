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

package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.DevelopmentMode;

/**
 * DevelopmentMode settings.
 *
 * <p>
 * In the normal host-app startup flow, mode is set by launcher.
 * For standalone plugin startup (directly running plugin main class),
 * the mode can be injected via JVM properties/environment:
 * </p>
 * <ul>
 *     <li>plugin.developmentMode</li>
 *     <li>spring-boot3-brick-bootkit.developmentMode</li>
 *     <li>developmentMode</li>
 *     <li>PLUGIN_DEVELOPMENT_MODE (env)</li>
 * </ul>
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.1
 */
public class DevelopmentModeSetting {

    private static final String[] DEVELOPMENT_MODE_KEYS = new String[]{
            "plugin.developmentMode",
            "spring-boot3-brick-bootkit.developmentMode",
            "developmentMode"
    };
    private static final String DEVELOPMENT_MODE_ENV_KEY = "PLUGIN_DEVELOPMENT_MODE";

    private static String developmentMode;

    static void setDevelopmentMode(String developmentMode) {
        DevelopmentModeSetting.developmentMode = checkModeKey(developmentMode);
    }

    /**
     * Configure development mode explicitly for standalone plugin run.
     * Empty value means keep current mode.
     */
    public static void setStandaloneDevelopmentMode(String developmentMode) {
        if (developmentMode == null || "".equals(developmentMode.trim())) {
            return;
        }
        DevelopmentModeSetting.developmentMode = checkModeKey(developmentMode.trim());
    }

    public static boolean isolation() {
        tryInitFromProperties();
        return DevelopmentMode.ISOLATION.equalsIgnoreCase(developmentMode);
    }

    public static boolean coexist() {
        tryInitFromProperties();
        return DevelopmentMode.COEXIST.equalsIgnoreCase(developmentMode);
    }

    public static String getDevelopmentMode() {
        tryInitFromProperties();
        return developmentMode;
    }

    public static IllegalStateException getUnknownModeException() {
        return getUnknownModeException(null);
    }

    public static IllegalStateException getUnknownModeException(String developmentMode) {
        if (developmentMode == null || "".equals(developmentMode)) {
            developmentMode = DevelopmentModeSetting.developmentMode;
        }
        return new IllegalStateException("Unsupported development mode: " + developmentMode);
    }

    /**
     * Initialize from system properties or environment if not set yet.
     * This is mainly for standalone plugin startup.
     */
    public static void tryInitFromProperties() {
        if (!(developmentMode == null || "".equals(developmentMode))) {
            return;
        }
        String mode = resolveDevelopmentModeFromProperties();
        if (mode == null || "".equals(mode.trim())) {
            return;
        }
        DevelopmentModeSetting.developmentMode = mode;
    }

    /**
     * Resolve development mode from JVM properties/environment.
     * Returns null if not configured.
     */
    public static String resolveDevelopmentModeFromProperties() {
        String mode = null;
        for (String key : DEVELOPMENT_MODE_KEYS) {
            mode = System.getProperty(key);
            if (mode != null && !"".equals(mode.trim())) {
                break;
            }
        }
        if (mode == null || "".equals(mode.trim())) {
            mode = System.getenv(DEVELOPMENT_MODE_ENV_KEY);
        }
        if (mode == null || "".equals(mode.trim())) {
            return null;
        }
        return checkModeKey(mode.trim());
    }

    private static String checkModeKey(String developmentMode) {
        if (developmentMode == null || "".equals(developmentMode)) {
            throw new RuntimeException("developmentMode cannot be empty");
        }
        if (DevelopmentMode.ISOLATION.equalsIgnoreCase(developmentMode)) {
            return developmentMode;
        } else if (DevelopmentMode.COEXIST.equalsIgnoreCase(developmentMode)) {
            return developmentMode;
        } else {
            throw getUnknownModeException(developmentMode);
        }
    }

}
