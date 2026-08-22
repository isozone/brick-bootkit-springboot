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



package com.zqzqq.bootkits.core.security;

import java.util.HashSet;
import java.util.Set;

/**
 * 插件安全配置
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginSecurityConfiguration {

    private boolean enableCodeScanning = true;
    private boolean enablePermissionCheck = true;
    private boolean enableFileSystemCheck = true;
    private boolean enableNetworkCheck = true;
    private boolean strictMode = false;

    // 代码扫描配置
    private Set<String> dangerousPatterns = new HashSet<>();
    private Set<String> allowedPackages = new HashSet<>();
    private Set<String> blockedPackages = new HashSet<>();

    // 文件系统配置
    private Set<String> allowedPaths = new HashSet<>();
    private Set<String> blockedPaths = new HashSet<>();

    // 网络配置
    private Set<String> allowedHosts = new HashSet<>();
    private Set<String> blockedHosts = new HashSet<>();
    private Set<Integer> allowedPorts = new HashSet<>();
    private Set<Integer> blockedPorts = new HashSet<>();

    public PluginSecurityConfiguration() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        // 默认危险模式
        dangerousPatterns.add("Runtime\\.getRuntime\\(\\)\\.exec");
        dangerousPatterns.add("ProcessBuilder");
        dangerousPatterns.add("System\\.exit");
        dangerousPatterns.add("System\\.setProperty");
        dangerousPatterns.add("Class\\.forName");
        dangerousPatterns.add("URLClassLoader");
        dangerousPatterns.add("sun\\.misc\\.Unsafe");

        // 默认阻止的包
        blockedPackages.add("sun.misc");
        blockedPackages.add("com.sun.management");
        blockedPackages.add("java.lang.management");

        // 默认允许的包
        allowedPackages.add("java.lang");
        allowedPackages.add("java.util");
        allowedPackages.add("java.io");
        allowedPackages.add("org.springframework");

        // 默认阻止的路径
        blockedPaths.add("/etc");
        blockedPaths.add("/proc");
        blockedPaths.add("/sys");
        blockedPaths.add("C:\\Windows\\System32");

        // 默认阻止的端口
        blockedPorts.add(22);   // SSH
        blockedPorts.add(23);   // Telnet
        blockedPorts.add(135);  // RPC
        blockedPorts.add(445);  // SMB
    }

    // Getters and Setters
    public boolean isEnableCodeScanning() {
        return enableCodeScanning;
    }

    public void setEnableCodeScanning(boolean enableCodeScanning) {
        this.enableCodeScanning = enableCodeScanning;
    }

    public boolean isEnablePermissionCheck() {
        return enablePermissionCheck;
    }

    public void setEnablePermissionCheck(boolean enablePermissionCheck) {
        this.enablePermissionCheck = enablePermissionCheck;
    }

    public boolean isEnableFileSystemCheck() {
        return enableFileSystemCheck;
    }

    public void setEnableFileSystemCheck(boolean enableFileSystemCheck) {
        this.enableFileSystemCheck = enableFileSystemCheck;
    }

    public boolean isEnableNetworkCheck() {
        return enableNetworkCheck;
    }

    public void setEnableNetworkCheck(boolean enableNetworkCheck) {
        this.enableNetworkCheck = enableNetworkCheck;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Set<String> getDangerousPatterns() {
        return dangerousPatterns;
    }

    public void setDangerousPatterns(Set<String> dangerousPatterns) {
        this.dangerousPatterns = dangerousPatterns;
    }

    public Set<String> getAllowedPackages() {
        return allowedPackages;
    }

    public void setAllowedPackages(Set<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    public Set<String> getBlockedPackages() {
        return blockedPackages;
    }

    public void setBlockedPackages(Set<String> blockedPackages) {
        this.blockedPackages = blockedPackages;
    }

    public Set<String> getAllowedPaths() {
        return allowedPaths;
    }

    public void setAllowedPaths(Set<String> allowedPaths) {
        this.allowedPaths = allowedPaths;
    }

    public Set<String> getBlockedPaths() {
        return blockedPaths;
    }

    public void setBlockedPaths(Set<String> blockedPaths) {
        this.blockedPaths = blockedPaths;
    }

    public Set<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(Set<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public Set<String> getBlockedHosts() {
        return blockedHosts;
    }

    public void setBlockedHosts(Set<String> blockedHosts) {
        this.blockedHosts = blockedHosts;
    }

    public Set<Integer> getAllowedPorts() {
        return allowedPorts;
    }

    public void setAllowedPorts(Set<Integer> allowedPorts) {
        this.allowedPorts = allowedPorts;
    }

    public Set<Integer> getBlockedPorts() {
        return blockedPorts;
    }

    public void setBlockedPorts(Set<Integer> blockedPorts) {
        this.blockedPorts = blockedPorts;
    }

    // 资源限制配置
    private long defaultMaxMemoryUsage = 128 * 1024 * 1024; // 128MB
    private int defaultMaxThreadCount = 10;
    private long globalMaxMemoryUsage = 1024 * 1024 * 1024; // 1GB
    private int globalMaxThreadCount = 100;

    public long getDefaultMaxMemoryUsage() {
        return defaultMaxMemoryUsage;
    }

    public void setDefaultMaxMemoryUsage(long defaultMaxMemoryUsage) {
        this.defaultMaxMemoryUsage = defaultMaxMemoryUsage;
    }

    public int getDefaultMaxThreadCount() {
        return defaultMaxThreadCount;
    }

    public void setDefaultMaxThreadCount(int defaultMaxThreadCount) {
        this.defaultMaxThreadCount = defaultMaxThreadCount;
    }

    public long getGlobalMaxMemoryUsage() {
        return globalMaxMemoryUsage;
    }

    public void setGlobalMaxMemoryUsage(long globalMaxMemoryUsage) {
        this.globalMaxMemoryUsage = globalMaxMemoryUsage;
    }

    public int getGlobalMaxThreadCount() {
        return globalMaxThreadCount;
    }

    public void setGlobalMaxThreadCount(int globalMaxThreadCount) {
        this.globalMaxThreadCount = globalMaxThreadCount;
    }
}