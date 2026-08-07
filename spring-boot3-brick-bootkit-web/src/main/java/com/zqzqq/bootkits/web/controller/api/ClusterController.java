package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.integration.cluster.ClusterNodeInfo;
import com.zqzqq.bootkits.integration.cluster.PluginClusterStateSync;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.ClusterWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 集群管理 API 控制器。
 * 提供集群节点列表、插件状态同步与总览接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/cluster")
@Tag(name = "集群管理", description = "集群节点与插件状态同步接口")
public class ClusterController {

    private final ObjectProvider<ClusterWebService> clusterWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public ClusterController(ObjectProvider<ClusterWebService> clusterWebServiceProvider,
                             PluginWebAuthorizationService authorizationService) {
        this.clusterWebServiceProvider = clusterWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private ClusterWebService getService() {
        ClusterWebService service = clusterWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("集群管理服务未启用");
        }
        return service;
    }

    /**
     * 获取集群总览（节点 + 插件状态）
     */
    @GetMapping("/overview")
    @Operation(summary = "获取集群总览")
    public ApiResult<ClusterWebService.ClusterOverview> overview() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getOverview());
    }

    /**
     * 获取所有在线节点
     */
    @GetMapping("/nodes")
    @Operation(summary = "获取所有在线节点")
    public ApiResult<List<ClusterNodeInfo>> nodes() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().listNodes());
    }

    /**
     * 获取当前节点信息
     */
    @GetMapping("/nodes/current")
    @Operation(summary = "获取当前节点信息")
    public ApiResult<ClusterNodeInfo> currentNode() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getCurrentNode());
    }

    /**
     * 获取集群插件状态列表
     */
    @GetMapping("/plugins/states")
    @Operation(summary = "获取集群插件状态列表")
    public ApiResult<List<PluginClusterStateSync.PluginClusterState>> pluginStates() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().listPluginStates());
    }

    /**
     * 手动同步本节点插件状态到集群
     */
    @PostMapping("/plugins/sync")
    @Operation(summary = "同步本节点插件状态到集群")
    public ApiResult<Integer> syncPluginStates() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().syncLocalPluginStates());
    }
}
