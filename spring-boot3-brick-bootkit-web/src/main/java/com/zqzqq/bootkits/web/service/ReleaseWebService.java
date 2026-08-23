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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 发布治理 Web 服务：对外提供发布记录的查询、删除与集群聚合能力。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@Service
public class ReleaseWebService {

    private final ReleaseService releaseService;
    private final ObjectProvider<ClusterWebService> clusterWebServiceProvider;
    private final ObjectProvider<PeerReleaseFetcher> peerReleaseFetcherProvider;
    private final String internalToken;

    public ReleaseWebService(ReleaseService releaseService,
                             ObjectProvider<ClusterWebService> clusterWebServiceProvider,
                             ObjectProvider<PeerReleaseFetcher> peerReleaseFetcherProvider,
                             @Value("${plugin.cluster.internal-token:}") String internalToken) {
        this.releaseService = releaseService;
        this.clusterWebServiceProvider = clusterWebServiceProvider;
        this.peerReleaseFetcherProvider = peerReleaseFetcherProvider;
        this.internalToken = internalToken == null ? "" : internalToken;
    }

    public List<ReleaseRecord> listReleases(int limit) {
        return releaseService.list(limit <= 0 ? 50 : limit);
    }

    public List<ReleaseRecord> listReleasesByPlugin(String pluginId, int limit) {
        return releaseService.listByPlugin(pluginId, limit);
    }

    public ReleaseRecord getRelease(String releaseId) {
        return releaseService.get(releaseId);
    }

    public void removeRelease(String releaseId) {
        releaseService.remove(releaseId);
    }

    /**
     * 集群聚合视图：合并本节点与所有在线对端节点的发布记录（按所属节点 ID 标记）。
     * 仅当集群启用、配置了内部令牌且对端节点携带可访问的 Web 基址时才拉取对端记录；
     * 任一节点拉取失败均降级跳过，保证聚合视图始终可用。
     */
    public ClusterReleases aggregateCluster() {
        ClusterReleases result = new ClusterReleases();
        String currentNodeId = "unknown";
        List<ClusterNodeInfo> nodes = Collections.emptyList();
        boolean clusterEnabled = false;
        String currentWebBaseUrl = "";
        ClusterWebService clusterWebService = clusterWebServiceProvider.getIfAvailable();
        if (clusterWebService != null) {
            clusterEnabled = true;
            try {
                ClusterNodeInfo currentNode = clusterWebService.getCurrentNode();
                currentNodeId = currentNode != null ? currentNode.getNodeId() : "unknown";
                currentWebBaseUrl = currentNode != null && currentNode.getWebBaseUrl() != null
                        ? currentNode.getWebBaseUrl() : "";
                nodes = clusterWebService.listNodes();
            } catch (Exception e) {
                log.warn("获取集群节点信息失败", e);
            }
        }
        result.setClusterEnabled(clusterEnabled);
        result.setCurrentNodeId(currentNodeId);
        result.setNodes(nodes);

        List<ReleaseRecord> all = new ArrayList<>();
        List<ReleaseRecord> local = releaseService.list(0);
        for (ReleaseRecord record : local) {
            if (record.getNodeId() == null) {
                record.setNodeId(currentNodeId);
            }
            all.add(record);
        }

        boolean peerFetchEnabled = clusterEnabled
                && peerReleaseFetcherProvider.getIfAvailable() != null
                && !internalToken.isEmpty();
        if (peerFetchEnabled) {
            PeerReleaseFetcher fetcher = peerReleaseFetcherProvider.getIfAvailable();
            for (ClusterNodeInfo node : nodes) {
                if (node == null || currentNodeId.equals(node.getNodeId())) {
                    continue;
                }
                String baseUrl = node.getWebBaseUrl();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    continue;
                }
                try {
                    List<ReleaseRecord> peerRecords = fetcher.fetch(baseUrl, 200, internalToken);
                    for (ReleaseRecord record : peerRecords) {
                        if (record.getNodeId() == null) {
                            record.setNodeId(node.getNodeId());
                        }
                        all.add(record);
                    }
                } catch (Exception e) {
                    log.warn("聚合节点 {} 的发布记录失败", node.getNodeId(), e);
                }
            }
        }
        result.setReleases(all);
        return result;
    }
}
