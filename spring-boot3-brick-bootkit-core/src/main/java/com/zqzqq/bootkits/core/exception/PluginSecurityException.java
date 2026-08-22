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


package com.zqzqq.bootkits.core.exception;

import com.zqzqq.bootkits.core.security.SecurityViolationType;

/**
 * 插件安全异常
 */
public class PluginSecurityException extends EnhancedPluginException {
    
    private SecurityViolationType violationType;
    
    public PluginSecurityException(PluginErrorCode errorCode, String message) {
        super(errorCode, message);
        this.violationType = SecurityViolationType.fromErrorCode(String.valueOf(errorCode.getCode()));
    }
    
    public PluginSecurityException(PluginErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message);
        initCause(cause);
        this.violationType = SecurityViolationType.fromErrorCode(String.valueOf(errorCode.getCode()));
    }
    
    public PluginSecurityException(PluginErrorCode errorCode, String message, SecurityViolationType violationType) {
        super(errorCode, message);
        this.violationType = violationType;
    }
    
    public PluginSecurityException(PluginErrorCode errorCode, String message, Throwable cause, SecurityViolationType violationType) {
        super(errorCode, message);
        initCause(cause);
        this.violationType = violationType;
    }
    
    /**
     * 获取违规类型
     */
    public SecurityViolationType getViolationType() {
        return violationType != null ? violationType : SecurityViolationType.UNKNOWN;
    }
    
    /**
     * 设置违规类型
     */
    public PluginSecurityException withViolationType(SecurityViolationType violationType) {
        this.violationType = violationType;
        return this;
    }
    
    public static PluginSecurityException permissionDenied(String pluginId, String operation, String permission) {
        return new PluginSecurityException(PluginErrorCode.SECURITY_PERMISSION_DENIED, 
                "权限被拒绝: " + operation + " 需要权限: " + permission)
                .withPluginId(pluginId)
                .withContext("operation", operation)
                .withContext("permission", permission);
    }
    
    public static PluginSecurityException signatureInvalid(String pluginId, String reason) {
        return new PluginSecurityException(PluginErrorCode.SECURITY_SIGNATURE_INVALID, 
                "签名验证失败: " + reason)
                .withPluginId(pluginId)
                .withContext("reason", reason);
    }
    
    public static PluginSecurityException encryptionError(String pluginId, String algorithm, Throwable cause) {
        return new PluginSecurityException(PluginErrorCode.SECURITY_ENCRYPTION_ERROR, 
                "加密错误: " + algorithm, cause)
                .withPluginId(pluginId)
                .withContext("algorithm", algorithm);
    }
    
    public static PluginSecurityException authenticationFailed(String pluginId, String user, String reason) {
        return new PluginSecurityException(PluginErrorCode.SECURITY_AUTHENTICATION_FAILED, 
                "认证失败: " + user + " - " + reason)
                .withPluginId(pluginId)
                .withContext("user", user)
                .withContext("reason", reason);
    }
}