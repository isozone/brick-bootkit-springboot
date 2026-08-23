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
import com.zqzqq.bootkits.web.dto.ClusterReleases;
import com.zqzqq.bootkits.web.dto.ReleaseRecord;
import com.zqzqq.bootkits.web.service.ReleaseWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发布治理 API 控制器：暴露插件发布记录与审计接口。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/releases")
@Tag(name = "发布治理", description = "插件发布记录与审计接口")
public class ReleaseController {

    private final ObjectProvider<ReleaseWebService> releaseWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public ReleaseController(ObjectProvider<ReleaseWebService> releaseWebServiceProvider,
                             PluginWebAuthorizationService authorizationService) {
        this.releaseWebServiceProvider = releaseWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private void authorize(PluginWebPermission permission) {
        authorizationService.check(permission, null);
    }

    private void authorize(PluginWebPermission permission, String pluginId) {
        authorizationService.check(permission, pluginId);
    }

    @GetMapping
    @Operation(summary = "获取发布记录列表")
    public ApiResult<List<ReleaseRecord>> list(@RequestParam(defaultValue = "50") int limit) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ);
        ReleaseWebService service = releaseWebServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResult.error(500, "插件功能未启用");
        }
        return ApiResult.success(service.listReleases(limit));
    }

    @GetMapping("/{releaseId}")
    @Operation(summary = "获取发布记录详情")
    public ApiResult<ReleaseRecord> get(@PathVariable String releaseId) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ);
        ReleaseWebService service = releaseWebServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResult.error(500, "插件功能未启用");
        }
        ReleaseRecord record = service.getRelease(releaseId);
        if (record == null) {
            return ApiResult.error(404, "发布记录不存在");
        }
        return ApiResult.success(record);
    }

    @DeleteMapping("/{releaseId}")
    @Operation(summary = "删除发布记录")
    public ApiResult<Void> delete(@PathVariable String releaseId) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_WRITE);
        ReleaseWebService service = releaseWebServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResult.error(500, "插件功能未启用");
        }
        service.removeRelease(releaseId);
        return ApiResult.success();
    }

    @GetMapping("/cluster")
    @Operation(summary = "集群发布聚合视图（本节点发布 + 在线节点清单）")
    public ApiResult<ClusterReleases> cluster() {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ);
        ReleaseWebService service = releaseWebServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResult.error(500, "插件功能未启用");
        }
        return ApiResult.success(service.aggregateCluster());
    }
}
