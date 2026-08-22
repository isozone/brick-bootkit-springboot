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


package com.zqzqq.bootkits.scripts.security.impl;

import com.zqzqq.bootkits.scripts.security.*;
import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

/**
 * 脚本权限管理器默认实现
 * 提供完整的脚本权限管理功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptPermissionManager implements ScriptPermissionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptPermissionManager.class);
    
    // 脚本权限映射: scriptPath -> (userId -> permissions)
    private final Map<String, Map<String, Set<ScriptPermission>>> scriptPermissions = new ConcurrentHashMap<>();
    
    // 用户权限映射: userId -> (scriptPath -> permissions)
    private final Map<String, Map<String, Set<ScriptPermission>>> userPermissions = new ConcurrentHashMap<>();
    
    // 禁用的脚本
    private final Map<String, DisabledScriptInfo> disabledScripts = new ConcurrentHashMap<>();
    
    // 自定义权限规则
    private final Map<String, CustomPermissionRule> customRules = new ConcurrentHashMap<>();
    
    // 安全规则配置
    private final SecurityConfiguration securityConfig;
    
    public DefaultScriptPermissionManager() {
        this(new SecurityConfiguration());
    }
    
    public DefaultScriptPermissionManager(SecurityConfiguration securityConfig) {
        this.securityConfig = securityConfig;
        initializeDefaultRules();
    }
    
    @Override
    public PermissionValidationResult validatePermission(String scriptPath, ScriptPermission permission) {
        try {
            if (scriptPath == null || scriptPath.trim().isEmpty()) {
                return new PermissionValidationResult(false, null, "脚本路径不能为空");
            }
            
            // 检查脚本是否被禁用
            if (isScriptDisabled(scriptPath)) {
                DisabledScriptInfo disabledInfo = disabledScripts.get(scriptPath);
                String reason = disabledInfo != null ? disabledInfo.getReason() : "脚本已被禁用";
                return new PermissionValidationResult(false, null, reason);
            }
            
            // 检查脚本文件是否存在
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                return new PermissionValidationResult(false, null, "脚本文件不存在: " + scriptPath);
            }
            
            // 检查脚本是否在安全目录中
            if (!isScriptInSafeDirectory(scriptPath)) {
                return new PermissionValidationResult(false, null, 
                    "脚本不在安全目录中: " + scriptPath);
            }
            
            // 检查脚本内容是否安全
            ScriptContentSecurityCheck securityCheck = performSecurityCheck(scriptPath, null);
            if (!securityCheck.isSafe()) {
                return new PermissionValidationResult(false, null, 
                    "脚本内容不安全: " + securityCheck.getReason());
            }
            
            // 检查默认权限
            ScriptPermission defaultPermission = getDefaultPermission(scriptPath);
            if (permission.ordinal() <= defaultPermission.ordinal()) {
                return new PermissionValidationResult(true, permission, "权限验证通过");
            }
            
            // 检查是否有明确的权限授予
            Set<ScriptPermission> grantedPermissions = getGrantedPermissions(scriptPath, "system");
            if (grantedPermissions != null && hasPermission(grantedPermissions, permission)) {
                return new PermissionValidationResult(true, permission, "权限验证通过");
            }
            
            return new PermissionValidationResult(false, defaultPermission, 
                "权限不足，需要: " + permission + ", 当前: " + defaultPermission);
                
        } catch (Exception e) {
            logger.error("权限验证失败: " + scriptPath, e);
            return new PermissionValidationResult(false, null, "权限验证异常: " + e.getMessage());
        }
    }
    
    @Override
    public PermissionValidationResult validatePermission(String scriptContent, ScriptType scriptType, ScriptPermission permission) {
        try {
            if (scriptContent == null || scriptContent.trim().isEmpty()) {
                return new PermissionValidationResult(false, null, "脚本内容不能为空");
            }
            
            if (scriptType == null) {
                return new PermissionValidationResult(false, null, "脚本类型不能为空");
            }
            
            // 检查脚本内容是否安全
            ScriptContentSecurityCheck securityCheck = performSecurityCheck(null, scriptContent);
            if (!securityCheck.isSafe()) {
                return new PermissionValidationResult(false, null, 
                    "脚本内容不安全: " + securityCheck.getReason());
            }
            
            // 检查脚本类型是否允许
            if (!isScriptTypeAllowed(scriptType)) {
                return new PermissionValidationResult(false, null, 
                    "不允许执行此类型脚本: " + scriptType);
            }
            
            // 检查内容长度限制
            if (scriptContent.length() > securityConfig.getMaxScriptLength()) {
                return new PermissionValidationResult(false, null, 
                    "脚本内容过长，最大允许: " + securityConfig.getMaxScriptLength() + " 字符");
            }
            
            // 检查默认权限
            ScriptPermission defaultPermission = getDefaultPermission(scriptType);
            if (permission.ordinal() <= defaultPermission.ordinal()) {
                return new PermissionValidationResult(true, permission, "权限验证通过");
            }
            
            return new PermissionValidationResult(false, defaultPermission, 
                "权限不足，需要: " + permission + ", 当前: " + defaultPermission);
                
        } catch (Exception e) {
            logger.error("权限验证失败: 脚本类型=" + scriptType, e);
            return new PermissionValidationResult(false, null, "权限验证异常: " + e.getMessage());
        }
    }
    
    @Override
    public boolean grantPermission(String scriptPath, ScriptPermission permission, String userId) {
        if (scriptPath == null || permission == null || userId == null) {
            return false;
        }
        
        try {
            // 更新脚本权限
            scriptPermissions.computeIfAbsent(scriptPath, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(permission);
            
            // 更新用户权限
            userPermissions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(scriptPath, k -> ConcurrentHashMap.newKeySet())
                .add(permission);
            
            logger.info("权限授予成功: 用户={}, 脚本={}, 权限={}", userId, scriptPath, permission);
            return true;
            
        } catch (Exception e) {
            logger.error("权限授予失败: 用户=" + userId + ", 脚本=" + scriptPath, e);
            return false;
        }
    }
    
    @Override
    public boolean revokePermission(String scriptPath, ScriptPermission permission, String userId) {
        if (scriptPath == null || permission == null || userId == null) {
            return false;
        }
        
        try {
            Map<String, Set<ScriptPermission>> userPerms = scriptPermissions.get(scriptPath);
            if (userPerms != null) {
                Set<ScriptPermission> perms = userPerms.get(userId);
                if (perms != null) {
                    perms.remove(permission);
                    if (perms.isEmpty()) {
                        userPerms.remove(userId);
                    }
                }
            }
            
            Map<String, Set<ScriptPermission>> scriptPerms = userPermissions.get(userId);
            if (scriptPerms != null) {
                Set<ScriptPermission> perms = scriptPerms.get(scriptPath);
                if (perms != null) {
                    perms.remove(permission);
                    if (perms.isEmpty()) {
                        scriptPerms.remove(scriptPath);
                    }
                }
            }
            
            logger.info("权限撤销成功: 用户={}, 脚本={}, 权限={}", userId, scriptPath, permission);
            return true;
            
        } catch (Exception e) {
            logger.error("权限撤销失败: 用户=" + userId + ", 脚本=" + scriptPath, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Set<ScriptPermission>> getScriptPermissions(String scriptPath) {
        if (scriptPath == null) {
            return new HashMap<>();
        }
        
        Map<String, Set<ScriptPermission>> result = new HashMap<>();
        Map<String, Set<ScriptPermission>> permissions = scriptPermissions.get(scriptPath);
        if (permissions != null) {
            permissions.forEach((userId, perms) -> {
                result.put(userId, new HashSet<>(perms));
            });
        }
        return result;
    }
    
    @Override
    public Map<String, Set<ScriptPermission>> getUserPermissions(String userId) {
        if (userId == null) {
            return new HashMap<>();
        }
        
        Map<String, Set<ScriptPermission>> result = new HashMap<>();
        Map<String, Set<ScriptPermission>> permissions = userPermissions.get(userId);
        if (permissions != null) {
            permissions.forEach((scriptPath, perms) -> {
                result.put(scriptPath, new HashSet<>(perms));
            });
        }
        return result;
    }
    
    @Override
    public boolean isScriptDisabled(String scriptPath) {
        return disabledScripts.containsKey(scriptPath);
    }
    
    @Override
    public boolean disableScript(String scriptPath, String reason, String userId) {
        if (scriptPath == null || reason == null || userId == null) {
            return false;
        }
        
        try {
            DisabledScriptInfo info = new DisabledScriptInfo(scriptPath, reason, userId, System.currentTimeMillis());
            disabledScripts.put(scriptPath, info);
            
            // 清除该脚本的所有权限
            scriptPermissions.remove(scriptPath);
            
            logger.warn("脚本已禁用: 脚本={}, 原因={}, 操作者={}", scriptPath, reason, userId);
            return true;
            
        } catch (Exception e) {
            logger.error("脚本禁用失败: " + scriptPath, e);
            return false;
        }
    }
    
    @Override
    public boolean enableScript(String scriptPath, String userId) {
        if (scriptPath == null || userId == null) {
            return false;
        }
        
        try {
            disabledScripts.remove(scriptPath);
            logger.info("脚本已启用: 脚本={}, 操作者={}", scriptPath, userId);
            return true;
            
        } catch (Exception e) {
            logger.error("脚本启用失败: " + scriptPath, e);
            return false;
        }
    }
    
    @Override
    public List<DisabledScriptInfo> getDisabledScripts() {
        return new ArrayList<>(disabledScripts.values());
    }
    
    @Override
    public String createCustomPermissionRule(CustomPermissionRule rule, String userId) {
        if (rule == null || rule.getRuleId() == null) {
            return null;
        }
        
        try {
            customRules.put(rule.getRuleId(), rule);
            logger.info("自定义权限规则已创建: 规则={}, 创建者={}", rule.getRuleId(), userId);
            return rule.getRuleId();
            
        } catch (Exception e) {
            logger.error("自定义权限规则创建失败: " + rule.getRuleId(), e);
            return null;
        }
    }
    
    @Override
    public boolean deleteCustomPermissionRule(String ruleId, String userId) {
        if (ruleId == null) {
            return false;
        }
        
        try {
            customRules.remove(ruleId);
            logger.info("自定义权限规则已删除: 规则={}, 操作者={}", ruleId, userId);
            return true;
            
        } catch (Exception e) {
            logger.error("自定义权限规则删除失败: " + ruleId, e);
            return false;
        }
    }
    
    @Override
    public List<CustomPermissionRule> getCustomPermissionRules() {
        return new ArrayList<>(customRules.values());
    }
    
    // 私有辅助方法
    
    private void initializeDefaultRules() {
        // 创建默认的自定义权限规则
        // 这里可以添加一些默认的安全规则
    }
    
    private boolean isScriptInSafeDirectory(String scriptPath) {
        if (scriptPath == null) {
            return false;
        }
        
        List<String> safeDirectories = securityConfig.getSafeDirectories();
        if (safeDirectories.isEmpty()) {
            return true; // 如果没有配置安全目录，允许所有路径
        }
        
        try {
            File scriptFile = new File(scriptPath).getCanonicalFile();
            for (String safeDir : safeDirectories) {
                File safeDirectory = new File(safeDir).getCanonicalFile();
                if (scriptFile.getAbsolutePath().startsWith(safeDirectory.getAbsolutePath())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("无法验证脚本路径安全: " + scriptPath, e);
            return false;
        }
    }
    
    private ScriptContentSecurityCheck performSecurityCheck(String scriptPath, String scriptContent) {
        try {
            String content = scriptContent;
            if (content == null && scriptPath != null) {
                content = new String(Files.readAllBytes(Paths.get(scriptPath)), StandardCharsets.UTF_8);
            }
            
            if (content == null) {
                return new ScriptContentSecurityCheck(false, "无法读取脚本内容");
            }
            
            // 检查危险命令
            List<String> dangerousPatterns = securityConfig.getDangerousPatterns();
            for (String pattern : dangerousPatterns) {
                if (content.toLowerCase().contains(pattern.toLowerCase())) {
                    return new ScriptContentSecurityCheck(false, "检测到危险命令: " + pattern);
                }
            }
            
            // 检查脚本哈希值（如果配置了禁止的哈希）
            String scriptHash = calculateContentHash(content);
            if (securityConfig.getForbiddenHashes().contains(scriptHash)) {
                return new ScriptContentSecurityCheck(false, "脚本内容被禁止");
            }
            
            return new ScriptContentSecurityCheck(true, "内容安全检查通过");
            
        } catch (Exception e) {
            return new ScriptContentSecurityCheck(false, "安全检查异常: " + e.getMessage());
        }
    }
    
    private boolean isScriptTypeAllowed(ScriptType scriptType) {
        if (scriptType == null) {
            return false;
        }
        
        List<ScriptType> allowedTypes = securityConfig.getAllowedScriptTypes();
        return allowedTypes.isEmpty() || allowedTypes.contains(scriptType);
    }
    
    private ScriptPermission getDefaultPermission(String scriptPath) {
        // 根据脚本路径确定默认权限
        // 这里可以实现更复杂的逻辑
        return ScriptPermission.BASIC_EXECUTION;
    }
    
    private ScriptPermission getDefaultPermission(ScriptType scriptType) {
        // 根据脚本类型确定默认权限
        if (scriptType == null) {
            return ScriptPermission.READ_ONLY;
        }
        
        switch (scriptType) {
            case SHELL:
            case BATCH:
            case POWERSHELL:
                return ScriptPermission.BASIC_EXECUTION;
            case PYTHON:
            case RUBY:
            case PERL:
            case NODEJS:
            case GROOVY:
                return ScriptPermission.STANDARD_EXECUTION;
            case LUA:
            case JAVASCRIPT:
                return ScriptPermission.BASIC_EXECUTION;
            case EXECUTABLE:
                return ScriptPermission.READ_ONLY;
            default:
                return ScriptPermission.READ_ONLY;
        }
    }
    
    private Set<ScriptPermission> getGrantedPermissions(String scriptPath, String userId) {
        Map<String, Set<ScriptPermission>> permissions = scriptPermissions.get(scriptPath);
        return permissions != null ? permissions.get(userId) : null;
    }
    
    private boolean hasPermission(Set<ScriptPermission> grantedPermissions, ScriptPermission requiredPermission) {
        return grantedPermissions.stream().anyMatch(p -> 
            p.ordinal() >= requiredPermission.ordinal());
    }
    
    private String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 安全配置类
     */
    public static class SecurityConfiguration {
        private final List<String> safeDirectories = new ArrayList<>();
        private final List<String> dangerousPatterns = new ArrayList<>();
        private final List<String> forbiddenHashes = new ArrayList<>();
        private final List<ScriptType> allowedScriptTypes = new ArrayList<>();
        private int maxScriptLength = 10000;
        
        public SecurityConfiguration() {
            initializeDefaults();
        }
        
        private void initializeDefaults() {
            // 添加默认的安全目录
            safeDirectories.add(System.getProperty("user.home"));
            safeDirectories.add(System.getProperty("java.io.tmpdir"));
            
            // 添加默认的危险命令模式
            dangerousPatterns.add("rm -rf");
            dangerousPatterns.add("del /s");
            dangerousPatterns.add("format");
            dangerousPatterns.add("fdisk");
            dangerousPatterns.add("mkfs");
            dangerousPatterns.add("dd if=");
            dangerousPatterns.add("> /dev/null");
            dangerousPatterns.add("chmod 777");
            dangerousPatterns.add("sudo");
            dangerousPatterns.add("su -");
            
            // 默认允许所有脚本类型
            allowedScriptTypes.addAll(Arrays.asList(ScriptType.values()));
        }
        
        public SecurityConfiguration addSafeDirectory(String directory) {
            safeDirectories.add(directory);
            return this;
        }
        
        public SecurityConfiguration addDangerousPattern(String pattern) {
            dangerousPatterns.add(pattern);
            return this;
        }
        
        public SecurityConfiguration addForbiddenHash(String hash) {
            forbiddenHashes.add(hash);
            return this;
        }
        
        public SecurityConfiguration setMaxScriptLength(int maxLength) {
            this.maxScriptLength = maxLength;
            return this;
        }
        
        public List<String> getSafeDirectories() { return new ArrayList<>(safeDirectories); }
        public List<String> getDangerousPatterns() { return new ArrayList<>(dangerousPatterns); }
        public List<String> getForbiddenHashes() { return new ArrayList<>(forbiddenHashes); }
        public List<ScriptType> getAllowedScriptTypes() { return new ArrayList<>(allowedScriptTypes); }
        public int getMaxScriptLength() { return maxScriptLength; }
    }
    
    /**
     * 脚本内容安全检查结果
     */
    public static class ScriptContentSecurityCheck {
        private final boolean safe;
        private final String reason;
        
        public ScriptContentSecurityCheck(boolean safe, String reason) {
            this.safe = safe;
            this.reason = reason;
        }
        
        public boolean isSafe() { return safe; }
        public String getReason() { return reason; }
    }
}
