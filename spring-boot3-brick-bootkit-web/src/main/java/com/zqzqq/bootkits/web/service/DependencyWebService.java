package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.common.DependencyPlugin;
import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.dependency.PluginCompatibilityResult;
import com.zqzqq.bootkits.core.dependency.PluginDependencyManager;
import com.zqzqq.bootkits.core.dependency.PluginDependencyResolution;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 插件依赖分析 Web 服务。
 * 基于主框架注册的 {@link PluginDependencyManager} 与真实插件描述符，
 * 提供依赖图、兼容性检查、升级影响面分析能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class DependencyWebService {

    private final ObjectProvider<PluginDependencyManager> dependencyManagerProvider;
    private final ObjectProvider<PluginManager> pluginManagerProvider;

    public DependencyWebService(ObjectProvider<PluginDependencyManager> dependencyManagerProvider,
                                ObjectProvider<PluginManager> pluginManagerProvider) {
        this.dependencyManagerProvider = dependencyManagerProvider;
        this.pluginManagerProvider = pluginManagerProvider;
    }

    private PluginDependencyManager getManager() {
        PluginDependencyManager manager = dependencyManagerProvider.getIfAvailable();
        if (manager == null) {
            throw new PluginException("插件依赖分析服务未启用");
        }
        return manager;
    }

    private PluginManager getPluginManager() {
        PluginManager manager = pluginManagerProvider.getIfAvailable();
        if (manager == null) {
            throw new PluginException("插件管理器不可用");
        }
        return manager;
    }

    /**
     * 获取全部已加载插件的依赖图（节点 + 边）
     */
    public DependencyGraph getDependencyGraph() {
        PluginManager pluginManager = getPluginManager();
        List<PluginInfo> plugins = pluginManager.getPlugins();

        DependencyGraph graph = new DependencyGraph();
        for (PluginInfo pluginInfo : plugins) {
            String pluginId = pluginInfo.getPluginId();
            graph.addNode(pluginId, pluginInfo.getPluginDescriptor().getName());

            List<DependencyPlugin> deps = pluginInfo.getPluginDescriptor().getDependencyPlugin();
            if (deps == null) {
                continue;
            }
            for (DependencyPlugin dep : deps) {
                if (dep == null || dep.getId() == null) {
                    continue;
                }
                graph.addEdge(pluginId, dep.getId(), Boolean.TRUE.equals(dep.getOptional()));
            }
        }
        return graph;
    }

    /**
     * 获取指定插件的依赖详情
     */
    public Map<String, Object> getPluginDependencyDetail(String pluginId) {
        PluginManager pluginManager = getPluginManager();
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException("插件不存在: " + pluginId);
        }

        List<String> required = new ArrayList<>();
        List<String> optional = new ArrayList<>();
        Map<String, String> versions = new LinkedHashMap<>();

        List<DependencyPlugin> deps = pluginInfo.getPluginDescriptor().getDependencyPlugin();
        if (deps != null) {
            for (DependencyPlugin dep : deps) {
                if (dep == null || dep.getId() == null) {
                    continue;
                }
                versions.put(dep.getId(), dep.getVersion());
                if (Boolean.TRUE.equals(dep.getOptional())) {
                    optional.add(dep.getId());
                } else {
                    required.add(dep.getId());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("name", pluginInfo.getPluginDescriptor().getName());
        result.put("version", pluginInfo.getPluginDescriptor().getPluginVersion());
        result.put("required", required);
        result.put("optional", optional);
        result.put("versions", versions);
        result.put("reverseDependencies", getReverseDependencies(pluginId));
        return result;
    }

    /**
     * 获取指定插件的依赖解析结果
     */
    public PluginDependencyResolution resolveDependencies(String pluginId) {
        return getManager().resolveDependencies(pluginId);
    }

    /**
     * 检查指定插件与现有插件的兼容性
     */
    public PluginCompatibilityResult checkCompatibility(String pluginId) {
        PluginManager pluginManager = getPluginManager();
        Set<String> others = new HashSet<>();
        for (PluginInfo pluginInfo : pluginManager.getPlugins()) {
            if (!pluginInfo.getPluginId().equals(pluginId)) {
                others.add(pluginInfo.getPluginId());
            }
        }
        return getManager().checkCompatibility(pluginId, others);
    }

    /**
     * 升级影响面分析：谁依赖该插件
     */
    public List<String> getReverseDependencies(String pluginId) {
        // 基于真实插件描述符计算反向依赖
        PluginManager pluginManager = getPluginManager();
        List<String> dependents = new ArrayList<>();
        for (PluginInfo pluginInfo : pluginManager.getPlugins()) {
            List<DependencyPlugin> deps = pluginInfo.getPluginDescriptor().getDependencyPlugin();
            if (deps == null) {
                continue;
            }
            for (DependencyPlugin dep : deps) {
                if (dep != null && pluginId.equals(dep.getId())) {
                    dependents.add(pluginInfo.getPluginId());
                    break;
                }
            }
        }
        dependents.sort(String::compareTo);
        return dependents;
    }

    /**
     * 获取所有插件的版本信息（版本兼容性矩阵基础数据）
     */
    public List<PluginVersionRow> getVersionMatrix() {
        PluginManager pluginManager = getPluginManager();
        List<PluginVersionRow> rows = new ArrayList<>();
        for (PluginInfo pluginInfo : pluginManager.getPlugins()) {
            PluginDescriptor descriptor = pluginInfo.getPluginDescriptor();
            PluginVersionRow row = new PluginVersionRow();
            row.setPluginId(pluginInfo.getPluginId());
            row.setName(descriptor.getName());
            row.setVersion(descriptor.getPluginVersion());
            row.setRequires(descriptor.getRequires());
            row.setState(pluginInfo.getPluginState() == null ? "UNKNOWN" : pluginInfo.getPluginState().name());
            row.setDependencyCount(descriptor.getDependencyPlugin() == null
                    ? 0 : descriptor.getDependencyPlugin().size());
            rows.add(row);
        }
        rows.sort(Comparator.comparing(PluginVersionRow::getPluginId));
        return rows;
    }

    /**
     * 依赖图（节点 + 边）
     */
    public static class DependencyGraph {
        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Map<String, Node> nodeIndex = new LinkedHashMap<>();

        public void addNode(String id, String name) {
            nodeIndex.computeIfAbsent(id, k -> {
                Node node = new Node(k, name == null ? k : name);
                nodes.add(node);
                return node;
            });
        }

        public void addEdge(String from, String to, boolean optional) {
            edges.add(new Edge(from, to, optional));
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public List<Edge> getEdges() {
            return edges;
        }
    }

    public static class Node {
        private final String id;
        private final String name;

        public Node(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Edge {
        private final String from;
        private final String to;
        private final boolean optional;

        public Edge(String from, String to, boolean optional) {
            this.from = from;
            this.to = to;
            this.optional = optional;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public boolean isOptional() {
            return optional;
        }
    }

    /**
     * 版本矩阵行
     */
    public static class PluginVersionRow {
        private String pluginId;
        private String name;
        private String version;
        private String requires;
        private String state;
        private int dependencyCount;

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getRequires() {
            return requires;
        }

        public void setRequires(String requires) {
            this.requires = requires;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public int getDependencyCount() {
            return dependencyCount;
        }

        public void setDependencyCount(int dependencyCount) {
            this.dependencyCount = dependencyCount;
        }
    }
}
