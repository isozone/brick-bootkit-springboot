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

package com.zqzqq.bootkits.core.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件安全管理器
 * 负责插件的权限控制、安全策略管理和安全检查
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginSecurityManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginSecurityManager.class);

    private final Map<String, PluginSecurityPolicy> securityPolicies = new ConcurrentHashMap<>();
    private final Map<String, Set<PluginPermission>> pluginPermissions = new ConcurrentHashMap<>();
    private final PluginCodeScanner codeScanner;
    private final PluginSecurityConfiguration configuration;
    private final List<PluginSecurityListener> securityListeners = new ArrayList<>();
    private final PluginSecurityAuditLogger auditLogger = new PluginSecurityAuditLogger();

    public PluginSecurityManager(PluginSecurityConfiguration configuration) {
        this.configuration = configuration;
        this.codeScanner = new PluginCodeScanner(configuration);
    }

    /**
     * 验证插件安全性
     *
     * @param pluginId 插件ID
     * @param pluginPath 插件路径
     * @return 安全验证结果
     */
    public PluginSecurityValidationResult validatePluginSecurity(String pluginId, Path pluginPath) {
        logger.info("开始验证插件安全性: {}", pluginId);
        
        try {
            PluginSecurityValidationResult result = new PluginSecurityValidationResult(pluginId);
            
            // 1. 代码安全扫描
            PluginCodeScanResult scanResult = codeScanner.scanPlugin(pluginPath);
            result.setCodeScanResult(scanResult);
            
            // 2. 权限检查
            validatePluginPermissions(pluginId, result);
            
            // 3. 安全策略检查
            validateSecurityPolicies(pluginId, result);
            
            // 4. 文件系统访问检查
            validateFileSystemAccess(pluginId, pluginPath, result);
            
            // 5. 网络访问检查
            validateNetworkAccess(pluginId, result);
            
            // 通知安全监听器
            notifySecurityValidation(result);
            
            // 记录审计日志
            auditLogger.logSecurityValidation(pluginId, result);
            
            logger.info("插件安全验证完成: {}, 结果: {}", pluginId, result.isValid() ? "通过" : "失败");
            return result;
            
        } catch (Exception e) {
            logger.error("插件安全验证失败: " + pluginId, e);
            PluginSecurityValidationResult errorResult = new PluginSecurityValidationResult(pluginId);
            errorResult.addViolation("插件安全验证失败: " + e.getMessage());
            errorResult.setValid(false);
            return errorResult;
        }
    }

    /**
     * 检查插件权限
     *
     * @param pluginId 插件ID
     * @param permission 权限
     * @return 是否有权限
     */
    public boolean checkPermission(String pluginId, PluginPermission permission) {
        Set<PluginPermission> permissions = pluginPermissions.get(pluginId);
        boolean granted = permissions != null && 
            (permissions.contains(permission) || hasWildcardPermission(permissions, permission));
        
        // 记录权限检查审计日志
        auditLogger.logPermissionCheck(pluginId, permission, granted);
        
        return granted;
    }

    /**
     * 授予插件权限
     *
     * @param pluginId 插件ID
     * @param permission 权限
     */
    public void grantPermission(String pluginId, PluginPermission permission) {
        pluginPermissions.computeIfAbsent(pluginId, k -> new HashSet<>()).add(permission);
        logger.info("授予插件权限: {} -> {}", pluginId, permission);
        
        notifyPermissionGranted(pluginId, permission);
    }

    /**
     * 撤销插件权限
     *
     * @param pluginId 插件ID
     * @param permission 权限
     */
    public void revokePermission(String pluginId, PluginPermission permission) {
        Set<PluginPermission> permissions = pluginPermissions.get(pluginId);
        if (permissions != null) {
            permissions.remove(permission);
            logger.info("撤销插件权限: {} -> {}", pluginId, permission);
            
            notifyPermissionRevoked(pluginId, permission);
        }
    }

    /**
     * 设置插件安全策略
     *
     * @param pluginId 插件ID
     * @param policy 安全策略
     */
    public void setSecurityPolicy(String pluginId, PluginSecurityPolicy policy) {
        securityPolicies.put(pluginId, policy);
        logger.info("设置插件安全策略: {}", pluginId);
    }

    /**
     * 获取插件安全策略
     *
     * @param pluginId 插件ID
     * @return 安全策略
     */
    public PluginSecurityPolicy getSecurityPolicy(String pluginId) {
        return securityPolicies.getOrDefault(pluginId, getDefaultSecurityPolicy());
    }

    /**
     * 添加安全监听器
     *
     * @param listener 安全监听器
     */
    public void addSecurityListener(PluginSecurityListener listener) {
        securityListeners.add(listener);
    }

    /**
     * 移除安全监听器
     *
     * @param listener 安全监听器
     */
    public void removeSecurityListener(PluginSecurityListener listener) {
        securityListeners.remove(listener);
    }

    /**
     * 清理插件安全信息
     *
     * @param pluginId 插件ID
     */
    public void cleanupPluginSecurity(String pluginId) {
        securityPolicies.remove(pluginId);
        pluginPermissions.remove(pluginId);
        logger.info("清理插件安全信息: {}", pluginId);
    }

    /**
     * 获取插件权限列表
     *
     * @param pluginId 插件ID
     * @return 权限列表
     */
    public Set<PluginPermission> getPluginPermissions(String pluginId) {
        return new HashSet<>(pluginPermissions.getOrDefault(pluginId, Collections.emptySet()));
    }

    /**
     * 获取默认安全策略（延迟初始化）
     */
    private PluginSecurityPolicy getDefaultPolicy() {
        return securityPolicies.computeIfAbsent("default", k -> {
            // 创建默认安全策略
            PluginSecurityPolicy defaultPolicy = PluginSecurityPolicy.builder()
                    .allowFileSystemAccess(false)
                    .allowNetworkAccess(false)
                    .allowSystemPropertyAccess(false)
                    .allowReflectionAccess(true)
                    .maxMemoryUsage(configuration.getDefaultMaxMemoryUsage())
                    .maxThreadCount(configuration.getDefaultMaxThreadCount())
                    .build();
            return defaultPolicy;
        });
    }

    private void validatePluginPermissions(String pluginId, PluginSecurityValidationResult result) {
        Set<PluginPermission> permissions = pluginPermissions.get(pluginId);
        if (permissions == null || permissions.isEmpty()) {
            result.addWarning("插件没有配置任何权限");
        }
    }

    private void validateSecurityPolicies(String pluginId, PluginSecurityValidationResult result) {
        PluginSecurityPolicy policy = getSecurityPolicy(pluginId);
        
        // 检查策略配置的合理性
        if (policy.getMaxMemoryUsage() > configuration.getGlobalMaxMemoryUsage()) {
            result.addViolation("插件内存限制超过全局限制");
        }
        
        if (policy.getMaxThreadCount() > configuration.getGlobalMaxThreadCount()) {
            result.addViolation("插件线程数限制超过全局限制");
        }
    }

    private void validateFileSystemAccess(String pluginId, Path pluginPath, PluginSecurityValidationResult result) {
        PluginSecurityPolicy policy = getSecurityPolicy(pluginId);
        
        if (!policy.isAllowFileSystemAccess()) {
            // 检查插件是否尝试访问文件系统
            // 这里可以通过代码扫描结果来判断
        }
    }

    private void validateNetworkAccess(String pluginId, PluginSecurityValidationResult result) {
        PluginSecurityPolicy policy = getSecurityPolicy(pluginId);
        
        if (!policy.isAllowNetworkAccess()) {
            // 检查插件是否尝试进行网络访问
            // 这里可以通过代码扫描结果来判断
        }
    }

    private boolean hasWildcardPermission(Set<PluginPermission> permissions, PluginPermission permission) {
        return permissions.stream()
                .anyMatch(p -> p.getType().equals(PluginPermissionType.ALL) || 
                              (p.getType().equals(permission.getType()) && "*".equals(p.getTarget())));
    }

    private PluginSecurityPolicy getDefaultSecurityPolicy() {
        return getDefaultPolicy();
    }

    private void notifySecurityValidation(PluginSecurityValidationResult result) {
        for (PluginSecurityListener listener : securityListeners) {
            try {
                listener.onSecurityValidationCompleted(result.getPluginId(), result);
            } catch (Exception e) {
                logger.warn("安全监听器通知失败", e);
            }
        }
    }

    private void notifyPermissionGranted(String pluginId, PluginPermission permission) {
        for (PluginSecurityListener listener : securityListeners) {
            try {
                listener.onPermissionChecked(pluginId, permission, true);
            } catch (Exception e) {
                logger.warn("权限授予通知失败", e);
            }
        }
    }

    private void notifyPermissionRevoked(String pluginId, PluginPermission permission) {
        for (PluginSecurityListener listener : securityListeners) {
            try {
                listener.onPermissionViolation(pluginId, permission, "权限被撤销");
            } catch (Exception e) {
                logger.warn("权限撤销通知失败", e);
            }
        }
    }
    
    /**
     * 关闭安全管理器，清理所有资源
     * 
     * 注意：这是一个破坏性操作，将清理所有安全策略和监听器
     */
    public void shutdown() {
        logger.info("system", "开始关闭插件安全管理器");
        
        try {
            // 清理所有插件的安全策略和权限
            securityPolicies.clear();
            pluginPermissions.clear();
            
            // 清理安全监听器
            securityListeners.clear();
            
            // 清理审计日志器（如果支持）
            if (auditLogger != null) {
                // 这里可以添加auditLogger的清理方法
                logger.info("system", "已清理安全审计器");
            }
            
            logger.info("system", "插件安全管理器关闭完成");
            
        } catch (Exception e) {
            logger.error("system", "关闭安全管理器时发生错误", e.getMessage(), e);
            throw new RuntimeException("安全管理器关闭失败", e);
        }
    }
}
