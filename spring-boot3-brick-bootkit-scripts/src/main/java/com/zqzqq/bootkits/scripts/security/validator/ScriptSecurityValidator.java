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


package com.zqzqq.bootkits.scripts.security.validator;

import com.zqzqq.bootkits.scripts.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本安全验证器接口
 * 负责对脚本进行安全检查和验证
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptSecurityValidator {
    
    /**
     * 验证脚本文件
     *
     * @param scriptPath 脚本路径
     * @return 验证结果
     */
    SecurityValidationResult validateScriptFile(String scriptPath);
    
    /**
     * 验证脚本内容
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @return 验证结果
     */
    SecurityValidationResult validateScriptContent(String scriptContent, ScriptType scriptType);
    
    /**
     * 验证脚本权限
     *
     * @param scriptPath 脚本路径
     * @param requiredPermission 所需权限
     * @return 权限验证结果
     */
    PermissionValidationResult validatePermission(String scriptPath, ScriptType requiredPermission);
    
    /**
     * 检查脚本完整性
     *
     * @param scriptPath 脚本路径
     * @param expectedHash 预期哈希值
     * @return 完整性检查结果
     */
    IntegrityCheckResult checkIntegrity(String scriptPath, String expectedHash);
    
    /**
     * 扫描恶意内容
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @return 扫描结果
     */
    MaliciousContentScanResult scanMaliciousContent(String scriptContent, ScriptType scriptType);
    
    /**
     * 检查网络访问
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @return 网络访问检查结果
     */
    NetworkAccessCheckResult checkNetworkAccess(String scriptContent, ScriptType scriptType);
    
    /**
     * 检查文件访问
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @return 文件访问检查结果
     */
    FileAccessCheckResult checkFileAccess(String scriptContent, ScriptType scriptType);
    
    /**
     * 检查系统命令执行
     *
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @return 系统命令检查结果
     */
    SystemCommandCheckResult checkSystemCommands(String scriptContent, ScriptType scriptType);
    
    /**
     * 生成安全报告
     *
     * @param scriptPath 脚本路径
     * @param reportType 报告类型
     * @return 安全报告
     */
    SecurityReport generateSecurityReport(String scriptPath, SecurityReportType reportType);
    
    /**
     * 获取安全规则
     *
     * @return 安全规则列表
     */
    List<SecurityRule> getSecurityRules();
    
    /**
     * 添加安全规则
     *
     * @param rule 安全规则
     */
    void addSecurityRule(SecurityRule rule);
    
    /**
     * 移除安全规则
     *
     * @param ruleId 规则ID
     */
    void removeSecurityRule(String ruleId);
    
    /**
     * 启用/禁用安全规则
     *
     * @param ruleId 规则ID
     * @param enabled 是否启用
     */
    void setSecurityRuleEnabled(String ruleId, boolean enabled);
    
    /**
     * 基础验证结果
     */
    class SecurityValidationResult {
        private final boolean isValid;
        private final String message;
        private final SecuritySeverity severity;
        private final List<SecurityIssue> issues;
        private final Map<String, Object> metadata;
        
        public SecurityValidationResult(boolean isValid, String message, SecuritySeverity severity) {
            this(isValid, message, severity, new ArrayList<>(), new HashMap<>());
        }
        
        public SecurityValidationResult(boolean isValid, String message, SecuritySeverity severity, 
                                      List<SecurityIssue> issues, Map<String, Object> metadata) {
            this.isValid = isValid;
            this.message = message;
            this.severity = severity;
            this.issues = issues != null ? issues : new ArrayList<>();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        public boolean isValid() { return isValid; }
        public String getMessage() { return message; }
        public SecuritySeverity getSeverity() { return severity; }
        public List<SecurityIssue> getIssues() { return new ArrayList<>(issues); }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        
        public void addIssue(SecurityIssue issue) {
            issues.add(issue);
        }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }
    
    /**
     * 权限验证结果
     */
    class PermissionValidationResult {
        private final boolean hasPermission;
        private final String requiredPermission;
        private final String grantedPermission;
        private final String reason;
        private final List<String> warnings;
        
        public PermissionValidationResult(boolean hasPermission, String requiredPermission, 
                                        String grantedPermission, String reason) {
            this(hasPermission, requiredPermission, grantedPermission, reason, new ArrayList<>());
        }
        
        public PermissionValidationResult(boolean hasPermission, String requiredPermission, 
                                        String grantedPermission, String reason, List<String> warnings) {
            this.hasPermission = hasPermission;
            this.requiredPermission = requiredPermission;
            this.grantedPermission = grantedPermission;
            this.reason = reason;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public boolean hasPermission() { return hasPermission; }
        public String getRequiredPermission() { return requiredPermission; }
        public String getGrantedPermission() { return grantedPermission; }
        public String getReason() { return reason; }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
    }
    
    /**
     * 完整性检查结果
     */
    class IntegrityCheckResult {
        private final boolean isIntact;
        private final String expectedHash;
        private final String actualHash;
        private final String algorithm;
        private final String reason;
        
        public IntegrityCheckResult(boolean isIntact, String expectedHash, String actualHash, 
                                  String algorithm, String reason) {
            this.isIntact = isIntact;
            this.expectedHash = expectedHash;
            this.actualHash = actualHash;
            this.algorithm = algorithm;
            this.reason = reason;
        }
        
        public boolean isIntact() { return isIntact; }
        public String getExpectedHash() { return expectedHash; }
        public String getActualHash() { return actualHash; }
        public String getAlgorithm() { return algorithm; }
        public String getReason() { return reason; }
    }
    
    /**
     * 恶意内容扫描结果
     */
    class MaliciousContentScanResult {
        private final boolean isClean;
        private final List<MaliciousPattern> detectedPatterns;
        private final SecuritySeverity overallSeverity;
        private final String scanEngine;
        private final long scanTimeMs;
        
        public MaliciousContentScanResult(boolean isClean, List<MaliciousPattern> detectedPatterns,
                                        SecuritySeverity overallSeverity, String scanEngine, long scanTimeMs) {
            this.isClean = isClean;
            this.detectedPatterns = detectedPatterns != null ? detectedPatterns : new ArrayList<>();
            this.overallSeverity = overallSeverity;
            this.scanEngine = scanEngine;
            this.scanTimeMs = scanTimeMs;
        }
        
        public boolean isClean() { return isClean; }
        public List<MaliciousPattern> getDetectedPatterns() { return new ArrayList<>(detectedPatterns); }
        public SecuritySeverity getOverallSeverity() { return overallSeverity; }
        public String getScanEngine() { return scanEngine; }
        public long getScanTimeMs() { return scanTimeMs; }
    }
    
    /**
     * 网络访问检查结果
     */
    class NetworkAccessCheckResult {
        private final boolean isAllowed;
        private final List<NetworkAccess> allowedAccesses;
        private final List<NetworkAccess> blockedAccesses;
        private final SecuritySeverity severity;
        private final List<String> recommendations;
        
        public NetworkAccessCheckResult(boolean isAllowed, List<NetworkAccess> allowedAccesses,
                                      List<NetworkAccess> blockedAccesses, SecuritySeverity severity,
                                      List<String> recommendations) {
            this.isAllowed = isAllowed;
            this.allowedAccesses = allowedAccesses != null ? allowedAccesses : new ArrayList<>();
            this.blockedAccesses = blockedAccesses != null ? blockedAccesses : new ArrayList<>();
            this.severity = severity;
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
        
        public boolean isAllowed() { return isAllowed; }
        public List<NetworkAccess> getAllowedAccesses() { return new ArrayList<>(allowedAccesses); }
        public List<NetworkAccess> getBlockedAccesses() { return new ArrayList<>(blockedAccesses); }
        public SecuritySeverity getSeverity() { return severity; }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
    }
    
    /**
     * 文件访问检查结果
     */
    class FileAccessCheckResult {
        private final boolean isAllowed;
        private final List<FileAccess> allowedAccesses;
        private final List<FileAccess> blockedAccesses;
        private final SecuritySeverity severity;
        private final List<String> recommendations;
        
        public FileAccessCheckResult(boolean isAllowed, List<FileAccess> allowedAccesses,
                                   List<FileAccess> blockedAccesses, SecuritySeverity severity,
                                   List<String> recommendations) {
            this.isAllowed = isAllowed;
            this.allowedAccesses = allowedAccesses != null ? allowedAccesses : new ArrayList<>();
            this.blockedAccesses = blockedAccesses != null ? blockedAccesses : new ArrayList<>();
            this.severity = severity;
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
        
        public boolean isAllowed() { return isAllowed; }
        public List<FileAccess> getAllowedAccesses() { return new ArrayList<>(allowedAccesses); }
        public List<FileAccess> getBlockedAccesses() { return new ArrayList<>(blockedAccesses); }
        public SecuritySeverity getSeverity() { return severity; }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
    }
    
    /**
     * 系统命令检查结果
     */
    class SystemCommandCheckResult {
        private final boolean isAllowed;
        private final List<SystemCommand> allowedCommands;
        private final List<SystemCommand> blockedCommands;
        private final SecuritySeverity severity;
        private final List<String> recommendations;
        
        public SystemCommandCheckResult(boolean isAllowed, List<SystemCommand> allowedCommands,
                                      List<SystemCommand> blockedCommands, SecuritySeverity severity,
                                      List<String> recommendations) {
            this.isAllowed = isAllowed;
            this.allowedCommands = allowedCommands != null ? allowedCommands : new ArrayList<>();
            this.blockedCommands = blockedCommands != null ? blockedCommands : new ArrayList<>();
            this.severity = severity;
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
        
        public boolean isAllowed() { return isAllowed; }
        public List<SystemCommand> getAllowedCommands() { return new ArrayList<>(allowedCommands); }
        public List<SystemCommand> getBlockedCommands() { return new ArrayList<>(blockedCommands); }
        public SecuritySeverity getSeverity() { return severity; }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
    }
    
    /**
     * 安全报告
     */
    class SecurityReport {
        private final SecurityReportType reportType;
        private final String scriptPath;
        private final SecuritySummary summary;
        private final List<SecurityFinding> findings;
        private final List<String> recommendations;
        private final Map<String, Object> details;
        
        public SecurityReport(SecurityReportType reportType, String scriptPath, SecuritySummary summary,
                            List<SecurityFinding> findings, List<String> recommendations, 
                            Map<String, Object> details) {
            this.reportType = reportType;
            this.scriptPath = scriptPath;
            this.summary = summary;
            this.findings = findings != null ? findings : new ArrayList<>();
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
            this.details = details != null ? details : new HashMap<>();
        }
        
        public SecurityReportType getReportType() { return reportType; }
        public String getScriptPath() { return scriptPath; }
        public SecuritySummary getSummary() { return summary; }
        public List<SecurityFinding> getFindings() { return new ArrayList<>(findings); }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
        public Map<String, Object> getDetails() { return new HashMap<>(details); }
    }
    
    /**
     * 安全规则
     */
    class SecurityRule {
        private final String ruleId;
        private final String name;
        private final String description;
        private final SecurityRuleType ruleType;
        private final SecuritySeverity severity;
        private final boolean enabled;
        private final Map<String, Object> parameters;
        private final String pattern;
        private final RuleAction action;
        
        public SecurityRule(String ruleId, String name, String description, SecurityRuleType ruleType,
                          SecuritySeverity severity, boolean enabled, Map<String, Object> parameters,
                          String pattern, RuleAction action) {
            this.ruleId = ruleId;
            this.name = name;
            this.description = description;
            this.ruleType = ruleType;
            this.severity = severity;
            this.enabled = enabled;
            this.parameters = parameters != null ? parameters : new HashMap<>();
            this.pattern = pattern;
            this.action = action;
        }
        
        public String getRuleId() { return ruleId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public SecurityRuleType getRuleType() { return ruleType; }
        public SecuritySeverity getSeverity() { return severity; }
        public boolean isEnabled() { return enabled; }
        public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
        public String getPattern() { return pattern; }
        public RuleAction getAction() { return action; }
    }
    
    // 支持类
    class SecurityIssue {
        private final String type;
        private final String description;
        private final SecuritySeverity severity;
        private final String location;
        private final String recommendation;
        
        public SecurityIssue(String type, String description, SecuritySeverity severity, 
                           String location, String recommendation) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.location = location;
            this.recommendation = recommendation;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public SecuritySeverity getSeverity() { return severity; }
        public String getLocation() { return location; }
        public String getRecommendation() { return recommendation; }
    }
    
    class MaliciousPattern {
        private final String pattern;
        private final String description;
        private final SecuritySeverity severity;
        private final int lineNumber;
        private final String context;
        
        public MaliciousPattern(String pattern, String description, SecuritySeverity severity,
                              int lineNumber, String context) {
            this.pattern = pattern;
            this.description = description;
            this.severity = severity;
            this.lineNumber = lineNumber;
            this.context = context;
        }
        
        public String getPattern() { return pattern; }
        public String getDescription() { return description; }
        public SecuritySeverity getSeverity() { return severity; }
        public int getLineNumber() { return lineNumber; }
        public String getContext() { return context; }
    }
    
    class NetworkAccess {
        private final String host;
        private final int port;
        private final String protocol;
        private final boolean isAllowed;
        
        public NetworkAccess(String host, int port, String protocol, boolean isAllowed) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.isAllowed = isAllowed;
        }
        
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getProtocol() { return protocol; }
        public boolean isAllowed() { return isAllowed; }
    }
    
    class FileAccess {
        private final String filePath;
        private final String accessType; // READ, WRITE, EXECUTE
        private final boolean isAllowed;
        
        public FileAccess(String filePath, String accessType, boolean isAllowed) {
            this.filePath = filePath;
            this.accessType = accessType;
            this.isAllowed = isAllowed;
        }
        
        public String getFilePath() { return filePath; }
        public String getAccessType() { return accessType; }
        public boolean isAllowed() { return isAllowed; }
    }
    
    class SystemCommand {
        private final String command;
        private final String arguments;
        private final boolean isAllowed;
        
        public SystemCommand(String command, String arguments, boolean isAllowed) {
            this.command = command;
            this.arguments = arguments;
            this.isAllowed = isAllowed;
        }
        
        public String getCommand() { return command; }
        public String getArguments() { return arguments; }
        public boolean isAllowed() { return isAllowed; }
    }
    
    class SecuritySummary {
        private final int totalChecks;
        private final int passedChecks;
        private final int failedChecks;
        private final int warnings;
        private final SecuritySeverity overallSeverity;
        private final boolean isSecure;
        
        public SecuritySummary(int totalChecks, int passedChecks, int failedChecks, int warnings,
                             SecuritySeverity overallSeverity, boolean isSecure) {
            this.totalChecks = totalChecks;
            this.passedChecks = passedChecks;
            this.failedChecks = failedChecks;
            this.warnings = warnings;
            this.overallSeverity = overallSeverity;
            this.isSecure = isSecure;
        }
        
        public int getTotalChecks() { return totalChecks; }
        public int getPassedChecks() { return passedChecks; }
        public int getFailedChecks() { return failedChecks; }
        public int getWarnings() { return warnings; }
        public SecuritySeverity getOverallSeverity() { return overallSeverity; }
        public boolean isSecure() { return isSecure; }
    }
    
    class SecurityFinding {
        private final String findingId;
        private final String title;
        private final String description;
        private final SecuritySeverity severity;
        private final String category;
        private final String recommendation;
        private final Map<String, Object> evidence;
        
        public SecurityFinding(String findingId, String title, String description, SecuritySeverity severity,
                             String category, String recommendation, Map<String, Object> evidence) {
            this.findingId = findingId;
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.category = category;
            this.recommendation = recommendation;
            this.evidence = evidence != null ? evidence : new HashMap<>();
        }
        
        public String getFindingId() { return findingId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public SecuritySeverity getSeverity() { return severity; }
        public String getCategory() { return category; }
        public String getRecommendation() { return recommendation; }
        public Map<String, Object> getEvidence() { return new HashMap<>(evidence); }
    }
    
    // 枚举类型
    enum SecuritySeverity {
        LOW("LOW", 1),
        MEDIUM("MEDIUM", 2),
        HIGH("HIGH", 3),
        CRITICAL("CRITICAL", 4);
        
        private final String name;
        private final int level;
        
        SecuritySeverity(String name, int level) {
            this.name = name;
            this.level = level;
        }
        
        public String getName() { return name; }
        public int getLevel() { return level; }
    }
    
    enum SecurityReportType {
        QUICK_SCAN("QUICK_SCAN", "快速扫描"),
        COMPREHENSIVE_SCAN("COMPREHENSIVE_SCAN", "全面扫描"),
        COMPLIANCE_REPORT("COMPLIANCE_REPORT", "合规报告"),
        THREAT_ANALYSIS("THREAT_ANALYSIS", "威胁分析");
        
        private final String name;
        private final String description;
        
        SecurityReportType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    enum SecurityRuleType {
        PATTERN_MATCH("PATTERN_MATCH", "模式匹配"),
        WHITELIST("WHITELIST", "白名单"),
        BLACKLIST("BLACKLIST", "黑名单"),
        PERMISSION_CHECK("PERMISSION_CHECK", "权限检查"),
        CONTENT_ANALYSIS("CONTENT_ANALYSIS", "内容分析");
        
        private final String name;
        private final String description;
        
        SecurityRuleType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    enum RuleAction {
        BLOCK("BLOCK", "阻止"),
        WARN("WARN", "警告"),
        ALLOW("ALLOW", "允许"),
        QUARANTINE("QUARANTINE", "隔离");
        
        private final String name;
        private final String description;
        
        RuleAction(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}