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


package com.zqzqq.bootkits.distributed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 分布式插件模块配置。
 * <p>
 * 前缀：{@code plugin.distributed}。
 */
@ConfigurationProperties(prefix = "plugin.distributed")
public class DistributedPluginProperties {

    /**
     * 是否启用分布式插件模块。默认关闭时整个分布式模块不装载。
     */
    private boolean enabled = false;

    /**
     * 服务目录后端类型：{@code redis}（默认，沿用既有 Redis 目录）或 {@code nacos}
     * （复用 Spring Cloud Alibaba 的 Nacos 注册中心，使插件能力发现与微服务发现共用同一真相源）。
     */
    private String registryType = "redis";

    /**
     * Nacos 服务目录配置（{@code registry-type=nacos} 时生效）。
     */
    private final Nacos nacos = new Nacos();

    /**
     * 是否启用 Feign → 插件服务桥接：把「既是 @FeignClient、又被注册为插件能力」的接口，
     * 自动改为走 {@code PluginServiceRegistry}（本地优先 / 远端 gRPC），调用方零改动；
     * 非插件服务自动回落到原 Feign HTTP。默认开启。
     */
    private boolean feignBridgeEnabled = true;

    /**
     * 是否启用「宿主级 {@code @PluginService} / {@code @BrickService} 自动注册」：
     * 在宿主（主应用）Spring 上下文就绪后，自动扫描并注册标注了服务注解的 {@code @Service}
     * bean，使其可作为分布式能力被其它容器（经 LOCATOR/gRPC）跨容器调用。
     * <p>
     * 面向「分离容器」拓扑——提供方微服务无需改造为 brick 插件、也无需引入插件打包流程，
     * 只需给业务实现加一个注解即可发布能力。默认开启。
     */
    private boolean hostServiceAutoRegister = true;

    /**
     * 宿主级自动注册使用的 pluginId。多个容器默认共用 {@code "host"}，目录以
     * {@code pluginId@nodeId} 区分不同节点；如需显式指定（如按业务域命名）可在此覆盖。
     */
    private String hostServicePluginId = "host";

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
     * WORKER 节点的该标记会随注册写入 Redis 目录；HOST 按<b>每个节点</b>声明选择传输方式，
     * 因此支持明文 / TLS 混合部署（滚动升级灰度 TLS）：只要所有 TLS 节点使用同一 CA，
     * HOST 即可同时连通明文节点与 TLS 节点。启用时，执行节点需要 {@code tls-cert-chain} /
     * {@code tls-private-key}，宿主需要 {@code tls-ca-cert}（用于校验收到的 server 证书）。
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

    /**
     * Redis 服务目录本地兜底缓存有效期（毫秒）。
     * <p>
     * Redis 故障时，装兜底使用不超过该生效期的 last-known-good 快照，保证高可用；
     * Redis 恢复后自动切回实时目录。设为 0 可关闭兜底缓存（严格实时语义）。默认 30 秒。
     */
    private long registryCacheTtlMillis = 30_000L;

    /**
     * 按方法维度覆盖调用超时（毫秒）。
     * <p>
     * Map 的 key 优先用「接口全限定名.方法名」，其次「方法名」；命中则覆盖全局
     * {@link #callTimeoutMillis}。用于对个别重/慢方法单独放宽或收紧超时，避免一刀切。
     * 例：{@code plugin.distributed.method-timeouts.com.example.UserService.getUserName=8000}
     */
    private Map<String, Long> methodTimeouts = new HashMap<>();

    /**
     * 远端传输层不可达（UNAVAILABLE）时的<b>有限次自动重试</b>次数（默认 0，不重试）。
     * <p>
     * 仅对「所有副本在当前轮均不可达」的场景生效：在抛错前，将整组候选副本重试
     * 至多该次数（间隔一个 base 退避节拍），用于吸收瞬时网络抖动。注意：自动重试
     * 对幂等方法安全，对非幂等(如写操作)可能造成重复执行，请按业务谨慎开启。
     */
    private int maxFailoverRetries = 0;

    /**
     * WORKER 节点 gRPC 服务优雅停机时长（秒）：关停时等待在途调用排空后再强制关闭，
     * 避免升级/关停期间丢调用。默认 10 秒。
     */
    private long gracefulShutdownSeconds = 10L;

    /**
     * gRPC 客户端 keepAlive ping 间隔（毫秒，默认 60000）。跨 NAT/公网/容器网络时长连接
     * 可能被中间设备静默断开，开启 keepAlive 让 gRPC 在空闲期主动探测、尽早发现死连接，
     * 避免首次真实调用吃一次完整超时。设为 <=0 关闭（保留旧版语义）。
     */
    private long keepAliveTimeMillis = 60_000L;

