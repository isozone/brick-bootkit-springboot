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

import com.zqzqq.bootkits.core.logging.PluginLogger;

/**
 * 默认异常处理器实现
 */
public class DefaultExceptionHandler implements PluginExceptionHandler {
    private static final PluginLogger logger = PluginLogger.getLogger(DefaultExceptionHandler.class);

    @Override
    public void handle(EnhancedPluginException exception, String phase) {
        // 使用统一的日志记录器
        logger.error(exception);
        
        // 根据错误级别采取不同的处理策略
        switch (exception.getErrorCode().getSeverity()) {
            case CRITICAL:
                handleCriticalError(exception, phase);
                break;
            case HIGH:
                handleHighSeverityError(exception, phase);
                break;
            case MEDIUM:
                handleMediumSeverityError(exception, phase);
                break;
            case LOW:
                handleLowSeverityError(exception, phase);
                break;
        }
    }

    @Override
    public boolean isRecoverable(EnhancedPluginException exception) {
        return exception.getErrorCode().isRecoverable();
    }

    @Override
    public String getAdvice(EnhancedPluginException exception) {
        switch (exception.getErrorCode()) {
            case INSTALL_DEPENDENCY_FAILED:
                return "请检查插件依赖是否正确安装，确认版本兼容性";
            case START_CONFIGURATION_ERROR:
                return "请检查插件配置文件，确认所有必需的配置项都已正确设置";
            case RUNTIME_MEMORY_ERROR:
                return "请增加JVM内存配置或优化插件内存使用";
            case SECURITY_PERMISSION_DENIED:
                return "请检查插件权限配置，确认具有必要的操作权限";
            case DEPENDENCY_CIRCULAR:
                return "检测到循环依赖，请重新设计插件依赖关系";
            default:
                return "请检查插件配置和运行时环境，查看详细日志获取更多信息";
        }
    }

    private void handleCriticalError(EnhancedPluginException exception, String phase) {
        logger.security(exception.getPluginId(), "CRITICAL_ERROR", 
            "严重错误发生在阶段: " + phase + ", ErrorId: " + exception.getErrorId());
        // 可以在这里添加告警通知逻辑
    }

    private void handleHighSeverityError(EnhancedPluginException exception, String phase) {
        logger.warn(exception.getPluginId(), phase, 
            "高级别错误: {}, 建议: {}", exception.getMessage(), getAdvice(exception));
    }

    private void handleMediumSeverityError(EnhancedPluginException exception, String phase) {
        logger.warn(exception.getPluginId(), phase, 
            "中等级别错误: {}", exception.getMessage());
    }

    private void handleLowSeverityError(EnhancedPluginException exception, String phase) {
        logger.info(exception.getPluginId(), phase, 
            "低级别错误: {}", exception.getMessage());
    }
}