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

import com.zqzqq.bootkits.integration.cluster.ClusterNodeInfo;
import com.zqzqq.bootkits.web.dto.ClusterReleases;
import com.zqzqq.bootkits.web.dto.ReleaseRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReleaseWebService Cluster Aggregate Test")
class ReleaseWebServiceTest {

    private ReleaseService releaseService;
    private ClusterWebService clusterWebService;
    private ObjectProvider<ClusterWebService> clusterProvider;
    private PeerReleaseFetcher peerFetcher;
    private ObjectProvider<PeerReleaseFetcher> fetcherProvider;

    private ClusterNodeInfo selfNode;
    private ClusterNodeInfo peerNode;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        releaseService = mock(ReleaseService.class);
        clusterWebService = mock(ClusterWebService.class);
        clusterProvider = mock(ObjectProvider.class);
        when(clusterProvider.getIfAvailable()).thenReturn(clusterWebService);
        peerFetcher = mock(PeerReleaseFetcher.class);
        fetcherProvider = mock(ObjectProvider.class);
        when(fetcherProvider.getIfAvailable()).thenReturn(peerFetcher);

        selfNode = new ClusterNodeInfo("n1", "host-1", 0L, 0L, 1, "ONLINE");
        selfNode.setWebBaseUrl("http://n1:8080");
        peerNode = new ClusterNodeInfo("n2", "host-2", 0L, 0L, 1, "ONLINE");
        peerNode.setWebBaseUrl("http://n2:8080");

        when(clusterWebService.getCurrentNode()).thenReturn(selfNode);
        when(clusterWebService.listNodes()).thenReturn(Arrays.asList(selfNode, peerNode));
    }

    @Test
    @DisplayName("未配置内部令牌时不拉取对端记录")
    void noTokenSkipsPeerFetch() {
        ReleaseRecord local = new ReleaseRecord();
        local.setReleaseId("local-1");
        when(releaseService.list(0)).thenReturn(Collections.singletonList(local));

        ReleaseWebService service = new ReleaseWebService(releaseService, clusterProvider, fetcherProvider, "");

        ClusterReleases result = service.aggregateCluster();

        assertThat(result.isClusterEnabled()).isTrue();
        assertThat(result.getCurrentNodeId()).isEqualTo("n1");
        assertThat(result.getReleases()).hasSize(1);
        assertThat(result.getReleases().get(0).getNodeId()).isEqualTo("n1");
        verify(peerFetcher, never()).fetch(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("配置令牌后合并对端节点发布记录")
    void withTokenMergesPeerRecords() {
        ReleaseRecord local = new ReleaseRecord();
        local.setReleaseId("local-1");
        when(releaseService.list(0)).thenReturn(Collections.singletonList(local));

        ReleaseRecord peer = new ReleaseRecord();
        peer.setReleaseId("peer-1");
        when(peerFetcher.fetch("http://n2:8080", 200, "tok")).thenReturn(Collections.singletonList(peer));

        ReleaseWebService service = new ReleaseWebService(releaseService, clusterProvider, fetcherProvider, "tok");

        ClusterReleases result = service.aggregateCluster();

        assertThat(result.getReleases()).hasSize(2);
        List<ReleaseRecord> peerRecords = result.getReleases().stream()
                .filter(r -> "peer-1".equals(r.getReleaseId())).toList();
        assertThat(peerRecords).hasSize(1);
        assertThat(peerRecords.get(0).getNodeId()).isEqualTo("n2");
        verify(peerFetcher).fetch("http://n2:8080", 200, "tok");
    }

    @Test
    @DisplayName("对端节点无 Web 基址时跳过")
    void peerWithoutBaseUrlSkipped() {
        peerNode.setWebBaseUrl("");
        ReleaseRecord local = new ReleaseRecord();
        local.setReleaseId("local-1");
        when(releaseService.list(0)).thenReturn(Collections.singletonList(local));

        ReleaseWebService service = new ReleaseWebService(releaseService, clusterProvider, fetcherProvider, "tok");

        ClusterReleases result = service.aggregateCluster();

        assertThat(result.getReleases()).hasSize(1);
        verify(peerFetcher, never()).fetch(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }
}
