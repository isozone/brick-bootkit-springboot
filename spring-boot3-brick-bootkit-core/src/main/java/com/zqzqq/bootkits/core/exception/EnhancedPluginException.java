package com.zqzqq.bootkits.core.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.zqzqq.bootkits.core.exception.PluginErrorCode.ErrorSeverity;

/**
 * 增强版插件异常（支持错误码和上下文）
 */
public class EnhancedPluginException extends RuntimeException {
    private final PluginErrorCode errorCode;
    private final Map<String, Object> context = new HashMap<>();
    private final String errorId;
    private final LocalDateTime timestamp;
    private String pluginId;

    public EnhancedPluginException() {
        this(PluginErrorCode.SYSTEM_ERROR);
    }

    public EnhancedPluginException(String message) {
        this(PluginErrorCode.SYSTEM_ERROR, message);
    }

    public EnhancedPluginException(String message, Throwable cause) {
        this(PluginErrorCode.SYSTEM_ERROR, message, cause);
    }

    public EnhancedPluginException(Throwable cause) {
        this(PluginErrorCode.SYSTEM_ERROR, cause);
    }

    public EnhancedPluginException(PluginErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.errorId = UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = LocalDateTime.now();
    }

    public EnhancedPluginException(PluginErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorId = UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = LocalDateTime.now();
    }

    public EnhancedPluginException(PluginErrorCode errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.errorId = UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = LocalDateTime.now();
    }

    public EnhancedPluginException(PluginErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorId = UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    public <T extends EnhancedPluginException> T withContext(String key, Object value) {
        context.put(key, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends EnhancedPluginException> T withPluginId(String pluginId) {
        this.pluginId = pluginId;
        return (T) this;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public ErrorSeverity getSeverity() {
        return errorCode.getSeverity();
    }

    public boolean isRecoverable() {
        return errorCode.isRecoverable();
    }

    // Getters
    public PluginErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getErrorId() {
        return errorId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] %s - %s (ErrorId: %s)", 
            errorCode.name(), 
            errorCode.getDescription(), 
            super.getMessage(),
            errorId);
    }

    /**
     * 获取详细的错误信息，包含上下文
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        
        if (pluginId != null) {
            sb.append("\n  插件ID: ").append(pluginId);
        }
        
        sb.append("\n  错误级别: ").append(errorCode.getSeverity());
        sb.append("\n  可恢复: ").append(errorCode.isRecoverable() ? "是" : "否");
        sb.append("\n  时间戳: ").append(timestamp);
        
        if (!context.isEmpty()) {
            sb.append("\n  上下文信息:");
            context.forEach((k, v) -> sb.append("\n    ").append(k).append(": ").append(v));
        }
        
        return sb.toString();
    }
}
