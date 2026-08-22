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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 集群管理 Web 服务。
 * 提供集群节点、插件状态同步等能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class ClusterWebService {

    private final ObjectProvider<ClusterNodeRegistry> nodeRegistryProvider;
    private final ObjectProvider<PluginClusterStateSync> stateSyncProvider;
    private final ObjectProvider<PluginManager> pluginManagerProvider;

    public ClusterWebService(ObjectProvider<ClusterNodeRegistry> nodeRegistryProvider,
                             ObjectProvider<PluginClusterStateSync> stateSyncProvider,
                             ObjectProvider<PluginManager> pluginManagerProvider) {
        this.nodeRegistryProvider = nodeRegistryProvider;
        this.stateSyncProvider = stateSyncProvider;
        this.pluginManagerProvider = pluginManagerProvider;
    }

    private ClusterNodeRegistry getNodeRegistry() {
        ClusterNodeRegistry registry = nodeRegistryProvider.getIfAvailable();
        if (registry == null) {
            throw new PluginException("集群节点注册服务未启用（plugin.cluster.enabled=false）");
        }
        return registry;
    }

    private PluginClusterStateSync getStateSync() {
        PluginClusterStateSync stateSync = stateSyncProvider.getIfAvailable();
        if (stateSync == null) {
            throw new PluginException("集群插件状态同步服务未启用");
        }
        return stateSync;
    }

    /**
     * 获取所有在线节点
     */
    public List<ClusterNodeInfo> listNodes() {
        return getNodeRegistry().listNodes();
    }

    /**
     * 获取当前节点信息
     */
    public ClusterNodeInfo getCurrentNode() {
        return getNodeRegistry().getCurrentNode();
    }

    /**
     * 获取集群插件状态列表
     */
    public List<PluginClusterStateSync.PluginClusterState> listPluginStates() {
        return getStateSync().listPluginStates();
    }

    /**
     * 手动同步本节点所有已加载插件的状态到集群
     */
    public int syncLocalPluginStates() {
        ClusterNodeRegistry nodeRegistry = getNodeRegistry();
        PluginClusterStateSync stateSync = getStateSync();
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            throw new PluginException("插件管理器不可用");
        }

        List<PluginInfo> plugins = pluginManager.getPlugins();
        for (PluginInfo pluginInfo : plugins) {
            String state = pluginInfo.getPluginState() == null
                    ? "UNKNOWN" : pluginInfo.getPluginState().name();
            stateSync.syncPluginState(pluginInfo.getPluginId(), state, nodeRegistry.getNodeId());
        }
        log.info("已同步 {} 个插件状态到集群", plugins.size());
        return plugins.size();
    }

    /**
     * 获取本节点插件数（用于节点信息展示）
     */
    public int getLocalPluginCount() {
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            return 0;
        }
        return pluginManager.getPlugins().size();
    }

    /**
     * 集群总览视图（节点 + 插件状态合并）
     */
    public ClusterOverview getOverview() {
        ClusterOverview overview = new ClusterOverview();
        overview.setNodes(listNodes());
        overview.setPluginStates(listPluginStates());
        overview.setLocalPluginCount(getLocalPluginCount());
        return overview;
    }

    /**
     * 集群总览
     */
    public static class ClusterOverview {
        private List<ClusterNodeInfo> nodes;
        private List<PluginClusterStateSync.PluginClusterState> pluginStates;
        private int localPluginCount;

        public List<ClusterNodeInfo> getNodes() {
            return nodes;
        }

        public void setNodes(List<ClusterNodeInfo> nodes) {
            this.nodes = nodes;
        }

        public List<PluginClusterStateSync.PluginClusterState> getPluginStates() {
            return pluginStates;
        }

        public void setPluginStates(List<PluginClusterStateSync.PluginClusterState> pluginStates) {
            this.pluginStates = pluginStates;
        }

        public int getLocalPluginCount() {
            return localPluginCount;
        }

        public void setLocalPluginCount(int localPluginCount) {
            this.localPluginCount = localPluginCount;
        }
    }
}
