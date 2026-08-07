package com.zqzqq.bootkits.integration.doctor;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.config.PluginConfigurationManager;
import com.zqzqq.bootkits.core.dependency.PluginDependencyManager;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.performance.PluginPerformanceAnalyzer;
import com.zqzqq.bootkits.core.security.PluginSecurityManager;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.cluster.ClusterNodeRegistry;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.utils.FilesUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Host/plugin environment diagnostics.
 */
public class PluginDoctorService {

    private static final String STATUS_OK = "OK";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_ERROR = "ERROR";

    private final IntegrationConfiguration configuration;
    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final ObjectProvider<PluginSecurityManager> securityManagerProvider;
    private final ObjectProvider<PluginServiceRegistry> serviceRegistryProvider;
    private final ObjectProvider<PluginConfigurationManager> configurationManagerProvider;
    private final ObjectProvider<PluginEventBus> eventBusProvider;
    private final ObjectProvider<PluginDependencyManager> dependencyManagerProvider;
    private final ObjectProvider<PluginPerformanceAnalyzer> performanceAnalyzerProvider;
    private final ObjectProvider<ClusterNodeRegistry> clusterNodeRegistryProvider;
    private final List<PluginRolloutProbe> rolloutProbes;

    public PluginDoctorService(IntegrationConfiguration configuration,
                               ObjectProvider<PluginManager> pluginManagerProvider) {
        this(configuration, pluginManagerProvider,
                null, null, null, null, null, null, null, Collections.emptyList());
    }

    public PluginDoctorService(IntegrationConfiguration configuration,
                               ObjectProvider<PluginManager> pluginManagerProvider,
                               ObjectProvider<PluginSecurityManager> securityManagerProvider,
                               ObjectProvider<PluginServiceRegistry> serviceRegistryProvider,
                               ObjectProvider<PluginConfigurationManager> configurationManagerProvider,
                               ObjectProvider<PluginEventBus> eventBusProvider,
                               ObjectProvider<PluginDependencyManager> dependencyManagerProvider,
                               ObjectProvider<PluginPerformanceAnalyzer> performanceAnalyzerProvider,
                               ObjectProvider<ClusterNodeRegistry> clusterNodeRegistryProvider,
                               List<PluginRolloutProbe> rolloutProbes) {
        this.configuration = configuration;
        this.pluginManagerProvider = pluginManagerProvider;
        this.securityManagerProvider = securityManagerProvider;
        this.serviceRegistryProvider = serviceRegistryProvider;
        this.configurationManagerProvider = configurationManagerProvider;
        this.eventBusProvider = eventBusProvider;
        this.dependencyManagerProvider = dependencyManagerProvider;
        this.performanceAnalyzerProvider = performanceAnalyzerProvider;
        this.clusterNodeRegistryProvider = clusterNodeRegistryProvider;
        this.rolloutProbes = rolloutProbes == null ? Collections.emptyList() : rolloutProbes;
    }

    public PluginDoctorReport diagnose() {
        List<PluginDoctorReport.Item> items = new ArrayList<>();
        int errorCount = 0;
        int warningCount = 0;

        boolean enabled = Boolean.TRUE.equals(configuration.enable());
        if (!enabled) {
            items.add(item("PLUGIN_DISABLED", 0, STATUS_WARN,
                    "插件框架当前处于禁用状态",
                    "如果你期望加载插件，请设置 plugin.enable=true",
                    "/quickstart",
                    "yaml"));
            warningCount++;
        }

        String mainPackage = configuration.mainPackage();
        if (!StringUtils.hasText(mainPackage)) {
            items.add(item(PluginIssueDefinition.MAIN_PACKAGE_MISSING,
                    "未检测到主应用包名",
                    "保持标准 @SpringBootApplication 启动结构，或显式配置 plugin.mainPackage"));
            errorCount++;
        } else {
            items.add(item("MAIN_PACKAGE_READY", 0, STATUS_OK,
                    "主应用包名: " + mainPackage,
                    "主应用包名已就绪",
                    "/quickstart",
                    "yaml"));
        }

        List<String> pluginRoots = configuration.pluginPath() == null
                ? Collections.emptyList()
                : new ArrayList<>(configuration.pluginPath());
        if (pluginRoots.isEmpty()) {
            items.add(item(PluginIssueDefinition.PLUGIN_PATH_EMPTY,
                    "未配置插件目录",
                    "请至少配置一个 plugin.pluginPath"));
            errorCount++;
        } else {
            for (String root : pluginRoots) {
                PluginDoctorReport.Item pathItem = inspectPluginRoot(root);
                items.add(pathItem);
                if (STATUS_ERROR.equals(pathItem.getSeverity())) {
                    errorCount++;
                } else if (STATUS_WARN.equals(pathItem.getSeverity())) {
                    warningCount++;
                }
            }
        }

        PluginDoctorReport.Item uploadTempItem = inspectUploadTempPath(configuration.uploadTempPath());
        items.add(uploadTempItem);
        if (STATUS_ERROR.equals(uploadTempItem.getSeverity())) {
            errorCount++;
        } else if (STATUS_WARN.equals(uploadTempItem.getSeverity())) {
            warningCount++;
        }

        int pluginCount = 0;
        int startedPluginCount = 0;
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            items.add(item(PluginIssueDefinition.PLUGIN_MANAGER_UNAVAILABLE,
                    "PluginManager Bean 当前不可用",
                    "如果此检查发生在启动早期可忽略；启动完成后请确认插件框架是否正常初始化"));
            warningCount++;
        } else {
            List<PluginInfo> plugins = pluginManager.getPlugins();
            pluginCount = plugins.size();
            startedPluginCount = (int) plugins.stream()
                    .filter(plugin -> plugin.getPluginState() == EnhancedPluginState.STARTED)
                    .count();
            if (pluginCount == 0) {
                items.add(item(PluginIssueDefinition.NO_PLUGINS_FOUND,
                        "当前未发现任何插件",
                        "请检查 plugin.pluginPath 是否指向正确目录，并确认插件包已放入该目录"));
                warningCount++;
            } else {
                items.add(item("PLUGIN_SCAN_READY", 0, STATUS_OK,
                        "已发现插件数量: " + pluginCount + "，其中已启动: " + startedPluginCount,
                        "插件扫描与装载链路正常",
                        "/quickstart",
                        "doctor"));
            }
        }

