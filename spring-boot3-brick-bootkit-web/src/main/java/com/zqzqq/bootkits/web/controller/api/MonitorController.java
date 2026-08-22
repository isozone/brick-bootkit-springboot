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

import com.zqzqq.bootkits.distributed.metrics.DistributedStatusProvider;
import com.zqzqq.bootkits.distributed.metrics.DistributedStatusProvider.DistributedStatus;
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

import java.util.LinkedHashMap;
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
    private final ObjectProvider<DistributedStatusProvider> distributedStatusProvider;

    public MonitorController(ObjectProvider<MonitorWebService> monitorWebServiceProvider,
                             PluginWebAuthorizationService authorizationService,
                             ObjectProvider<DistributedStatusProvider> distributedStatusProvider) {
        this.monitorWebServiceProvider = monitorWebServiceProvider;
        this.authorizationService = authorizationService;
        this.distributedStatusProvider = distributedStatusProvider;
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

    // ==================== 分布式插件模块状态（可选） ====================

    /**
     * 分布式插件模块运行时状态：failover 计数 / 节点健康 / Redis 目录兜底命中 / 调用耗时 等。
     * <p>
     * 仅当宿主启用 {@code plugin.distributed.enabled=true} 时，Spring 容器才会存在
     * {@link DistributedStatusProvider} Bean，本端点返回实时状态快照；
     * 未启用分布式模块时返回 {@code disabled=true}，便于运维一眼区分「模块未启用」与「指标全零」。
     *
     * @since 4.0.9 分布式可观测性（方向一）
     */
    @GetMapping("/distributed")
    @Operation(summary = "获取分布式插件模块运行时状态",
               description = "返回 failover 计数、节点健康、Redis 目录兜底命中、远端调用耗时等指标。"
                       + "未启用分布式模块时返回 disabled=true。")
    public ApiResult<Map<String, Object>> distributed() {
        authorize();
        DistributedStatusProvider provider = distributedStatusProvider.getIfAvailable();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (provider == null) {
            payload.put("disabled", true);
            payload.put("message", "plugin.distributed.enabled=false 或分布式模块未引入；本端点仅在该模块启用时返回状态。");
            return ApiResult.success(payload);
        }
        payload.put("disabled", false);
        payload.put("healthy", provider.isHealthy());
        DistributedStatus status = provider.status();
        payload.put("role", status.getRole());
        payload.put("nodeId", status.getNodeId());
        payload.put("remoteCalls", status.getRemoteCallCount());
        payload.put("remoteCallSuccess", status.getRemoteCallSuccessCount());
        payload.put("errorRate", status.getErrorRate());
        payload.put("failoverCount", status.getFailoverCount());
        payload.put("circuitBreakerTripped", status.getCircuitBreakerTrippedCount());
        payload.put("avgCallMillis", status.getAvgRemoteCallMillis());
        payload.put("activeChannels", status.getActiveChannels());
        payload.put("registryLookupSuccess", status.getRegistryLookupSuccessCount());
        payload.put("registryFallbackUsed", status.getRegistryFallbackUsedCount());
        payload.put("registryFallbackHit", status.getRegistryFallbackHitCount());
        payload.put("registryFallbackMiss", status.getRegistryFallbackMissCount());
        payload.put("registryAvailability", status.getRegistryAvailability());
        payload.put("registeredServiceInterfaces", status.getServiceInterfaces());
        payload.put("nodes", status.getNodes());
        return ApiResult.success(payload);
    }
}
