package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.RegistryStatistics;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 插件服务注册中心 Web 服务。
 * 基于主框架注册的 {@link PluginServiceRegistry} Bean，提供注册中心可视化所需数据。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class RegistryWebService {

    private final ObjectProvider<PluginServiceRegistry> registryProvider;

    public RegistryWebService(ObjectProvider<PluginServiceRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    private PluginServiceRegistry getRegistry() {
        PluginServiceRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            throw new PluginException("插件服务注册中心未启用");
        }
        return registry;
    }

    /**
     * 获取注册中心统计信息
     */
    public RegistryStatistics getStatistics() {
        return getRegistry().getStatistics();
    }

    /**
     * 获取所有注册的服务（按插件分组）
     */
    public List<PluginServiceGroup> getServicesGroupedByPlugin() {
        PluginServiceRegistry registry = getRegistry();
        Map<String, List<ServiceDescriptor>> grouped = new TreeMap<>();
        for (String pluginId : registry.getRegisteredPlugins()) {
            Set<Class<?>> interfaces = registry.getServicesByPlugin(pluginId);
            if (interfaces == null || interfaces.isEmpty()) {
                continue;
            }
            List<ServiceDescriptor> descriptors = new ArrayList<>();
            for (Class<?> iface : interfaces) {
                ServiceDescriptor descriptor = registry.getServiceDescriptor(pluginId, iface);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                }
            }
            descriptors.sort(Comparator.comparing(d -> d.getServiceInterface().getName()));
            grouped.put(pluginId, descriptors);
        }
        List<PluginServiceGroup> result = new ArrayList<>();
        grouped.forEach((pluginId, services) -> result.add(new PluginServiceGroup(pluginId, services)));
        return result;
    }

    /**
     * 获取单个插件的服务列表
     */
    public List<ServiceDescriptor> getServicesByPlugin(String pluginId) {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("插件 ID 不能为空");
        }
        PluginServiceRegistry registry = getRegistry();
        Set<Class<?>> interfaces = registry.getServicesByPlugin(pluginId);
        if (interfaces == null || interfaces.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceDescriptor> descriptors = new ArrayList<>();
        for (Class<?> iface : interfaces) {
            ServiceDescriptor descriptor = registry.getServiceDescriptor(pluginId, iface);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
        descriptors.sort(Comparator.comparing(d -> d.getServiceInterface().getName()));
        return descriptors;
    }

    /**
     * 获取所有注册的插件 ID
     */
    public Set<String> getRegisteredPlugins() {
        return getRegistry().getRegisteredPlugins();
    }

    /**
     * 插件服务分组
     */
    public static class PluginServiceGroup {
        private final String pluginId;
        private final List<ServiceDescriptor> services;

        public PluginServiceGroup(String pluginId, List<ServiceDescriptor> services) {
            this.pluginId = pluginId;
            this.services = services;
        }

        public String getPluginId() {
            return pluginId;
        }

        public List<ServiceDescriptor> getServices() {
            return services;
        }
    }
}
