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


package com.zqzqq.bootkits.core.sandbox;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件安全沙箱
 * 限制插件的文件系统访问和网络连接
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class PluginSandbox {

    private final Map<String, PluginSandboxPolicy> policies = new ConcurrentHashMap<>();

    /**
     * 插件沙箱策略
     */
    public static class PluginSandboxPolicy {
        private final String pluginId;
        private final Set<String> allowedDirectories = new HashSet<>();
        private final Set<String> blockedDirectories = new HashSet<>();
        private final Set<String> allowedDomains = new HashSet<>();
        private final Set<String> blockedDomains = new HashSet<>();
        private final boolean allowFileAccess;
        private final boolean allowNetworkAccess;

        public PluginSandboxPolicy(String pluginId, boolean allowFileAccess, boolean allowNetworkAccess) {
            this.pluginId = pluginId;
            this.allowFileAccess = allowFileAccess;
            this.allowNetworkAccess = allowNetworkAccess;
        }

        public void allowDirectory(String dir) {
            allowedDirectories.add(dir);
            blockedDirectories.remove(dir);
        }

        public void blockDirectory(String dir) {
            blockedDirectories.add(dir);
            allowedDirectories.remove(dir);
        }

        public void allowDomain(String domain) {
            allowedDomains.add(domain);
            blockedDomains.remove(domain);
        }

        public void blockDomain(String domain) {
            blockedDomains.add(domain);
            allowedDomains.remove(domain);
        }

        public boolean isAllowedDirectory(Path path) {
            if (!allowFileAccess) return false;
            if (blockedDirectories.contains(path.toString())) return false;
            if (allowedDirectories.isEmpty()) return true;
            return allowedDirectories.stream().anyMatch(path.toString()::startsWith);
        }

        public boolean isAllowedDomain(String domain) {
            if (!allowNetworkAccess) return false;
            if (blockedDomains.contains(domain)) return false;
            if (allowedDomains.isEmpty()) return true;
            return allowedDomains.stream().anyMatch(domain::startsWith);
        }
    }

    /**
     * 创建插件沙箱策略
     */
    public void createPolicy(String pluginId, boolean allowFileAccess, boolean allowNetworkAccess) {
        policies.put(pluginId, new PluginSandboxPolicy(pluginId, allowFileAccess, allowNetworkAccess));
    }

    /**
     * 获取插件沙箱策略
     */
    public PluginSandboxPolicy getPolicy(String pluginId) {
        return policies.get(pluginId);
    }

    /**
     * 检查文件访问是否允许
     */
    public boolean isFileAccessAllowed(String pluginId, Path path) {
        PluginSandboxPolicy policy = policies.get(pluginId);
        return policy != null && policy.isAllowedDirectory(path);
    }

    /**
     * 检查网络访问是否允许
     */
    public boolean isNetworkAccessAllowed(String pluginId, String domain) {
        PluginSandboxPolicy policy = policies.get(pluginId);
        return policy != null && policy.isAllowedDomain(domain);
    }
}
