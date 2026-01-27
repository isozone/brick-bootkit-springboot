package com.zqzqq.bootkits.core.exception;

/**
 * 插件异常处理器接口
 */
public interface PluginExceptionHandler {
    /**
     * 处理插件异常
     * @param exception 异常实例
     * @param phase 当前阶段（install/start/stop等）
     */
    void handle(EnhancedPluginException exception, String phase);

    /**
     * 是否可恢复异常?
     */
    boolean isRecoverable(EnhancedPluginException exception);

    /**
     * 获取错误处理建议
     */
    default String getAdvice(EnhancedPluginException exception) {
        return "请检查插件配置和运行时环境";
    }
}