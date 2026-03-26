package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO;
import com.zqzqq.bootkits.web.dto.ThreadDetailDTO;
import com.zqzqq.bootkits.web.service.MonitorWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统监控 API 控制器
 *
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/monitor")
@Tag(name = "系统监控", description = "系统监控相关接口")
public class MonitorController {

    private final ObjectProvider<MonitorWebService> monitorWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public MonitorController(ObjectProvider<MonitorWebService> monitorWebServiceProvider,
                             PluginWebAuthorizationService authorizationService) {
        this.monitorWebServiceProvider = monitorWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private void authorize() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    /**
     * 获取系统监控概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取监控概览")
    public ApiResult<MonitorOverviewDTO> overview() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getOverview());
    }

    /**
     * 获取 JVM 内存信息
     */
    @GetMapping("/memory")
    @Operation(summary = "获取内存信息")
    public ApiResult<MonitorOverviewDTO.MemoryInfo> memory() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getMemoryInfo());
    }

    /**
     * 获取 CPU 信息
     */
    @GetMapping("/cpu")
    @Operation(summary = "获取 CPU 信息")
    public ApiResult<MonitorOverviewDTO.CpuInfo> cpu() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getCpuInfo());
    }

    /**
     * 获取线程信息（简化版）
     */
    @GetMapping("/threads")
    @Operation(summary = "获取线程信息")
    public ApiResult<MonitorOverviewDTO.ThreadInfo> threads() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getThreadInfo());
    }

    /**
     * 获取线程详细信息（包含线程列表和死锁检测）
     */
    @GetMapping("/threads/detail")
    @Operation(summary = "获取线程详细信息")
    public ApiResult<ThreadDetailDTO> threadDetail() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getThreadDetail());
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统信息")
    public ApiResult<MonitorOverviewDTO.SystemInfo> system() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getSystemInfo());
    }

    /**
     * 获取历史监控数据
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史监控数据")
    public ApiResult<Map<String, Object>> history(
            @RequestParam(defaultValue = "memory") String type,
            @RequestParam(defaultValue = "3600") long since) {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getHistoryData(type, since));
    }

    /**
     * 获取线程池信息
     */
    @GetMapping("/thread-pools")
    @Operation(summary = "获取线程池信息")
    public ApiResult<List<MonitorOverviewDTO.ThreadPoolInfo>> threadPools() {
        authorize();
        return ApiResult.success(monitorWebServiceProvider.getObject().getThreadPools());
    }
}
