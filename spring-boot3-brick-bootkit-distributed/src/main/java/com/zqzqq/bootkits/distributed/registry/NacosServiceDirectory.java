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

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.distributed.metrics.DistributedMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Nacos 命名服务的分布式服务目录。
 * <p>
 * 与 {@link RedisServiceDirectory} 语义对齐，但把「服务接口 ↔ 执行节点」映射托管到
 * Nacos，从而使插件能力的发现与现有微服务（Spring Cloud Alibaba）的服务发现共用
 * <b>同一个 Nacos 注册中心</b>——这是「微服务以插件形态组合部署」时，让
 * {@code DistributedServiceLocator} 与 Feign/Ribbon/LoadBalancer 共用一个真相源的关键。
 * <p>
 * 映射方式：
 * <pre>
 *   Nacos serviceName : {prefix}:svc:{serviceInterface}   （独立 group，避免污染业务服务列表）
 *   Nacos instance    : ip=节点 host, port=gRPC 端口, ephemeral=true
 *   instance.metadata : reg=JSON(RemoteServiceRegistration), pluginId, nodeId, serviceInterface
 * </pre>
 * 利用 Nacos 临时实例的心跳自动过期机制，天然等价于 Redis 的 TTL 续期；实例掉线后
 * Nacos 自动剔除，宿主不会再路由到已崩溃节点。
 */
public class NacosServiceDirectory implements ServiceDirectory {

    private static final Logger log = LoggerFactory.getLogger(NacosServiceDirectory.class);

    /** Nacos 分组：插件能力目录独立分组，不混入业务服务的 DEFAULT_GROUP。 */
    public static final String DEFAULT_GROUP = "BRICK_BOOTKIT_DISTRIBUTED";

    private static final String SVC_KEY_SUFFIX = ":svc:";
    private static final String MD_REG = "reg";
    private static final String MD_PLUGIN_ID = "pluginId";
    private static final String MD_NODE_ID = "nodeId";
    private static final String MD_SERVICE_INTERFACE = "serviceInterface";

    private final NamingService namingService;
    private final String prefix;
    private final String group;
    private final long heartbeatTtlSeconds;
    private final DistributedMetrics metrics;
    private final ObjectMapper mapper;

    public NacosServiceDirectory(NamingService namingService,
                                 String prefix,
                                 String group,
                                 long heartbeatTtlSeconds) {
        this(namingService, prefix, group, heartbeatTtlSeconds, null, null);
    }

    public NacosServiceDirectory(NamingService namingService,
                                 String prefix,
                                 String group,
                                 long heartbeatTtlSeconds,
                                 DistributedMetrics metrics) {
        this(namingService, prefix, group, heartbeatTtlSeconds, metrics, null);
    }

