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
 * 异常处理工具类
 * 提供统一的异常处理方法，替代System.out.print和printStackTrace的使用
 */
public class ExceptionHandlerUtils {
    
    private static final PluginExceptionHandler defaultHandler = new DefaultExceptionHandler();
    private static PluginExceptionHandler customHandler = defaultHandler;
    
    private ExceptionHandlerUtils() {}
    
    /**
     * 设置自定义异常处理器
     */
    public static void setExceptionHandler(PluginExceptionHandler handler) {
        customHandler = handler != null ? handler : defaultHandler;
    }
    
    /**
     * 处理插件异常
     */
    public static void handleException(EnhancedPluginException exception, String phase) {
        customHandler.handle(exception, phase);
    }
    
    /**
     * 处理通用异常并转换为插件异常
     */
    public static void handleException(String pluginId, String operation, Throwable throwable) {
        EnhancedPluginException pluginException = PluginExceptionFactory.convertException(pluginId, operation, throwable);
        handleException(pluginException, operation);
    }
    
    /**
     * 安全地执行操作，自动处理异常
     */
    public static <T> T safeExecute(String pluginId, String operation, PluginOperation<T> pluginOperation) {
        try {
            return pluginOperation.execute();
        } catch (Exception e) {
            handleException(pluginId, operation, e);
            return null;
        }
    }
    
    /**
     * 安全地执行操作，自动处理异常，带默认值
     */
    public static <T> T safeExecute(String pluginId, String operation, PluginOperation<T> pluginOperation, T defaultValue) {
        try {
            return pluginOperation.execute();
        } catch (Exception e) {
            handleException(pluginId, operation, e);
            return defaultValue;
        }
    }
    
    /**
     * 安全地执行无返回值操作
     */
    public static void safeExecuteVoid(String pluginId, String operation, PluginVoidOperation pluginOperation) {
        try {
            pluginOperation.execute();
        } catch (Exception e) {
            handleException(pluginId, operation, e);
        }
    }
    
    /**
     * 检查异常是否可恢复
     */
    public static boolean isRecoverable(EnhancedPluginException exception) {
        return customHandler.isRecoverable(exception);
    }
    
    /**
     * 获取异常处理建议
     */
    public static String getAdvice(EnhancedPluginException exception) {
        return customHandler.getAdvice(exception);
    }
    
    /**
     * 记录并重新抛出异常
     */
    public static void logAndThrow(EnhancedPluginException exception, String phase) throws EnhancedPluginException {
        handleException(exception, phase);
        throw exception;
    }
    
    /**
     * 记录并重新抛出转换后的异常
     */
    public static void logAndThrow(String pluginId, String operation, Throwable throwable) throws EnhancedPluginException {
        EnhancedPluginException pluginException = PluginExceptionFactory.convertException(pluginId, operation, throwable);
        logAndThrow(pluginException, operation);
    }
    
    /**
     * 创建异常链
     */
    public static EnhancedPluginException createExceptionChain(String pluginId, String operation, Throwable... throwables) {
        if (throwables.length == 0) {
            return PluginRuntimeException.runtimeError(pluginId, operation, new RuntimeException("Unknown error"));
        }
        
        EnhancedPluginException result = PluginExceptionFactory.convertException(pluginId, operation, throwables[0]);
        
        for (int i = 1; i < throwables.length; i++) {
            result.addSuppressed(throwables[i]);
        }
        
        return result;
    }
    
    /**
     * 插件操作接口
     */
    @FunctionalInterface
    public interface PluginOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * 插件无返回值操作接口
     */
    @FunctionalInterface
    public interface PluginVoidOperation {
        void execute() throws Exception;
    }
    
    /**
     * 异常统计信息
     */
    public static class ExceptionStats {
        private static volatile long totalExceptions = 0;
        private static volatile long recoverableExceptions = 0;
        private static volatile long criticalExceptions = 0;
        
        public static void recordException(EnhancedPluginException exception) {
            totalExceptions++;
            if (exception.getErrorCode().isRecoverable()) {
                recoverableExceptions++;
            }
            if (exception.getErrorCode().getSeverity() == PluginErrorCode.ErrorSeverity.CRITICAL) {
                criticalExceptions++;
            }
        }
        
        public static long getTotalExceptions() {
            return totalExceptions;
        }
        
        public static long getRecoverableExceptions() {
            return recoverableExceptions;
        }
        
        public static long getCriticalExceptions() {
            return criticalExceptions;
        }
        
        public static void reset() {
            totalExceptions = 0;
            recoverableExceptions = 0;
            criticalExceptions = 0;
        }
    }
}