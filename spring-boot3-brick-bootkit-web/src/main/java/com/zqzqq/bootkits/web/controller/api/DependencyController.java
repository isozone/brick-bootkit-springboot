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

import com.zqzqq.bootkits.core.dependency.PluginCompatibilityResult;
import com.zqzqq.bootkits.core.dependency.PluginDependencyResolution;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.DependencyWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件依赖分析 API 控制器。
 * 提供依赖图、依赖详情、兼容性检查与升级影响面分析接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/dependency")
@Tag(name = "依赖分析", description = "插件依赖图、兼容性与升级影响面分析接口")
public class DependencyController {

    private final ObjectProvider<DependencyWebService> dependencyWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public DependencyController(ObjectProvider<DependencyWebService> dependencyWebServiceProvider,
                                PluginWebAuthorizationService authorizationService) {
        this.dependencyWebServiceProvider = dependencyWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private DependencyWebService getService() {
        DependencyWebService service = dependencyWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("插件依赖分析服务未启用");
        }
        return service;
    }

    /**
     * 获取插件依赖图
     */
    @GetMapping("/graph")
    @Operation(summary = "获取插件依赖图")
    public ApiResult<DependencyWebService.DependencyGraph> graph() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getDependencyGraph());
    }

    /**
     * 获取指定插件依赖详情
     */
    @GetMapping("/{pluginId}")
    @Operation(summary = "获取插件依赖详情")
    public ApiResult<Map<String, Object>> detail(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getPluginDependencyDetail(pluginId));
    }

    /**
     * 获取插件依赖解析结果
     */
    @GetMapping("/{pluginId}/resolve")
    @Operation(summary = "获取插件依赖解析结果")
    public ApiResult<PluginDependencyResolution> resolve(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().resolveDependencies(pluginId));
    }

    /**
     * 检查插件兼容性
     */
    @GetMapping("/{pluginId}/compatibility")
    @Operation(summary = "检查插件兼容性")
    public ApiResult<PluginCompatibilityResult> compatibility(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().checkCompatibility(pluginId));
    }

    /**
     * 升级影响面分析（谁依赖该插件）
     */
    @GetMapping("/{pluginId}/impact")
    @Operation(summary = "升级影响面分析")
    public ApiResult<List<String>> impact(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getReverseDependencies(pluginId));
    }

    /**
     * 获取版本兼容性矩阵
     */
    @GetMapping("/matrix")
    @Operation(summary = "获取版本兼容性矩阵")
    public ApiResult<List<DependencyWebService.PluginVersionRow>> matrix() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getVersionMatrix());
    }
}