    public NacosServiceDirectory(NamingService namingService,
                                 String prefix,
                                 String group,
                                 long heartbeatTtlSeconds,
                                 DistributedMetrics metrics,
                                 ObjectMapper mapper) {
        this.namingService = namingService;
        this.prefix = prefix;
        this.group = (group == null || group.isEmpty()) ? DEFAULT_GROUP : group;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
        this.metrics = metrics;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    private String serviceName(String serviceInterface) {
        return prefix + SVC_KEY_SUFFIX + serviceInterface;
    }

    @Override
    public void register(RemoteServiceRegistration registration) {
        try {
            namingService.registerInstance(serviceName(registration.getServiceInterface()),
                    group, toInstance(registration));
        } catch (Exception e) {
            log.warn("注册远端服务到 Nacos 失败: {}", registration, e);
        }
    }

    @Override
    public void registerAll(List<RemoteServiceRegistration> registrations) {
        for (RemoteServiceRegistration registration : registrations) {
            register(registration);
        }
    }

    @Override
    public void heartbeat(String serviceInterface, String pluginId, String nodeId) {
        // Nacos 临时实例由客户端自动发心跳保活；此处用一次幂等 registerInstance 兜底刷新
        // registeredAt（兼容目录侧的新鲜度过滤），即便心跳线程短暂异常也不致被误判下线。
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName(serviceInterface), group);
            for (Instance inst : instances) {
                Map<String, String> md = inst.getMetadata();
                if (pluginId.equals(md.get(MD_PLUGIN_ID)) && nodeId.equals(md.get(MD_NODE_ID))) {
                    namingService.registerInstance(serviceName(serviceInterface), group, inst);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("远端服务续期失败: {} {} {}", serviceInterface, pluginId, nodeId, e.getMessage());
        }
    }

    @Override
    public List<RemoteServiceRegistration> lookup(String serviceInterface) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName(serviceInterface), group);
            List<RemoteServiceRegistration> result = parse(instances);
            if (metrics != null) {
                metrics.recordRegistryLookupSuccess();
            }
            return result;
        } catch (Exception e) {
            log.warn("查询远端服务失败: serviceInterface={}, err={}", serviceInterface, e.getMessage());
            if (metrics != null) {
                metrics.recordRegistryFallbackUsed();
            }
            return new ArrayList<>();
        }
    }

    @Override
    public RemoteServiceRegistration lookup(String serviceInterface, String pluginId) {
        for (RemoteServiceRegistration registration : lookup(serviceInterface)) {
            if (registration.getPluginId().equals(pluginId)) {
                return registration;
            }
        }
        return null;
    }

    @Override
    public void unregister(String serviceInterface, String pluginId, String nodeId) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName(serviceInterface), group);
            for (Instance inst : instances) {
                Map<String, String> md = inst.getMetadata();
                if (pluginId.equals(md.get(MD_PLUGIN_ID)) && nodeId.equals(md.get(MD_NODE_ID))) {
                    namingService.deregisterInstance(serviceName(serviceInterface), group,
                            inst.getIp(), inst.getPort());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("注销远端服务失败: {} {} {}", serviceInterface, pluginId, nodeId, e.getMessage());
        }
    }

    @Override
    public void unregisterAllByNode(String nodeId) {
        try {
            int pageNo = 1;
            final int pageSize = 100;
            while (true) {
                ListView<String> view = namingService.getServicesOfServer(pageNo, pageSize, group);
                List<String> serviceNames = view.getData();
                if (serviceNames == null || serviceNames.isEmpty()) {
                    break;
                }
                for (String serviceName : serviceNames) {
                    if (!serviceName.startsWith(prefix + SVC_KEY_SUFFIX)) {
                        continue;
                    }
                    List<Instance> instances = namingService.getAllInstances(serviceName, group);
                    for (Instance inst : instances) {
                        if (nodeId.equals(inst.getMetadata().get(MD_NODE_ID))) {
                            namingService.deregisterInstance(serviceName, group,
                                    inst.getIp(), inst.getPort());
                        }
                    }
                }
                if (serviceNames.size() < pageSize) {
                    break;
                }
                pageNo++;
            }
        } catch (Exception e) {
            log.warn("按节点注销远端服务失败: nodeId={}", nodeId, e.getMessage());
        }
    }

    @Override
    public Set<String> allServiceInterfaces() {
        Set<String> interfaces = new HashSet<>();
        try {
            int pageNo = 1;
            final int pageSize = 100;
            String prefixKey = prefix + SVC_KEY_SUFFIX;
            while (true) {
                ListView<String> view = namingService.getServicesOfServer(pageNo, pageSize, group);
                List<String> serviceNames = view.getData();
                if (serviceNames == null || serviceNames.isEmpty()) {
                    break;
                }
                for (String serviceName : serviceNames) {
                    if (serviceName.startsWith(prefixKey)) {
                        interfaces.add(serviceName.substring(prefixKey.length()));
                    }
                }
                if (serviceNames.size() < pageSize) {
                    break;
                }
                pageNo++;
            }
        } catch (Exception e) {
            log.warn("扫描服务目录接口列表失败", e);
        }
        return interfaces;
    }

    private Instance toInstance(RemoteServiceRegistration registration) throws NacosException {
        Instance instance = new Instance();
        instance.setServiceName(serviceName(registration.getServiceInterface()));
        instance.setIp(registration.getHost());
        instance.setPort(registration.getPort());
        instance.setEphemeral(true);
        Map<String, String> md = new HashMap<>();
        try {
            md.put(MD_REG, mapper.writeValueAsString(registration));
        } catch (Exception e) {
            log.warn("序列化远端服务注册信息失败, 回退手写 JSON: {}", registration, e);
            md.put(MD_REG, registration.toRedisValue());
        }
        md.put(MD_PLUGIN_ID, registration.getPluginId());
        md.put(MD_NODE_ID, registration.getNodeId());
        md.put(MD_SERVICE_INTERFACE, registration.getServiceInterface());
        instance.setMetadata(md);
        return instance;
    }

    private List<RemoteServiceRegistration> parse(List<Instance> instances) {
        List<RemoteServiceRegistration> result = new ArrayList<>();
        if (instances == null) {
            return result;
        }
        long staleBefore = System.currentTimeMillis() - heartbeatTtlSeconds * 1000L;
        for (Instance instance : instances) {
            Map<String, String> md = instance.getMetadata();
            if (md == null) {
                continue;
            }
            String json = md.get(MD_REG);
            if (json == null) {
                continue;
            }
            RemoteServiceRegistration registration = parse(json);
            if (registration == null) {
                continue;
            }
            // 与 Redis 实现一致：依据 registeredAt 过滤「已掉线但未被 Nacos 及时剔除」的陈旧实例
            if (registration.getRegisteredAt() < staleBefore) {
                continue;
            }
            result.add(registration);
        }
        return result;
    }

    private RemoteServiceRegistration parse(String json) {
        try {
            return mapper.readValue(json, RemoteServiceRegistration.class);
        } catch (Exception e) {
            log.warn("解析远端服务注册信息失败: {}", json, e);
            return null;
        }
    }
}
