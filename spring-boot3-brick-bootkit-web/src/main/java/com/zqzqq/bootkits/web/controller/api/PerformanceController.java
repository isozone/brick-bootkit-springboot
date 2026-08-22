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


package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.isolation.PluginResourceMonitor;
import com.zqzqq.bootkits.core.isolation.PluginResourceUsage;
import com.zqzqq.bootkits.core.isolation.ResourceQuota;
import com.zqzqq.bootkits.core.isolation.SystemResourceInfo;
import com.zqzqq.bootkits.core.performance.PerformanceAnalysis;
import com.zqzqq.bootkits.core.performance.PerformanceBaseline;
import com.zqzqq.bootkits.core.performance.PerformanceComparison;
import com.zqzqq.bootkits.core.performance.PerformanceSnapshot;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.PerformanceWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件性能分析与资源隔离 API 控制器。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/performance")
@Tag(name = "性能分析", description = "插件性能分析、资源监控与配额管理接口")
public class PerformanceController {

    private final ObjectProvider<PerformanceWebService> performanceWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public PerformanceController(ObjectProvider<PerformanceWebService> performanceWebServiceProvider,
                                 PluginWebAuthorizationService authorizationService) {
        this.performanceWebServiceProvider = performanceWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private PerformanceWebService getService() {
        PerformanceWebService service = performanceWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("插件性能分析服务未启用");
        }
        return service;
    }

    /**
     * 分析指定插件性能
     */
    @GetMapping("/analyze/{pluginId}")
    @Operation(summary = "分析插件性能")
    public ApiResult<PerformanceAnalysis> analyze(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().analyzePlugin(pluginId));
    }

    /**
     * 获取插件资源使用情况
     */
    @GetMapping("/usage/{pluginId}")
    @Operation(summary = "获取插件资源使用情况")
    public ApiResult<PluginResourceUsage> usage(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getResourceUsage(pluginId));
    }

    /**
     * 获取所有插件资源使用情况
     */
    @GetMapping("/usage")
    @Operation(summary = "获取所有插件资源使用情况")
    public ApiResult<Map<String, PluginResourceUsage>> allUsage() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getAllResourceUsage());
    }

    /**
     * 获取资源监控摘要
     */
    @GetMapping("/summary")
    @Operation(summary = "获取资源监控摘要")
    public ApiResult<PluginResourceMonitor.PluginResourceSummary> summary() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getResourceSummary());
    }

    /**
     * 获取系统资源信息
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统资源信息")
    public ApiResult<SystemResourceInfo> system() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getSystemResourceInfo());
    }

    /**
     * 获取插件性能历史
     */
    @GetMapping("/history/{pluginId}")
    @Operation(summary = "获取插件性能历史")
    public ApiResult<List<PerformanceSnapshot>> history(@PathVariable String pluginId,
                                                        @RequestParam(defaultValue = "20") int limit) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getPerformanceHistory(pluginId, limit));
    }

    /**
     * 获取所有插件性能评分
     */
    @GetMapping("/scores")
    @Operation(summary = "获取所有插件性能评分")
    public ApiResult<Map<String, Double>> scores() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getAllPerformanceScores());
    }

    /**
     * 获取插件配额
     */
    @GetMapping("/quota/{pluginId}")
    @Operation(summary = "获取插件配额")
    public ApiResult<ResourceQuota> quota(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getPluginQuota(pluginId));
    }

    /**
     * 设置插件配额
     */
    @PostMapping("/quota/{pluginId}")
    @Operation(summary = "设置插件配额")
    public ApiResult<Void> setQuota(@PathVariable String pluginId, @RequestBody ResourceQuota quota) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, pluginId);
        getService().setPluginQuota(pluginId, quota);
        return ApiResult.success();
    }

    /**
     * 获取默认配额
     */
    @GetMapping("/quota/default")
    @Operation(summary = "获取默认配额")
    public ApiResult<ResourceQuota> defaultQuota() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getDefaultQuota());
    }

    /**
     * 对比插件性能与基线
     */
    @GetMapping("/baseline/compare/{pluginId}")
    @Operation(summary = "对比插件性能与基线")
    public ApiResult<PerformanceComparison> compareBaseline(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().compareWithBaseline(pluginId));
    }

    /**
     * 获取插件性能基线
     */
    @GetMapping("/baseline/{pluginId}")
    @Operation(summary = "获取插件性能基线")
    public ApiResult<PerformanceBaseline> baseline(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getBaseline(pluginId));
    }
}
