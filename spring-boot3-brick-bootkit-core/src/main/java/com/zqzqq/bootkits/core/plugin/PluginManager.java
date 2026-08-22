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


package com.zqzqq.bootkits.core.plugin;

import com.zqzqq.bootkits.core.exception.PluginErrorCode;
import com.zqzqq.bootkits.core.exception.PluginInstallException;
import com.zqzqq.bootkits.core.exception.PluginStartException;
import com.zqzqq.bootkits.core.exception.PluginStopException;
import com.zqzqq.bootkits.core.logging.PluginLogger;
import com.zqzqq.bootkits.core.health.PluginHealthChecker;
import com.zqzqq.bootkits.core.health.PluginHealthStatus;
import com.zqzqq.bootkits.core.health.PluginHealthReport;
import com.zqzqq.bootkits.core.health.PluginAutoRecoveryManager;
import com.zqzqq.bootkits.core.version.PluginVersionInfo;
import com.zqzqq.bootkits.core.dependency.*;
import com.zqzqq.bootkits.core.isolation.*;
import com.zqzqq.bootkits.core.performance.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器
 * 负责插件的安装、启动、停止和卸载
 *
 * @deprecated 这是 core 模块内部使用的独立插件模型，不是宿主 Spring Boot 项目的正式接入入口。
 * 宿主集成请使用 {@code com.zqzqq.bootkits.core.PluginManager}。
 */
@Deprecated(since = "4.0.6", forRemoval = false)
public class PluginManager {

    private static final PluginLogger logger = PluginLogger.getLogger(PluginManager.class);

    private final PluginLoader pluginLoader;
    private final PluginValidator pluginValidator;
    
    // 新增的健康检查和恢复系统
    private final PluginHealthChecker healthChecker;
    private final PluginAutoRecoveryManager autoRecoveryManager;
    
    // 新增的版本验证系统
    private final Map<String, PluginVersionInfo> pluginVersions = new ConcurrentHashMap<>();
    
    // 新增的依赖管理系统
    private final PluginDependencyManager dependencyManager;
    
    // 新增的资源隔离管理系统
    private final PluginResourceIsolation resourceIsolation;
    private final PluginResourceMonitor resourceMonitor;
    
    // 新增的性能分析系统
    private final PluginPerformanceAnalyzer performanceAnalyzer;
    
    // 已安装的插件
    private final ConcurrentHashMap<String, Plugin> installedPlugins = new ConcurrentHashMap<>();
    
    // 正在运行的插件
    private final ConcurrentHashMap<String, Plugin> runningPlugins = new ConcurrentHashMap<>();

    public PluginManager(PluginLoader pluginLoader, PluginValidator pluginValidator) {
        this(pluginLoader, pluginValidator, null, null, null, null, null);
    }
    
    public PluginManager(PluginLoader pluginLoader, PluginValidator pluginValidator,
                        PluginHealthChecker healthChecker,
                        PluginAutoRecoveryManager autoRecoveryManager,
                        PluginDependencyManager dependencyManager) {
        this(pluginLoader, pluginValidator, healthChecker, autoRecoveryManager, dependencyManager, null, null);
    }
    
    public PluginManager(PluginLoader pluginLoader, PluginValidator pluginValidator,
                        PluginHealthChecker healthChecker,
                        PluginAutoRecoveryManager autoRecoveryManager,
                        PluginDependencyManager dependencyManager,
                        PluginResourceIsolation resourceIsolation,
                        PluginPerformanceAnalyzer performanceAnalyzer) {
        this.pluginLoader = pluginLoader;
        this.pluginValidator = pluginValidator;
        this.healthChecker = healthChecker;
        this.autoRecoveryManager = autoRecoveryManager;
        this.dependencyManager = dependencyManager;
        this.resourceIsolation = resourceIsolation;
        this.resourceMonitor = resourceIsolation != null ? resourceIsolation.getResourceMonitor() : null;
        this.performanceAnalyzer = performanceAnalyzer;
    }

