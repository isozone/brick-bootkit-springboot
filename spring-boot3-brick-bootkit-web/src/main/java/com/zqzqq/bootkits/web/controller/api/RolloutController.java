package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.RolloutWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件灰度发布 API 控制器。
 * 提供灰度配置查看、探针列表与手动灰度决策模拟接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/rollout")
@Tag(name = "灰度发布", description = "插件灰度发布配置与决策接口")
public class RolloutController {

    private final ObjectProvider<RolloutWebService> rolloutWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public RolloutController(ObjectProvider<RolloutWebService> rolloutWebServiceProvider,
                             PluginWebAuthorizationService authorizationService) {
        this.rolloutWebServiceProvider = rolloutWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private RolloutWebService getService() {
        RolloutWebService service = rolloutWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("灰度发布服务未启用");
        }
        return service;
    }

    /**
     * 获取灰度发布配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取灰度发布配置")
    public ApiResult<Map<String, Object>> config() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getRolloutConfig());
    }

    /**
     * 获取已注册的灰度探针列表
     */
    @GetMapping("/probes")
    @Operation(summary = "获取灰度探针列表")
    public ApiResult<List<String>> probes() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getProbeNames());
    }

    /**
     * 对指定插件运行全部灰度探针，模拟灰度决策
     */
    @PostMapping("/check/{pluginId}")
    @Operation(summary = "模拟灰度决策")
    public ApiResult<RolloutWebService.RolloutDecision> check(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().checkPlugin(pluginId));
    }
}
