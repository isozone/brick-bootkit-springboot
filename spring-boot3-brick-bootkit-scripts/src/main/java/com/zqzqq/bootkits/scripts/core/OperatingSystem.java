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


package com.zqzqq.bootkits.scripts.core;

/**
 * 操作系统枚举
 * 定义了支持的操作系统类型
 *
 * @author starBlues
 * @since 4.0.1
 */
public enum OperatingSystem {
    
    /**
     * Linux操作系统
     */
    LINUX("linux", "Linux"),
    
    /**
     * Windows操作系统
     */
    WINDOWS("windows", "Windows"),
    
    /**
     * macOS操作系统
     */
    MACOS("macos", "macOS"),
    
    /**
     * Unix操作系统
     */
    UNIX("unix", "Unix"),
    
    /**
     * 未知操作系统
     */
    UNKNOWN("unknown", "Unknown");
    
    private final String id;
    private final String displayName;
    
    OperatingSystem(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检测当前操作系统
     *
     * @return 当前操作系统
     */
    public static OperatingSystem detectCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return MACOS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return LINUX;
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            return UNIX;
        } else {
            return UNKNOWN;
        }
    }
    
    /**
     * 检查当前是否为Windows系统
     *
     * @return 是否为Windows
     */
    public static boolean isWindows() {
        return detectCurrentOS() == WINDOWS;
    }
    
    /**
     * 检查当前是否为Linux系统
     *
     * @return 是否为Linux
     */
    public static boolean isLinux() {
        return detectCurrentOS() == LINUX;
    }
    
    /**
     * 检查当前是否为macOS系统
     *
     * @return 是否为macOS
     */
    public static boolean isMacOS() {
        return detectCurrentOS() == MACOS;
    }
    
    /**
     * 检查当前是否为Unix系统
     *
     * @return 是否为Unix
     */
    public static boolean isUnix() {
        return detectCurrentOS() == UNIX;
    }
}