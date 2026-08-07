package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.LogWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 插件日志查看 API 控制器。
 * 提供应用日志读取与插件日志过滤接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/logs")
@Tag(name = "日志查看", description = "应用日志与插件日志查看接口")
public class LogController {

    private final ObjectProvider<LogWebService> logWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public LogController(ObjectProvider<LogWebService> logWebServiceProvider,
                         PluginWebAuthorizationService authorizationService) {
        this.logWebServiceProvider = logWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private LogWebService getService() {
        LogWebService service = logWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("日志查看服务未启用");
        }
        return service;
    }

    /**
     * 获取当前日志文件路径
     */
    @GetMapping("/file")
    @Operation(summary = "获取当前日志文件路径")
    public ApiResult<String> logFile() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getLogFile());
    }

    /**
     * 读取最近日志（可按插件 ID / 关键字过滤）
     */
    @GetMapping
    @Operation(summary = "读取最近日志")
    public ApiResult<List<String>> logs(@RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "200") int lines) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().readLogs(keyword, lines));
    }
}
