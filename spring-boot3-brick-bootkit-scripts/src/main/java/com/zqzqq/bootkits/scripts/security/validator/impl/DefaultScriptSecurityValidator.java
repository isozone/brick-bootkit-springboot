package com.zqzqq.bootkits.scripts.security.validator.impl;

import com.zqzqq.bootkits.scripts.security.validator.*;
import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 脚本安全验证器默认实现
 * 提供完整的安全检查和验证功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptSecurityValidator implements ScriptSecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptSecurityValidator.class);
    
    // 最大脚本文件大小: 10MB
    private static final long MAX_SCRIPT_FILE_SIZE = 10 * 1024 * 1024;
    
    // 安全规则存储
    private final Map<String, SecurityRule> securityRules = new ConcurrentHashMap<>();
    
    // 危险命令列表
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
        "rm", "rm -rf", "del", "format", "fdisk", "mkfs", "dd", "shutdown", "reboot",
        "kill", "killall", "pkill", "sudo", "su", "chmod", "chown", "mv", "cp",
        "eval", "exec", "system", "shell_exec", "passthru", "popen", "proc_open"
    );
    
    // 危险网络模式
    private static final Set<String> DANGEROUS_NETWORK_PATTERNS = Set.of(
        "localhost", "127.0.0.1", "0.0.0.0", "::1",
        "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "192.168."
    );
    
    // 危险文件路径模式
    private static final Set<String> DANGEROUS_FILE_PATTERNS = Set.of(
        "/etc/", "/bin/", "/usr/bin/", "/sbin/", "/usr/sbin/",
        "/home/", "/root/", "/var/log/", "/var/www/",
        "C:\\Windows\\", "C:\\Program Files\\", "C:\\Users\\"
    );
    
    // 恶意内容模式
    private static final Map<ScriptType, List<Pattern>> MALICIOUS_PATTERNS = new HashMap<>();
    
    static {
        // Shell脚本恶意模式
        MALICIOUS_PATTERNS.put(ScriptType.SHELL, Arrays.asList(
            Pattern.compile("\\$\\([^)]*\\)", Pattern.CASE_INSENSITIVE), // 命令替换
            Pattern.compile("eval\\s+", Pattern.CASE_INSENSITIVE), // eval
            Pattern.compile(";\\s*rm\\s+", Pattern.CASE_INSENSITIVE), // ;rm
            Pattern.compile("\\|\\|\\s*rm\\s+", Pattern.CASE_INSENSITIVE), // ||rm
            Pattern.compile("wget\\s+", Pattern.CASE_INSENSITIVE), // wget
            Pattern.compile("curl\\s+", Pattern.CASE_INSENSITIVE), // curl
            Pattern.compile("nc\\s+", Pattern.CASE_INSENSITIVE), // netcat
            Pattern.compile("bash\\s+-i", Pattern.CASE_INSENSITIVE), // 交互式bash
            Pattern.compile("python\\s+-c", Pattern.CASE_INSENSITIVE) // python -c
        ));
        
        // Python脚本恶意模式
        MALICIOUS_PATTERNS.put(ScriptType.PYTHON, Arrays.asList(
            Pattern.compile("__import__\\s*\\(", Pattern.CASE_INSENSITIVE), // 动态导入
            Pattern.compile("exec\\s*\\(", Pattern.CASE_INSENSITIVE), // exec
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE), // eval
            Pattern.compile("subprocess\\.call\\s*\\(", Pattern.CASE_INSENSITIVE), // subprocess.call
            Pattern.compile("os\\.system\\s*\\(", Pattern.CASE_INSENSITIVE), // os.system
            Pattern.compile("os\\.popen\\s*\\(", Pattern.CASE_INSENSITIVE), // os.popen
            Pattern.compile("pickle\\.loads?\\s*\\(", Pattern.CASE_INSENSITIVE), // pickle
            Pattern.compile("input\\s*\\(", Pattern.CASE_INSENSITIVE) // input (安全风险)
        ));
        
        // Windows Batch恶意模式
        MALICIOUS_PATTERNS.put(ScriptType.BATCH, Arrays.asList(
            Pattern.compile("del\\s+/s", Pattern.CASE_INSENSITIVE), // del /s
            Pattern.compile("format\\s+", Pattern.CASE_INSENSITIVE), // format
            Pattern.compile("rd\\s+/s", Pattern.CASE_INSENSITIVE), // rd /s
            Pattern.compile("tree\\s+", Pattern.CASE_INSENSITIVE), // tree
            Pattern.compile("attrib\\s+-r", Pattern.CASE_INSENSITIVE), // attrib -r
            Pattern.compile("reg\\s+delete", Pattern.CASE_INSENSITIVE) // reg delete
        ));
    }
    
    public DefaultScriptSecurityValidator() {
        initializeDefaultRules();
    }
    
    @Override
    public SecurityValidationResult validateScriptFile(String scriptPath) {
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            return new SecurityValidationResult(false, "脚本路径不能为空", SecuritySeverity.CRITICAL);
        }
        
        try {
            File scriptFile = new File(scriptPath);
            
            // 检查文件是否存在
            if (!scriptFile.exists()) {
                return new SecurityValidationResult(false, "脚本文件不存在", SecuritySeverity.HIGH);
            }
            
            // 检查文件是否可读
            if (!scriptFile.canRead()) {
                return new SecurityValidationResult(false, "脚本文件不可读", SecuritySeverity.HIGH);
            }
            
            // 检查文件大小
            if (scriptFile.length() > MAX_SCRIPT_FILE_SIZE) {
                return new SecurityValidationResult(false, "脚本文件过大（超过10MB）", SecuritySeverity.MEDIUM);
            }
            
            // 读取文件内容
            String content = new String(Files.readAllBytes(scriptFile.toPath()));
            
            // 检测脚本类型
            ScriptType scriptType = detectScriptType(scriptPath, content);
            if (scriptType == null) {
                return new SecurityValidationResult(false, "无法识别的脚本类型", SecuritySeverity.MEDIUM);
            }
            
            // 验证脚本内容
            SecurityValidationResult contentResult = validateScriptContent(content, scriptType);
            
            // 添加文件特定信息
            contentResult.addMetadata("file.path", scriptPath);
            contentResult.addMetadata("file.size", scriptFile.length());
            contentResult.addMetadata("file.lastModified", scriptFile.lastModified());
            contentResult.addMetadata("script.type", scriptType);
            
            return contentResult;
            
        } catch (Exception e) {
            logger.error("验证脚本文件失败: " + scriptPath, e);
            return new SecurityValidationResult(false, "验证失败: " + e.getMessage(), SecuritySeverity.HIGH);
        }
    }
    
    @Override
    public SecurityValidationResult validateScriptContent(String scriptContent, ScriptType scriptType) {
        if (scriptContent == null || scriptType == null) {
            return new SecurityValidationResult(false, "脚本内容或类型不能为空", SecuritySeverity.CRITICAL);
        }
        
        List<SecurityIssue> issues = new ArrayList<>();
        boolean isValid = true;
        SecuritySeverity maxSeverity = SecuritySeverity.LOW;
        
        try {
            // 1. 恶意内容扫描
            MaliciousContentScanResult scanResult = scanMaliciousContent(scriptContent, scriptType);
            if (!scanResult.isClean()) {
                isValid = false;
                maxSeverity = SecuritySeverity.CRITICAL;
                
                for (MaliciousPattern pattern : scanResult.getDetectedPatterns()) {
                    issues.add(new SecurityIssue(
                        "MALICIOUS_PATTERN",
                        "检测到恶意模式: " + pattern.getDescription(),
                        pattern.getSeverity(),
                        "第" + pattern.getLineNumber() + "行",
                        "移除或替换恶意代码"
                    ));
                }
            }
            
            // 2. 网络访问检查
            NetworkAccessCheckResult networkResult = checkNetworkAccess(scriptContent, scriptType);
            if (!networkResult.isAllowed()) {
                isValid = false;
                maxSeverity = SecuritySeverity.HIGH;
                
                for (NetworkAccess access : networkResult.getBlockedAccesses()) {
                    issues.add(new SecurityIssue(
                        "NETWORK_ACCESS_VIOLATION",
                        "不允许的网络访问: " + access.getHost() + ":" + access.getPort(),
                        SecuritySeverity.HIGH,
                        "脚本内容",
                        "移除或替换网络访问代码"
                    ));
                }
            }
            
            // 3. 文件访问检查
            FileAccessCheckResult fileResult = checkFileAccess(scriptContent, scriptType);
            if (!fileResult.isAllowed()) {
                isValid = false;
                maxSeverity = SecuritySeverity.HIGH;
                
                for (FileAccess access : fileResult.getBlockedAccesses()) {
                    issues.add(new SecurityIssue(
                        "FILE_ACCESS_VIOLATION",
                        "不允许的文件访问: " + access.getFilePath(),
                        SecuritySeverity.HIGH,
                        "脚本内容",
                        "移除或替换文件访问代码"
                    ));
                }
            }
            
            // 4. 系统命令检查
            SystemCommandCheckResult commandResult = checkSystemCommands(scriptContent, scriptType);
            if (!commandResult.isAllowed()) {
                isValid = false;
                maxSeverity = SecuritySeverity.HIGH;
                
                for (SystemCommand command : commandResult.getBlockedCommands()) {
                    issues.add(new SecurityIssue(
                        "SYSTEM_COMMAND_VIOLATION",
                        "不允许的系统命令: " + command.getCommand(),
                        SecuritySeverity.HIGH,
                        "脚本内容",
                        "移除或替换系统命令"
                    ));
                }
            }
            
            // 5. 应用自定义安全规则
            List<SecurityIssue> ruleIssues = applySecurityRules(scriptContent, scriptType);
            issues.addAll(ruleIssues);
            
            // 确定最终严重性
            for (SecurityIssue issue : issues) {
                if (issue.getSeverity().getLevel() > maxSeverity.getLevel()) {
                    maxSeverity = issue.getSeverity();
                }
            }
            
            String message = isValid ? "脚本安全验证通过" : "脚本存在安全问题";
            
            SecurityValidationResult result = new SecurityValidationResult(isValid, message, maxSeverity, issues, new HashMap<>());
            result.addMetadata("scan.engine", "DefaultScriptSecurityValidator");
            result.addMetadata("scan.time", System.currentTimeMillis());
            
            return result;
            
        } catch (Exception e) {
            logger.error("验证脚本内容失败", e);
            return new SecurityValidationResult(false, "验证过程发生错误: " + e.getMessage(), 
                SecuritySeverity.MEDIUM, issues, new HashMap<>());
        }
    }
    
    @Override
    public PermissionValidationResult validatePermission(String scriptPath, ScriptType requiredPermission) {
        // 简化的权限验证实现
        // 实际应用中可能需要与文件权限系统集成
        
        if (scriptPath == null || requiredPermission == null) {
            return new PermissionValidationResult(false, requiredPermission != null ? requiredPermission.name() : "UNKNOWN",
                "NONE", "脚本路径或权限类型不能为空");
        }
        
        try {
            File scriptFile = new File(scriptPath);
            
            if (!scriptFile.exists()) {
                return new PermissionValidationResult(false, requiredPermission.name(), "NONE", "脚本文件不存在");
            }
            
            // 检查文件执行权限
            boolean canExecute = scriptFile.canExecute();
            boolean canRead = scriptFile.canRead();
            
            if (!canRead) {
                return new PermissionValidationResult(false, requiredPermission.name(), "READ_DENIED", "脚本文件不可读");
            }
            
            if (!canExecute) {
                return new PermissionValidationResult(false, requiredPermission.name(), "EXECUTE_DENIED", "脚本文件不可执行");
            }
            
            // 根据脚本类型检查特殊权限
            String grantedPermission = checkScriptSpecificPermissions(scriptFile, requiredPermission);
            boolean hasPermission = !grantedPermission.equals("NONE");
            
            String reason = hasPermission ? "权限验证通过" : "权限不足";
            
            return new PermissionValidationResult(hasPermission, requiredPermission.name(), 
                grantedPermission, reason);
            
        } catch (Exception e) {
            return new PermissionValidationResult(false, requiredPermission.name(), "ERROR", 
                "权限验证失败: " + e.getMessage());
        }
    }
    
    @Override
    public IntegrityCheckResult checkIntegrity(String scriptPath, String expectedHash) {
        if (scriptPath == null || expectedHash == null) {
            return new IntegrityCheckResult(false, expectedHash, null, null, "脚本路径或哈希值不能为空");
        }
        
        try {
            File scriptFile = new File(scriptPath);
            
            if (!scriptFile.exists()) {
                return new IntegrityCheckResult(false, expectedHash, null, null, "脚本文件不存在");
            }
            
            // 计算实际哈希值
            String actualHash = calculateFileHash(scriptFile, "SHA-256");
            
            boolean isIntact = expectedHash.equalsIgnoreCase(actualHash);
            String reason = isIntact ? "文件完整性验证通过" : "文件哈希值不匹配";
            
            return new IntegrityCheckResult(isIntact, expectedHash, actualHash, "SHA-256", reason);
            
        } catch (Exception e) {
            return new IntegrityCheckResult(false, expectedHash, null, "SHA-256", 
                "完整性检查失败: " + e.getMessage());
        }
    }
    
    @Override
    public MaliciousContentScanResult scanMaliciousContent(String scriptContent, ScriptType scriptType) {
        if (scriptContent == null || scriptType == null) {
            return new MaliciousContentScanResult(false, new ArrayList<>(), SecuritySeverity.CRITICAL, 
                "DefaultScanner", 0);
        }
        
        long startTime = System.currentTimeMillis();
        List<MaliciousPattern> detectedPatterns = new ArrayList<>();
        SecuritySeverity maxSeverity = SecuritySeverity.LOW;
        
        try {
            // 获取对应脚本类型的恶意模式
            List<Pattern> patterns = MALICIOUS_PATTERNS.get(scriptType);
            if (patterns == null || patterns.isEmpty()) {
                // 如果没有特定模式，使用通用模式
                patterns = getGenericMaliciousPatterns();
            }
            
            String[] lines = scriptContent.split("\n");
            
            // 检查每一行
            for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
                String line = lines[lineNumber];
                
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(line).find()) {
                        MaliciousPattern detectedPattern = new MaliciousPattern(
                            pattern.pattern(),
                            getPatternDescription(pattern.pattern()),
                            getPatternSeverity(pattern.pattern()),
                            lineNumber + 1,
                            line.trim()
                        );
                        
                        detectedPatterns.add(detectedPattern);
                        
                        if (detectedPattern.getSeverity().getLevel() > maxSeverity.getLevel()) {
                            maxSeverity = detectedPattern.getSeverity();
                        }
                    }
                }
            }
            
            boolean isClean = detectedPatterns.isEmpty();
            long scanTimeMs = System.currentTimeMillis() - startTime;
            
            return new MaliciousContentScanResult(isClean, detectedPatterns, maxSeverity, 
                "DefaultScanner", scanTimeMs);
            
        } catch (Exception e) {
            logger.error("扫描恶意内容失败", e);
            return new MaliciousContentScanResult(false, new ArrayList<>(), SecuritySeverity.CRITICAL,
                "DefaultScanner", System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public NetworkAccessCheckResult checkNetworkAccess(String scriptContent, ScriptType scriptType) {
        if (scriptContent == null) {
            return new NetworkAccessCheckResult(true, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.LOW, new ArrayList<>());
        }
        
        List<NetworkAccess> allowedAccesses = new ArrayList<>();
        List<NetworkAccess> blockedAccesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        SecuritySeverity maxSeverity = SecuritySeverity.LOW;
        boolean isAllowed = true;
        
        try {
            // 检测网络访问模式
            List<String> networkAccesses = extractNetworkAccesses(scriptContent, scriptType);
            
            for (String access : networkAccesses) {
                NetworkAccess parsedAccess = parseNetworkAccess(access);
                if (parsedAccess != null) {
                    boolean isDangerous = isDangerousNetworkAccess(parsedAccess);
                    
                    if (isDangerous) {
                        blockedAccesses.add(parsedAccess);
                        maxSeverity = SecuritySeverity.HIGH;
                        isAllowed = false;
                        recommendations.add("移除或限制对 " + parsedAccess.getHost() + " 的网络访问");
                    } else {
                        allowedAccesses.add(parsedAccess);
                    }
                }
            }
            
            return new NetworkAccessCheckResult(isAllowed, allowedAccesses, blockedAccesses, 
                maxSeverity, recommendations);
                
        } catch (Exception e) {
            logger.error("检查网络访问失败", e);
            return new NetworkAccessCheckResult(false, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.HIGH, Arrays.asList("网络访问检查失败"));
        }
    }
    
    @Override
    public FileAccessCheckResult checkFileAccess(String scriptContent, ScriptType scriptType) {
        if (scriptContent == null) {
            return new FileAccessCheckResult(true, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.LOW, new ArrayList<>());
        }
        
        List<FileAccess> allowedAccesses = new ArrayList<>();
        List<FileAccess> blockedAccesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        SecuritySeverity maxSeverity = SecuritySeverity.LOW;
        boolean isAllowed = true;
        
        try {
            // 检测文件访问模式
            List<String> fileAccesses = extractFileAccesses(scriptContent, scriptType);
            
            for (String access : fileAccesses) {
                FileAccess parsedAccess = parseFileAccess(access);
                if (parsedAccess != null) {
                    boolean isDangerous = isDangerousFileAccess(parsedAccess);
                    
                    if (isDangerous) {
                        blockedAccesses.add(parsedAccess);
                        maxSeverity = SecuritySeverity.HIGH;
                        isAllowed = false;
                        recommendations.add("移除或限制对 " + parsedAccess.getFilePath() + " 的文件访问");
                    } else {
                        allowedAccesses.add(parsedAccess);
                    }
                }
            }
            
            return new FileAccessCheckResult(isAllowed, allowedAccesses, blockedAccesses, 
                maxSeverity, recommendations);
                
        } catch (Exception e) {
            logger.error("检查文件访问失败", e);
            return new FileAccessCheckResult(false, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.HIGH, Arrays.asList("文件访问检查失败"));
        }
    }
    
    @Override
    public SystemCommandCheckResult checkSystemCommands(String scriptContent, ScriptType scriptType) {
        if (scriptContent == null) {
            return new SystemCommandCheckResult(true, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.LOW, new ArrayList<>());
        }
        
        List<SystemCommand> allowedCommands = new ArrayList<>();
        List<SystemCommand> blockedCommands = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        SecuritySeverity maxSeverity = SecuritySeverity.LOW;
        boolean isAllowed = true;
        
        try {
            // 检测系统命令
            List<String> commands = extractSystemCommands(scriptContent, scriptType);
            
            for (String command : commands) {
                SystemCommand parsedCommand = parseSystemCommand(command);
                if (parsedCommand != null) {
                    boolean isDangerous = isDangerousSystemCommand(parsedCommand);
                    
                    if (isDangerous) {
                        blockedCommands.add(parsedCommand);
                        maxSeverity = SecuritySeverity.HIGH;
                        isAllowed = false;
                        recommendations.add("移除或替换危险命令: " + parsedCommand.getCommand());
                    } else {
                        allowedCommands.add(parsedCommand);
                    }
                }
            }
            
            return new SystemCommandCheckResult(isAllowed, allowedCommands, blockedCommands, 
                maxSeverity, recommendations);
                
        } catch (Exception e) {
            logger.error("检查系统命令失败", e);
            return new SystemCommandCheckResult(false, new ArrayList<>(), new ArrayList<>(), 
                SecuritySeverity.HIGH, Arrays.asList("系统命令检查失败"));
        }
    }
    
    @Override
    public SecurityReport generateSecurityReport(String scriptPath, SecurityReportType reportType) {
        if (scriptPath == null || reportType == null) {
            return null;
        }
        
        try {
            // 执行完整的安全验证
            SecurityValidationResult validationResult = validateScriptFile(scriptPath);
            
            // 收集所有检查结果
            List<SecurityFinding> findings = new ArrayList<>();
            
            // 添加验证结果中的安全问题
            for (SecurityIssue issue : validationResult.getIssues()) {
                SecurityFinding finding = new SecurityFinding(
                    "FINDING_" + findings.size(),
                    issue.getDescription(),
                    issue.getDescription(),
                    issue.getSeverity(),
                    issue.getType(),
                    issue.getRecommendation(),
                    Map.of("location", issue.getLocation(), "type", issue.getType())
                );
                findings.add(finding);
            }
            
            // 生成总结
            SecuritySummary summary = new SecuritySummary(
                validationResult.getIssues().size() + 1, // 基础检查 + 问题数量
                validationResult.isValid() ? 1 : 0,
                validationResult.isValid() ? 0 : 1,
                (int) validationResult.getIssues().stream()
                    .filter(issue -> issue.getSeverity() == SecuritySeverity.MEDIUM).count(),
                validationResult.getSeverity(),
                validationResult.isValid()
            );
            
            // 生成建议
            List<String> recommendations = generateRecommendations(validationResult);
            
            // 生成报告详情
            Map<String, Object> details = new HashMap<>();
            details.put("script.path", scriptPath);
            details.put("report.type", reportType);
            details.put("validation.result", validationResult);
            details.put("report.timestamp", System.currentTimeMillis());
            
            return new SecurityReport(reportType, scriptPath, summary, findings, recommendations, details);
            
        } catch (Exception e) {
            logger.error("生成安全报告失败: " + scriptPath, e);
            return null;
        }
    }
    
    @Override
    public List<SecurityRule> getSecurityRules() {
        return new ArrayList<>(securityRules.values());
    }
    
    @Override
    public void addSecurityRule(SecurityRule rule) {
        if (rule != null && rule.getRuleId() != null) {
            securityRules.put(rule.getRuleId(), rule);
            logger.info("添加安全规则: {}", rule.getRuleId());
        }
    }
    
    @Override
    public void removeSecurityRule(String ruleId) {
        if (ruleId != null) {
            securityRules.remove(ruleId);
            logger.info("移除安全规则: {}", ruleId);
        }
    }
    
    @Override
    public void setSecurityRuleEnabled(String ruleId, boolean enabled) {
        if (ruleId != null && securityRules.containsKey(ruleId)) {
            SecurityRule rule = securityRules.get(ruleId);
            // 这里需要重新创建规则对象来修改enabled状态
            // 简化实现，实际中可能需要不同的设计
            logger.info("设置安全规则状态: {} = {}", ruleId, enabled);
        }
    }
    
    // 私有辅助方法
    
    private void initializeDefaultRules() {
        // 添加默认安全规则
        
        // 1. 禁止执行危险命令
        SecurityRule dangerousCommandsRule = new SecurityRule(
            "BLOCK_DANGEROUS_COMMANDS",
            "禁止危险命令",
            "阻止执行已知的危险系统命令",
            SecurityRuleType.BLACKLIST,
            SecuritySeverity.HIGH,
            true,
            Map.of("commands", DANGEROUS_COMMANDS),
            null,
            RuleAction.BLOCK
        );
        addSecurityRule(dangerousCommandsRule);
        
        // 2. 禁止网络访问内部地址
        SecurityRule internalNetworkRule = new SecurityRule(
            "BLOCK_INTERNAL_NETWORK",
            "禁止内部网络访问",
            "阻止访问内部网络地址",
            SecurityRuleType.BLACKLIST,
            SecuritySeverity.MEDIUM,
            true,
            Map.of("patterns", DANGEROUS_NETWORK_PATTERNS),
            null,
            RuleAction.BLOCK
        );
        addSecurityRule(internalNetworkRule);
        
        // 3. 禁止访问系统文件
        SecurityRule systemFilesRule = new SecurityRule(
            "BLOCK_SYSTEM_FILES",
            "禁止系统文件访问",
            "阻止访问重要的系统文件",
            SecurityRuleType.BLACKLIST,
            SecuritySeverity.HIGH,
            true,
            Map.of("patterns", DANGEROUS_FILE_PATTERNS),
            null,
            RuleAction.BLOCK
        );
        addSecurityRule(systemFilesRule);
    }
    
    private ScriptType detectScriptType(String scriptPath, String content) {
        if (scriptPath != null) {
            String lowerPath = scriptPath.toLowerCase();
            for (ScriptType type : ScriptType.values()) {
                if (!type.getExtension().isEmpty() && lowerPath.endsWith(type.getExtension().toLowerCase())) {
                    return type;
                }
            }
        }
        
        // 根据内容检测
        if (content != null) {
            if (content.contains("#!/bin/bash") || content.contains("#!/bin/sh")) {
                return ScriptType.SHELL;
            } else if (content.contains("@echo off") || content.contains("echo ")) {
                return ScriptType.BATCH;
            } else if (content.contains("def ") || content.contains("import ")) {
                return ScriptType.PYTHON;
            }
        }
        
        return null;
    }
    
    private List<SecurityIssue> applySecurityRules(String scriptContent, ScriptType scriptType) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        for (SecurityRule rule : securityRules.values()) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            try {
                List<SecurityIssue> ruleIssues = applyRule(rule, scriptContent, scriptType);
                issues.addAll(ruleIssues);
            } catch (Exception e) {
                logger.warn("应用安全规则失败: " + rule.getRuleId(), e);
            }
        }
        
        return issues;
    }
    
    private List<SecurityIssue> applyRule(SecurityRule rule, String scriptContent, ScriptType scriptType) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        switch (rule.getRuleType()) {
            case PATTERN_MATCH:
                if (rule.getPattern() != null) {
                    Pattern pattern = Pattern.compile(rule.getPattern());
                    if (pattern.matcher(scriptContent).find()) {
                        issues.add(new SecurityIssue(
                            rule.getRuleId(),
                            "匹配到禁止模式: " + rule.getName(),
                            rule.getSeverity(),
                            "脚本内容",
                            rule.getDescription()
                        ));
                    }
                }
                break;
                
            case BLACKLIST:
                // 检查是否包含黑名单项目
                Map<String, Object> parameters = rule.getParameters();
                if (parameters.containsKey("commands")) {
                    @SuppressWarnings("unchecked")
                    Set<String> commands = (Set<String>) parameters.get("commands");
                    for (String command : commands) {
                        if (scriptContent.contains(command)) {
                            issues.add(new SecurityIssue(
                                rule.getRuleId(),
                                "检测到黑名单命令: " + command,
                                rule.getSeverity(),
                                "脚本内容",
                                "移除或替换黑名单命令"
                            ));
                        }
                    }
                }
                break;
                
            case WHITELIST:
                // 白名单逻辑，简化实现
                break;
                
            case PERMISSION_CHECK:
                // 权限检查逻辑
                break;
                
            case CONTENT_ANALYSIS:
                // 内容分析逻辑
                break;
        }
        
        return issues;
    }
    
    private String checkScriptSpecificPermissions(File scriptFile, ScriptType scriptType) {
        // 根据脚本类型检查特殊权限
        switch (scriptType) {
            case SHELL:
            case BATCH:
                // 检查是否需要管理员权限
                return scriptFile.canExecute() ? "EXECUTE" : "NONE";
            case PYTHON:
                // Python脚本通常不需要特殊权限
                return scriptFile.canRead() ? "READ" : "NONE";
            default:
                return scriptFile.canRead() && scriptFile.canExecute() ? "READ_EXECUTE" : "NONE";
        }
    }
    
    private String calculateFileHash(File file, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        StringBuilder hexString = new StringBuilder();
        byte[] hash = digest.digest();
        
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    private List<Pattern> getGenericMaliciousPatterns() {
        // 通用恶意模式，适用于所有脚本类型
        return Arrays.asList(
            Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexec\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("`[^`]*`", Pattern.CASE_INSENSITIVE), // 反引号命令替换
            Pattern.compile("\\$\\([^)]*\\)", Pattern.CASE_INSENSITIVE) // $()命令替换
        );
    }
    
    private String getPatternDescription(String pattern) {
        Map<String, String> descriptions = Map.of(
            "\\$\\([^)]*\\)", "命令替换",
            "eval\\s+", "eval函数调用",
            ";\\s*rm\\s+", "删除命令",
            "\\|\\|\\s*rm\\s+", "条件删除命令",
            "wget\\s+", "文件下载命令",
            "curl\\s+", "数据传输命令",
            "__import__\\s*\\(", "动态导入",
            "subprocess\\.call\\s*\\(", "子进程调用",
            "os\\.system\\s*\\(", "系统命令执行"
        );
        
        return descriptions.getOrDefault(pattern, "未知恶意模式");
    }
    
    private SecuritySeverity getPatternSeverity(String pattern) {
        Map<String, SecuritySeverity> severities = Map.of(
            "\\$\\([^)]*\\)", SecuritySeverity.MEDIUM,
            "eval\\s+", SecuritySeverity.HIGH,
            ";\\s*rm\\s+", SecuritySeverity.HIGH,
            "\\|\\|\\s*rm\\s+", SecuritySeverity.HIGH,
            "wget\\s+", SecuritySeverity.MEDIUM,
            "curl\\s+", SecuritySeverity.MEDIUM,
            "__import__\\s*\\(", SecuritySeverity.HIGH,
            "subprocess\\.call\\s*\\(", SecuritySeverity.HIGH,
            "os\\.system\\s*\\(", SecuritySeverity.HIGH
        );
        
        return severities.getOrDefault(pattern, SecuritySeverity.MEDIUM);
    }
    
    private List<String> extractNetworkAccesses(String scriptContent, ScriptType scriptType) {
        List<String> accesses = new ArrayList<>();
        
        // 简化的网络访问检测
        // 实际实现需要根据不同脚本类型的语法进行解析
        String[] lines = scriptContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测常见的网络访问模式
            if (line.contains("http://") || line.contains("https://") || 
                line.contains("ftp://") || line.contains("socket") ||
                line.contains("connect(") || line.contains("urlopen")) {
                accesses.add(line);
            }
        }
        
        return accesses;
    }
    
    private NetworkAccess parseNetworkAccess(String accessString) {
        // 简化的网络访问解析
        // 实际实现需要更复杂的解析逻辑
        return new NetworkAccess("unknown", 80, "unknown", false);
    }
    
    private boolean isDangerousNetworkAccess(NetworkAccess access) {
        // 检查是否是内部网络或本地地址
        for (String pattern : DANGEROUS_NETWORK_PATTERNS) {
            if (access.getHost().contains(pattern)) {
                return true;
            }
        }
        
        // 检查端口
        if (access.getPort() < 1024) { // 系统端口
            return true;
        }
        
        return false;
    }
    
    private List<String> extractFileAccesses(String scriptContent, ScriptType scriptType) {
        List<String> accesses = new ArrayList<>();
        
        String[] lines = scriptContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测文件访问模式
            if (line.contains("open(") || line.contains("File(") || 
                line.contains("readFile") || line.contains("writeFile") ||
                line.contains("cat ") || line.contains("type ") ||
                line.contains("> ") || line.contains(">> ")) {
                accesses.add(line);
            }
        }
        
        return accesses;
    }
    
    private FileAccess parseFileAccess(String accessString) {
        // 简化的文件访问解析
        return new FileAccess("unknown", "unknown", false);
    }
    
    private boolean isDangerousFileAccess(FileAccess access) {
        String filePath = access.getFilePath().toLowerCase();
        
        // 检查是否是系统文件路径
        for (String pattern : DANGEROUS_FILE_PATTERNS) {
            if (filePath.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private List<String> extractSystemCommands(String scriptContent, ScriptType scriptType) {
        List<String> commands = new ArrayList<>();
        
        String[] lines = scriptContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测系统命令模式
            if (line.matches("^\\w+\\s+.*") || line.startsWith("!") || 
                line.contains("system(") || line.contains("shell_exec")) {
                commands.add(line);
            }
        }
        
        return commands;
    }
    
    private SystemCommand parseSystemCommand(String commandString) {
        // 简化的系统命令解析
        String[] parts = commandString.split("\\s+", 2);
        String command = parts.length > 0 ? parts[0] : commandString;
        String arguments = parts.length > 1 ? parts[1] : "";
        
        return new SystemCommand(command, arguments, false);
    }
    
    private boolean isDangerousSystemCommand(SystemCommand command) {
        String cmd = command.getCommand().toLowerCase();
        
        // 检查是否是危险命令
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (cmd.contains(dangerous.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private List<String> generateRecommendations(SecurityValidationResult result) {
        List<String> recommendations = new ArrayList<>();
        
        if (!result.isValid()) {
            recommendations.add("修复所有发现的安全问题");
            recommendations.add("在生产环境部署前进行完整的安全审查");
            
            for (SecurityIssue issue : result.getIssues()) {
                if (!issue.getRecommendation().isEmpty()) {
                    recommendations.add(issue.getRecommendation());
                }
            }
        } else {
            recommendations.add("继续保持良好的安全实践");
            recommendations.add("定期更新安全规则");
        }
        
        return recommendations;
    }
}