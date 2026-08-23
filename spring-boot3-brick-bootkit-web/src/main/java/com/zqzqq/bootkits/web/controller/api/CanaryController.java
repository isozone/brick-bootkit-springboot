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

import com.zqzqq.bootkits.core.communication.ServiceRoutingGroup;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.CanaryWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 金丝雀路由配置 API 控制器。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/canary")
@Tag(name = "金丝雀路由", description = "多版本服务金丝雀权重配置接口")
public class CanaryController {

    private final ObjectProvider<CanaryWebService> canaryWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public CanaryController(ObjectProvider<CanaryWebService> canaryWebServiceProvider,
                            PluginWebAuthorizationService authorizationService) {
        this.canaryWebServiceProvider = canaryWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private CanaryWebService getService() {
        CanaryWebService service = canaryWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new PluginException("插件功能未启用");
        }
        return service;
    }

    @GetMapping("/routing")
    @Operation(summary = "获取多版本服务路由权重分组")
    public ApiResult<List<ServiceRoutingGroup>> routing() {
        authorizationService.check(PluginWebPermission.PLUGIN_HISTORY_READ, null);
        return ApiResult.success(getService().describeRouting());
    }

    @PostMapping("/weight")
    @Operation(summary = "更新服务实现的分流权重")
    public ApiResult<Void> updateWeight(@RequestBody WeightUpdateRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_HISTORY_WRITE, null);
        getService().updateWeight(request.getPluginId(), request.getInterfaceName(), request.getWeight());
        return ApiResult.success();
    }

    public static class WeightUpdateRequest {
        private String pluginId;
        private String interfaceName;
        private int weight;

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}