        // ===== 新增能力自检维度 =====

        if (securityManagerProvider != null && securityManagerProvider.getIfAvailable() != null) {
            items.add(item("SECURITY_CENTER_READY", 0, STATUS_OK,
                    "插件安全中心已启用",
                    "安全准入检查已在插件安装阶段生效",
                    "/capability-apis",
                    "security"));
        }

        if (serviceRegistryProvider != null && serviceRegistryProvider.getIfAvailable() != null) {
            items.add(item("SERVICE_REGISTRY_READY", 0, STATUS_OK,
                    "插件服务注册中心已启用",
                    "插件可通过 @PluginService 注册并发现服务",
                    "/capability-apis",
                    "registry"));
        }

        if (configurationManagerProvider != null && configurationManagerProvider.getIfAvailable() != null) {
            items.add(item("CONFIG_MANAGER_READY", 0, STATUS_OK,
                    "插件配置热更新已启用",
                    "支持配置热更新、版本管理与回滚",
                    "/capability-apis",
                    "configuration"));
        }

        if (eventBusProvider != null && eventBusProvider.getIfAvailable() != null) {
            items.add(item("EVENT_BUS_READY", 0, STATUS_OK,
                    "插件事件总线已启用",
                    "支持插件间事件发布与订阅",
                    "/capability-apis",
                    "eventbus"));
        }

        if (dependencyManagerProvider != null && dependencyManagerProvider.getIfAvailable() != null) {
            items.add(item("DEPENDENCY_ANALYSIS_READY", 0, STATUS_OK,
                    "插件依赖分析已启用",
                    "支持依赖图、兼容性与升级影响面分析",
                    "/capability-apis",
                    "dependency"));
        }

        if (performanceAnalyzerProvider != null && performanceAnalyzerProvider.getIfAvailable() != null) {
            items.add(item("PERFORMANCE_ANALYSIS_READY", 0, STATUS_OK,
                    "插件性能分析与资源隔离已启用",
                    "支持性能评分、资源占用监控与配额管理",
                    "/capability-apis",
                    "performance"));
        }

        boolean clusterEnabled = Boolean.TRUE.equals(configuration.clusterEnabled());
        if (clusterEnabled) {
            if (clusterNodeRegistryProvider != null && clusterNodeRegistryProvider.getIfAvailable() != null) {
                items.add(item("CLUSTER_READY", 0, STATUS_OK,
                        "集群模式已启用，节点注册与插件状态同步就绪",
                        "Web 控制台「集群管理」页面可查看节点与插件状态",
                        "/capability-apis",
                        "cluster"));
            } else {
                items.add(item("CLUSTER_NODE_REGISTRY_MISSING", 0, STATUS_WARN,
                        "集群模式已启用，但节点注册服务未装配",
                        "确认主框架已自动装配 ClusterNodeRegistry，或检查 plugin.clusterEnabled 配置",
                        "/capability-apis",
                        "cluster"));
                warningCount++;
            }
        }

        if (configuration.pluginRolloutMode() != null
                && configuration.pluginRolloutMode().name().equalsIgnoreCase("GRAY")) {
            if (rolloutProbes.isEmpty()) {
                items.add(item("ROLLOUT_GRAY_NO_PROBE", 0, STATUS_WARN,
                        "灰度发布模式已启用，但未注册任何 PluginRolloutProbe",
                        "灰度模式下建议注入至少一个灰度探针，否则所有升级都将直接通过",
                        "/capability-apis",
                        "rollout"));
                warningCount++;
            } else {
                items.add(item("ROLLOUT_GRAY_READY", 0, STATUS_OK,
                        "灰度发布已启用，已注册 " + rolloutProbes.size() + " 个灰度探针",
                        "升级时会依次执行探针校验",
                        "/capability-apis",
                        "rollout"));
            }
        }

