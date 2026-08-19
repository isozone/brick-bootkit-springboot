package com.zqzqq.bootkits.distributed.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 远端插件服务的注册信息。
 * <p>
 * 表示「某个插件提供的某个服务接口」当前位于「哪个执行节点」，
 * 以及该节点的 gRPC 地址。宿主据此类创建远程代理。
 */
public final class RemoteServiceRegistration {

    private final String pluginId;
    private final String serviceInterface;
    private final String version;
    private final String nodeId;
    private final String host;
    private final int port;
    private final long registeredAt;
    private final boolean tlsEnabled;

    @JsonCreator
    public RemoteServiceRegistration(@JsonProperty("pluginId") String pluginId,
                                     @JsonProperty("serviceInterface") String serviceInterface,
                                     @JsonProperty("version") String version,
                                     @JsonProperty("nodeId") String nodeId,
                                     @JsonProperty("host") String host,
                                     @JsonProperty("port") int port,
                                     @JsonProperty("registeredAt") long registeredAt,
                                     @JsonProperty("tlsEnabled") boolean tlsEnabled) {
        this.pluginId = pluginId;
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.registeredAt = registeredAt;
        this.tlsEnabled = tlsEnabled;
    }

    /**
     * 兼容构造（未声明 TLS 标记，默认明文）。沿用旧签名的调用方（如部分测试）
     * 不因新增字段而破坏。
     */
    public RemoteServiceRegistration(String pluginId, String serviceInterface, String version,
                                     String nodeId, String host, int port, long registeredAt) {
        this(pluginId, serviceInterface, version, nodeId, host, port, registeredAt, false);
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    /**
     * 该节点是否以 TLS 方式暴露 gRPC 服务。
     * <p>用于宿主在混合部署（部分节点 TLS、部分明文）时按节点选择传输方式，
     * 支撑滚动升级灰度 TLS。</p>
     */
    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    /**
     * 构造成 Redis Hash 表存储字段。
     * <p>
     * key = 服务接口名，field 采用复合键（pluginId + "@" + nodeId），
     * value = JSON 序列化字符串。
     */
    public String toRedisValue() {
        StringBuilder sb = new StringBuilder(216);
        sb.append("{\"pluginId\":\"").append(escape(pluginId))
            .append("\",\"serviceInterface\":\"").append(escape(serviceInterface))
            .append("\",\"version\":\"").append(escape(version))
            .append("\",\"nodeId\":\"").append(escape(nodeId))
            .append("\",\"host\":\"").append(escape(host))
            .append("\",\"port\":").append(port)
            .append(",\"registeredAt\":").append(registeredAt)
            .append(",\"tlsEnabled\":").append(tlsEnabled)
            .append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteServiceRegistration that)) {
            return false;
        }
        return port == that.port
                && Objects.equals(pluginId, that.pluginId)
                && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, nodeId, port);
    }

    @Override
    public String toString() {
        return "RemoteServiceRegistration{"
                + "pluginId='" + pluginId + '\''
                + ", serviceInterface='" + serviceInterface + '\''
                + ", version='" + version + '\''
                + ", nodeId='" + nodeId + '\''
                + ", host='" + host + '\''
                + ", port=" + port
                + ", tlsEnabled=" + tlsEnabled
                + '}';
    }
}