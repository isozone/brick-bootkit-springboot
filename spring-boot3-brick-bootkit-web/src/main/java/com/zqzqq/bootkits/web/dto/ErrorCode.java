package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 
 * @author brick-bootkit
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    SUCCESS(200, "操作成功"),
    
    // 插件相关错误 1001-1099
    PLUGIN_NOT_FOUND(1001, "插件不存在"),
    PLUGIN_ALREADY_EXISTS(1002, "插件已存在"),
    PLUGIN_INSTALL_FAILED(1003, "插件安装失败"),
    PLUGIN_UNINSTALL_FAILED(1004, "插件卸载失败"),
    PLUGIN_START_FAILED(1005, "插件启动失败"),
    PLUGIN_STOP_FAILED(1006, "插件停止失败"),
    PLUGIN_RESTART_FAILED(1007, "插件重启失败"),
    PLUGIN_FILE_INVALID(1008, "插件文件无效"),
    PLUGIN_VERSION_MISMATCH(1009, "插件版本不兼容"),
    PLUGIN_UPLOAD_FAILED(1010, "插件上传失败"),
    PLUGIN_CONFIG_INVALID(1011, "插件配置无效"),
    PLUGIN_MAIN_PACKAGE_MISSING(1012, "主应用包名未配置"),
    PLUGIN_PATH_INVALID(1013, "插件目录配置无效"),
    PLUGIN_PATH_MISSING(1014, "插件目录不存在"),
    PLUGIN_PATH_NOT_READABLE(1015, "插件目录不可读"),
    PLUGIN_UPLOAD_TEMP_INVALID(1016, "插件上传临时目录无效"),
    PLUGIN_UPLOAD_TEMP_NOT_WRITABLE(1017, "插件上传临时目录不可写"),
    PLUGIN_WEB_AUTHORIZER_MISSING(1018, "未配置插件 Web 鉴权器"),
    PLUGIN_DOCTOR_UNAVAILABLE(1019, "插件自检服务不可用"),
    PLUGIN_SCAN_EMPTY(1020, "当前未发现任何插件"),
    PLUGIN_MANAGER_UNAVAILABLE(1021, "插件管理器不可用"),
    
    // 系统错误 2001-2099
    SYSTEM_ERROR(2001, "系统错误"),
    PARAM_INVALID(2002, "参数无效"),
    FILE_NOT_FOUND(2003, "文件不存在"),
    PERMISSION_DENIED(2004, "权限不足"),
    
    // 监控相关错误 3001-3099
    MONITOR_DATA_EMPTY(3001, "监控数据为空"),
    MONITOR_COLLECT_FAILED(3002, "监控数据采集失败");
    
    private final int code;
    private final String message;
}
