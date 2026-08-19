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
     * 本节点对外暴露的 gRPC 监听/注册地址（WORKER 角色使用）。
     * <p>
     * 默认留空时由系统自动解析第一个非回环 IPv4。多网卡场景（容器、单机多网卡、
     * VPN + 内网等）建议显式指定业务网段的 IP，避免选到错误网卡导致宿主无法连通。
     * 该值既是 gRPC 服务端绑定的监听 IP，也是写入 Redis 注册目录的对外可达地址。
     */
    private String host = "";

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

    /**
     * 是否启用 gRPC TLS 加密传输。
     * <p>
     * 必须<b>所有节点一致</b>：宿主与执行节点要么都启用、要么都明文。
     * 启用时，执行节点需要 {@code tls-cert-chain} / {@code tls-private-key}，
     * 宿主需要 {@code tls-ca-cert}（用于校验收到的 server 证书）。
     * 生产跨网段/公网传输务必开启。
     */
    private boolean tlsEnabled = false;

    /**
     * TLS 服务端证书链文件路径（PEM，含叶子证书与中间证书）。
     * WORKER 执行节点启用 TLS 时必填。
     */
    private String tlsCertChainPath = "";

    /**
     * TLS 服务端私钥文件路径（PEM，PKCS#8 格式）。
     * WORKER 执行节点启用 TLS 时必填。
     */
    private String tlsPrivateKeyPath = "";

    /**
     * TLS 客户端信任的 CA 证书文件路径（PEM）。
     * HOST 宿主启用 TLS 时必填，用于校验执行节点的 server 证书。
     */
    private String tlsCaCertPath = "";

    /**
     * gRPC 鉴权 token。所有节点配置为<b>相同值</b>时启用鉴权；
     * 为空字符串则表示不启用鉴权。执行节点会拒绝携带错误/缺失 token 的调用，
     * 宿主会在每次调用的 gRPC 头（{@code authorization: Bearer <token>}）中附带。
     * 建议与 TLS 搭配使用，避免 token 明文传输。
     */
    private String authToken = "";

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getTlsCertChainPath() {
        return tlsCertChainPath;
    }

    public void setTlsCertChainPath(String tlsCertChainPath) {
        this.tlsCertChainPath = tlsCertChainPath;
    }

    public String getTlsPrivateKeyPath() {
        return tlsPrivateKeyPath;
    }

    public void setTlsPrivateKeyPath(String tlsPrivateKeyPath) {
        this.tlsPrivateKeyPath = tlsPrivateKeyPath;
    }

    public String getTlsCaCertPath() {
        return tlsCaCertPath;
    }

    public void setTlsCaCertPath(String tlsCaCertPath) {
        this.tlsCaCertPath = tlsCaCertPath;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
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