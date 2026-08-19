package com.zqzqq.bootkits.distributed.registry;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.core.communication.ServiceMetadata;
import com.zqzqq.bootkits.core.communication.ServiceRegistration;
import com.zqzqq.bootkits.core.communication.ServiceSubscription;
import com.zqzqq.bootkits.core.communication.ServiceChangeListener;
import com.zqzqq.bootkits.core.communication.ServiceDependencyCheckResult;
import com.zqzqq.bootkits.core.communication.event.ServiceEvent;
import com.zqzqq.bootkits.core.communication.exception.ServiceNotFoundException;
import com.zqzqq.bootkits.core.communication.VersionRange;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 分布式服务注册中心（宿主侧门面）。
 * <p>
 * 包装本地的 {@link PluginServiceRegistry}，提供「本地优先、远端兜底」的服务发现：
 * <ul>
 *   <li>注册类操作：全部委托本地注册中心；</li>
 *   <li>查询类操作：先查本地，本地没有则查 Redis 服务目录，命中后返回远程代理。</li>
 * </ul>
 * <p>
 * 对调用方完全透明——仍使用 {@code getService(pluginId, iface)} 这一统一 API，
 * 本地能解析就走本地，本地没有的异步插件服务自动路由到远端执行节点。
 */
public class DistributedPluginServiceRegistry implements PluginServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DistributedPluginServiceRegistry.class);

    private final PluginServiceRegistry local;
    private final ServiceDirectory directory;
    private final RemoteServiceProxyFactory remoteProxyFactory;

    public DistributedPluginServiceRegistry(PluginServiceRegistry local,
                                            ServiceDirectory directory,
                                            RemoteServiceProxyFactory remoteProxyFactory) {
        this.local = local;
        this.directory = directory;
        this.remoteProxyFactory = remoteProxyFactory;
    }

    // ==================== 注册类：委托本地 ====================

    @Override
    public ServiceDescriptor registerService(String pluginId,
                                             Class<?> serviceInterface,
                                             Object serviceInstance,
                                             ServiceMetadata metadata) {
        return local.registerService(pluginId, serviceInterface, serviceInstance, metadata);
    }

    @Override
    public void registerServices(String pluginId, List<ServiceRegistration> registrations) {
        local.registerServices(pluginId, registrations);
    }

    @Override
    public void unregisterAllServices(String pluginId) {
        local.unregisterAllServices(pluginId);
    }

    @Override
    public void unregisterService(String pluginId, Class<?> serviceInterface) {
        local.unregisterService(pluginId, serviceInterface);
    }

    @Override
    public void registerServiceDependency(String consumerPluginId,
                                          Class<?> requiredService,
                                          String versionRange,
                                          boolean optional) {
        local.registerServiceDependency(consumerPluginId, requiredService, versionRange, optional);
    }

    @Override
    public ServiceDependencyCheckResult checkDependencies(String pluginId) {
        return local.checkDependencies(pluginId);
    }

    // ==================== 查询类：本地优先，远端兜底 ====================

    @Override
    public <T> T getService(String pluginId, Class<T> serviceInterface) throws ServiceNotFoundException {
        try {
            T localService = local.getService(pluginId, serviceInterface);
            if (localService != null) {
                return localService;
            }
        } catch (ServiceNotFoundException ignored) {
            // 本地无该服务，尝试远端
        }

        // 尝试任意插件（未指定 pluginId）
        if (pluginId == null || pluginId.isEmpty()) {
            List<RemoteServiceRegistration> remote = directory.lookup(serviceInterface.getName());
            if (remote != null && !remote.isEmpty()) {
                return remoteProxyFactory.createProxy(remote.get(0).getPluginId(), serviceInterface);
            }
            throw new ServiceNotFoundException(serviceInterface.getName());
        }

        RemoteServiceRegistration registration = directory.lookup(serviceInterface.getName(), pluginId);
        if (registration != null) {
            log.debug("服务 {}@{} 由远端节点 {}:{} 提供，创建远程代理",
                    pluginId, serviceInterface.getName(), registration.getHost(), registration.getPort());
            return remoteProxyFactory.createProxy(pluginId, serviceInterface);
        }

        throw new ServiceNotFoundException(serviceInterface.getName());
    }

    @Override
    public <T> List<T> getServices(Class<T> serviceInterface) {
        List<T> result = new ArrayList<>();
        try {
            result.addAll(local.getServices(serviceInterface));
        } catch (Exception ignored) {
            // ignore
        }
        List<RemoteServiceRegistration> remote = directory.lookup(serviceInterface.getName());
        Set<String> seenPlugins = new LinkedHashSet<>();
        for (RemoteServiceRegistration registration : remote) {
            if (seenPlugins.add(registration.getPluginId())) {
                result.add(remoteProxyFactory.createProxy(registration.getPluginId(), serviceInterface));
            }
        }
        return result;
    }

    @Override
    public <T> List<T> getServicesByVersion(Class<T> serviceInterface, String versionRange) {
        List<T> result = new ArrayList<>();
        try {
            result.addAll(local.getServicesByVersion(serviceInterface, versionRange));
        } catch (Exception ignored) {
            // ignore
        }
        // 远端兜底：按版本范围过滤目录中注册的远端节点，命中的才建远程代理，
        // 与核心接口「按版本取服务」的语义保持一致。
        if (versionRange == null || versionRange.isEmpty()) {
            return result;
        }
        com.zqzqq.bootkits.core.communication.VersionRange range = VersionRange.parse(versionRange);
        Set<String> seenPlugins = new LinkedHashSet<>();
        List<RemoteServiceRegistration> remote =
                directory.lookup(serviceInterface.getName());
        for (RemoteServiceRegistration registration : remote) {
            if (range.isCompatible(registration.getVersion())
                    && seenPlugins.add(registration.getPluginId())) {
                result.add(remoteProxyFactory.createProxy(registration.getPluginId(), serviceInterface));
            }
        }
        return result;
    }

    @Override
    public ServiceSubscription subscribe(Class<?> serviceInterface, ServiceChangeListener listener) {
        return local.subscribe(serviceInterface, listener);
    }

    @Override
    public void publishEvent(ServiceEvent event) {
        local.publishEvent(event);
    }

    @Override
    public Set<Class<?>> getRegisteredInterfaces() {
        Set<Class<?>> result = new LinkedHashSet<>(local.getRegisteredInterfaces());
        for (String serviceName : directory.allServiceInterfaces()) {
            try {
                result.add(Class.forName(serviceName));
            } catch (ClassNotFoundException ignored) {
                // 远端接口可能不在本地 classpath，跳过
            }
        }
        return result;
    }

    @Override
    public Set<Class<?>> getServicesByPlugin(String pluginId) {
        return local.getServicesByPlugin(pluginId);
    }

    @Override
    public boolean isRegistered(String pluginId, Class<?> serviceInterface) {
        if (local.isRegistered(pluginId, serviceInterface)) {
            return true;
        }
        return directory.lookup(serviceInterface.getName(), pluginId) != null;
    }

    @Override
    public ServiceDescriptor getServiceDescriptor(String pluginId, Class<?> serviceInterface) {
        return local.getServiceDescriptor(pluginId, serviceInterface);
    }

    @Override
    public Set<String> getRegisteredPlugins() {
        Set<String> result = new LinkedHashSet<>(local.getRegisteredPlugins());
        for (String serviceName : directory.allServiceInterfaces()) {
            for (RemoteServiceRegistration registration : directory.lookup(serviceName)) {
                result.add(registration.getPluginId());
            }
        }
        return result;
    }

    @Override
    public com.zqzqq.bootkits.core.communication.RegistryStatistics getStatistics() {
        return local.getStatistics();
    }
}