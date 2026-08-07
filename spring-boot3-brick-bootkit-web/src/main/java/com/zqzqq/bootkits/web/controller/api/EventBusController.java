package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.EventBusWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件事件总线 API 控制器。
 * 提供事件统计、事件类型与最近事件流查询接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/eventbus")
@Tag(name = "事件总线", description = "插件事件统计与事件流查询接口")
public class EventBusController {

    private final ObjectProvider<EventBusWebService> eventBusWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public EventBusController(ObjectProvider<EventBusWebService> eventBusWebServiceProvider,
                              PluginWebAuthorizationService authorizationService) {
        this.eventBusWebServiceProvider = eventBusWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private EventBusWebService getService() {
        EventBusWebService service = eventBusWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("事件总线服务未启用");
        }
        return service;
    }

    /**
     * 获取事件统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取事件统计")
    public ApiResult<Map<String, Integer>> stats() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getEventCounts());
    }

    /**
     * 获取事件类型列表
     */
    @GetMapping("/types")
    @Operation(summary = "获取事件类型列表")
    public ApiResult<List<String>> types() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getEventTypes());
    }

    /**
     * 获取最近事件流
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近事件流")
    public ApiResult<List<EventBusWebService.RecentEvent>> recent(
            @RequestParam(defaultValue = "50") int limit) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getRecentEvents(limit));
    }
}
