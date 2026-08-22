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

import com.zqzqq.bootkits.core.security.PluginPermission;
import com.zqzqq.bootkits.core.security.PluginPermissionType;
import com.zqzqq.bootkits.core.security.PluginSecurityPolicy;
import com.zqzqq.bootkits.core.security.PluginSecurityValidationResult;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.SecurityWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 插件安全中心 API 控制器。
 * 提供插件安全扫描、安全策略与权限管理接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/security")
@Tag(name = "插件安全中心", description = "插件安全扫描、安全策略与权限管理接口")
public class SecurityController {

    private final ObjectProvider<SecurityWebService> securityWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public SecurityController(ObjectProvider<SecurityWebService> securityWebServiceProvider,
                              PluginWebAuthorizationService authorizationService) {
        this.securityWebServiceProvider = securityWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private SecurityWebService getService() {
        SecurityWebService service = securityWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("插件安全中心服务未启用");
        }
        return service;
    }

    /**
     * 扫描插件安全（按插件 ID）
     */
    @GetMapping("/scan/{pluginId}")
    @Operation(summary = "扫描插件安全（按插件 ID）")
    public ApiResult<PluginSecurityValidationResult> scanByPluginId(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VERIFY, pluginId);
        return ApiResult.success(getService().scanPluginById(pluginId));
    }

    /**
     * 扫描插件安全（按文件路径）
     */
    @GetMapping("/scan")
    @Operation(summary = "扫描插件安全（按文件路径）")
    public ApiResult<PluginSecurityValidationResult> scanByPath(@RequestParam String path) {
        authorizationService.check(PluginWebPermission.PLUGIN_VERIFY, null);
        return ApiResult.success(getService().scanPluginByPath(path));
    }

    /**
     * 获取插件安全策略
     */
    @GetMapping("/policy/{pluginId}")
    @Operation(summary = "获取插件安全策略")
    public ApiResult<PluginSecurityPolicy> getPolicy(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getPolicy(pluginId));
    }

    /**
     * 设置插件安全策略
     */
    @PostMapping("/policy")
    @Operation(summary = "设置插件安全策略")
    public ApiResult<Void> setPolicy(@RequestBody PolicyRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, request.getPluginId());
        PluginSecurityPolicy policy = PluginSecurityPolicy.builder()
                .allowFileSystemAccess(request.isAllowFileSystemAccess())
                .allowNetworkAccess(request.isAllowNetworkAccess())
                .allowSystemPropertyAccess(request.isAllowSystemPropertyAccess())
                .allowReflectionAccess(request.isAllowReflectionAccess())
                .maxMemoryUsage(request.getMaxMemoryUsage())
                .maxThreadCount(request.getMaxThreadCount())
                .build();
        getService().setPolicy(request.getPluginId(), policy);
        return ApiResult.success();
    }

    /**
     * 授予插件权限
     */
    @PostMapping("/permissions/grant")
    @Operation(summary = "授予插件权限")
    public ApiResult<Void> grantPermission(@RequestBody PermissionRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, request.getPluginId());
        PluginPermission permission = new PluginPermission(
                PluginPermissionType.fromCode(request.getType()),
                request.getTarget(),
                request.getAction(),
                request.getDescription());
        getService().grantPermission(request.getPluginId(), permission);
        return ApiResult.success();
    }

    /**
     * 撤销插件权限
     */
    @PostMapping("/permissions/revoke")
    @Operation(summary = "撤销插件权限")
    public ApiResult<Void> revokePermission(@RequestBody PermissionRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, request.getPluginId());
        PluginPermission permission = new PluginPermission(
                PluginPermissionType.fromCode(request.getType()),
                request.getTarget(),
                request.getAction(),
                request.getDescription());
        getService().revokePermission(request.getPluginId(), permission);
        return ApiResult.success();
    }

    /**
     * 获取插件已授予的权限
     */
    @GetMapping("/permissions/{pluginId}")
    @Operation(summary = "获取插件已授予的权限")
    public ApiResult<Set<PluginPermission>> getPermissions(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getPermissions(pluginId));
    }

    @Data
    public static class PolicyRequest {
        private String pluginId;
        private boolean allowFileSystemAccess;
        private boolean allowNetworkAccess;
        private boolean allowSystemPropertyAccess;
        private boolean allowReflectionAccess;
        private long maxMemoryUsage;
        private int maxThreadCount;
    }

    @Data
    public static class PermissionRequest {
        private String pluginId;
        private String type;
        private String target;
        private String action;
        private String description;
    }
}
