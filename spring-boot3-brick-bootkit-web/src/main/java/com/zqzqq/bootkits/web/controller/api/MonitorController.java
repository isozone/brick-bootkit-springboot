package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO;
import com.zqzqq.bootkits.web.dto.ThreadDetailDTO;
import com.zqzqq.bootkits.web.service.MonitorWebService;
import com.zqzqq.bootkits.web.service.SimpleMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统监控 API 控制器
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("${brick.web.api-prefix:/brick-web/api}/monitor")
@Tag(name = "系统监控", description = "系统监控相关接口")
public class MonitorController {

    private final ObjectProvider<MonitorWebService> monitorWebServiceProvider;
    private final SimpleMonitorService simpleMonitorService;

    public MonitorController(ObjectProvider<MonitorWebService> monitorWebServiceProvider, SimpleMonitorService simpleMonitorService) {
        this.monitorWebServiceProvider = monitorWebServiceProvider;
        this.simpleMonitorService = simpleMonitorService;
    }

    /**
     * 获取系统监控概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取监控概览")
    public ApiResult<MonitorOverviewDTO> overview() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getOverview());
        }
        return ApiResult.success(simpleMonitorService.getOverview());
    }

    /**
     * 获取 JVM 内存信息
     */
    @GetMapping("/memory")
    @Operation(summary = "获取内存信息")
    public ApiResult<MonitorOverviewDTO.MemoryInfo> memory() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getMemoryInfo());
        }
        return ApiResult.success(simpleMonitorService.getMemoryInfo());
    }

    /**
     * 获取 CPU 信息
     */
    @GetMapping("/cpu")
    @Operation(summary = "获取 CPU 信息")
    public ApiResult<MonitorOverviewDTO.CpuInfo> cpu() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getCpuInfo());
        }
        return ApiResult.success(simpleMonitorService.getCpuInfo());
    }

    /**
     * 获取线程信息（简化版）
     */
    @GetMapping("/threads")
    @Operation(summary = "获取线程信息")
    public ApiResult<MonitorOverviewDTO.ThreadInfo> threads() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getThreadInfo());
        }
        return ApiResult.success(simpleMonitorService.getThreadInfo());
    }

    /**
     * 获取线程详细信息（包含线程列表和死锁检测）
     */
    @GetMapping("/threads/detail")
    @Operation(summary = "获取线程详细信息")
    public ApiResult<ThreadDetailDTO> threadDetail() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getThreadDetail());
        }
        return ApiResult.success(simpleMonitorService.getThreadDetail());
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统信息")
    public ApiResult<MonitorOverviewDTO.SystemInfo> system() {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getSystemInfo());
        }
        return ApiResult.success(simpleMonitorService.getSystemInfo());
    }

    /**
     * 获取历史监控数据
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史监控数据")
    public ApiResult<Map<String, Object>> history(
            @RequestParam(defaultValue = "memory") String type,
            @RequestParam(defaultValue = "3600") long since) {
        MonitorWebService monitorWebService = monitorWebServiceProvider.getIfAvailable();
        if (monitorWebService != null) {
            return ApiResult.success(monitorWebService.getHistoryData(type, since));
        }
        return ApiResult.success(simpleMonitorService.getHistoryData(type, since));
    }
}