    /**
     * 安装插件
     */
    public Plugin installPlugin(File pluginFile) {
        logger.info("开始安装插件: {}", pluginFile.getName());
        
        try {
            // 验证插件
            if (!pluginValidator.validate(pluginFile)) {
                throw new PluginInstallException(
                    PluginErrorCode.PLUGIN_VALIDATION_FAILED,
                    "插件验证失败: " + pluginFile.getName()
                );
            }
            
            // 加载插件
            Plugin plugin = pluginLoader.loadPlugin(pluginFile);
            
            // 检查是否已安装
            if (installedPlugins.containsKey(plugin.getId())) {
                logger.warn("插件已存在，将覆盖安装: {}", plugin.getId());
            }
            
            // 版本兼容性验证
            PluginVersionInfo versionInfo = PluginVersionInfo.newBuilder(
                plugin.getId(), plugin.getVersion()).build();
            pluginVersions.put(plugin.getId(), versionInfo);
            
            // 依赖验证
            if (dependencyManager != null) {
                PluginCompatibilityResult compatibility = dependencyManager.checkCompatibility(
                    plugin.getId(), new ArrayList<>(installedPlugins.keySet()));
                if (!compatibility.isCompatible()) {
                    throw new PluginInstallException(
                        PluginErrorCode.PLUGIN_COMPATIBILITY_FAILED,
                        "插件依赖验证失败: " + String.join(", ", compatibility.getErrors())
                    );
                }
            }
            
            // 安装插件
            installedPlugins.put(plugin.getId(), plugin);
            
            // 初始化资源隔离（如果支持）
            if (resourceIsolation != null) {
                try {
                    resourceIsolation.initializePlugin(plugin.getId(), plugin.getClassLoader());
                    logger.debug(plugin.getId(), "resource-isolation", "插件资源隔离初始化完成");
                } catch (Exception e) {
                    logger.warn(plugin.getId(), "resource-isolation", "插件资源隔离初始化失败: {}", e.getMessage());
                }
            }
            
            logger.info("插件安装成功: {} ({})", plugin.getName(), plugin.getId());
            return plugin;
            
        } catch (Exception e) {
            logger.error("插件安装失败: {} - {}", pluginFile.getName(), e.getMessage(), e);
            throw new PluginInstallException(
                PluginErrorCode.PLUGIN_INSTALL_FAILED,
                "插件安装失败: " + pluginFile.getName(),
                e
            );
        }
    }

    /**
     * 启动插件
     */
    public void startPlugin(String pluginId) {
        logger.info("开始启动插件: {}", pluginId);
        
        Plugin plugin = installedPlugins.get(pluginId);
        if (plugin == null) {
            throw new PluginStartException(
                PluginErrorCode.PLUGIN_NOT_FOUND,
                "插件不存在: " + pluginId
            );
        }
        
        if (runningPlugins.containsKey(pluginId)) {
            logger.warn("插件已在运行: {}", pluginId);
            return;
        }
        
        try {
            // 启动前的健康检查
            if (healthChecker != null) {
                PluginHealthReport healthReport = healthChecker.checkHealth(plugin);
                if (healthReport.getOverallStatus() != PluginHealthStatus.HEALTHY &&
                    healthReport.getOverallStatus() != PluginHealthStatus.WARNING) {
                    throw new PluginStartException(
                        PluginErrorCode.PLUGIN_HEALTH_CHECK_FAILED,
                        "插件健康检查失败: " + healthReport.getOverallStatus()
                    );
                }
            }
            
            plugin.start();
            runningPlugins.put(pluginId, plugin);
            
            // 启动后注册自动恢复管理
            if (autoRecoveryManager != null) {
                // 不需要显式注册，健康检查器会自动监控运行中的插件
            }
            
            // 启动资源监控和性能分析
            if (resourceMonitor != null) {
                PluginResourceUsage usage = resourceIsolation != null ? 
                    resourceIsolation.getPluginResourceUsage(pluginId) : null;
                if (usage != null) {
                    resourceMonitor.startMonitoring(pluginId, usage);
                }
            }
            
            // 记录性能分析基线
            if (performanceAnalyzer != null) {
                PluginResourceUsage usage = resourceMonitor != null ? 
                    resourceMonitor.getPluginResourceUsage(pluginId) : null;
                if (usage != null) {
                    PerformanceAnalysis analysis = performanceAnalyzer.analyzePlugin(pluginId, usage);
                    // 可以在这里保存基线数据
                    logger.debug(pluginId, "performance-analysis", "插件性能分析完成: 评分 {:.2f}", analysis.getPerformanceScore());
                }
            }
            
            logger.info("插件启动成功: {}", pluginId);
            
        } catch (Exception e) {
            logger.error("插件启动失败: {} - {}", pluginId, e.getMessage(), e);
            
            // 启动失败后的处理
            if (autoRecoveryManager != null) {
                // 插件启动失败处理逻辑已经集成到autoRecoveryManager中
            }
            
            throw new PluginStartException(
                PluginErrorCode.PLUGIN_START_FAILED,
                "插件启动失败: " + pluginId,
                e
            );
        }
    }

