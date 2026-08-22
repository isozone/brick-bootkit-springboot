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

import com.zqzqq.bootkits.core.exception.PluginSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件安全审计日志记录器
 * 提供详细的安全事件记录和审计跟踪
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginSecurityAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(PluginSecurityAuditLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("PLUGIN_SECURITY_AUDIT");
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final Map<String, SecurityEventCounter> eventCounters = new ConcurrentHashMap<>();

    /**
     * 记录安全验证事件
     */
    public void logSecurityValidation(String pluginId, PluginSecurityValidationResult result) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "SECURITY_VALIDATION");
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            if (result.isValid()) {
                auditLogger.info("插件安全验证通过 - 插件ID: {}, 风险级别: {}, 验证时间: {}", 
                    pluginId, result.getRiskLevel(), result.getValidationTime());
                incrementEventCounter(pluginId, "VALIDATION_PASSED");
            } else {
                auditLogger.warn("插件安全验证失败 - 插件ID: {}, 违规数量: {}, 风险级别: {}, 详情: {}", 
                    pluginId, result.getViolations().size(), result.getRiskLevel(), 
                    String.join("; ", result.getViolations()));
                incrementEventCounter(pluginId, "VALIDATION_FAILED");
            }
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 记录权限检查事件
     */
    public void logPermissionCheck(String pluginId, PluginPermission permission, boolean granted) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "PERMISSION_CHECK");
            MDC.put("permission", permission.toString());
            MDC.put("result", granted ? "GRANTED" : "DENIED");
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            if (granted) {
                auditLogger.debug("权限检查通过 - 插件ID: {}, 权限: {}", pluginId, permission);
                incrementEventCounter(pluginId, "PERMISSION_GRANTED");
            } else {
                auditLogger.warn("权限检查失败 - 插件ID: {}, 权限: {}, 原因: 权限不足", pluginId, permission);
                incrementEventCounter(pluginId, "PERMISSION_DENIED");
            }
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 记录权限违规事件
     */
    public void logPermissionViolation(String pluginId, PluginPermission permission, String reason) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "PERMISSION_VIOLATION");
            MDC.put("permission", permission.toString());
            MDC.put("reason", reason);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            auditLogger.error("权限违规事件 - 插件ID: {}, 权限: {}, 原因: {}", pluginId, permission, reason);
            incrementEventCounter(pluginId, "PERMISSION_VIOLATION");
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 记录代码扫描事件
     */
    public void logCodeScan(String pluginId, PluginCodeScanResult scanResult) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "CODE_SCAN");
            MDC.put("riskScore", String.valueOf(scanResult.getRiskScore()));
            MDC.put("violationCount", String.valueOf(scanResult.getViolations().size()));
            MDC.put("warningCount", String.valueOf(scanResult.getWarnings().size()));
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            if (scanResult.hasViolations()) {
                auditLogger.warn("代码扫描发现安全问题 - 插件ID: {}, 风险评分: {}, 违规: {}, 警告: {}", 
                    pluginId, scanResult.getRiskScore(), scanResult.getViolations().size(), 
                    scanResult.getWarnings().size());
                incrementEventCounter(pluginId, "CODE_SCAN_VIOLATIONS");
            } else {
                auditLogger.info("代码扫描通过 - 插件ID: {}, 风险评分: {}, 警告: {}", 
                    pluginId, scanResult.getRiskScore(), scanResult.getWarnings().size());
                incrementEventCounter(pluginId, "CODE_SCAN_PASSED");
            }
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 记录安全策略更新事件
     */
    public void logSecurityPolicyUpdate(String pluginId, String policyType, String operation) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "POLICY_UPDATE");
            MDC.put("policyType", policyType);
            MDC.put("operation", operation);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            auditLogger.info("安全策略更新 - 插件ID: {}, 策略类型: {}, 操作: {}", pluginId, policyType, operation);
            incrementEventCounter(pluginId, "POLICY_UPDATE");
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 记录安全异常事件
     */
    public void logSecurityException(String pluginId, PluginSecurityException exception) {
        try {
            MDC.put("pluginId", pluginId);
            MDC.put("eventType", "SECURITY_EXCEPTION");
            MDC.put("violationType", exception.getViolationType().name());
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            auditLogger.error("安全异常事件 - 插件ID: {}, 违规类型: {}, 消息: {}", 
                pluginId, exception.getViolationType(), exception.getMessage(), exception);
            incrementEventCounter(pluginId, "SECURITY_EXCEPTION");
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取插件安全事件统计
     */
    public SecurityEventCounter getEventCounter(String pluginId) {
        return eventCounters.get(pluginId);
    }

    /**
     * 获取所有插件的安全事件统计
     */
    public Map<String, SecurityEventCounter> getAllEventCounters() {
        return new ConcurrentHashMap<>(eventCounters);
    }

    /**
     * 清除插件的事件计数器
     */
    public void clearEventCounter(String pluginId) {
        eventCounters.remove(pluginId);
        logger.info("已清除插件安全事件计数器: {}", pluginId);
    }

    /**
     * 增加事件计数
     */
    private void incrementEventCounter(String pluginId, String eventType) {
        eventCounters.computeIfAbsent(pluginId, k -> new SecurityEventCounter(pluginId))
                    .incrementEvent(eventType);
    }

    /**
     * 安全事件计数器
     */
    public static class SecurityEventCounter {
        private final String pluginId;
        private final Map<String, Long> eventCounts = new ConcurrentHashMap<>();
        private final LocalDateTime createdTime = LocalDateTime.now();

        public SecurityEventCounter(String pluginId) {
            this.pluginId = pluginId;
        }

        public void incrementEvent(String eventType) {
            eventCounts.merge(eventType, 1L, Long::sum);
        }

        public long getEventCount(String eventType) {
            return eventCounts.getOrDefault(eventType, 0L);
        }

        public Map<String, Long> getAllEventCounts() {
            return new ConcurrentHashMap<>(eventCounts);
        }

        public String getPluginId() {
            return pluginId;
        }

        public LocalDateTime getCreatedTime() {
            return createdTime;
        }

        public long getTotalEvents() {
            return eventCounts.values().stream().mapToLong(Long::longValue).sum();
        }
    }
}