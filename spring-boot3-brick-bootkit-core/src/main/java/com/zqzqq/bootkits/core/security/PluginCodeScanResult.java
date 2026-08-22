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

import java.util.ArrayList;
import java.util.List;

/**
 * 插件代码扫描结果
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginCodeScanResult {

    private final String pluginId;
    private final List<SecurityViolation> violations = new ArrayList<>();
    private final List<SecurityWarning> warnings = new ArrayList<>();
    private boolean hasViolations = false;
    private long scanDurationMs;

    public PluginCodeScanResult(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * 添加安全违规
     */
    public void addViolation(String type, String description, String location) {
        violations.add(new SecurityViolation(type, description, location));
        hasViolations = true;
    }

    /**
     * 添加安全警告
     */
    public void addWarning(String type, String description, String location) {
        warnings.add(new SecurityWarning(type, description, location));
    }

    /**
     * 添加安全问题
     */
    public void addIssue(SecurityIssueType issueType, String description, String location) {
        if (issueType.isHighRisk() || issueType == SecurityIssueType.DANGEROUS_CODE) {
            addViolation(issueType.getDescription(), description, location);
        } else {
            addWarning(issueType.getDescription(), description, location);
        }
    }

    /**
     * 计算风险评分
     */
    public int calculateRiskScore() {
        return violations.size() * 10 + warnings.size() * 3;
    }

    /**
     * 获取风险评分
     */
    public int getRiskScore() {
        return calculateRiskScore();
    }

    /**
     * 是否有安全违规
     */
    public boolean hasViolations() {
        return hasViolations;
    }

    /**
     * 获取违规列表
     */
    public List<SecurityViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * 获取警告列表
     */
    public List<SecurityWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取扫描耗时
     */
    public long getScanDurationMs() {
        return scanDurationMs;
    }

    /**
     * 设置扫描耗时
     */
    public void setScanDurationMs(long scanDurationMs) {
        this.scanDurationMs = scanDurationMs;
    }

    /**
     * 安全违规信息
     */
    public static class SecurityViolation {
        private final String type;
        private final String description;
        private final String location;

        public SecurityViolation(String type, String description, String location) {
            this.type = type;
            this.description = description;
            this.location = location;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s at %s", type, description, location);
        }
    }

    /**
     * 安全警告信息
     */
    public static class SecurityWarning {
        private final String type;
        private final String description;
        private final String location;

        public SecurityWarning(String type, String description, String location) {
            this.type = type;
            this.description = description;
            this.location = location;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return String.format("[WARNING-%s] %s at %s", type, description, location);
        }
    }

    @Override
    public String toString() {
        return String.format("PluginCodeScanResult{pluginId='%s', violations=%d, warnings=%d, scanDurationMs=%d}",
                pluginId, violations.size(), warnings.size(), scanDurationMs);
    }
}