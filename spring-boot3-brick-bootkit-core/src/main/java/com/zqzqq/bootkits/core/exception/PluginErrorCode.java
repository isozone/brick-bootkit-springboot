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


package com.zqzqq.bootkits.core.exception;

/**
 * 插件错误码体系
 * 错误码分类：
 * 1xxx - 安装阶段错误
 * 2xxx - 启动阶段错误  
 * 3xxx - 运行时错误
 * 4xxx - 停止阶段错误
 * 5xxx - 卸载阶段错误
 * 6xxx - 配置错误
 * 7xxx - 依赖错误
 * 8xxx - 安全错误
 * 9xxx - 系统错误
 */
public enum PluginErrorCode {
    // 安装阶段错误 (1xxx)
    INSTALL_FAILED(1001, "插件安装失败"),
    INSTALL_DEPENDENCY_FAILED(1002, "依赖检查失败"),
    INSTALL_INVALID_PLUGIN(1003, "无效的插件包"),
    INSTALL_DUPLICATE_PLUGIN(1004, "插件已存在"),
    INSTALL_VERSION_CONFLICT(1005, "版本冲突"),
    INSTALL_PERMISSION_DENIED(1006, "安装权限不足"),
    INSTALL_FILE_CORRUPTED(1007, "插件文件损坏"),
    
    // 启动阶段错误 (2xxx)  
    START_FAILED(2001, "插件启动失败"),
    START_TIMEOUT(2002, "启动超时"),
    START_DEPENDENCY_MISSING(2003, "启动依赖缺失"),
    START_CONFIGURATION_ERROR(2004, "启动配置错误"),
    START_CLASSLOADER_ERROR(2005, "类加载器错误"),
    START_SPRING_CONTEXT_ERROR(2006, "Spring上下文启动失败"),
    
    // 加载阶段错误 (2xxx)
    LOAD_FAILED(2007, "插件加载失败"),
    PLUGIN_LOAD_FAILED(2008, "插件加载失败"),
    PLUGIN_FILE_NOT_FOUND(2009, "插件文件未找到"),
    PLUGIN_FILE_EMPTY(2010, "插件文件为空"),
    PLUGIN_INVALID_FORMAT(2011, "插件格式无效"),
    PLUGIN_METADATA_INVALID(2012, "插件元数据无效"),
    PLUGIN_CLASS_NOT_FOUND(2013, "插件类未找到"),
    PLUGIN_INTERFACE_NOT_IMPLEMENTED(2014, "插件未实现必要接口"),
    PLUGIN_INSTANTIATION_FAILED(2015, "插件实例化失败"),
    PLUGIN_VALIDATION_FAILED(2016, "插件验证失败"),
    PLUGIN_NOT_FOUND(2017, "插件未找到"),
    PLUGIN_INSTALL_FAILED(2018, "插件安装失败"),
    PLUGIN_START_FAILED(2019, "插件启动失败"),
    PLUGIN_STOP_FAILED(2020, "插件停止失败"),
    PLUGIN_COMPATIBILITY_FAILED(2021, "插件兼容性检查失败"),
    PLUGIN_HEALTH_CHECK_FAILED(2022, "插件健康检查失败"),
    
    // 运行时错误 (3xxx)
    RUNTIME_ERROR(3001, "运行时异常"),
    RESOURCE_LEAK(3002, "资源泄漏"),
    RUNTIME_MEMORY_ERROR(3003, "内存不足"),
    RUNTIME_THREAD_ERROR(3004, "线程异常"),
    RUNTIME_IO_ERROR(3005, "IO异常"),
    RUNTIME_NETWORK_ERROR(3006, "网络异常"),
    PERFORMANCE_ISSUE(3007, "性能问题"),
    
    // 停止阶段错误 (4xxx)
    STOP_FAILED(4001, "插件停止失败"),
    STOP_TIMEOUT(4002, "停止超时"),
    STOP_RESOURCE_CLEANUP_FAILED(4003, "资源清理失败"),
    STOP_DEPENDENCY_CONFLICT(4004, "停止依赖冲突"),
    
    // 卸载阶段错误 (5xxx)
    UNINSTALL_FAILED(5001, "插件卸载失败"),
    UNINSTALL_DEPENDENCY_EXISTS(5002, "存在依赖插件"),
    UNINSTALL_RESOURCE_LOCKED(5003, "资源被锁定"),
    UNINSTALL_PERMISSION_DENIED(5004, "卸载权限不足"),
    
    // 配置错误 (6xxx)
    CONFIG_INVALID(6001, "配置无效"),
    CONFIG_MISSING(6002, "配置缺失"),
    CONFIG_PARSE_ERROR(6003, "配置解析错误"),
    CONFIG_VALIDATION_ERROR(6004, "配置验证失败"),
    CONFIG_LOAD_FAILED(6005, "配置加载失败"),
    CONFIG_UPDATED(6006, "配置已更新"),
    
    // 依赖错误 (7xxx)
    DEPENDENCY_NOT_FOUND(7001, "依赖未找到"),
    DEPENDENCY_VERSION_MISMATCH(7002, "依赖版本不匹配"),
    DEPENDENCY_CIRCULAR(7003, "循环依赖"),
    DEPENDENCY_CONFLICT(7004, "依赖冲突"),
    PLUGIN_CIRCULAR_DEPENDENCY(7005, "插件循环依赖"),
    
    // 安全错误 (8xxx)
    SECURITY_PERMISSION_DENIED(8001, "权限被拒绝"),
    SECURITY_SIGNATURE_INVALID(8002, "签名验证失败"),
    SECURITY_ENCRYPTION_ERROR(8003, "加密错误"),
    SECURITY_AUTHENTICATION_FAILED(8004, "认证失败"),
    SECURITY_VIOLATION(8005, "安全违规"),
    PERMISSION_DENIED(8006, "权限拒绝"),
    SIGNATURE_INVALID(8007, "签名无效"),
    
    // 系统错误 (9xxx)
    SYSTEM_ERROR(9001, "系统错误"),
    SYSTEM_RESOURCE_EXHAUSTED(9002, "系统资源耗尽"),
    SYSTEM_INTERNAL_ERROR(9003, "内部系统错误"),
    SYSTEM_COMPATIBILITY_ERROR(9004, "兼容性错误");

    private final int code;
    private final String description;
    private final ErrorSeverity severity;
    private final boolean recoverable;

    PluginErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
        this.severity = determineSeverity(code);
        this.recoverable = determineRecoverable(code);
    }

    private ErrorSeverity determineSeverity(int code) {
        if (code >= 9000) return ErrorSeverity.CRITICAL;
        if (code >= 8000) return ErrorSeverity.HIGH;
        if (code >= 5000) return ErrorSeverity.MEDIUM;
        return ErrorSeverity.LOW;
    }

    private boolean determineRecoverable(int code) {
        // 系统错误和安全错误通常不可恢复
        return code < 8000;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}