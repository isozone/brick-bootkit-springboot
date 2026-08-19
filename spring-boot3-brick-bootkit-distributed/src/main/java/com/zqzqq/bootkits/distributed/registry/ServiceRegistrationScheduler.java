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
     */
    public synchronized void syncAllServices() {
        try {
            List<RemoteServiceRegistration> regs = collectLocalServices();
            if (regs.isEmpty()) {
                directory.unregisterAllByNode(nodeId);
                return;
            }
            // 先注销本节点旧记录，再注册当前快照，保证目录与本地状态一致
            directory.unregisterAllByNode(nodeId);
            directory.registerAll(regs);
            log.debug("已同步 {} 个远端服务注册", regs.size());
        } catch (Exception e) {
            log.warn("同步本地插件服务到 Redis 目录失败", e);
        }
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