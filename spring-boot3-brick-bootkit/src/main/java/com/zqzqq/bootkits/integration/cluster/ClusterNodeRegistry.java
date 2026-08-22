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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 集群节点注册表。
 * <p>
 * 每个节点启动时在共享目录（clusterSharedRoot）下注册自身信息，
 * 并通过定时心跳刷新 lastHeartbeat；读取节点列表时过滤已过期的节点。
 * <p>
 * 存储格式：Properties 文件（{@code .plugin-state/cluster/nodes/<nodeId>.info}），
 * 避免引入额外的序列化依赖。
 */
public class ClusterNodeRegistry {

    private static final Logger log = LoggerFactory.getLogger(ClusterNodeRegistry.class);

    private static final String NODES_SUB_PATH = ".plugin-state/cluster/nodes";
    private static final long DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 5L;
    private static final long DEFAULT_NODE_EXPIRE_SECONDS = 20L;

    private final Path nodesDir;
    private final String nodeId;
    private final String host;
    private final long startTime;
    private final long heartbeatIntervalSeconds;
    private final long nodeExpireSeconds;

    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ClusterNodeHeartbeat");
                t.setDaemon(true);
                return t;
            });

    public ClusterNodeRegistry(Path clusterSharedRoot) {
        this(clusterSharedRoot, DEFAULT_HEARTBEAT_INTERVAL_SECONDS, DEFAULT_NODE_EXPIRE_SECONDS);
    }

    public ClusterNodeRegistry(Path clusterSharedRoot,
                               long heartbeatIntervalSeconds,
                               long nodeExpireSeconds) {
        this.nodesDir = clusterSharedRoot.resolve(NODES_SUB_PATH);
        this.nodeId = UUID.randomUUID().toString().substring(0, 8);
        this.host = resolveHost();
        this.startTime = System.currentTimeMillis();
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.nodeExpireSeconds = nodeExpireSeconds;
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    /**
     * 启动注册与心跳
     */
    public synchronized void start() {
        try {
            Files.createDirectories(nodesDir);
        } catch (IOException e) {
            log.warn("创建集群节点目录失败: {}", nodesDir, e);
        }
        heartbeat(); // 立即注册一次
        heartbeatExecutor.scheduleWithFixedDelay(this::heartbeat,
                heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
        log.info("集群节点已注册: nodeId={}, host={}, startTime={}", nodeId, host, startTime);
    }

    /**
     * 刷新心跳
     */
    public void heartbeat() {
        writeNodeInfo(System.currentTimeMillis());
    }

    /**
     * 更新当前节点的插件数量
     */
    public void updatePluginCount(int pluginCount) {
        writeNodeInfo(System.currentTimeMillis(), pluginCount);
    }

    /**
     * 获取当前节点信息
     */
    public ClusterNodeInfo getCurrentNode() {
        return new ClusterNodeInfo(nodeId, host, startTime, System.currentTimeMillis(), 0, "ONLINE");
    }

    /**
     * 获取所有在线节点
     */
    public List<ClusterNodeInfo> listNodes() {
        List<ClusterNodeInfo> nodes = new ArrayList<>();
        if (!Files.isDirectory(nodesDir)) {
            return nodes;
        }
        long now = System.currentTimeMillis();
        try (Stream<Path> files = Files.list(nodesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".info"))
                    .forEach(p -> {
                        ClusterNodeInfo info = readNodeInfo(p);
                        if (info == null) {
                            return;
                        }
                        boolean online = now - info.getLastHeartbeat() < nodeExpireSeconds * 1000;
                        if (online) {
                            info.setStatus("ONLINE");
                            nodes.add(info);
                        }
                    });
        } catch (IOException e) {
            log.warn("读取集群节点列表失败: {}", nodesDir, e);
        }
        return nodes;
    }

    /**
     * 获取节点数
     */
    public int getNodeCount() {
        return listNodes().size();
    }

    /**
     * 停止心跳并注销节点
     */
    public synchronized void shutdown() {
        heartbeatExecutor.shutdownNow();
        try {
            Path nodeFile = nodesDir.resolve(nodeId + ".info");
            Files.deleteIfExists(nodeFile);
        } catch (IOException ignored) {
            // ignore
        }
        log.info("集群节点已注销: nodeId={}", nodeId);
    }

    private void writeNodeInfo(long lastHeartbeat) {
        writeNodeInfo(lastHeartbeat, 0);
    }

    private void writeNodeInfo(long lastHeartbeat, int pluginCount) {
        try {
            Files.createDirectories(nodesDir);
            Properties props = new Properties();
            props.setProperty("nodeId", nodeId);
            props.setProperty("host", host);
            props.setProperty("startTime", String.valueOf(startTime));
            props.setProperty("lastHeartbeat", String.valueOf(lastHeartbeat));
            props.setProperty("pluginCount", String.valueOf(pluginCount));
            Path nodeFile = nodesDir.resolve(nodeId + ".info");
            try (OutputStream out = Files.newOutputStream(nodeFile)) {
                props.store(out, "brick-bootkit cluster node");
            }
        } catch (IOException e) {
            log.warn("写入集群节点信息失败: nodeId={}", nodeId, e);
        }
    }

    private ClusterNodeInfo readNodeInfo(Path file) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            log.warn("读取集群节点信息失败: {}", file, e);
            return null;
        }
        try {
            return new ClusterNodeInfo(
                    props.getProperty("nodeId"),
                    props.getProperty("host"),
                    Long.parseLong(props.getProperty("startTime", "0")),
                    Long.parseLong(props.getProperty("lastHeartbeat", "0")),
                    Integer.parseInt(props.getProperty("pluginCount", "0")),
                    "UNKNOWN"
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
