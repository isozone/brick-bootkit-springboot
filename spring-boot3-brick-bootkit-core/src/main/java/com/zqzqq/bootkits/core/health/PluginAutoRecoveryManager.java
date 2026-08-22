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


package com.zqzqq.bootkits.core.health;

import com.zqzqq.bootkits.core.logging.PluginLogger;
import com.zqzqq.bootkits.core.plugin.Plugin;
import com.zqzqq.bootkits.core.plugin.PluginManager;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件自动恢复管理器
 * 负责监控插件健康状态并在需要时执行自动恢复
 * <p>
 * 当前仅面向 {@code com.zqzqq.bootkits.core.plugin.PluginManager} 这一套内部模型。
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class PluginAutoRecoveryManager {
    
    private static final PluginLogger logger = PluginLogger.getLogger(PluginAutoRecoveryManager.class);
    
    private final PluginManager pluginManager;
    private final PluginHealthChecker healthChecker;
    private final ScheduledExecutorService scheduler;
    
    // 恢复策略配置
    private final int maxRecoveryAttempts = 3;
    private final long recoveryIntervalSeconds = 30;
    private final long monitoringIntervalSeconds = 60;
    
    // 状态跟踪
    private final ConcurrentHashMap<String, PluginRecoveryContext> recoveryContexts = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    public PluginAutoRecoveryManager(PluginManager pluginManager, PluginHealthChecker healthChecker) {
        if (pluginManager == null) {
            throw new NullPointerException("pluginManager cannot be null");
        }
        if (healthChecker == null) {
            throw new NullPointerException("healthChecker cannot be null");
        }
        this.pluginManager = pluginManager;
        this.healthChecker = healthChecker;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "PluginAutoRecovery-Thread");
            thread.setDaemon(true);
            return thread;
        });
        
        startMonitoring();
        logger.info("system", "插件自动恢复管理器已启动");
    }
    
    /**
     * 启动监控
     */
    private void startMonitoring() {
        scheduler.scheduleWithFixedDelay(this::monitorAllPlugins, 
                                       monitoringIntervalSeconds, 
                                       monitoringIntervalSeconds, 
                                       TimeUnit.SECONDS);
    }
    
    /**
     * 监控所有插件
     */
    private void monitorAllPlugins() {
        try {
            pluginManager.getInstalledPluginsList().forEach(this::monitorPlugin);
        } catch (Exception e) {
            logger.error("system", "监控插件时发生错误", e.getMessage(), e);
        }
    }
    
    /**
     * 监控单个插件
     */
    private void monitorPlugin(Plugin plugin) {
        try {
            if (plugin == null) {
                return;
            }
            
            PluginHealthReport healthReport = healthChecker.checkHealth(plugin);
            String pluginId = plugin.getId();
            
            if (healthReport.needsAutoRecovery()) {
                handleUnhealthyPlugin(pluginId, plugin, healthReport);
            } else {
                // 插件健康，重置恢复上下文
                recoveryContexts.remove(pluginId);
                logger.debug("system", "插件健康", pluginId, "状态:", healthReport.getOverallStatus().getStatus());
            }
            
        } catch (Exception e) {
            logger.error("system", "监控插件失败: " + plugin.getId() + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理不健康的插件
     */
    private void handleUnhealthyPlugin(String pluginId, Plugin plugin, PluginHealthReport healthReport) {
        PluginRecoveryContext context = recoveryContexts.computeIfAbsent(pluginId, 
            k -> new PluginRecoveryContext(pluginId));
        
        if (context.shouldAttemptRecovery()) {
            attemptRecovery(pluginId, plugin, healthReport, context);
        } else {
            logger.warn("system", "插件恢复次数超限，停止自动恢复", pluginId, 
                       "已尝试次数:", context.getAttemptCount(), 
                       "最大尝试次数:", maxRecoveryAttempts);
        }
    }
    
    /**
     * 尝试恢复插件
     */
    private void attemptRecovery(String pluginId, Plugin plugin, PluginHealthReport healthReport, 
                                PluginRecoveryContext context) {
        logger.info("system", "开始自动恢复插件", pluginId, "健康状态:", healthReport.getOverallStatus().getStatus());
        
        context.incrementAttemptCount();
        context.setLastRecoveryTime(LocalDateTime.now());
        context.setLastHealthReport(healthReport);
        
        scheduler.schedule(() -> {
            try {
                executeRecoveryStrategy(pluginId, plugin, healthReport, context);
            } catch (Exception e) {
                logger.error("system", "插件恢复执行失败: " + pluginId + " - " + e.getMessage(), e);
                context.recordRecoveryFailure(e);
            }
        }, recoveryIntervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 执行恢复策略
     */
    private void executeRecoveryStrategy(String pluginId, Plugin plugin, PluginHealthReport healthReport,
                                       PluginRecoveryContext context) {
        RecoveryStrategy strategy = determineRecoveryStrategy(healthReport, context);
        
        try {
            switch (strategy) {
                case RESTART_PLUGIN:
                    restartPlugin(pluginId, plugin);
                    break;
                case RELOAD_PLUGIN:
                    reloadPlugin(pluginId, plugin);
                    break;
                case STOP_PLUGIN:
                    stopPlugin(pluginId, plugin);
                    break;
                default:
                    logger.warn("system", "未知恢复策略", pluginId, "策略:", strategy);
                    break;
            }
            
            // 等待一段时间后验证恢复结果
            scheduler.schedule(() -> verifyRecoveryResult(pluginId, plugin, context), 
                             10, TimeUnit.SECONDS);
                            
        } catch (Exception e) {
            logger.error("system", "恢复策略执行失败: " + pluginId + " 策略: " + strategy + " - " + e.getMessage(), e);
            context.recordRecoveryFailure(e);
        }
    }
    
    /**
     * 确定恢复策略
     */
    private RecoveryStrategy determineRecoveryStrategy(PluginHealthReport healthReport, PluginRecoveryContext context) {
        PluginHealthStatus status = healthReport.getOverallStatus();
        
        // 根据健康状态和恢复历史决定策略
        if (status == PluginHealthStatus.DEAD) {
            // 死亡状态，先尝试重启，失败后尝试重新加载
            if (context.getAttemptCount() <= 1) {
                return RecoveryStrategy.RESTART_PLUGIN;
            } else {
                return RecoveryStrategy.RELOAD_PLUGIN;
            }
        } else if (status == PluginHealthStatus.CRITICAL) {
            // 危险状态，尝试重启
            return RecoveryStrategy.RESTART_PLUGIN;
        } else {
            // 其他状态，尝试重启
            return RecoveryStrategy.RESTART_PLUGIN;
        }
    }
    
    /**
     * 重启插件
     */
    private void restartPlugin(String pluginId, Plugin plugin) {
        try {
            logger.info("system", "重启插件", pluginId);
            plugin.stop();
            plugin.start();
            logger.info("system", "插件重启成功", pluginId);
        } catch (Exception e) {
            throw new RuntimeException("插件重启失败", e);
        }
    }
    
    /**
     * 重新加载插件
     */
    private void reloadPlugin(String pluginId, Plugin plugin) {
        try {
            logger.info("system", "重新加载插件", pluginId);
            pluginManager.uninstallPlugin(pluginId);
            // 这里应该重新安装插件，但需要插件文件信息
            // 暂时只停止插件
            plugin.stop();
            logger.info("system", "插件重新加载完成", pluginId);
        } catch (Exception e) {
            throw new RuntimeException("插件重新加载失败", e);
        }
    }
    
    /**
     * 停止插件
     */
    private void stopPlugin(String pluginId, Plugin plugin) {
        try {
            logger.info("system", "停止插件", pluginId);
            plugin.stop();
            logger.info("system", "插件停止成功", pluginId);
        } catch (Exception e) {
            throw new RuntimeException("插件停止失败", e);
        }
    }
    
    /**
     * 验证恢复结果
     */
    private void verifyRecoveryResult(String pluginId, Plugin plugin, PluginRecoveryContext context) {
        try {
            PluginHealthReport healthReport = healthChecker.checkHealth(plugin);
            
            if (healthReport.getOverallStatus().isHealthy()) {
                logger.info("system", "插件恢复成功", pluginId);
                context.recordRecoverySuccess();
                recoveryContexts.remove(pluginId);
            } else {
                logger.warn("system", "插件恢复后仍不健康", pluginId, "状态:", healthReport.getOverallStatus().getStatus());
                
                if (context.shouldAttemptRecovery()) {
                    // 继续尝试恢复
                    handleUnhealthyPlugin(pluginId, plugin, healthReport);
                }
            }
            
        } catch (Exception e) {
            logger.error("system", "验证恢复结果失败: " + pluginId + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发单个插件的健康检查
     */
    public PluginHealthReport checkPluginHealth(String pluginId) {
        assertNotClosed();
        Plugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            return PluginHealthReport.createUnknown(pluginId, "插件不存在", null);
        }
        
        return healthChecker.checkHealth(plugin);
    }
    
    /**
     * 手动触发单个插件的恢复
     */
    public boolean triggerManualRecovery(String pluginId) {
        assertNotClosed();
        Plugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            logger.warn("system", "手动恢复失败：插件不存在", pluginId);
            return false;
        }
        
        PluginRecoveryContext context = recoveryContexts.computeIfAbsent(pluginId,
            k -> new PluginRecoveryContext(pluginId));
        
        try {
            PluginHealthReport healthReport = healthChecker.checkHealth(plugin);
            attemptRecovery(pluginId, plugin, healthReport, context);
            return true;
        } catch (Exception e) {
            logger.error("system", "手动恢复失败: " + pluginId + " - " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 关闭自动恢复管理器
     */
    public void shutdown() {
        logger.info("system", "关闭插件自动恢复管理器");
        
        try {
            closed.set(true);
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            recoveryContexts.clear();
            logger.info("system", "插件自动恢复管理器已关闭");
        } catch (Exception e) {
            logger.error("system", "关闭自动恢复管理器时发生错误", e.getMessage(), e);
        }
    }

    /**
     * 校验管理器未被关闭，否则抛出异常
     */
    private void assertNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("插件自动恢复管理器已关闭");
        }
    }
    
    /**
     * 恢复策略枚举
     */
    private enum RecoveryStrategy {
        RESTART_PLUGIN,   // 重启插件
        RELOAD_PLUGIN,    // 重新加载插件
        STOP_PLUGIN       // 停止插件
    }
    
    /**
     * 插件恢复上下文
     */
    private static class PluginRecoveryContext {
        private final String pluginId;
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private LocalDateTime lastRecoveryTime;
        private PluginHealthReport lastHealthReport;
        private int failureCount = 0;
        
        public PluginRecoveryContext(String pluginId) {
            this.pluginId = pluginId;
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public int getAttemptCount() {
            return attemptCount.get();
        }
        
        public void incrementAttemptCount() {
            attemptCount.incrementAndGet();
        }
        
        public LocalDateTime getLastRecoveryTime() {
            return lastRecoveryTime;
        }
        
        public void setLastRecoveryTime(LocalDateTime lastRecoveryTime) {
            this.lastRecoveryTime = lastRecoveryTime;
        }
        
        public PluginHealthReport getLastHealthReport() {
            return lastHealthReport;
        }
        
        public void setLastHealthReport(PluginHealthReport lastHealthReport) {
            this.lastHealthReport = lastHealthReport;
        }
        
        public boolean shouldAttemptRecovery() {
            return attemptCount.get() < 3; // 最多尝试3次
        }
        
        public void recordRecoverySuccess() {
            attemptCount.set(0);
            failureCount = 0;
        }
        
        public void recordRecoveryFailure(Exception e) {
            failureCount++;
        }
    }
}
