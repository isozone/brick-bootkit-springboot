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
 * 异常处理重构示例
 * 展示如何将现有的通用异常处理替换为新的异常处理框架
 */
public class ExceptionHandlingExample {
    
    private static final PluginLogger logger = PluginLogger.getLogger(ExceptionHandlingExample.class);
    
    /**
     * 旧的异常处理方式 - 不推荐
     */
    @Deprecated
    public void oldWayExample(String pluginId) {
        try {
            // 一些可能抛出异常的操作
            performSomeOperation();
        } catch (Exception e) {
            // 旧方式：使用System.out.print和printStackTrace
            System.err.println("插件操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 新的异常处理方式 - 推荐
     */
    public void newWayExample(String pluginId) {
        try {
            // 一些可能抛出异常的操作
            performSomeOperation();
        } catch (ClassNotFoundException e) {
            // 具体异常类型处理
            PluginStartException exception = PluginStartException.classLoaderError(pluginId, e.getMessage(), e);
            ExceptionHandlerUtils.handleException(exception, "start");
        } catch (OutOfMemoryError e) {
            // 内存异常处理
            PluginRuntimeException exception = PluginRuntimeException.memoryError(pluginId, 0, 0);
            ExceptionHandlerUtils.handleException(exception, "runtime");
        } catch (SecurityException e) {
            // 安全异常处理
            PluginSecurityException exception = PluginSecurityException.permissionDenied(pluginId, "operation", e.getMessage());
            ExceptionHandlerUtils.handleException(exception, "security");
        } catch (Exception e) {
            // 其他异常转换处理
            ExceptionHandlerUtils.handleException(pluginId, "operation", e);
        }
    }
    
    /**
     * 使用安全执行方法 - 最简洁的方式
     */
    public String safeExecutionExample(String pluginId) {
        return ExceptionHandlerUtils.safeExecute(pluginId, "getData", () -> {
            // 可能抛出异常的操作
            return performDataOperation();
        }, "defaultValue");
    }
    
    /**
     * 使用工厂方法创建异常
     */
    public void factoryMethodExample(String pluginId) {
        try {
            performSomeOperation();
        } catch (Exception e) {
            // 使用工厂方法创建具体异常
            PluginInstallException exception = PluginExceptionFactory.Install.failed(pluginId, e.getMessage());
            exception.withContext("operation", "install")
                    .withContext("timestamp", System.currentTimeMillis());
            
            ExceptionHandlerUtils.handleException(exception, "install");
        }
    }
    
    /**
     * 生命周期日志记录示例
     */
    public void lifecycleLoggingExample(String pluginId) {
        // 记录生命周期事件
        logger.lifecycle(pluginId, PluginLogger.PluginLifecycleEvent.STARTING, "开始启动插件");
        
        try {
            performStartOperation();
            logger.lifecycle(pluginId, PluginLogger.PluginLifecycleEvent.STARTED, "插件启动成功");
        } catch (Exception e) {
            logger.lifecycle(pluginId, PluginLogger.PluginLifecycleEvent.FAILED, "插件启动失败: " + e.getMessage());
            ExceptionHandlerUtils.handleException(pluginId, "start", e);
        }
    }
    
    /**
     * 性能监控示例
     */
    public void performanceMonitoringExample(String pluginId) {
        long startTime = System.currentTimeMillis();
        
        try {
            performSomeOperation();
        } catch (Exception e) {
            ExceptionHandlerUtils.handleException(pluginId, "operation", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.performance(pluginId, "operation", duration);
        }
    }
    
    /**
     * 异常链处理示例
     */
    public void exceptionChainExample(String pluginId) {
        try {
            performComplexOperation();
        } catch (Exception e1) {
            try {
                performCleanupOperation();
            } catch (Exception e2) {
                // 创建异常链
                EnhancedPluginException chainedException = ExceptionHandlerUtils.createExceptionChain(
                    pluginId, "complexOperation", e1, e2);
                ExceptionHandlerUtils.handleException(chainedException, "cleanup");
            }
        }
    }
    
    /**
     * 可恢复异常处理示例
     */
    public boolean recoverableExceptionExample(String pluginId) {
        try {
            performSomeOperation();
            return true;
        } catch (Exception e) {
            EnhancedPluginException pluginException = PluginExceptionFactory.convertException(pluginId, "operation", e);
            ExceptionHandlerUtils.handleException(pluginException, "operation");
            
            // 检查是否可恢复
            if (ExceptionHandlerUtils.isRecoverable(pluginException)) {
                logger.info(pluginId, "recovery", "尝试恢复操作: {}", ExceptionHandlerUtils.getAdvice(pluginException));
                // 执行恢复逻辑
                return performRecoveryOperation();
            }
            return false;
        }
    }
    
    // 模拟方法
    private void performSomeOperation() throws Exception {
        // 模拟操作
    }
    
    private String performDataOperation() throws Exception {
        return "data";
    }
    
    private void performStartOperation() throws Exception {
        // 模拟启动操作
    }
    
    private void performComplexOperation() throws Exception {
        // 模拟复杂操作
    }
    
    private void performCleanupOperation() throws Exception {
        // 模拟清理操作
    }
    
    private boolean performRecoveryOperation() {
        return true;
    }
}