    /**
     * 停止插件
     */
    public void stopPlugin(String pluginId) {
        logger.info("开始停止插件: {}", pluginId);
        
        Plugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            logger.warn("插件未在运行: {}", pluginId);
            return;
        }
        
        try {
            plugin.stop();
            runningPlugins.remove(pluginId);
            
            // 停止资源监控
            if (resourceMonitor != null) {
                resourceMonitor.stopMonitoring(pluginId);
            }
            
            logger.info("插件停止成功: {}", pluginId);
            
        } catch (Exception e) {
            logger.error("插件停止失败: {} - {}", pluginId, e.getMessage(), e);
            throw new PluginStopException(
                PluginErrorCode.PLUGIN_STOP_FAILED,
                "插件停止失败: " + pluginId,
                e
            );
        }
    }

    /**
     * 卸载插件
     */
    public void uninstallPlugin(String pluginId) {
        logger.info("开始卸载插件: {}", pluginId);
        
        // 先停止插件
        if (runningPlugins.containsKey(pluginId)) {
            stopPlugin(pluginId);
        }
        
        Plugin plugin = installedPlugins.get(pluginId);
        if (plugin == null) {
            logger.warn("插件不存在: {}", pluginId);
            return;
        }
        
        try {
            plugin.uninstall();
            installedPlugins.remove(pluginId);
            
            // 清理资源隔离
            if (resourceIsolation != null) {
                try {
                    resourceIsolation.cleanupPlugin(pluginId);
                    logger.debug(pluginId, "resource-isolation", "插件资源隔离清理完成");
                } catch (Exception e) {
                    logger.warn(pluginId, "resource-isolation", "插件资源隔离清理失败: {}", e.getMessage());
                }
            }
            
            // 清理性能分析数据
            if (performanceAnalyzer != null) {
                try {
                    performanceAnalyzer.clearPluginHistory(pluginId);
                    logger.debug(pluginId, "performance-analysis", "插件性能分析数据清理完成");
                } catch (Exception e) {
                    logger.warn(pluginId, "performance-analysis", "插件性能分析数据清理失败: {}", e.getMessage());
                }
            }
            
            logger.info("插件卸载成功: {}", pluginId);
            
        } catch (Exception e) {
            logger.error("插件卸载失败: {} - {}", pluginId, e.getMessage(), e);
            // 卸载失败不抛异常，只记录日志
        }
    }

    /**
     * 获取已安装插件列表
     */
    public List<Plugin> getInstalledPluginsList() {
        return new ArrayList<>(installedPlugins.values());
    }

    /**
     * 获取正在运行的插件列表
     */
    public List<Plugin> getRunningPluginsList() {
        return new ArrayList<>(runningPlugins.values());
    }

    /**
     * 获取插件信息
     */
    public Plugin getPlugin(String pluginId) {
        return installedPlugins.get(pluginId);
    }

    /**
     * 检查插件是否已安装
     */
    public boolean isPluginInstalled(String pluginId) {
        return installedPlugins.containsKey(pluginId);
    }

    /**
     * 检查插件是否正在运行
     */
    public boolean isPluginRunning(String pluginId) {
        return runningPlugins.containsKey(pluginId);
    }

    /**
     * 获取已安装插件映射（用于测试）
     */
    public ConcurrentHashMap<String, Plugin> getInstalledPlugins() {
        return installedPlugins;
    }

    /**
     * 获取正在运行插件映射（用于测试）
     */
    public ConcurrentHashMap<String, Plugin> getRunningPlugins() {
        return runningPlugins;
    }
    
    /**
     * 关闭插件管理器，清理所有资源
     * 
     * 注意：这是一个破坏性操作，将停止并清理所有插件
     */
    public void shutdown() {
        logger.info("system", "开始关闭插件管理器");
        
        try {
            // 关闭自动恢复管理器
            if (autoRecoveryManager != null) {
                autoRecoveryManager.shutdown();
            }
            
            // 关闭资源监控
            if (resourceMonitor != null) {
                resourceMonitor.shutdown();
            }
            
            // 关闭资源隔离系统
            if (resourceIsolation != null) {
                // 资源隔离系统会在resourceMonitor.shutdown()中自动关闭
            }
            
            // 停止所有运行中的插件
            List<String> runningPluginIds = new ArrayList<>(runningPlugins.keySet());
            for (String pluginId : runningPluginIds) {
                try {
                    stopPlugin(pluginId);
                } catch (Exception e) {
                    logger.warn("system", "停止插件失败", pluginId, e.getMessage());
                }
            }
            
            // 清理所有插件引用
            runningPlugins.clear();
            installedPlugins.clear();
            pluginVersions.clear();
            
            logger.info("system", "插件管理器关闭完成");
            
        } catch (Exception e) {
            logger.error("system", "关闭插件管理器时发生错误", e.getMessage(), e);
            throw new RuntimeException("插件管理器关闭失败", e);
        }
    }
    
    // ===== 新增的健康检查和恢复功能 =====
    
    /**
     * 检查插件健康状态
     */
    public PluginHealthReport checkPluginHealth(String pluginId) {
        if (healthChecker == null) {
            throw new IllegalStateException("健康检查器未初始化");
        }
        
        Plugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件未运行: " + pluginId);
        }
        
        return healthChecker.checkHealth(plugin);
    }
    
    /**
     * 获取插件健康状态
     */
    public PluginHealthStatus getPluginHealthStatus(String pluginId) {
        PluginHealthReport report = checkPluginHealth(pluginId);
        return report.getOverallStatus();
    }
    
    /**
     * 手动触发插件恢复
     */
    public boolean recoverPlugin(String pluginId) {
        if (autoRecoveryManager == null) {
            return false;
        }
        
        Plugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            return false;
        }
        
        return autoRecoveryManager.triggerManualRecovery(pluginId);
    }
    
    /**
     * 获取所有运行插件的健康状态
     */
    public Map<String, PluginHealthStatus> getAllPluginHealthStatus() {
        Map<String, PluginHealthStatus> result = new HashMap<>();
        
        for (String pluginId : runningPlugins.keySet()) {
            try {
                result.put(pluginId, getPluginHealthStatus(pluginId));
            } catch (Exception e) {
                logger.warn(pluginId, "获取插件健康状态失败: " + e.getMessage());
                result.put(pluginId, PluginHealthStatus.UNKNOWN);
            }
        }
        
        return result;
    }
    
    // ===== 新增的版本验证功能 =====
    
    /**
     * 注册插件版本信息
     */
    public void registerPluginVersion(PluginVersionInfo versionInfo) {
        if (versionInfo != null) {
            pluginVersions.put(versionInfo.getPluginId(), versionInfo);
        }
    }
    
    /**
     * 获取插件版本信息
     */
    public PluginVersionInfo getPluginVersionInfo(String pluginId) {
        return pluginVersions.get(pluginId);
    }
    
    /**
     * 验证插件版本兼容性
     */
    public boolean validateVersionCompatibility(String pluginId1, String pluginId2) {
        PluginVersionInfo info1 = pluginVersions.get(pluginId1);
        PluginVersionInfo info2 = pluginVersions.get(pluginId2);
        
        if (info1 == null || info2 == null) {
            return false;
        }
        
        return info1.validateCompatibility(info2).isCompatible();
    }
    
    /**
     * 检查插件版本是否满足依赖要求
     */
    public boolean checkVersionConstraints(String pluginId) {
        if (dependencyManager == null) {
            return true;
        }
        
        PluginVersionInfo versionInfo = pluginVersions.get(pluginId);
        if (versionInfo == null) {
            return false;
        }
        
        // 注意：这里需要从PluginDependencyManager获取，但字段是private的
        // 暂时简化处理，先检查依赖是否存在
        boolean hasDependency = dependencyManager.getPluginDependencies(pluginId).contains(pluginId);
        PluginDependency dep = null;
        if (hasDependency) {
            // 这里需要通过其他方式获取依赖信息，暂时代码逻辑处理
            dep = PluginDependency.newBuilder(pluginId).build();
        }
        if (dep == null) {
            return true;
        }
        
        for (Map.Entry<String, VersionConstraint> entry : dep.getVersionConstraints().entrySet()) {
            String constraintId = entry.getKey();
            VersionConstraint constraint = entry.getValue();
            
            PluginVersionInfo constraintInfo = pluginVersions.get(constraintId);
            if (constraintInfo != null) {
                if (!constraint.isSatisfied(constraintInfo.getVersion().toString())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // ===== 新增的依赖管理功能 =====
    
    /**
     * 注册插件依赖信息
     */
    public void registerPluginDependency(PluginDependency dependency) {
        if (dependencyManager != null && dependency != null) {
            dependencyManager.registerPluginDependency(dependency.getPluginId(), dependency);
        }
    }
    
    /**
     * 解析插件依赖
     */
    public PluginDependencyResolution resolveDependencies(String pluginId) {
        if (dependencyManager == null) {
            return PluginDependencyResolution.failure(Arrays.asList("依赖管理器未初始化"));
        }
        
        return dependencyManager.resolveDependencies(pluginId);
    }
    
    /**
     * 检查插件兼容性
     */
    public PluginCompatibilityResult checkCompatibility(String pluginId, Collection<String> otherPlugins) {
        if (dependencyManager == null) {
            return PluginCompatibilityResult.compatibleWithWarnings(Arrays.asList("依赖管理器未初始化"));
        }
        
        return dependencyManager.checkCompatibility(pluginId, otherPlugins);
    }
    
    /**
     * 获取插件依赖列表
     */
    public Collection<String> getDependencies(String pluginId) {
        if (dependencyManager == null) {
            return new ArrayList<>();
        }
        
        return dependencyManager.getPluginDependencies(pluginId);
    }
    
    /**
     * 获取反向依赖列表
     */
    public Collection<String> getReverseDependencies(String pluginId) {
        if (dependencyManager == null) {
            return new ArrayList<>();
        }
        
        return dependencyManager.getReverseDependencies(pluginId);
    }
    
    /**
     * 检查依赖循环
     */
    public boolean hasDependencyCycle(String pluginId) {
        if (dependencyManager == null) {
            return false;
        }
        
        return dependencyManager.hasDependencyCycle(pluginId);
    }
    
    /**
     * 获取依赖拓扑排序
     */
    public List<String> getDependencyOrder() {
        if (dependencyManager == null) {
            return new ArrayList<>();
        }
        
        return dependencyManager.getTopologicalOrder();
    }
    
    /**
     * 获取注册插件数量
     */
    public int getRegisteredPluginCount() {
        return installedPlugins.size();
    }
    
    /**
     * 获取运行插件数量
     */
    public int getRunningPluginCount() {
        return runningPlugins.size();
    }
    
    /**
     * 获取版本信息数量
     */
    public int getVersionInfoCount() {
        return pluginVersions.size();
    }
    
    /**
     * 获取依赖信息数量
     */
    public int getDependencyInfoCount() {
        return dependencyManager != null ? dependencyManager.getRegisteredPluginCount() : 0;
    }
    
    // ===== 第二阶段：资源隔离与配额管理功能 =====
    
    /**
     * 设置插件资源配额
     */
    public void setPluginResourceQuota(String pluginId, ResourceQuota quota) {
        if (resourceIsolation == null) {
            throw new IllegalStateException("资源隔离系统未初始化");
        }
        
        Plugin plugin = installedPlugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在: " + pluginId);
        }
        
        resourceIsolation.setPluginQuota(pluginId, quota);
        logger.info(pluginId, "resource-quota", "设置插件资源配额: {}", quota.toString());
    }
    
    /**
     * 获取插件资源配额
     */
    public ResourceQuota getPluginResourceQuota(String pluginId) {
        if (resourceIsolation == null) {
            return null;
        }
        
        return resourceIsolation.getPluginQuota(pluginId);
    }
    
    /**
     * 获取插件资源使用情况
     */
    public PluginResourceUsage getPluginResourceUsage(String pluginId) {
        if (resourceMonitor == null) {
            return null;
        }
        
        return resourceMonitor.getPluginResourceUsage(pluginId);
    }
    
    /**
     * 检查插件是否超过资源配额
     */
    public boolean isPluginQuotaExceeded(String pluginId) {
        PluginResourceUsage usage = getPluginResourceUsage(pluginId);
        return usage != null && usage.isQuotaExceeded();
    }
    
    /**
     * 获取所有插件资源使用摘要
     */
    public PluginResourceMonitor.PluginResourceSummary getAllPluginsResourceSummary() {
        if (resourceMonitor == null) {
            return new PluginResourceMonitor.PluginResourceSummary(0, 0, 0, 0);
        }
        
        return resourceMonitor.getResourceSummary();
    }
    
    /**
     * 获取系统资源信息
     */
    public SystemResourceInfo getSystemResourceInfo() {
        if (resourceIsolation == null) {
            return null;
        }
        
        return resourceIsolation.getSystemResourceInfo();
    }
    
    /**
     * 强制停止插件（当资源配额严重超限时）
     */
    public void forceStopPlugin(String pluginId) {
        logger.warn("system", "强制停止插件（资源配额超限）", pluginId);
        stopPlugin(pluginId);
        
        if (resourceIsolation != null) {
            resourceIsolation.isolatePlugin(pluginId);
        }
    }
    
    // ===== 第二阶段：性能分析与优化建议功能 =====
    
    /**
     * 分析插件性能
     */
    public PerformanceAnalysis analyzePluginPerformance(String pluginId) {
        if (performanceAnalyzer == null) {
            throw new IllegalStateException("性能分析系统未初始化");
        }
        
        Plugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件未运行: " + pluginId);
        }
        
        PluginResourceUsage usage = getPluginResourceUsage(pluginId);
        if (usage == null) {
            throw new IllegalStateException("无法获取插件资源使用情况: " + pluginId);
        }
        
        return performanceAnalyzer.analyzePlugin(pluginId, usage);
    }
    
    /**
     * 获取插件性能历史
     */
    public List<PerformanceSnapshot> getPluginPerformanceHistory(String pluginId, int limit) {
        if (performanceAnalyzer == null) {
            return new ArrayList<>();
        }
        
        return performanceAnalyzer.getPerformanceHistory(pluginId, limit);
    }
    
    /**
     * 设置插件性能基线
     */
    public void setPluginPerformanceBaseline(String pluginId, PerformanceBaseline baseline) {
        if (performanceAnalyzer == null) {
            throw new IllegalStateException("性能分析系统未初始化");
        }
        
        performanceAnalyzer.setPerformanceBaseline(pluginId, baseline);
        logger.info("设置插件性能基线: {}", pluginId);
    }
    
    /**
     * 获取插件性能基线
     */
    public PerformanceBaseline getPluginPerformanceBaseline(String pluginId) {
        if (performanceAnalyzer == null) {
            return null;
        }
        
        return performanceAnalyzer.getPerformanceBaseline(pluginId);
    }
    
    /**
     * 对比插件性能与基线
     */
    public PerformanceComparison comparePluginWithBaseline(String pluginId) {
        if (performanceAnalyzer == null) {
            return null;
        }
        
        PerformanceAnalysis current = analyzePluginPerformance(pluginId);
        return performanceAnalyzer.compareWithBaseline(pluginId, current);
    }
    
    /**
     * 获取所有插件性能评分
     */
    public Map<String, Double> getAllPluginsPerformanceScores() {
        if (performanceAnalyzer == null) {
            return new HashMap<>();
        }
        
        return performanceAnalyzer.getAllPluginPerformanceScores();
    }
    
    /**
     * 清理插件性能历史数据
     */
    public void clearPluginPerformanceHistory(String pluginId) {
        if (performanceAnalyzer != null) {
            performanceAnalyzer.clearPluginHistory(pluginId);
            logger.info("清理插件性能历史数据: {}", pluginId);
        }
    }
    
    /**
     * 获取性能阈值配置
     */
    public PerformanceThresholds getPerformanceThresholds() {
        if (performanceAnalyzer == null) {
            return null;
        }
        
        // Note: 这里需要从analyzer中暴露thresholds，暂时返回默认值
        return PerformanceThresholds.defaultThresholds();
    }
}
