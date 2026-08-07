package com.zqzqq.bootkits.integration.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * 集群插件状态同步。
 * <p>
 * 插件生命周期变化时（安装/启动/停止/卸载），将插件状态写入共享目录，
 * 各节点可读取全集群的插件状态视图，实现多节点插件状态同步。
 * <p>
 * 存储格式：Properties 文件（{@code .plugin-state/cluster/plugins/<pluginId>.state}）。
 */
public class PluginClusterStateSync {

    private static final Logger log = LoggerFactory.getLogger(PluginClusterStateSync.class);

    private static final String PLUGINS_SUB_PATH = ".plugin-state/cluster/plugins";

    private final Path pluginsDir;

    public PluginClusterStateSync(Path clusterSharedRoot) {
        this.pluginsDir = clusterSharedRoot.resolve(PLUGINS_SUB_PATH);
    }

    /**
     * 同步插件状态
     *
     * @param pluginId 插件 ID
     * @param state    插件状态（STARTED/STOPPED/INSTALLED/UNINSTALLED 等）
     * @param nodeId   状态来源节点
     */
    public void syncPluginState(String pluginId, String state, String nodeId) {
        try {
            Files.createDirectories(pluginsDir);
            Properties props = new Properties();
            props.setProperty("pluginId", pluginId);
            props.setProperty("state", state == null ? "UNKNOWN" : state);
            props.setProperty("nodeId", nodeId == null ? "" : nodeId);
            props.setProperty("updatedAt", String.valueOf(System.currentTimeMillis()));
            Path stateFile = pluginsDir.resolve(sanitize(pluginId) + ".state");
            try (OutputStream out = Files.newOutputStream(stateFile)) {
                props.store(out, "brick-bootkit cluster plugin state");
            }
        } catch (IOException e) {
            log.warn("同步插件状态失败: pluginId={}, state={}", pluginId, state, e);
        }
    }

    /**
     * 移除插件状态
     */
    public void removePluginState(String pluginId) {
        try {
            Path stateFile = pluginsDir.resolve(sanitize(pluginId) + ".state");
            Files.deleteIfExists(stateFile);
        } catch (IOException e) {
            log.warn("移除插件状态失败: pluginId={}", pluginId, e);
        }
    }

    /**
     * 获取全集群插件状态列表
     */
    public List<PluginClusterState> listPluginStates() {
        List<PluginClusterState> states = new ArrayList<>();
        if (!Files.isDirectory(pluginsDir)) {
            return states;
        }
        try (Stream<Path> files = Files.list(pluginsDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".state"))
                    .forEach(p -> {
                        PluginClusterState state = readState(p);
                        if (state != null) {
                            states.add(state);
                        }
                    });
        } catch (IOException e) {
            log.warn("读取集群插件状态失败: {}", pluginsDir, e);
        }
        states.sort((a, b) -> a.getPluginId().compareTo(b.getPluginId()));
        return states;
    }

    /**
     * 获取指定插件的集群状态
     */
    public PluginClusterState getPluginState(String pluginId) {
        Path stateFile = pluginsDir.resolve(sanitize(pluginId) + ".state");
        return readState(stateFile);
    }

    private PluginClusterState readState(Path file) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            log.warn("读取插件状态失败: {}", file, e);
            return null;
        }
        try {
            return new PluginClusterState(
                    props.getProperty("pluginId"),
                    props.getProperty("state"),
                    props.getProperty("nodeId"),
                    Long.parseLong(props.getProperty("updatedAt", "0"))
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitize(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return "unknown";
        }
        return pluginId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * 集群插件状态
     */
    public static class PluginClusterState {
        private final String pluginId;
        private final String state;
        private final String nodeId;
        private final long updatedAt;

        public PluginClusterState(String pluginId, String state, String nodeId, long updatedAt) {
            this.pluginId = pluginId;
            this.state = state;
            this.nodeId = nodeId;
            this.updatedAt = updatedAt;
        }

        public String getPluginId() {
            return pluginId;
        }

        public String getState() {
            return state;
        }

        public String getNodeId() {
            return nodeId;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }
    }
}
