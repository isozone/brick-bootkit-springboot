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

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ClusterReleases;
import com.zqzqq.bootkits.web.dto.ReleaseRecord;
import com.zqzqq.bootkits.web.service.ReleaseWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReleaseController Test")
class ReleaseControllerTest {

    private ReleaseWebService releaseWebService;
    private PluginWebAuthorizationService authorizationService;
    private ReleaseController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        releaseWebService = mock(ReleaseWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<ReleaseWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(releaseWebService);
        controller = new ReleaseController(provider, authorizationService);
    }

    @Test
    @DisplayName("列表查询返回发布记录")
    void listShouldReturnRecords() {
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId("r1");
        r.setPluginId("p1");
        when(releaseWebService.listReleases(50)).thenReturn(Collections.singletonList(r));

        ApiResult<List<ReleaseRecord>> result = controller.list(50);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getReleaseId()).isEqualTo("r1");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_HISTORY_READ, null);
    }

    @Test
    @DisplayName("详情查询返回单条记录")
    void getShouldReturnRecord() {
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId("r1");
        when(releaseWebService.getRelease("r1")).thenReturn(r);

        ApiResult<ReleaseRecord> result = controller.get("r1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getReleaseId()).isEqualTo("r1");
    }

    @Test
    @DisplayName("删除记录调用服务")
    void deleteShouldInvokeService() {
        ApiResult<Void> result = controller.delete("r1");

        assertThat(result.isSuccess()).isTrue();
        verify(releaseWebService).removeRelease("r1");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_HISTORY_WRITE, null);
    }

    @Test
    @DisplayName("插件未启用时返回错误")
    void unavailableShouldReturnError() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ReleaseWebService> empty = mock(ObjectProvider.class);
        when(empty.getIfAvailable()).thenReturn(null);
        ReleaseController offline = new ReleaseController(empty, authorizationService);

        ApiResult<List<ReleaseRecord>> result = offline.list(10);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("集群聚合视图返回节点与发布")
    void clusterShouldReturnAggregate() {
        ClusterReleases aggregate = new ClusterReleases();
        aggregate.setClusterEnabled(true);
        aggregate.setCurrentNodeId("node-1");
        aggregate.setNodes(Collections.emptyList());
        aggregate.setReleases(Collections.emptyList());
        when(releaseWebService.aggregateCluster()).thenReturn(aggregate);

        ApiResult<ClusterReleases> result = controller.cluster();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCurrentNodeId()).isEqualTo("node-1");
        assertThat(result.getData().isClusterEnabled()).isTrue();
        verify(authorizationService).check(PluginWebPermission.PLUGIN_HISTORY_READ, null);
    }
}
