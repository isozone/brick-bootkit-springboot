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


package com.zqzqq.bootkits.distributed.registry;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务注册调度器（执行节点侧）。
 * <p>
 * 周期性扫描本地 {@link PluginServiceRegistry} 中所有插件提供的服务接口，
 * 把它们作为「本节点可远程提供服务」注册到 Redis 服务目录，并定时续期。
 * 这样其他节点（宿主）即可通过目录发现并远程调用本节点承载的插件服务。
 */
public class ServiceRegistrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistrationScheduler.class);

    private final PluginServiceRegistry localRegistry;
    private final ServiceDirectory directory;
    private final String nodeId;
    private final String host;
    private final int port;
    private final boolean tlsEnabled;
    private final long intervalSeconds;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DistributedServiceRegistration");
                t.setDaemon(true);
                return t;
            });

    /**
     * 记录本节点上一轮已注册的服务：复合键 serviceInterface@pluginId@nodeId。
     * 用于 diff：仅注销「上一轮存在、本轮已消失」的服务，避免每周期对整片目录
     * 做全量 SCAN + 删后再建，也避免由此带来的短暂缺失窗口。
     * <p>注意必须用「接口+插件+节点」三元复合键：同一插件可提供多个接口，各自是
     * Redis 中不同的 key（svc:IfaceA / svc:IfaceB），字段虽都是 pluginId@nodeId，
     * 但对应不同服务，若只按 pluginId@nodeId 记录会导致卸载时漏删。
     */
    private final java.util.Set<String> previouslyRegistered = new java.util.HashSet<>();

    public ServiceRegistrationScheduler(PluginServiceRegistry localRegistry,
                                        ServiceDirectory directory,
                                        String nodeId,
                                        String host,
                                        int port,
                                        boolean tlsEnabled,
                                        long intervalSeconds) {
        this.localRegistry = localRegistry;
        this.directory = directory;
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.tlsEnabled = tlsEnabled;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * 兼容构造：默认明文（不声明 TLS 标记）。
     */
    public ServiceRegistrationScheduler(PluginServiceRegistry localRegistry,
                                        ServiceDirectory directory,
                                        String nodeId,
                                        String host,
                                        int port,
                                        long intervalSeconds) {
        this(localRegistry, directory, nodeId, host, port, false, intervalSeconds);
    }

    /**
     * 启动定时同步。立即执行一次，之后按固定间隔续期。
     */
    public synchronized void start() {
        executor.scheduleWithFixedDelay(this::syncAllServices,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        syncAllServices();
    }

    /**
     * 同步全部本地插件服务到 Redis 目录。
     * <p>
     * 每周期对当前仍在服务的字段做<b>就地 upsert</b>（HSET 覆盖，同时刷新 {@code registeredAt}
     * 以满足目录的新鲜度过滤），仅对「上一轮注册过、本轮已消失」的服务做定点注销。
     * 相比旧实现「先 unregisterAllByNode(全量SCAN) 再 registerAll」，避免：
     * <ul>
     *   <li>每次心跳都对整片前缀做 SCAN + 重建，Redis 写放大；</li>
     *   <li>删后再建的短暂窗口内，宿主查询会暂时看不到本节点服务。</li>
     * </ul>
     */
    public synchronized void syncAllServices() {
        try {
            List<RemoteServiceRegistration> regs = collectLocalServices();
            // 计算本轮服务复合键 serviceInterface@pluginId@nodeId，用于 diff
            java.util.Set<String> current = new java.util.HashSet<>();
            for (RemoteServiceRegistration reg : regs) {
                directory.register(reg); // HSET 就地覆盖，幂等且刷新 registeredAt
                current.add(compositeKey(reg));
            }
            // 注销上一轮有、本轮已消失的服务（定点删除，避免全量 SCAN）
            for (String key : previouslyRegistered) {
                if (!current.contains(key)) {
                    directory.unregister(parseServiceInterface(key),
                            parsePluginId(key), parseNodeId(key));
                }
            }
            previouslyRegistered.clear();
            previouslyRegistered.addAll(current);
            log.debug("已同步 {} 个远端服务注册", regs.size());
        } catch (Exception e) {
            log.warn("同步本地插件服务到 Redis 目录失败", e);
        }
    }

    /**
     * 立即触发一次本地服务同步（供「本地插件注册/注销时主动推送目录变更」，把远端
     * 发现延迟从「最多一个心跳周期(默认10s)」压缩到「亚秒级」，近似注册中心 watch）。
     * 幂等：diff 式同步，频繁调用仅做就地 upsert + 定点注销，开销小。
     */
    public synchronized void resyncNow() {
        syncAllServices();
    }

    private static String compositeKey(RemoteServiceRegistration reg) {
        return reg.getServiceInterface() + "@" + reg.getPluginId() + "@" + reg.getNodeId();
    }

    private static String parseServiceInterface(String key) {
        return key.substring(0, key.indexOf('@'));
    }

    private static String parsePluginId(String key) {
        String rest = key.substring(key.indexOf('@') + 1);
        return rest.substring(0, rest.indexOf('@'));
    }

    private static String parseNodeId(String key) {
        return key.substring(key.lastIndexOf('@') + 1);
    }

    private List<RemoteServiceRegistration> collectLocalServices() {
        List<RemoteServiceRegistration> registrations = new ArrayList<>();
        Set<String> plugins = localRegistry.getRegisteredPlugins();
        if (plugins == null) {
            return registrations;
        }
        long now = System.currentTimeMillis();
        for (String pluginId : plugins) {
            try {
                Set<Class<?>> interfaces = localRegistry.getServicesByPlugin(pluginId);
                if (interfaces == null) {
                    continue;
                }
                for (Class<?> iface : interfaces) {
                    ServiceDescriptor descriptor = localRegistry.getServiceDescriptor(pluginId, iface);
                    String version = descriptor != null
                            ? (descriptor.getVersion() != null ? descriptor.getVersion() : "1.0.0")
                            : "1.0.0";
                    registrations.add(new RemoteServiceRegistration(
                            pluginId,
                            iface.getName(),
                            version,
                            nodeId,
                            host,
                            port,
                            now,
                            tlsEnabled));
                }
            } catch (Exception e) {
                log.warn("收集插件 {} 的服务注册信息失败", pluginId, e);
            }
        }
        return registrations;
    }

    public synchronized void shutdown() {
        executor.shutdownNow();
        try {
            directory.unregisterAllByNode(nodeId);
        } catch (Exception e) {
            log.warn("注销本节点远端服务失败: nodeId={}", nodeId, e.getMessage());
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * 解析本机局域网 IPv4 地址。
     * <p>
     * 注意：不能返回主机名（getHostName），因为在同一集群内其他节点很可能解析不了
     * 本机主机名（容器、多网卡、非 FQDN 场景尤其如此），会导致宿主无法连接执行节点。
     * 优先返回非回环、非 site-local 的 IPv4 地址。
     */
    public static String resolveHost() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            if (local instanceof java.net.Inet4Address && !local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
            // ListNetworkInterfaces 兜底：挑选一个可达的非回环 IPv4
            java.util.Enumeration<java.net.NetworkInterface> nis =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                java.util.Enumeration<InetAddress> addrs = nis.nextElement().getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return local.getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}