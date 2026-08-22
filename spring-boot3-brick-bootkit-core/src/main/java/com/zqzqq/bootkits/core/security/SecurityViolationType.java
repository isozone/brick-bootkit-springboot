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

/**
 * 安全违规类型枚举
 * 定义插件安全框架中的各种违规类型
 *
 * @author starBlues
 * @since 4.0.0
 */
public enum SecurityViolationType {
    
    /**
     * 权限被拒绝
     */
    PERMISSION_DENIED("权限被拒绝"),
    
    /**
     * 签名验证失败
     */
    SIGNATURE_INVALID("签名验证失败"),
    
    /**
     * 加密错误
     */
    ENCRYPTION_ERROR("加密错误"),
    
    /**
     * 认证失败
     */
    AUTHENTICATION_FAILED("认证失败"),
    
    /**
     * 代码安全扫描失败
     */
    CODE_SCAN_VIOLATION("代码安全扫描违规"),
    
    /**
     * 恶意代码检测
     */
    MALICIOUS_CODE_DETECTED("恶意代码检测"),
    
    /**
     * 资源访问违规
     */
    RESOURCE_ACCESS_VIOLATION("资源访问违规"),
    
    /**
     * 网络访问违规
     */
    NETWORK_ACCESS_VIOLATION("网络访问违规"),
    
    /**
     * 文件系统访问违规
     */
    FILE_SYSTEM_ACCESS_VIOLATION("文件系统访问违规"),
    
    /**
     * 反射调用违规
     */
    REFLECTION_ACCESS_VIOLATION("反射调用违规"),
    
    /**
     * 类加载违规
     */
    CLASS_LOADING_VIOLATION("类加载违规"),
    
    /**
     * 系统属性访问违规
     */
    SYSTEM_PROPERTY_ACCESS_VIOLATION("系统属性访问违规"),
    
    /**
     * 环境变量访问违规
     */
    ENVIRONMENT_ACCESS_VIOLATION("环境变量访问违规"),
    
    /**
     * 线程操作违规
     */
    THREAD_OPERATION_VIOLATION("线程操作违规"),
    
    /**
     * JVM操作违规
     */
    JVM_OPERATION_VIOLATION("JVM操作违规"),
    
    /**
     * 安全策略违规
     */
    SECURITY_POLICY_VIOLATION("安全策略违规"),
    
    /**
     * 未知违规类型
     */
    UNKNOWN("未知违规类型");
    
    private final String description;
    
    SecurityViolationType(String description) {
        this.description = description;
    }
    
    /**
     * 获取违规类型描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据错误代码获取违规类型
     */
    public static SecurityViolationType fromErrorCode(String errorCode) {
        if (errorCode == null) {
            return UNKNOWN;
        }
        
        // 支持数字错误码
        try {
            int code = Integer.parseInt(errorCode);
            return fromErrorCode(code);
        } catch (NumberFormatException e) {
            // 继续处理字符串错误码
        }
        
        switch (errorCode.toUpperCase()) {
            case "SECURITY_PERMISSION_DENIED":
                return PERMISSION_DENIED;
            case "SECURITY_SIGNATURE_INVALID":
                return SIGNATURE_INVALID;
            case "SECURITY_ENCRYPTION_ERROR":
                return ENCRYPTION_ERROR;
            case "SECURITY_AUTHENTICATION_FAILED":
                return AUTHENTICATION_FAILED;
            case "SECURITY_CODE_SCAN_VIOLATION":
                return CODE_SCAN_VIOLATION;
            case "SECURITY_MALICIOUS_CODE_DETECTED":
                return MALICIOUS_CODE_DETECTED;
            case "SECURITY_RESOURCE_ACCESS_VIOLATION":
                return RESOURCE_ACCESS_VIOLATION;
            case "SECURITY_NETWORK_ACCESS_VIOLATION":
                return NETWORK_ACCESS_VIOLATION;
            case "SECURITY_FILE_SYSTEM_ACCESS_VIOLATION":
                return FILE_SYSTEM_ACCESS_VIOLATION;
            case "SECURITY_REFLECTION_ACCESS_VIOLATION":
                return REFLECTION_ACCESS_VIOLATION;
            case "SECURITY_CLASS_LOADING_VIOLATION":
                return CLASS_LOADING_VIOLATION;
            case "SECURITY_SYSTEM_PROPERTY_ACCESS_VIOLATION":
                return SYSTEM_PROPERTY_ACCESS_VIOLATION;
            case "SECURITY_ENVIRONMENT_ACCESS_VIOLATION":
                return ENVIRONMENT_ACCESS_VIOLATION;
            case "SECURITY_THREAD_OPERATION_VIOLATION":
                return THREAD_OPERATION_VIOLATION;
            case "SECURITY_JVM_OPERATION_VIOLATION":
                return JVM_OPERATION_VIOLATION;
            case "SECURITY_POLICY_VIOLATION":
                return SECURITY_POLICY_VIOLATION;
            default:
                return UNKNOWN;
        }
    }
    
    /**
     * 根据数字错误代码获取违规类型
     */
    public static SecurityViolationType fromErrorCode(int errorCode) {
        // 8xxx 系列为安全错误
        if (errorCode >= 8000 && errorCode < 9000) {
            switch (errorCode) {
                case 8001: // SECURITY_PERMISSION_DENIED
                    return PERMISSION_DENIED;
                case 8002: // SECURITY_SIGNATURE_INVALID
                    return SIGNATURE_INVALID;
                case 8003: // SECURITY_ENCRYPTION_ERROR
                    return ENCRYPTION_ERROR;
                case 8004: // SECURITY_AUTHENTICATION_FAILED
                    return AUTHENTICATION_FAILED;
                case 8005: // SECURITY_VIOLATION
                    return SECURITY_POLICY_VIOLATION;
                case 8006: // PERMISSION_DENIED
                    return PERMISSION_DENIED;
                case 8007: // SIGNATURE_INVALID
                    return SIGNATURE_INVALID;
                default:
                    return SECURITY_POLICY_VIOLATION;
            }
        }
        return UNKNOWN;
    }
}