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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件安全验证结果
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginSecurityValidationResult {

    private final String pluginId;
    private final LocalDateTime validationTime;
    private boolean valid = true;
    private final List<String> violations = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private PluginCodeScanResult codeScanResult;
    private SecurityRiskLevel riskLevel = SecurityRiskLevel.LOW;

    public PluginSecurityValidationResult(String pluginId) {
        this.pluginId = pluginId;
        this.validationTime = LocalDateTime.now();
    }

    public static PluginSecurityValidationResult success(String pluginId, String message) {
        PluginSecurityValidationResult result = new PluginSecurityValidationResult(pluginId);
        if (message != null && !message.isEmpty()) {
            result.addWarning(message);
        }
        result.setValid(true);
        result.setRiskLevel(SecurityRiskLevel.LOW);
        return result;
    }

    public static PluginSecurityValidationResult failure(String pluginId, String message) {
        PluginSecurityValidationResult result = new PluginSecurityValidationResult(pluginId);
        result.addViolation(message);
        return result;
    }
    /**
     * 添加安全违规
     *
     * @param violation 违规描述
     */
    public void addViolation(String violation) {
        violations.add(violation);
        valid = false;
        updateRiskLevel();
    }

    /**
     * 添加警告
     *
     * @param warning 警告描述
     */
    public void addWarning(String warning) {
        warnings.add(warning);
        updateRiskLevel();
    }

    /**
     * 检查是否有违规
     *
     * @return 是否有违规
     */
    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    /**
     * 检查是否有警告
     *
     * @return 是否有警告
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 获取总问题数
     *
     * @return 总问题数
     */
    public int getTotalIssues() {
        return violations.size() + warnings.size();
    }

    /**
     * 更新风险级别
     */
    private void updateRiskLevel() {
        if (violations.size() >= 5) {
            riskLevel = SecurityRiskLevel.CRITICAL;
        } else if (violations.size() >= 3) {
            riskLevel = SecurityRiskLevel.HIGH;
        } else if (violations.size() >= 1 || warnings.size() >= 5) {
            riskLevel = SecurityRiskLevel.MEDIUM;
        } else if (warnings.size() >= 1) {
            riskLevel = SecurityRiskLevel.LOW;
        }
    }

    /**
     * 获取验证摘要
     *
     * @return 验证摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("插件安全验证结果 - ").append(pluginId).append("\n");
        summary.append("验证时间: ").append(validationTime).append("\n");
        summary.append("验证结果: ").append(valid ? "通过" : "失败").append("\n");
        summary.append("风险级别: ").append(riskLevel.getDescription()).append("\n");
        summary.append("违规数量: ").append(violations.size()).append("\n");
        summary.append("警告数量: ").append(warnings.size()).append("\n");
        
        if (!violations.isEmpty()) {
            summary.append("\n违规详情:\n");
            for (int i = 0; i < violations.size(); i++) {
                summary.append("  ").append(i + 1).append(". ").append(violations.get(i)).append("\n");
            }
        }
        
        if (!warnings.isEmpty()) {
            summary.append("\n警告详情:\n");
            for (int i = 0; i < warnings.size(); i++) {
                summary.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
            }
        }
        
        return summary.toString();
    }

    // Getters and Setters
    public String getPluginId() {
        return pluginId;
    }

    public LocalDateTime getValidationTime() {
        return validationTime;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getViolations() {
        return violations;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public PluginCodeScanResult getCodeScanResult() {
        return codeScanResult;
    }

    public void setCodeScanResult(PluginCodeScanResult codeScanResult) {
        this.codeScanResult = codeScanResult;
        if (codeScanResult != null && codeScanResult.hasViolations()) {
            this.valid = false;
        }
    }

    public SecurityRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(SecurityRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    /**
     * 安全风险级别
     */
    public enum SecurityRiskLevel {
        LOW("低风险"),
        MEDIUM("中风险"),
        HIGH("高风险"),
        CRITICAL("严重风险");

        private final String description;

        SecurityRiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}