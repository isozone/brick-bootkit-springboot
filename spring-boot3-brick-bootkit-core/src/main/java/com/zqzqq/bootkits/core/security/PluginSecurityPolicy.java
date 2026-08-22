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

import java.util.*;

/**
 * 插件安全策略
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginSecurityPolicy {

    private boolean allowFileSystemAccess;
    private boolean allowNetworkAccess;
    private boolean allowSystemPropertyAccess;
    private boolean allowReflectionAccess;
    private boolean allowRuntimeAccess;
    private boolean allowDatabaseAccess;
    private boolean allowJmxAccess;
    private boolean allowThreadCreation;
    private boolean allowClassLoaderAccess;

    private long maxMemoryUsage;
    private int maxThreadCount;
    private int maxFileDescriptors;
    private long maxExecutionTime;

    private Set<String> allowedPackages;
    private Set<String> forbiddenPackages;
    private Set<String> allowedHosts;
    private Set<String> forbiddenHosts;
    private Set<String> allowedFilePaths;
    private Set<String> forbiddenFilePaths;

    private Map<String, Object> customProperties;

    public PluginSecurityPolicy() {
        this.allowedPackages = new HashSet<>();
        this.forbiddenPackages = new HashSet<>();
        this.allowedHosts = new HashSet<>();
        this.forbiddenHosts = new HashSet<>();
        this.allowedFilePaths = new HashSet<>();
        this.forbiddenFilePaths = new HashSet<>();
        this.customProperties = new HashMap<>();
    }

    /**
     * 创建建造者
     *
     * @return 建造者实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 检查是否允许访问指定包
     *
     * @param packageName 包名
     * @return 是否允许
     */
    public boolean isPackageAllowed(String packageName) {
        if (forbiddenPackages.contains(packageName)) {
            return false;
        }
        
        if (allowedPackages.isEmpty()) {
            return true;
        }
        
        return allowedPackages.stream().anyMatch(packageName::startsWith);
    }

    /**
     * 检查是否允许访问指定主机
     *
     * @param host 主机地址
     * @return 是否允许
     */
    public boolean isHostAllowed(String host) {
        if (forbiddenHosts.contains(host)) {
            return false;
        }
        
        if (allowedHosts.isEmpty()) {
            return allowNetworkAccess;
        }
        
        return allowedHosts.contains(host) || allowedHosts.contains("*");
    }

    /**
     * 检查是否允许访问指定文件路径
     *
     * @param filePath 文件路径
     * @return 是否允许
     */
    public boolean isFilePathAllowed(String filePath) {
        if (forbiddenFilePaths.stream().anyMatch(filePath::startsWith)) {
            return false;
        }
        
        if (allowedFilePaths.isEmpty()) {
            return allowFileSystemAccess;
        }
        
        return allowedFilePaths.stream().anyMatch(filePath::startsWith);
    }

    // Getters and Setters
    public boolean isAllowFileSystemAccess() {
        return allowFileSystemAccess;
    }

    public void setAllowFileSystemAccess(boolean allowFileSystemAccess) {
        this.allowFileSystemAccess = allowFileSystemAccess;
    }

    public boolean isAllowNetworkAccess() {
        return allowNetworkAccess;
    }

    public void setAllowNetworkAccess(boolean allowNetworkAccess) {
        this.allowNetworkAccess = allowNetworkAccess;
    }

    public boolean isAllowSystemPropertyAccess() {
        return allowSystemPropertyAccess;
    }

    public void setAllowSystemPropertyAccess(boolean allowSystemPropertyAccess) {
        this.allowSystemPropertyAccess = allowSystemPropertyAccess;
    }

    public boolean isAllowReflectionAccess() {
        return allowReflectionAccess;
    }

    public void setAllowReflectionAccess(boolean allowReflectionAccess) {
        this.allowReflectionAccess = allowReflectionAccess;
    }

    public boolean isAllowRuntimeAccess() {
        return allowRuntimeAccess;
    }

    public void setAllowRuntimeAccess(boolean allowRuntimeAccess) {
        this.allowRuntimeAccess = allowRuntimeAccess;
    }

    public boolean isAllowDatabaseAccess() {
        return allowDatabaseAccess;
    }

    public void setAllowDatabaseAccess(boolean allowDatabaseAccess) {
        this.allowDatabaseAccess = allowDatabaseAccess;
    }

    public boolean isAllowJmxAccess() {
        return allowJmxAccess;
    }

    public void setAllowJmxAccess(boolean allowJmxAccess) {
        this.allowJmxAccess = allowJmxAccess;
    }

    public boolean isAllowThreadCreation() {
        return allowThreadCreation;
    }

    public void setAllowThreadCreation(boolean allowThreadCreation) {
        this.allowThreadCreation = allowThreadCreation;
    }

    public boolean isAllowClassLoaderAccess() {
        return allowClassLoaderAccess;
    }

    public void setAllowClassLoaderAccess(boolean allowClassLoaderAccess) {
        this.allowClassLoaderAccess = allowClassLoaderAccess;
    }

    public long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }

    public void setMaxMemoryUsage(long maxMemoryUsage) {
        this.maxMemoryUsage = maxMemoryUsage;
    }

    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
    }

    public int getMaxFileDescriptors() {
        return maxFileDescriptors;
    }

    public void setMaxFileDescriptors(int maxFileDescriptors) {
        this.maxFileDescriptors = maxFileDescriptors;
    }

    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public void setMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    public Set<String> getAllowedPackages() {
        return allowedPackages;
    }

    public void setAllowedPackages(Set<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    public Set<String> getForbiddenPackages() {
        return forbiddenPackages;
    }

    public void setForbiddenPackages(Set<String> forbiddenPackages) {
        this.forbiddenPackages = forbiddenPackages;
    }

    public Set<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(Set<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public Set<String> getForbiddenHosts() {
        return forbiddenHosts;
    }

    public void setForbiddenHosts(Set<String> forbiddenHosts) {
        this.forbiddenHosts = forbiddenHosts;
    }

    public Set<String> getAllowedFilePaths() {
        return allowedFilePaths;
    }

    public void setAllowedFilePaths(Set<String> allowedFilePaths) {
        this.allowedFilePaths = allowedFilePaths;
    }

    public Set<String> getForbiddenFilePaths() {
        return forbiddenFilePaths;
    }

    public void setForbiddenFilePaths(Set<String> forbiddenFilePaths) {
        this.forbiddenFilePaths = forbiddenFilePaths;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private final PluginSecurityPolicy policy = new PluginSecurityPolicy();

        public Builder allowFileSystemAccess(boolean allow) {
            policy.setAllowFileSystemAccess(allow);
            return this;
        }

        public Builder allowNetworkAccess(boolean allow) {
            policy.setAllowNetworkAccess(allow);
            return this;
        }

        public Builder allowSystemPropertyAccess(boolean allow) {
            policy.setAllowSystemPropertyAccess(allow);
            return this;
        }

        public Builder allowReflectionAccess(boolean allow) {
            policy.setAllowReflectionAccess(allow);
            return this;
        }

        public Builder maxMemoryUsage(long maxMemoryUsage) {
            policy.setMaxMemoryUsage(maxMemoryUsage);
            return this;
        }

        public Builder maxThreadCount(int maxThreadCount) {
            policy.setMaxThreadCount(maxThreadCount);
            return this;
        }

        public Builder allowedPackages(Set<String> packages) {
            policy.setAllowedPackages(packages);
            return this;
        }

        public Builder forbiddenPackages(Set<String> packages) {
            policy.setForbiddenPackages(packages);
            return this;
        }

        public PluginSecurityPolicy build() {
            return policy;
        }
    }
}