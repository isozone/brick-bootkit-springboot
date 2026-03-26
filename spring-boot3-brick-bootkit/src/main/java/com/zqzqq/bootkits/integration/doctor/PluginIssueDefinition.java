package com.zqzqq.bootkits.integration.doctor;

import org.springframework.util.StringUtils;

/**
 * Fixed issue catalog for doctor results and user-facing plugin diagnostics.
 */
public enum PluginIssueDefinition {
    MAIN_PACKAGE_MISSING("MAIN_PACKAGE_MISSING", 1012, "ERROR", "/troubleshooting", "common-errors",
            "未检测到主应用包名",
            "保持标准 @SpringBootApplication 启动结构，或显式配置 plugin.mainPackage"),
    PLUGIN_PATH_EMPTY("PLUGIN_PATH_EMPTY", 1013, "ERROR", "/troubleshooting", "common-errors",
            "未配置插件目录",
            "请至少配置一个 plugin.pluginPath"),
    PLUGIN_PATH_INVALID("PLUGIN_PATH_INVALID", 1013, "ERROR", "/troubleshooting", "common-errors",
            "插件目录配置为空",
            "请为 plugin.pluginPath 提供有效目录"),
    PLUGIN_PATH_MISSING("PLUGIN_PATH_MISSING", 1014, "WARN", "/troubleshooting", "common-errors",
            "插件目录不存在",
            "首次接入可先创建该目录，再把插件包放进去"),
    PLUGIN_PATH_NOT_DIRECTORY("PLUGIN_PATH_NOT_DIRECTORY", 1013, "ERROR", "/troubleshooting", "common-errors",
            "插件路径不是目录",
            "请把 plugin.pluginPath 指向目录而不是单个文件"),
    PLUGIN_PATH_NOT_READABLE("PLUGIN_PATH_NOT_READABLE", 1015, "ERROR", "/troubleshooting", "common-errors",
            "插件目录不可读",
            "请确认应用进程对该目录具有读取权限"),
    UPLOAD_TEMP_INVALID("UPLOAD_TEMP_INVALID", 1016, "ERROR", "/troubleshooting", "common-errors",
            "插件上传临时目录无效",
            "请检查 plugin.uploadTempPath 配置"),
    UPLOAD_TEMP_NOT_WRITABLE("UPLOAD_TEMP_NOT_WRITABLE", 1017, "ERROR", "/troubleshooting", "common-errors",
            "插件上传临时目录不可写",
            "请确认应用进程对该目录具有写权限"),
    UPLOAD_TEMP_CREATE_FAILED("UPLOAD_TEMP_CREATE_FAILED", 1017, "ERROR", "/troubleshooting", "common-errors",
            "无法创建或访问插件上传临时目录",
            "请检查 plugin.uploadTempPath 和目录权限"),
    PLUGIN_MANAGER_UNAVAILABLE("PLUGIN_MANAGER_UNAVAILABLE", 1021, "WARN", "/troubleshooting", "doctor-first",
            "PluginManager Bean 当前不可用",
            "如果此检查发生在启动早期可忽略；启动完成后请确认插件框架是否正常初始化"),
    NO_PLUGINS_FOUND("NO_PLUGINS_FOUND", 1020, "WARN", "/troubleshooting", "common-errors",
            "当前未发现任何插件",
            "请检查 plugin.pluginPath 是否指向正确目录，并确认插件包已放入该目录"),
    WEB_AUTHORIZER_MISSING("WEB_AUTHORIZER_MISSING", 1018, "ERROR", "/faq", "faq-auth",
            "未提供宿主侧 PluginWebAuthorizer",
            "生产环境注入自定义 PluginWebAuthorizer，避免依赖默认 fallback"),
    PLUGIN_PACKAGE_INVALID("PLUGIN_PACKAGE_INVALID", 1008, "ERROR", "/troubleshooting", "common-errors",
            "插件文件校验失败",
            "先使用 /plugins-web/api/doctor 检查环境，再确认插件包是否按框架要求打包");

    private final String key;
    private final int apiCode;
    private final String severity;
    private final String docPath;
    private final String docAnchor;
    private final String message;
    private final String suggestion;

    PluginIssueDefinition(String key,
                          int apiCode,
                          String severity,
                          String docPath,
                          String docAnchor,
                          String message,
                          String suggestion) {
        this.key = key;
        this.apiCode = apiCode;
        this.severity = severity;
        this.docPath = docPath;
        this.docAnchor = docAnchor;
        this.message = message;
        this.suggestion = suggestion;
    }

    public String getKey() {
        return key;
    }

    public String getSeverity() {
        return severity;
    }

    public int getApiCode() {
        return apiCode;
    }

    public String getDocPath() {
        return docPath;
    }

    public String getDocAnchor() {
        return docAnchor;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public static PluginIssueDefinition detect(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("[plugin.mainPackage] 不能为空") || text.contains("未检测到主应用包名")) {
            return MAIN_PACKAGE_MISSING;
        }
        if (text.contains("plugin.pluginPath") || text.contains("插件根路径未配置")) {
            return PLUGIN_PATH_EMPTY;
        }
        if (text.contains("插件目录不存在")) {
            return PLUGIN_PATH_MISSING;
        }
        if (text.contains("插件路径不是目录")) {
            return PLUGIN_PATH_NOT_DIRECTORY;
        }
        if (text.contains("插件目录不可读")) {
            return PLUGIN_PATH_NOT_READABLE;
        }
        if (text.contains("上传临时目录无效")) {
            return UPLOAD_TEMP_INVALID;
        }
        if (text.contains("上传临时目录不可写")) {
            return UPLOAD_TEMP_NOT_WRITABLE;
        }
        if (text.contains("无法创建或访问插件上传临时目录")) {
            return UPLOAD_TEMP_CREATE_FAILED;
        }
        if (text.contains("PluginManager Bean 当前不可用")) {
            return PLUGIN_MANAGER_UNAVAILABLE;
        }
        if (text.contains("当前未发现任何插件")) {
            return NO_PLUGINS_FOUND;
        }
        if (text.contains("PluginWebAuthorizer")) {
            return WEB_AUTHORIZER_MISSING;
        }
        if (text.contains("插件文件校验失败") || text.contains("Only jar/zip plugin package is supported")
                || text.contains("只能上传 JAR")) {
            return PLUGIN_PACKAGE_INVALID;
        }
        return null;
    }
}
