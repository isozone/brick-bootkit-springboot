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


package com.zqzqq.bootkits.integration.cluster;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.integration.spi.PluginLifecycleExtension;
import com.zqzqq.bootkits.integration.spi.PluginLifecycleExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群插件状态同步生命周期扩展。
 * <p>
 * 实现 {@link PluginLifecycleExtension} SPI，在插件安装/启动/停止/卸载时
 * 将插件状态写入共享目录，供其他节点读取，实现多节点插件状态同步。
 */
public class ClusterLifecycleExtension implements PluginLifecycleExtension {

    private static final Logger log = LoggerFactory.getLogger(ClusterLifecycleExtension.class);

    private final PluginClusterStateSync stateSync;
    private final ClusterNodeRegistry nodeRegistry;
    private boolean enabled = true;

    public ClusterLifecycleExtension(PluginClusterStateSync stateSync,
                                     ClusterNodeRegistry nodeRegistry) {
        this.stateSync = stateSync;
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    public String getExtensionId() {
        return "cluster-state-sync";
    }

    @Override
    public void initialize(PluginLifecycleExtensionContext context) {
        log.info("集群插件状态同步扩展已初始化");
    }

    @Override
    public void afterInstall(PluginInsideInfo pluginInsideInfo) {
        sync(pluginInsideInfo, "INSTALLED");
    }

    @Override
    public void afterStart(PluginInsideInfo pluginInsideInfo) {
        sync(pluginInsideInfo, "STARTED");
        refreshNodePluginCount();
    }

    @Override
    public void afterStop(PluginInsideInfo pluginInsideInfo) {
        sync(pluginInsideInfo, "STOPPED");
        refreshNodePluginCount();
    }

    @Override
    public void afterUninstall(PluginInsideInfo pluginInsideInfo) {
        String pluginId = pluginInsideInfo == null ? null : pluginInsideInfo.getPluginId();
        if (pluginId != null) {
            stateSync.removePluginState(pluginId);
        }
        refreshNodePluginCount();
    }

    private void sync(PluginInsideInfo info, String state) {
        if (!enabled || info == null || info.getPluginId() == null) {
            return;
        }
        stateSync.syncPluginState(info.getPluginId(), state, nodeRegistry.getNodeId());
    }

    private void refreshNodePluginCount() {
        // 节点插件数量统计由节点注册表心跳维护，此处不额外处理
    }
}
