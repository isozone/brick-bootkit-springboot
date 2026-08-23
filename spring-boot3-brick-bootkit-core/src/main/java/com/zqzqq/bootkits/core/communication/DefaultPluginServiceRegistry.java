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


/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zqzqq.bootkits.core.communication.event.*;
import com.zqzqq.bootkits.core.communication.exception.*;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PluginServiceRegistry}.
 * <p>
 * Thread-safe implementation using concurrent collections.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class DefaultPluginServiceRegistry implements PluginServiceRegistry, CanaryRoutingManageable {

    private static final Logger log = LoggerFactory.getLogger(DefaultPluginServiceRegistry.class);

    // ==================== Core Storage ====================

    // pluginId → (interface → descriptor)
    private final Map<String, Map<Class<?>, ServiceDescriptor>> pluginServices = new ConcurrentHashMap<>();

    // interface → (pluginId → descriptor)
    private final Map<Class<?>, Map<String, ServiceDescriptor>> interfaceServices = new ConcurrentHashMap<>();

    // serviceId → descriptor (for fast lookup)
    private final Map<String, ServiceDescriptor> servicesById = new ConcurrentHashMap<>();

    // consumer plugin → list of required services
    private final Map<String, List<ServiceDependencyInfo>> serviceDependencies = new ConcurrentHashMap<>();

    // event listeners
    private final List<WeakReference<ServiceChangeListener>> listeners = new CopyOnWriteArrayList<>();

    // proxy cache: serviceId → proxy
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();

    // 注册顺序（用于灰度模式下自动分配基线/金丝雀权重）
    private final AtomicLong seqCounter = new AtomicLong(0);
    private final Map<String, Long> registeredSeqById = new ConcurrentHashMap<>();

    // 金丝雀/灰度路由策略解析器（默认不启用）
    private CanaryRoutingResolver canaryRoutingResolver = new NoOpCanaryRoutingResolver();

    // ==================== Service Registration ====================

    @Override
    public ServiceDescriptor registerService(
        String pluginId,
        Class<?> serviceInterface,
        Object serviceInstance,
        ServiceMetadata metadata
    ) throws ServiceRegistrationException {
        Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        Objects.requireNonNull(serviceInterface, "Service interface cannot be null");
        Objects.requireNonNull(serviceInstance, "Service instance cannot be null");

        // Check if already registered
        Map<Class<?>, ServiceDescriptor> pluginMap = pluginServices.computeIfAbsent(
            pluginId, k -> new ConcurrentHashMap<>()
        );

        synchronized (pluginMap) {
            if (pluginMap.containsKey(serviceInterface)) {
                throw new DuplicateServiceException(
                    "Service already registered: " + serviceInterface.getName() +
                    " for plugin: " + pluginId
                );
            }

            // Validate implementation
            if (!serviceInterface.isInstance(serviceInstance)) {
                throw new InvalidServiceException(
                    "Service instance does not implement interface: " +
                    serviceInterface.getName()
                );
            }

            // Create descriptor
            String version = metadata != null ? metadata.getVersion() : "1.0.0";
            String serviceId = buildServiceId(pluginId, serviceInterface, version);

            ServiceDescriptor descriptor = new DefaultServiceDescriptor(
                serviceId,
                pluginId,
                serviceInterface,
                serviceInstance.getClass(),
                serviceInstance,
                metadata != null ? metadata : ServiceMetadata.builder().build()
            );

            // Register
            pluginMap.put(serviceInterface, descriptor);
            interfaceServices.computeIfAbsent(
                serviceInterface, k -> new ConcurrentHashMap<>()
            ).put(pluginId, descriptor);
            servicesById.put(serviceId, descriptor);
            registeredSeqById.put(serviceId, seqCounter.incrementAndGet());

            // Publish event
            publishEvent(new ServiceRegisteredEvent(descriptor));

            return descriptor;
        }
    }

    @Override
    public void registerServices(String pluginId, List<ServiceRegistration> registrations) {
        for (ServiceRegistration reg : registrations) {
            registerService(
                pluginId,
                reg.getServiceInterface(),
                reg.getServiceInstance(),
                reg.getMetadata()
            );
        }
    }

    @Override
    public void unregisterAllServices(String pluginId) {
        Map<Class<?>, ServiceDescriptor> removed = pluginServices.remove(pluginId);
        if (removed != null) {
            removed.values().forEach(descriptor -> {
                // Remove from interface map
                Map<String, ServiceDescriptor> interfaceMap = interfaceServices.get(
                    descriptor.getServiceInterface()
                );
                if (interfaceMap != null) {
                    interfaceMap.remove(pluginId);
                    if (interfaceMap.isEmpty()) {
                        interfaceServices.remove(descriptor.getServiceInterface());
                    }
                }

                // Remove from ID map
                servicesById.remove(descriptor.getServiceId());
                registeredSeqById.remove(descriptor.getServiceId());

                // Clear proxy cache
                proxyCache.remove(descriptor.getServiceId());

                // Publish event
                publishEvent(new ServiceUnregisteredEvent(descriptor));
            });
        }

        // Remove dependencies
        serviceDependencies.remove(pluginId);
    }

    @Override
    public void unregisterService(String pluginId, Class<?> serviceInterface) {
        Map<Class<?>, ServiceDescriptor> pluginMap = pluginServices.get(pluginId);
        if (pluginMap == null) {
            return;
        }

        synchronized (pluginMap) {
            ServiceDescriptor descriptor = pluginMap.remove(serviceInterface);
            if (descriptor == null) {
                return;
            }

            // Remove from interface map
            Map<String, ServiceDescriptor> interfaceMap = interfaceServices.get(serviceInterface);
            if (interfaceMap != null) {
                interfaceMap.remove(pluginId);
                if (interfaceMap.isEmpty()) {
                    interfaceServices.remove(serviceInterface);
                }
            }

            // Remove from ID map
            servicesById.remove(descriptor.getServiceId());
            registeredSeqById.remove(descriptor.getServiceId());

            // Clear proxy cache
            proxyCache.remove(descriptor.getServiceId());

            // Publish event
            publishEvent(new ServiceUnregisteredEvent(descriptor));
        }
    }

    // ==================== Service Discovery ====================

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(String pluginId, Class<T> serviceInterface)
        throws ServiceNotFoundException {

        ServiceDescriptor descriptor = findBestService(pluginId, serviceInterface);
        if (descriptor == null) {
            throw new ServiceNotFoundException(
                "Service not found: " + serviceInterface.getName() +
                (pluginId != null ? " for plugin: " + pluginId : "")
            );
        }

        return createProxy(descriptor, serviceInterface);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getServices(Class<T> serviceInterface) {
        Map<String, ServiceDescriptor> descriptors = interfaceServices.get(serviceInterface);
        if (descriptors == null || descriptors.isEmpty()) {
            return Collections.emptyList();
        }

        return descriptors.values().stream()
            .sorted(Comparator.comparingInt(ServiceDescriptor::getPriority))
            .map(d -> createProxy(d, serviceInterface))
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getServicesByVersion(Class<T> serviceInterface, String versionRange) {
        Map<String, ServiceDescriptor> descriptors = interfaceServices.get(serviceInterface);
        if (descriptors == null || descriptors.isEmpty()) {
            return Collections.emptyList();
        }

        VersionRange range = VersionRange.parse(versionRange);

        return descriptors.values().stream()
            .filter(d -> range.isCompatible(d.getVersion()))
            .sorted(Comparator.comparingInt(ServiceDescriptor::getPriority))
            .map(d -> createProxy(d, serviceInterface))
            .collect(Collectors.toList());
    }

    // ==================== Dependency Management ====================

    @Override
    public void registerServiceDependency(
        String consumerPluginId,
        Class<?> requiredService,
        String versionRange,
        boolean optional
    ) {
        serviceDependencies.computeIfAbsent(consumerPluginId, k -> new CopyOnWriteArrayList<>())
            .add(new ServiceDependencyInfo(consumerPluginId, requiredService, versionRange, optional));
    }

    @Override
    public ServiceDependencyCheckResult checkDependencies(String pluginId) {
        List<ServiceDependencyInfo> deps = serviceDependencies.get(pluginId);
        if (deps == null || deps.isEmpty()) {
            return ServiceDependencyCheckResult.success(pluginId);
        }

        List<ServiceDependencyCheckResult.DependencyInfo> unsatisfied = new ArrayList<>();
        List<ServiceDependencyCheckResult.DependencyInfo> satisfied = new ArrayList<>();

        for (ServiceDependencyInfo dep : deps) {
            Map<String, ServiceDescriptor> providers = interfaceServices.get(dep.getRequiredService());
            boolean found = false;

            if (providers != null) {
                VersionRange range = VersionRange.parse(dep.getVersionRange());
                for (ServiceDescriptor desc : providers.values()) {
                    if (range.isCompatible(desc.getVersion())) {
                        found = true;
                        satisfied.add(createDependencyInfo(dep, desc.getVersion(), true));
                        break;
                    }
                }
            }

            if (!found) {
                if (dep.isOptional()) {
                    satisfied.add(createDependencyInfo(dep, null, true));
                } else {
                    unsatisfied.add(createDependencyInfo(dep, null, false));
                }
            }
        }

        if (unsatisfied.isEmpty()) {
            return ServiceDependencyCheckResult.success(pluginId);
        }

        return new ServiceDependencyCheckResult.Builder(pluginId)
            .satisfied(false)
            .unsatisfiedDependencies(unsatisfied)
            .satisfiedDependencies(satisfied)
            .build();
    }

    private ServiceDependencyCheckResult.DependencyInfo createDependencyInfo(
        ServiceDependencyInfo dep, String actualVersion, boolean satisfied) {
        return new ServiceDependencyCheckResult.DependencyInfo(
            dep.getRequiredService(),
            dep.getVersionRange(),
            actualVersion,
            satisfied,
            satisfied ? "Dependency satisfied" : "Dependency not found"
        );
    }

    // ==================== Subscription/Publishing ====================

    @Override
    public ServiceSubscription subscribe(
        Class<?> serviceInterface,
        ServiceChangeListener listener
    ) {
        listeners.add(new WeakReference<>(listener));
        return new ServiceSubscription() {
            @Override
            public Class<?> getServiceInterface() {
                return serviceInterface;
            }

            @Override
            public void unsubscribe() {
                listeners.removeIf(ref -> ref.get() == listener || ref.get() == null);
            }
        };
    }

    @Override
    public void publishEvent(ServiceEvent event) {
        listeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<ServiceChangeListener> ref : listeners) {
            ServiceChangeListener listener = ref.get();
            if (listener != null) {
                try {
                    listener.onServiceChange(event);
                } catch (Exception e) {
                    log.error("Error in service change listener", e);
                }
            }
        }
    }

    // ==================== Query ====================

    @Override
    public Set<Class<?>> getRegisteredInterfaces() {
        return Collections.unmodifiableSet(interfaceServices.keySet());
    }

    @Override
    public Set<Class<?>> getServicesByPlugin(String pluginId) {
        Map<Class<?>, ServiceDescriptor> pluginMap = pluginServices.get(pluginId);
        if (pluginMap == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(pluginMap.keySet());
    }

    @Override
    public boolean isRegistered(String pluginId, Class<?> serviceInterface) {
        Map<Class<?>, ServiceDescriptor> pluginMap = pluginServices.get(pluginId);
        return pluginMap != null && pluginMap.containsKey(serviceInterface);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor(String pluginId, Class<?> serviceInterface) {
        Map<Class<?>, ServiceDescriptor> pluginMap = pluginServices.get(pluginId);
        if (pluginMap == null) {
            return null;
        }
        return pluginMap.get(serviceInterface);
    }

    @Override
    public Set<String> getRegisteredPlugins() {
        return Collections.unmodifiableSet(pluginServices.keySet());
    }

    @Override
    public RegistryStatistics getStatistics() {
        return new RegistryStatistics(
            servicesById.size(),
            pluginServices.size(),
            interfaceServices.size(),
            serviceDependencies.size()
        );
    }

    // ==================== Helper Methods ====================

    private ServiceDescriptor findBestService(String pluginId, Class<?> serviceInterface) {
        Map<String, ServiceDescriptor> providers = interfaceServices.get(serviceInterface);
        if (providers == null || providers.isEmpty()) {
            return null;
        }

        // If specific plugin requested
        if (pluginId != null && !pluginId.isEmpty()) {
            return providers.get(pluginId);
        }

        // 多实现场景：显式 WEIGHTED 或灰度/金丝雀模式下，按权重分流（调用级）
        boolean canary = canaryRoutingResolver.isCanaryEnabled();
        boolean anyWeighted = providers.values().stream()
                .anyMatch(d -> d.getMetadata().getLoadBalancing() == ServiceMetadata.LoadBalancingStrategy.WEIGHTED);
        if (providers.size() > 1 && (anyWeighted || canary)) {
            return weightedPick(providers.values(), canary && !anyWeighted);
        }

        // Find highest priority service
        return providers.values().stream()
            .max(Comparator.comparingInt(ServiceDescriptor::getPriority))
            .orElse(null);
    }

    /**
     * 设置金丝雀/灰度路由策略解析器（由集成层注入，根据当前发布模式决定是否按权重分流）
     */
    public void setCanaryRoutingResolver(CanaryRoutingResolver resolver) {
        if (resolver != null) {
            this.canaryRoutingResolver = resolver;
        }
    }

    @Override
    public void setServiceWeight(String pluginId, Class<?> serviceInterface, int weight) {
        Map<String, ServiceDescriptor> providers = interfaceServices.get(serviceInterface);
        if (providers == null) {
            return;
        }
        ServiceDescriptor descriptor = providers.get(pluginId);
        if (descriptor == null) {
            return;
        }
        descriptor.getMetadata().setWeight(Math.max(0, weight));
        if (weight > 0) {
            descriptor.getMetadata().setLoadBalancing(ServiceMetadata.LoadBalancingStrategy.WEIGHTED);
        }
    }

    @Override
    public List<ServiceRoutingGroup> describeRouting() {
        List<ServiceRoutingGroup> groups = new ArrayList<>();
        boolean canary = canaryRoutingResolver.isCanaryEnabled();
        for (Map.Entry<Class<?>, Map<String, ServiceDescriptor>> entry : interfaceServices.entrySet()) {
            Map<String, ServiceDescriptor> providers = entry.getValue();
            if (providers == null || providers.size() < 2) {
                continue;
            }
            ServiceRoutingGroup group = new ServiceRoutingGroup();
            group.setInterfaceClass(entry.getKey().getName());
            group.setInterfaceName(entry.getKey().getSimpleName());
            List<ServiceRoutingProvider> providerViews = new ArrayList<>();
            for (ServiceDescriptor d : providers.values()) {
                ServiceRoutingProvider p = new ServiceRoutingProvider();
                p.setPluginId(d.getPluginId());
                p.setWeight(d.getMetadata().getWeight());
                p.setLoadBalancing(d.getMetadata().getLoadBalancing().name());
                p.setPriority(d.getPriority());
                p.setCanaryEligible(d.getMetadata().getLoadBalancing() == ServiceMetadata.LoadBalancingStrategy.WEIGHTED || canary);
                providerViews.add(p);
            }
            group.setProviders(providerViews);
            groups.add(group);
        }
        return groups;
    }

    /**
     * 按权重随机选择服务实现，用于灰度/金丝雀流量按比例分流。
     * 权重 <= 0 视为 1，避免零权重导致永远选不中。
     *
     * @param autoCanary 是否为灰度模式自动分流（无显式 WEIGHTED 时按注册顺序分配基线/金丝雀权重）
     */
    private ServiceDescriptor weightedPick(Collection<ServiceDescriptor> providers, boolean autoCanary) {
        List<ServiceDescriptor> ordered = new ArrayList<>(providers);
        Map<ServiceDescriptor, Integer> effective = new HashMap<>();
        if (autoCanary) {
            ServiceDescriptor baseline = ordered.stream()
                    .min(Comparator.comparingLong(d -> registeredSeqById.getOrDefault(d.getServiceId(), 0L)))
                    .orElse(ordered.get(0));
            for (ServiceDescriptor d : ordered) {
                int w = canaryRoutingResolver.resolveWeight(d);
                if (w <= 0) {
                    w = d == baseline ? canaryRoutingResolver.baselineWeight()
                            : canaryRoutingResolver.canaryWeight();
                }
                effective.put(d, Math.max(1, w));
            }
        } else {
            for (ServiceDescriptor d : ordered) {
                effective.put(d, Math.max(1, d.getMetadata().getWeight()));
            }
        }
        int total = 0;
        for (int w : effective.values()) {
            total += w;
        }
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (ServiceDescriptor d : ordered) {
            acc += effective.get(d);
            if (r < acc) {
                return d;
            }
        }
        return ordered.get(ordered.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private <T> T createProxy(ServiceDescriptor descriptor, Class<T> serviceInterface) {
        String cacheKey = descriptor.getServiceId();

        // Check cache
        Object cached = proxyCache.get(cacheKey);
        if (cached != null) {
            return (T) cached;
        }

        // Create proxy
        Object proxy = ServiceProxyFactory.createProxy(
            descriptor.getServiceInstance(),
            serviceInterface,
            descriptor.getServiceInstance().getClass().getClassLoader()
        );

        // Cache if singleton
        if (descriptor.getMetadata().isSingleton()) {
            proxyCache.put(cacheKey, proxy);
        }

        return (T) proxy;
    }

    private String buildServiceId(String pluginId, Class<?> interfaceClass, String version) {
        return pluginId + ":" + interfaceClass.getName() + ":" + version;
    }

    // ==================== Inner Classes ====================

    /**
     * Default service descriptor implementation.
     */
    private static class DefaultServiceDescriptor implements ServiceDescriptor {
        private final String serviceId;
        private final String pluginId;
        private final Class<?> serviceInterface;
        private final Class<?> serviceImplementation;
        private final Object serviceInstance;
        private final ServiceMetadata metadata;
        private volatile ServiceState state = ServiceState.REGISTERED;
        private volatile long registeredAt = System.currentTimeMillis();
        private volatile long lastCalledAt = 0;
        private volatile long callCount = 0;
        private volatile boolean healthy = true;

        public DefaultServiceDescriptor(
            String serviceId,
            String pluginId,
            Class<?> serviceInterface,
            Class<?> serviceImplementation,
            Object serviceInstance,
            ServiceMetadata metadata
        ) {
            this.serviceId = serviceId;
            this.pluginId = pluginId;
            this.serviceInterface = serviceInterface;
            this.serviceImplementation = serviceImplementation;
            this.serviceInstance = serviceInstance;
            this.metadata = metadata;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public Class<?> getServiceInterface() {
            return serviceInterface;
        }

        @Override
        public Class<?> getServiceImplementation() {
            return serviceImplementation;
        }

        @Override
        public String getPluginId() {
            return pluginId;
        }

        @Override
        public String getVersion() {
            return metadata.getVersion();
        }

        @Override
        public Object getServiceInstance() {
            return serviceInstance;
        }

        @Override
        public ServiceMetadata getMetadata() {
            return metadata;
        }

        @Override
        public ServiceState getState() {
            return state;
        }

        @Override
        public long getRegisteredAt() {
            return registeredAt;
        }

        @Override
        public long getLastCalledAt() {
            return lastCalledAt;
        }

        @Override
        public long getCallCount() {
            return callCount;
        }

        @Override
        public int getPriority() {
            return metadata.getPriority();
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        @Override
        public void recordInvocation(boolean success, long durationMs) {
            this.lastCalledAt = System.currentTimeMillis();
            this.callCount++;
            if (!success) {
                this.healthy = false;
            }
        }

        @Override
        public void setState(ServiceState newState) {
            this.state = newState;
        }

        @Override
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
    }

    /**
     * Service dependency information.
     */
    private static class ServiceDependencyInfo {
        private final String consumerPluginId;
        private final Class<?> requiredService;
        private final String versionRange;
        private final boolean optional;

        public ServiceDependencyInfo(
            String consumerPluginId,
            Class<?> requiredService,
            String versionRange,
            boolean optional
        ) {
            this.consumerPluginId = consumerPluginId;
            this.requiredService = requiredService;
            this.versionRange = versionRange;
            this.optional = optional;
        }

        public String getConsumerPluginId() {
            return consumerPluginId;
        }

        public Class<?> getRequiredService() {
            return requiredService;
        }

        public String getVersionRange() {
            return versionRange;
        }

        public boolean isOptional() {
            return optional;
        }
    }
}
