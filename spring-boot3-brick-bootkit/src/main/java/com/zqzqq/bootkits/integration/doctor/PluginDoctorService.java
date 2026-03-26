package com.zqzqq.bootkits.integration.doctor;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
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

    public PluginDoctorService(IntegrationConfiguration configuration,
                               ObjectProvider<PluginManager> pluginManagerProvider) {
        this.configuration = configuration;
        this.pluginManagerProvider = pluginManagerProvider;
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
