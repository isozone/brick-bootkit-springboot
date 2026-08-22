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

import com.zqzqq.bootkits.core.communication.RegistryStatistics;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.RegistryWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 插件服务注册中心 API 控制器。
 * 展示插件间服务注册、发现与依赖关系。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/registry")
@Tag(name = "服务注册中心", description = "插件服务注册、发现与依赖关系接口")
public class RegistryController {

    private final ObjectProvider<RegistryWebService> registryWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public RegistryController(ObjectProvider<RegistryWebService> registryWebServiceProvider,
                              PluginWebAuthorizationService authorizationService) {
        this.registryWebServiceProvider = registryWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private RegistryWebService getService() {
        RegistryWebService service = registryWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("服务注册中心服务未启用");
        }
        return service;
    }

    /**
     * 获取注册中心统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取注册中心统计信息")
    public ApiResult<RegistryStatistics> statistics() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getStatistics());
    }

    /**
     * 获取所有注册服务（按插件分组）
     */
    @GetMapping("/services")
    @Operation(summary = "获取所有注册服务（按插件分组）")
    public ApiResult<List<RegistryWebService.PluginServiceGroup>> services() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getServicesGroupedByPlugin());
    }

    /**
     * 获取指定插件的服务列表
     */
    @GetMapping("/services/{pluginId}")
    @Operation(summary = "获取指定插件的服务列表")
    public ApiResult<List<ServiceDescriptor>> servicesByPlugin(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getServicesByPlugin(pluginId));
    }

    /**
     * 获取所有注册的插件 ID
     */
    @GetMapping("/plugins")
    @Operation(summary = "获取所有注册的插件 ID")
    public ApiResult<Set<String>> plugins() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getRegisteredPlugins());
    }
}
