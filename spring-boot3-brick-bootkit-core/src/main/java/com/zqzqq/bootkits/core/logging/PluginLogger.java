package com.zqzqq.bootkits.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件日志器
 * 支持按插件 ID 隔离日志，自动设置 MDC
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class PluginLogger {

    private final Logger logger;
    private final String pluginId;

    public PluginLogger(String pluginId, Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.pluginId = pluginId;
    }

    /**
     * 获取插件日志器
     */
    public static PluginLogger getLogger(String pluginId, Class<?> clazz) {
        return new PluginLogger(pluginId, clazz);
    }

    public static PluginLogger getLogger(Class<?> clazz) {
        return new PluginLogger("unknown", clazz);
    }

    /**
     * 执行带 MDC 的日志
     */
    private void logWithMdc(Level level, String format, Object... args) {
        MDC.put("plugin.id", pluginId);
        try {
            switch (level) {
                case DEBUG:
                    logger.debug(format, args);
                    break;
                case INFO:
                    logger.info(format, args);
                    break;
                case WARN:
                    logger.warn(format, args);
                    break;
                case ERROR:
                    logger.error(format, args);
                    break;
            }
        } finally {
            MDC.remove("plugin.id");
        }
    }

    public void debug(String format, Object... args) {
        logWithMdc(Level.DEBUG, format, args);
    }

    public void info(String format, Object... args) {
        logWithMdc(Level.INFO, format, args);
    }

    public void warn(String format, Object... args) {
        logWithMdc(Level.WARN, format, args);
    }

    public void error(String format, Object... args) {
        logWithMdc(Level.ERROR, format, args);
    }

    public void error(String sourcePluginId, String component, String format, Object... args) {
        MDC.put("plugin.id", sourcePluginId);
        MDC.put("component", component);
        try {
            logger.error(format, args);
        } finally {
            MDC.remove("plugin.id");
            MDC.remove("component");
        }
    }

    public void error(Throwable throwable) {
        logWithMdc(Level.ERROR, throwable.getMessage(), throwable);
    }

    public void lifecycle(String sourcePluginId, PluginLifecycleEvent event, String message) {
        MDC.put("plugin.id", sourcePluginId);
        MDC.put("component", "lifecycle");
        MDC.put("lifecycle.event", event.name());
        try {
            logger.info(message);
        } finally {
            MDC.remove("plugin.id");
            MDC.remove("component");
            MDC.remove("lifecycle.event");
        }
    }

    public void performance(String sourcePluginId, String operation, long durationMillis) {
        MDC.put("plugin.id", sourcePluginId);
        MDC.put("component", "performance");
        try {
            logger.info("操作性能: {} 耗时={}ms", operation, durationMillis);
        } finally {
            MDC.remove("plugin.id");
            MDC.remove("component");
        }
    }

    public void security(String sourcePluginId, String action, String message) {
        MDC.put("plugin.id", sourcePluginId);
        MDC.put("component", "security");
        try {
            logger.warn("安全事件: {} - {}", action, message);
        } finally {
            MDC.remove("plugin.id");
            MDC.remove("component");
        }
    }
    /**
     * 插件生命周期事件。
     */
    public enum PluginLifecycleEvent {
        STARTING, STARTED, STOPPING, STOPPED, FAILED
    }

    /**
     * 日志级别
     */
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
}
