package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.integration.cluster.ClusterNodeInfo;
import com.zqzqq.bootkits.integration.cluster.PluginClusterStateSync;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.ClusterWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 集群管理 Controller 单元测试。
 */
@DisplayName("ClusterController Test")
class ClusterControllerTest {

    private ClusterWebService clusterWebService;
    private PluginWebAuthorizationService authorizationService;
    private ClusterController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        clusterWebService = mock(ClusterWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<ClusterWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(clusterWebService);
        controller = new ClusterController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取集群总览")
    void overviewShouldReturnData() {
        ClusterWebService.ClusterOverview overview = new ClusterWebService.ClusterOverview();
        overview.setNodes(Collections.emptyList());
        overview.setPluginStates(Collections.emptyList());
        overview.setLocalPluginCount(1);
        when(clusterWebService.getOverview()).thenReturn(overview);

        ApiResult<ClusterWebService.ClusterOverview> result = controller.overview();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getLocalPluginCount()).isEqualTo(1);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("获取所有在线节点")
    void nodesShouldReturnList() {
        ClusterNodeInfo node = new ClusterNodeInfo("node-1", "host-a", 1000L, 2000L, 1, "ONLINE");
        when(clusterWebService.listNodes()).thenReturn(Collections.singletonList(node));

        ApiResult<List<ClusterNodeInfo>> result = controller.nodes();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getNodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("获取当前节点信息")
    void currentNodeShouldReturnData() {
        ClusterNodeInfo node = new ClusterNodeInfo("node-1", "host-a", 1000L, 2000L, 1, "ONLINE");
        when(clusterWebService.getCurrentNode()).thenReturn(node);

        ApiResult<ClusterNodeInfo> result = controller.currentNode();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getHost()).isEqualTo("host-a");
    }

    @Test
    @DisplayName("获取集群插件状态列表")
    void pluginStatesShouldReturnList() {
        PluginClusterStateSync.PluginClusterState state =
                new PluginClusterStateSync.PluginClusterState("plugin-a", "STARTED", "node-1", 1000L);
        when(clusterWebService.listPluginStates()).thenReturn(Collections.singletonList(state));

        ApiResult<List<PluginClusterStateSync.PluginClusterState>> result = controller.pluginStates();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getPluginId()).isEqualTo("plugin-a");
    }

    @Test
    @DisplayName("手动同步本节点插件状态")
    void syncPluginStatesShouldReturnCount() {
        when(clusterWebService.syncLocalPluginStates()).thenReturn(2);

        ApiResult<Integer> result = controller.syncPluginStates();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(2);
        verify(clusterWebService).syncLocalPluginStates();
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void overviewShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.overview())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(clusterWebService);
    }
}
