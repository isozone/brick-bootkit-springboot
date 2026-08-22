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


package com.zqzqq.bootkits.scripts.security;

import com.zqzqq.bootkits.scripts.core.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 脚本权限管理器接口
 * 负责管理脚本的权限控制和验证
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptPermissionManager {
    
    /**
     * 验证脚本是否具有指定权限
     *
     * @param scriptPath 脚本路径
     * @param permission 需要的权限
     * @return 验证结果
     */
    PermissionValidationResult validatePermission(String scriptPath, ScriptPermission permission);
    
    /**
     * 验证脚本内容是否具有指定权限
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @param permission 需要的权限
     * @return 验证结果
     */
    PermissionValidationResult validatePermission(String scriptContent, ScriptType scriptType, ScriptPermission permission);
    
    /**
     * 为脚本分配权限
     *
     * @param scriptPath 脚本路径
     * @param permission 权限
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean grantPermission(String scriptPath, ScriptPermission permission, String userId);
    
    /**
     * 撤销脚本权限
     *
     * @param scriptPath 脚本路径
     * @param permission 权限
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean revokePermission(String scriptPath, ScriptPermission permission, String userId);
    
    /**
     * 获取脚本的所有权限
     *
     * @param scriptPath 脚本路径
     * @return 权限映射
     */
    Map<String, Set<ScriptPermission>> getScriptPermissions(String scriptPath);
    
    /**
     * 获取用户的脚本权限
     *
     * @param userId 用户ID
     * @return 权限映射
     */
    Map<String, Set<ScriptPermission>> getUserPermissions(String userId);
    
    /**
     * 检查脚本是否被禁用
     *
     * @param scriptPath 脚本路径
     * @return 是否被禁用
     */
    boolean isScriptDisabled(String scriptPath);
    
    /**
     * 禁用脚本
     *
     * @param scriptPath 脚本路径
     * @param reason 禁用原因
     * @param userId 操作者ID
     * @return 是否成功
     */
    boolean disableScript(String scriptPath, String reason, String userId);
    
    /**
     * 启用脚本
     *
     * @param scriptPath 脚本路径
     * @param userId 操作者ID
     * @return 是否成功
     */
    boolean enableScript(String scriptPath, String userId);
    
    /**
     * 获取所有禁用的脚本
     *
     * @return 禁用脚本列表
     */
    List<DisabledScriptInfo> getDisabledScripts();
    
    /**
     * 创建自定义权限规则
     *
     * @param rule 权限规则
     * @param userId 创建者ID
     * @return 规则ID
     */
    String createCustomPermissionRule(CustomPermissionRule rule, String userId);
    
    /**
     * 删除自定义权限规则
     *
     * @param ruleId 规则ID
     * @param userId 操作者ID
     * @return 是否成功
     */
    boolean deleteCustomPermissionRule(String ruleId, String userId);
    
    /**
     * 获取所有自定义权限规则
     *
     * @return 规则列表
     */
    List<CustomPermissionRule> getCustomPermissionRules();
    
    /**
     * 权限验证结果
     */
    class PermissionValidationResult {
        private final boolean allowed;
        private final ScriptPermission grantedPermission;
        private final String reason;
        private final List<String> warnings;
        
        public PermissionValidationResult(boolean allowed, ScriptPermission grantedPermission, String reason) {
            this.allowed = allowed;
            this.grantedPermission = grantedPermission;
            this.reason = reason;
            this.warnings = new java.util.ArrayList<>();
        }
        
        public PermissionValidationResult(boolean allowed, ScriptPermission grantedPermission, String reason, List<String> warnings) {
            this.allowed = allowed;
            this.grantedPermission = grantedPermission;
            this.reason = reason;
            this.warnings = warnings != null ? warnings : new java.util.ArrayList<>();
        }
        
        public boolean isAllowed() { return allowed; }
        public ScriptPermission getGrantedPermission() { return grantedPermission; }
        public String getReason() { return reason; }
        public List<String> getWarnings() { return warnings; }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
    }
    
    /**
     * 禁用脚本信息
     */
    class DisabledScriptInfo {
        private final String scriptPath;
        private final String reason;
        private final String disabledBy;
        private final long disabledAt;
        
        public DisabledScriptInfo(String scriptPath, String reason, String disabledBy, long disabledAt) {
            this.scriptPath = scriptPath;
            this.reason = reason;
            this.disabledBy = disabledBy;
            this.disabledAt = disabledAt;
        }
        
        public String getScriptPath() { return scriptPath; }
        public String getReason() { return reason; }
        public String getDisabledBy() { return disabledBy; }
        public long getDisabledAt() { return disabledAt; }
    }
    
    /**
     * 自定义权限规则
     */
    class CustomPermissionRule {
        private final String ruleId;
        private final String name;
        private final String description;
        private final Map<String, Object> conditions;
        private final ScriptPermission grantedPermission;
        private final boolean enabled;
        private final String createdBy;
        private final long createdAt;
        
        public CustomPermissionRule(String ruleId, String name, String description, 
                                  Map<String, Object> conditions, ScriptPermission grantedPermission,
                                  boolean enabled, String createdBy, long createdAt) {
            this.ruleId = ruleId;
            this.name = name;
            this.description = description;
            this.conditions = conditions;
            this.grantedPermission = grantedPermission;
            this.enabled = enabled;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }
        
        public String getRuleId() { return ruleId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getConditions() { return conditions; }
        public ScriptPermission getGrantedPermission() { return grantedPermission; }
        public boolean isEnabled() { return enabled; }
        public String getCreatedBy() { return createdBy; }
        public long getCreatedAt() { return createdAt; }
    }
}