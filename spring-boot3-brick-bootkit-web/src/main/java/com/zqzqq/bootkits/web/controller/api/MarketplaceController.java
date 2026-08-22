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

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.MarketplacePluginDTO;
import com.zqzqq.bootkits.web.service.PluginMarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 插件市场 API 控制器。
 * 提供插件市场清单查询与一键下载安装接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/marketplace")
@Tag(name = "插件市场", description = "插件市场清单与下载安装接口")
public class MarketplaceController {

    private final ObjectProvider<PluginMarketplaceService> marketplaceServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public MarketplaceController(ObjectProvider<PluginMarketplaceService> marketplaceServiceProvider,
                                 PluginWebAuthorizationService authorizationService) {
        this.marketplaceServiceProvider = marketplaceServiceProvider;
        this.authorizationService = authorizationService;
    }

    private PluginMarketplaceService getService() {
        PluginMarketplaceService service = marketplaceServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("插件市场服务未启用");
        }
        return service;
    }

    /**
     * 获取插件市场清单
     */
    @GetMapping
    @Operation(summary = "获取插件市场清单")
    public ApiResult<List<MarketplacePluginDTO>> list() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().listMarketplace());
    }

    /**
     * 下载并安装市场插件
     */
    @PostMapping("/install/{pluginId}")
    @Operation(summary = "下载并安装市场插件")
    public ApiResult<String> install(@PathVariable String pluginId,
                                     @RequestBody(required = false) InstallRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, pluginId);
        boolean autoStart = request == null || request.isAutoStart();
        return ApiResult.success(getService().installFromMarketplace(pluginId, autoStart));
    }

    @Data
    public static class InstallRequest {
        private boolean autoStart = true;
    }
}
