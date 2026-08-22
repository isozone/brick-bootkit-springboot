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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.integration.cluster.ClusterNodeInfo;
import com.zqzqq.bootkits.integration.cluster.ClusterNodeRegistry;
import com.zqzqq.bootkits.integration.cluster.PluginClusterStateSync;
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
 * 集群管理 Web 服务单元测试。
 */
@DisplayName("ClusterWebService Test")
class ClusterWebServiceTest {

    private ClusterNodeRegistry nodeRegistry;
    private PluginClusterStateSync stateSync;
    private PluginManager pluginManager;
    private ClusterWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        nodeRegistry = mock(ClusterNodeRegistry.class);
        stateSync = mock(PluginClusterStateSync.class);
        pluginManager = mock(PluginManager.class);

        ObjectProvider<ClusterNodeRegistry> nrProvider = mock(ObjectProvider.class);
        when(nrProvider.getIfAvailable()).thenReturn(nodeRegistry);
        ObjectProvider<PluginClusterStateSync> ssProvider = mock(ObjectProvider.class);
        when(ssProvider.getIfAvailable()).thenReturn(stateSync);
        ObjectProvider<PluginManager> pmProvider = mock(ObjectProvider.class);
        when(pmProvider.getIfAvailable()).thenReturn(pluginManager);

        service = new ClusterWebService(nrProvider, ssProvider, pmProvider);
    }

    @Test
    @DisplayName("获取所有在线节点")
    void listNodesShouldReturnList() {
        ClusterNodeInfo node = new ClusterNodeInfo("node-1", "host-a", 1000L, 2000L, 1, "ONLINE");
        when(nodeRegistry.listNodes()).thenReturn(Collections.singletonList(node));

        List<ClusterNodeInfo> actual = service.listNodes();

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getNodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("获取当前节点信息")
    void getCurrentNodeShouldReturnData() {
        ClusterNodeInfo node = new ClusterNodeInfo("node-1", "host-a", 1000L, 2000L, 1, "ONLINE");
        when(nodeRegistry.getCurrentNode()).thenReturn(node);

        ClusterNodeInfo actual = service.getCurrentNode();

        assertThat(actual.getHost()).isEqualTo("host-a");
    }

    @Test
    @DisplayName("获取集群插件状态列表")
    void listPluginStatesShouldReturnList() {
        PluginClusterStateSync.PluginClusterState state =
                new PluginClusterStateSync.PluginClusterState("plugin-a", "STARTED", "node-1", 1000L);
        when(stateSync.listPluginStates()).thenReturn(Collections.singletonList(state));

        List<PluginClusterStateSync.PluginClusterState> actual = service.listPluginStates();

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPluginId()).isEqualTo("plugin-a");
    }

    @Test
    @DisplayName("手动同步本节点插件状态")
    void syncLocalPluginStatesShouldReturnCount() {
        when(nodeRegistry.getNodeId()).thenReturn("node-1");
        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginInfo.getPluginId()).thenReturn("plugin-a");
        when(pluginInfo.getPluginState()).thenReturn(null);
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginInfo));

        int count = service.syncLocalPluginStates();

        assertThat(count).isEqualTo(1);
        verify(stateSync).syncPluginState(eq("plugin-a"), anyString(), eq("node-1"));
    }

    @Test
    @DisplayName("获取本节点插件数")
    void getLocalPluginCountShouldReturnCount() {
        when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());

        int count = service.getLocalPluginCount();

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("获取集群总览")
    void getOverviewShouldReturnData() {
        when(nodeRegistry.listNodes()).thenReturn(Collections.emptyList());
        when(stateSync.listPluginStates()).thenReturn(Collections.emptyList());
        when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());

        ClusterWebService.ClusterOverview overview = service.getOverview();

        assertThat(overview.getNodes()).isEmpty();
        assertThat(overview.getPluginStates()).isEmpty();
        assertThat(overview.getLocalPluginCount()).isZero();
    }

    @Test
    @DisplayName("节点注册服务缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenNodeRegistryMissing() {
        ObjectProvider<ClusterNodeRegistry> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        ClusterWebService emptyService = new ClusterWebService(emptyProvider,
                mock(ObjectProvider.class), mock(ObjectProvider.class));

        assertThatThrownBy(emptyService::listNodes)
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
