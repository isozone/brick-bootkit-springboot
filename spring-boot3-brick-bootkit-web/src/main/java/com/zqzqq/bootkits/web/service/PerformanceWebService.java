package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.isolation.PluginResourceIsolation;
import com.zqzqq.bootkits.core.isolation.PluginResourceMonitor;
import com.zqzqq.bootkits.core.isolation.PluginResourceUsage;
import com.zqzqq.bootkits.core.isolation.QuotaManager;
import com.zqzqq.bootkits.core.isolation.ResourceQuota;
import com.zqzqq.bootkits.core.isolation.SystemResourceInfo;
import com.zqzqq.bootkits.core.performance.PerformanceAnalysis;
import com.zqzqq.bootkits.core.performance.PerformanceBaseline;
import com.zqzqq.bootkits.core.performance.PerformanceComparison;
import com.zqzqq.bootkits.core.performance.PerformanceSnapshot;
import com.zqzqq.bootkits.core.performance.PluginPerformanceAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 插件性能分析与资源隔离 Web 服务。
 * 基于主框架注册的 {@link PluginPerformanceAnalyzer}、{@link PluginResourceMonitor}
 * 与 {@link QuotaManager} Bean，提供性能评分、资源占用、配额管理能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class PerformanceWebService {

    private final ObjectProvider<PluginPerformanceAnalyzer> analyzerProvider;
    private final ObjectProvider<PluginResourceMonitor> monitorProvider;
    private final ObjectProvider<PluginResourceIsolation> isolationProvider;
    private final ObjectProvider<QuotaManager> quotaManagerProvider;

    public PerformanceWebService(ObjectProvider<PluginPerformanceAnalyzer> analyzerProvider,
                                 ObjectProvider<PluginResourceMonitor> monitorProvider,
                                 ObjectProvider<PluginResourceIsolation> isolationProvider,
                                 ObjectProvider<QuotaManager> quotaManagerProvider) {
        this.analyzerProvider = analyzerProvider;
        this.monitorProvider = monitorProvider;
        this.isolationProvider = isolationProvider;
        this.quotaManagerProvider = quotaManagerProvider;
    }

    private PluginPerformanceAnalyzer getAnalyzer() {
        PluginPerformanceAnalyzer analyzer = analyzerProvider.getIfAvailable();
        if (analyzer == null) {
            throw new PluginException("插件性能分析服务未启用");
        }
        return analyzer;
    }

    private PluginResourceMonitor getMonitor() {
        PluginResourceMonitor monitor = monitorProvider.getIfAvailable();
        if (monitor == null) {
            throw new PluginException("插件资源监控服务未启用");
        }
        return monitor;
    }

    private QuotaManager getQuotaManager() {
        QuotaManager quotaManager = quotaManagerProvider.getIfAvailable();
        if (quotaManager == null) {
            throw new PluginException("插件配额管理服务未启用");
        }
        return quotaManager;
    }

    private PluginResourceIsolation getIsolation() {
        PluginResourceIsolation isolation = isolationProvider.getIfAvailable();
        if (isolation == null) {
            throw new PluginException("插件资源隔离服务未启用");
        }
        return isolation;
    }

    /**
     * 分析指定插件性能
     */
    public PerformanceAnalysis analyzePlugin(String pluginId) {
        PluginResourceUsage usage = getMonitor().getPluginResourceUsage(pluginId);
        if (usage == null) {
            throw new PluginException("插件资源使用数据不存在: " + pluginId);
        }
        return getAnalyzer().analyzePlugin(pluginId, usage);
    }

    /**
     * 获取插件资源使用情况
     */
    public PluginResourceUsage getResourceUsage(String pluginId) {
        PluginResourceUsage usage = getMonitor().getPluginResourceUsage(pluginId);
        if (usage == null) {
            throw new PluginException("插件资源使用数据不存在: " + pluginId);
        }
        return usage;
    }

    /**
     * 获取所有插件资源使用情况
     */
    public Map<String, PluginResourceUsage> getAllResourceUsage() {
        return getMonitor().getAllPluginResourceUsage();
    }

    /**
     * 获取资源监控摘要
     */
    public PluginResourceMonitor.PluginResourceSummary getResourceSummary() {
        return getMonitor().getResourceSummary();
    }

    /**
     * 获取系统资源信息
     */
    public SystemResourceInfo getSystemResourceInfo() {
        return getIsolation().getSystemResourceInfo();
    }

    /**
     * 获取插件性能历史
     */
    public List<PerformanceSnapshot> getPerformanceHistory(String pluginId, int limit) {
        return getAnalyzer().getPerformanceHistory(pluginId, limit);
    }

    /**
     * 获取所有插件性能评分
     */
    public Map<String, Double> getAllPerformanceScores() {
        return getAnalyzer().getAllPluginPerformanceScores();
    }

    /**
     * 获取插件配额
     */
    public ResourceQuota getPluginQuota(String pluginId) {
        return getQuotaManager().getPluginQuota(pluginId);
    }

    /**
     * 设置插件配额
     */
    public void setPluginQuota(String pluginId, ResourceQuota quota) {
        if (quota == null) {
            throw new PluginException("配额不能为空");
        }
        getQuotaManager().setPluginQuota(pluginId, quota);
        getIsolation().setPluginQuota(pluginId, quota);
    }

    /**
     * 获取默认配额
     */
    public ResourceQuota getDefaultQuota() {
        return getQuotaManager().getDefaultQuota();
    }

    /**
     * 对比插件性能与基线
     */
    public PerformanceComparison compareWithBaseline(String pluginId) {
        PerformanceAnalysis current = analyzePlugin(pluginId);
        return getAnalyzer().compareWithBaseline(pluginId, current);
    }

    /**
     * 获取插件性能基线
     */
    public PerformanceBaseline getBaseline(String pluginId) {
        return getAnalyzer().getPerformanceBaseline(pluginId);
    }
}
