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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * 插件异常工厂类
 * 用于创建和转换各种插件异常
 */
public class PluginExceptionFactory {
    
    private PluginExceptionFactory() {}
    
    /**
     * 将通用异常转换为具体的插件异常
     */
    public static EnhancedPluginException convertException(String pluginId, String operation, Throwable throwable) {
        if (throwable instanceof EnhancedPluginException) {
            return (EnhancedPluginException) throwable;
        }
        
        // 根据异常类型和操作类型进行转换
        if (throwable instanceof ClassNotFoundException || throwable instanceof NoClassDefFoundError) {
            return PluginStartException.classLoaderError(pluginId, throwable.getMessage(), throwable);
        }
        
        if (throwable instanceof OutOfMemoryError) {
            return PluginRuntimeException.memoryError(pluginId, 0, 0);
        }
        
        if (throwable instanceof IOException) {
            return PluginRuntimeException.ioError(pluginId, operation, "", throwable);
        }
        
        if (throwable instanceof ConnectException || throwable instanceof SocketTimeoutException) {
            return PluginRuntimeException.networkError(pluginId, "", throwable);
        }
        
        if (throwable instanceof TimeoutException) {
            if ("start".equals(operation)) {
                return PluginStartException.startTimeout(pluginId, 0);
            } else {
                return PluginRuntimeException.runtimeError(pluginId, operation, throwable);
            }
        }
        
        if (throwable instanceof SecurityException) {
            return PluginSecurityException.permissionDenied(pluginId, operation, throwable.getMessage());
        }
        
        if (throwable instanceof IllegalArgumentException || throwable instanceof IllegalStateException) {
            return PluginConfigException.invalidConfig(pluginId, operation, throwable.getMessage());
        }
        
        // 默认转换为运行时异常
        return PluginRuntimeException.runtimeError(pluginId, operation, throwable);
    }
    
    /**
     * 创建安装相关异常
     */
    public static class Install {
        public static PluginInstallException failed(String pluginId, String reason) {
            return PluginInstallException.installFailed(pluginId, reason);
        }
        
        public static PluginInstallException dependencyFailed(String pluginId, String dependency) {
            return PluginInstallException.dependencyFailed(pluginId, dependency);
        }
        
        public static PluginInstallException invalidPlugin(String pluginId, String reason) {
            return PluginInstallException.invalidPlugin(pluginId, reason);
        }
        
        public static PluginInstallException duplicate(String pluginId) {
            return PluginInstallException.duplicatePlugin(pluginId);
        }
        
        public static PluginInstallException versionConflict(String pluginId, String current, String required) {
            return PluginInstallException.versionConflict(pluginId, current, required);
        }
    }
    
    /**
     * 创建启动相关异常
     */
    public static class Start {
        public static PluginStartException failed(String pluginId, String reason) {
            return PluginStartException.startFailed(pluginId, reason);
        }
        
        public static PluginStartException timeout(String pluginId, long timeoutMs) {
            return PluginStartException.startTimeout(pluginId, timeoutMs);
        }
        
        public static PluginStartException dependencyMissing(String pluginId, String dependency) {
            return PluginStartException.dependencyMissing(pluginId, dependency);
        }
        
        public static PluginStartException configError(String pluginId, String configKey, String reason) {
            return PluginStartException.configurationError(pluginId, configKey, reason);
        }
        
        public static PluginStartException classLoaderError(String pluginId, String className, Throwable cause) {
            return PluginStartException.classLoaderError(pluginId, className, cause);
        }
        
        public static PluginStartException springContextError(String pluginId, Throwable cause) {
            return PluginStartException.springContextError(pluginId, cause);
        }
    }
    
    /**
     * 创建运行时相关异常
     */
    public static class Runtime {
        public static PluginRuntimeException error(String pluginId, String operation, Throwable cause) {
            return PluginRuntimeException.runtimeError(pluginId, operation, cause);
        }
        
        public static PluginRuntimeException resourceLeak(String pluginId, String resourceType, String details) {
            return PluginRuntimeException.resourceLeak(pluginId, resourceType, details);
        }
        
        public static PluginRuntimeException memoryError(String pluginId, long used, long max) {
            return PluginRuntimeException.memoryError(pluginId, used, max);
        }
        
        public static PluginRuntimeException threadError(String pluginId, String threadName, Throwable cause) {
            return PluginRuntimeException.threadError(pluginId, threadName, cause);
        }
        
        public static PluginRuntimeException ioError(String pluginId, String operation, String path, Throwable cause) {
            return PluginRuntimeException.ioError(pluginId, operation, path, cause);
        }
        
        public static PluginRuntimeException networkError(String pluginId, String url, Throwable cause) {
            return PluginRuntimeException.networkError(pluginId, url, cause);
        }
    }
    
    /**
     * 创建配置相关异常
     */
    public static class Config {
        public static PluginConfigException invalid(String pluginId, String configKey, String reason) {
            return PluginConfigException.invalidConfig(pluginId, configKey, reason);
        }
        
        public static PluginConfigException missing(String pluginId, String configKey) {
            return PluginConfigException.missingConfig(pluginId, configKey);
        }
        
        public static PluginConfigException parseError(String pluginId, String configFile, Throwable cause) {
            return PluginConfigException.parseError(pluginId, configFile, cause);
        }
        
        public static PluginConfigException validationError(String pluginId, String field, String value, String constraint) {
            return PluginConfigException.validationError(pluginId, field, value, constraint);
        }
    }
    
    /**
     * 创建安全相关异常
     */
    public static class Security {
        public static PluginSecurityException permissionDenied(String pluginId, String operation, String permission) {
            return PluginSecurityException.permissionDenied(pluginId, operation, permission);
        }
        
        public static PluginSecurityException signatureInvalid(String pluginId, String reason) {
            return PluginSecurityException.signatureInvalid(pluginId, reason);
        }
        
        public static PluginSecurityException encryptionError(String pluginId, String algorithm, Throwable cause) {
            return PluginSecurityException.encryptionError(pluginId, algorithm, cause);
        }
        
        public static PluginSecurityException authenticationFailed(String pluginId, String user, String reason) {
            return PluginSecurityException.authenticationFailed(pluginId, user, reason);
        }
    }
}