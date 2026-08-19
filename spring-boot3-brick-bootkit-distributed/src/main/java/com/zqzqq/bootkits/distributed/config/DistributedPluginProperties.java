package com.zqzqq.bootkits.distributed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式插件模块配置。
 * <p>
 * 前缀：{@code plugin.distributed}。
 */
@ConfigurationProperties(prefix = "plugin.distributed")
public class DistributedPluginProperties {

    /**
     * 是否启用分布式插件模块。默认关闭，宿主显式开启。
     */
    private boolean enabled = false;

    /**
     * 当前节点的角色：HOST（宿主/只消费远端服务）或 WORKER（执行节点/提供插件服务）。
     */
    private NodeRole role = NodeRole.HOST;

    /**
     * 本节点对外暴露的 gRPC 监听端口（WORKER 角色使用）。
     */
    private int port = 9090;

    /**
     * 节点唯一标识。多节点时建议显式配置（如 node-1 / node-2），
     * 保证重启后身份稳定、不残留陈旧注册；为空时自动取「host:port」。
     */
    private String nodeId = "";

    /**
     * 服务目录 Redis 键前缀。
     */
    private String registryPrefix = "brick-bootkit:distributed:services";

    /**
     * 服务注册的心跳有效期（秒），超过该时间未续期的服务视为下线。
     */
    private long heartbeatTtlSeconds = 30L;

    /**
     * 节点心跳刷新间隔（秒）。
     */
    private long heartbeatIntervalSeconds = 10L;

    /**
     * gRPC 最大消息字节数（默认 16MB）。
     */
    private int maxInboundMessageSize = 16 * 1024 * 1024;

    /**
     * 调用远端超时（毫秒），0 表示不设超时。
     */
    private long callTimeoutMillis = 5000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRegistryPrefix() {
        return registryPrefix;
    }

    public void setRegistryPrefix(String registryPrefix) {
        this.registryPrefix = registryPrefix;
    }

    public long getHeartbeatTtlSeconds() {
        return heartbeatTtlSeconds;
    }

    public void setHeartbeatTtlSeconds(long heartbeatTtlSeconds) {
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    public long getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(long heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public void setMaxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    public long getCallTimeoutMillis() {
        return callTimeoutMillis;
    }

    public void setCallTimeoutMillis(long callTimeoutMillis) {
        this.callTimeoutMillis = callTimeoutMillis;
    }

    /**
     * 节点角色。
     */
    public enum NodeRole {
        /**
         * 宿主节点：本身是网关，消费远端插件服务，也可承载少量本地插件。
         */
        HOST,
        /**
         * 执行节点：承载插件，向 Redis 注册服务并向外部提供 gRPC 调用入口。
         */
        WORKER
    }
}