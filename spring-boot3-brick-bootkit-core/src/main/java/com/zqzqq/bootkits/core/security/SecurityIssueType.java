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
 * 安全问题类型枚举
 *
 * @author starBlues
 * @since 4.0.0
 */
public enum SecurityIssueType {
    
    /**
     * 危险代码
     */
    DANGEROUS_CODE("危险代码", 10),
    
    /**
     * 可疑代码
     */
    SUSPICIOUS_CODE("可疑代码", 5),
    
    /**
     * 权限违规
     */
    PERMISSION_VIOLATION("权限违规", 8),
    
    /**
     * 文件系统访问违规
     */
    FILE_ACCESS_VIOLATION("文件系统访问违规", 7),
    
    /**
     * 网络访问违规
     */
    NETWORK_ACCESS_VIOLATION("网络访问违规", 6),
    
    /**
     * 反射使用
     */
    REFLECTION_USAGE("反射使用", 4),
    
    /**
     * 本地库加载
     */
    NATIVE_LIBRARY_LOADING("本地库加载", 9),
    
    /**
     * 系统属性修改
     */
    SYSTEM_PROPERTY_MODIFICATION("系统属性修改", 7),
    
    /**
     * 安全管理器操作
     */
    SECURITY_MANAGER_OPERATION("安全管理器操作", 10),
    
    /**
     * 未知格式
     */
    UNKNOWN_FORMAT("未知格式", 3),
    
    /**
     * 扫描错误
     */
    SCAN_ERROR("扫描错误", 1);

    private final String description;
    private final int riskScore;

    SecurityIssueType(String description, int riskScore) {
        this.description = description;
        this.riskScore = riskScore;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取风险评分
     */
    public int getRiskScore() {
        return riskScore;
    }

    /**
     * 是否为高风险
     */
    public boolean isHighRisk() {
        return riskScore >= 8;
    }

    /**
     * 是否为中风险
     */
    public boolean isMediumRisk() {
        return riskScore >= 5 && riskScore < 8;
    }

    /**
     * 是否为低风险
     */
    public boolean isLowRisk() {
        return riskScore < 5;
    }
}