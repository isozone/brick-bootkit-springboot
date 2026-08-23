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
import org.springframework.stereotype.Service;

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

    public ReleaseWebService(ReleaseService releaseService,
                             ObjectProvider<ClusterWebService> clusterWebServiceProvider) {
        this.releaseService = releaseService;
        this.clusterWebServiceProvider = clusterWebServiceProvider;
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
     * 集群聚合视图：本节点发布记录（按当前节点 ID 标记）+ 在线节点清单。
     * 由于集群节点注册信息不包含对端 Web 地址，无法直接拉取对端发布记录，
     * 故以「本节点发布 + 在线节点列表」的形式提供多节点聚合视图。
     */
    public ClusterReleases aggregateCluster() {
        ClusterReleases result = new ClusterReleases();
        String currentNodeId = "unknown";
        List<ClusterNodeInfo> nodes = Collections.emptyList();
        boolean clusterEnabled = false;
        ClusterWebService clusterWebService = clusterWebServiceProvider.getIfAvailable();
        if (clusterWebService != null) {
            clusterEnabled = true;
            try {
                ClusterNodeInfo currentNode = clusterWebService.getCurrentNode();
                currentNodeId = currentNode != null ? currentNode.getNodeId() : "unknown";
                nodes = clusterWebService.listNodes();
            } catch (Exception e) {
                log.warn("获取集群节点信息失败", e);
            }
        }
        result.setClusterEnabled(clusterEnabled);
        result.setCurrentNodeId(currentNodeId);
        result.setNodes(nodes);
        List<ReleaseRecord> local = releaseService.list(0);
        for (ReleaseRecord record : local) {
            if (record.getNodeId() == null) {
                record.setNodeId(currentNodeId);
            }
        }
        result.setReleases(local);
        return result;
    }
}