    /**
     * gRPC 客户端 keepAlive ping 等待 ACK 超时（毫秒，默认 20000）。仅 keepAliveTimeMillis>0 时生效。
     */
    private long keepAliveTimeoutMillis = 20_000L;

    /**
     * 客户端是否在<b>无活跃调用</b>时也发 keepAlive ping（默认 true）。
     * 保活长连接，避免反复重建；与服务端 {@code permitKeepAliveWithoutCalls} 配合生效。
     */
    private boolean keepAliveWithoutCalls = true;

    /**
     * 服务端允许客户端 keepAlive ping 的最小间隔（毫秒，默认 30000）。
     * 低于该值的客户端 ping 会被服务端拒绝并关闭连接，用于防御恶意/失控客户端的高频 ping。
     */
    private long permitKeepAliveTimeMillis = 30_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public Nacos getNacos() {
        return nacos;
    }

    public boolean isFeignBridgeEnabled() {
        return feignBridgeEnabled;
    }

    public void setFeignBridgeEnabled(boolean feignBridgeEnabled) {
        this.feignBridgeEnabled = feignBridgeEnabled;
    }

    public boolean isHostServiceAutoRegister() {
        return hostServiceAutoRegister;
    }

    public void setHostServiceAutoRegister(boolean hostServiceAutoRegister) {
        this.hostServiceAutoRegister = hostServiceAutoRegister;
    }

    public String getHostServicePluginId() {
        return hostServicePluginId;
    }

    public void setHostServicePluginId(String hostServicePluginId) {
        this.hostServicePluginId = hostServicePluginId;
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

    public long getRegistryCacheTtlMillis() {
        return registryCacheTtlMillis;
    }

    public void setRegistryCacheTtlMillis(long registryCacheTtlMillis) {
        this.registryCacheTtlMillis = registryCacheTtlMillis;
    }

    public Map<String, Long> getMethodTimeouts() {
        return methodTimeouts;
    }

    public void setMethodTimeouts(Map<String, Long> methodTimeouts) {
        this.methodTimeouts = methodTimeouts != null ? methodTimeouts : new HashMap<>();
    }

    public int getMaxFailoverRetries() {
        return maxFailoverRetries;
    }

    public void setMaxFailoverRetries(int maxFailoverRetries) {
        this.maxFailoverRetries = maxFailoverRetries;
    }

    public long getGracefulShutdownSeconds() {
        return gracefulShutdownSeconds;
    }

    public void setGracefulShutdownSeconds(long gracefulShutdownSeconds) {
        this.gracefulShutdownSeconds = gracefulShutdownSeconds;
    }

    public long getKeepAliveTimeMillis() {
        return keepAliveTimeMillis;
    }

    public void setKeepAliveTimeMillis(long keepAliveTimeMillis) {
        this.keepAliveTimeMillis = keepAliveTimeMillis;
    }

    public long getKeepAliveTimeoutMillis() {
        return keepAliveTimeoutMillis;
    }

    public void setKeepAliveTimeoutMillis(long keepAliveTimeoutMillis) {
        this.keepAliveTimeoutMillis = keepAliveTimeoutMillis;
    }

    public boolean isKeepAliveWithoutCalls() {
        return keepAliveWithoutCalls;
    }

    public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
        this.keepAliveWithoutCalls = keepAliveWithoutCalls;
    }

    public long getPermitKeepAliveTimeMillis() {
        return permitKeepAliveTimeMillis;
    }

    public void setPermitKeepAliveTimeMillis(long permitKeepAliveTimeMillis) {
        this.permitKeepAliveTimeMillis = permitKeepAliveTimeMillis;
    }

    /**
     * Nacos 服务目录配置。
     * <p>
     * 未显式配置 {@code server-addr} 时，自动复用 {@code spring.cloud.nacos.discovery.server-addr}
     * （即 Spring Cloud Alibaba 的 Nacos 地址），实现零额外配置接入。
     */
    public static class Nacos {
        /**
         * Nacos 服务器地址（IP:PORT，逗号分隔多地址）。
         * 留空时取 {@code spring.cloud.nacos.discovery.server-addr}。
         */
        private String serverAddr = "";

        /**
         * Nacos 命名空间 ID。留空时使用默认命名空间（public）。
         */
        private String namespace = "";

        /**
         * Nacos 分组。插件能力目录独立分组，避免污染业务服务列表。
         */
        private String group = "BRICK_BOOTKIT_DISTRIBUTED";

        private String username = "";
        private String password = "";
        private String accessKey = "";
        private String secretKey = "";

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
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