        String overallStatus = errorCount > 0 ? STATUS_ERROR : (warningCount > 0 ? STATUS_WARN : STATUS_OK);
        String summary = buildSummary(overallStatus, errorCount, warningCount, pluginCount, startedPluginCount);
        return new PluginDoctorReport(
                System.currentTimeMillis(),
                enabled,
                mainPackage,
                resolvePluginRoots(pluginRoots),
                pluginCount,
                startedPluginCount,
                errorCount,
                warningCount,
                overallStatus,
                summary,
                items
        );
    }

    private PluginDoctorReport.Item inspectPluginRoot(String root) {
        Path resolvedPath = resolvePath(root);
        if (resolvedPath == null) {
            return item(PluginIssueDefinition.PLUGIN_PATH_INVALID,
                    "插件目录配置为空",
                    "请为 plugin.pluginPath 提供有效目录");
        }
        if (!Files.exists(resolvedPath)) {
            return item(PluginIssueDefinition.PLUGIN_PATH_MISSING,
                    "插件目录不存在: " + resolvedPath,
                    "首次接入可先创建该目录，再把插件包放进去");
        }
        if (!Files.isDirectory(resolvedPath)) {
            return item(PluginIssueDefinition.PLUGIN_PATH_INVALID,
                    "插件路径不是目录: " + resolvedPath,
                    "请把 plugin.pluginPath 指向目录而不是单个文件");
        }
        if (!Files.isReadable(resolvedPath)) {
            return item(PluginIssueDefinition.PLUGIN_PATH_NOT_READABLE,
                    "插件目录不可读: " + resolvedPath,
                    "请确认应用进程对该目录具有读取权限");
        }
        return item("PLUGIN_PATH_READY", 0, STATUS_OK,
                "插件目录可用: " + resolvedPath,
                "插件目录已就绪",
                "/quickstart",
                "yaml");
    }

    private PluginDoctorReport.Item inspectUploadTempPath(String uploadTempPath) {
        Path resolvedPath = resolvePath(uploadTempPath);
        if (resolvedPath == null) {
            return item(PluginIssueDefinition.UPLOAD_TEMP_INVALID,
                    "插件上传临时目录无效",
                    "请检查 plugin.uploadTempPath 配置");
        }
        try {
            Files.createDirectories(resolvedPath);
            if (!Files.isWritable(resolvedPath)) {
                return item(PluginIssueDefinition.UPLOAD_TEMP_NOT_WRITABLE,
                        "插件上传临时目录不可写: " + resolvedPath,
                        "请确认应用进程对该目录具有写权限");
            }
            return item("UPLOAD_TEMP_READY", 0, STATUS_OK,
                    "插件上传临时目录可用: " + resolvedPath,
                    "上传链路已具备基础条件",
                    "/troubleshooting",
                    "doctor-first");
        } catch (IOException e) {
            return item(PluginIssueDefinition.UPLOAD_TEMP_CREATE_FAILED,
                    "无法创建或访问插件上传临时目录: " + resolvedPath,
                    "请检查 plugin.uploadTempPath 和目录权限: " + e.getMessage());
        }
    }

    private List<String> resolvePluginRoots(List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> resolvedRoots = new ArrayList<>(roots.size());
        for (String root : roots) {
            Path resolved = resolvePath(root);
            resolvedRoots.add(resolved != null ? resolved.toString() : root);
        }
        return resolvedRoots;
    }

    private Path resolvePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String resolved = path;
        if (FilesUtils.isRelativePath(path)) {
            resolved = FilesUtils.resolveRelativePath(System.getProperty("user.dir"), path);
        }
        return Paths.get(resolved).toAbsolutePath().normalize();
    }

    private PluginDoctorReport.Item item(PluginIssueDefinition definition, String message, String suggestion) {
        return new PluginDoctorReport.Item(
                definition.getKey(),
                definition.getApiCode(),
                definition.getSeverity(),
                message,
                suggestion,
                definition.getDocPath(),
                definition.getDocAnchor()
        );
    }

    private PluginDoctorReport.Item item(String code,
                                         int errorCode,
                                         String severity,
                                         String message,
                                         String suggestion,
                                         String docPath,
                                         String docAnchor) {
        return new PluginDoctorReport.Item(code, errorCode, severity, message, suggestion, docPath, docAnchor);
    }

    private String buildSummary(String overallStatus,
                                int errorCount,
                                int warningCount,
                                int pluginCount,
                                int startedPluginCount) {
        return String.format("doctor=%s, errors=%d, warnings=%d, plugins=%d, started=%d",
                overallStatus, errorCount, warningCount, pluginCount, startedPluginCount);
    }
}
