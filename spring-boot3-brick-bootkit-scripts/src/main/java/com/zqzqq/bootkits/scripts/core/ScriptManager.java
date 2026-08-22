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


package com.zqzqq.bootkits.scripts.core;

import java.util.List;

/**
 * 脚本管理器接口
 * 提供脚本执行的统一管理接口
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptManager {
    
    /**
     * 执行脚本文件
     *
     * @param scriptPath 脚本文件路径
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeScript(String scriptPath) throws Exception;
    
    /**
     * 执行脚本文件（带参数）
     *
     * @param scriptPath 脚本文件路径
     * @param arguments 执行参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeScript(String scriptPath, String... arguments) throws Exception;
    
    /**
     * 执行脚本文件（带配置）
     *
     * @param scriptPath 脚本文件路径
     * @param configuration 执行配置
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeScript(String scriptPath, ScriptConfiguration configuration) throws Exception;
    
    /**
     * 执行脚本文件（带参数和配置）
     *
     * @param scriptPath 脚本文件路径
     * @param arguments 执行参数
     * @param configuration 执行配置
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeScript(String scriptPath, String[] arguments, ScriptConfiguration configuration) throws Exception;

    /**
     * 执行当前操作系统上的单行命令
     *
     * @param command 命令内容
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeCommand(String command) throws Exception;

    /**
     * 执行当前操作系统上的单行命令（带配置）
     *
     * @param command 命令内容
     * @param configuration 执行配置
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeCommand(String command, ScriptConfiguration configuration) throws Exception;
    
    /**
     * 执行指定类型的脚本
     *
     * @param scriptType 脚本类型
     * @param scriptContent 脚本内容
     * @param arguments 执行参数
     * @param configuration 执行配置
     * @return 执行结果
     * @throws Exception 执行异常
     */
    ScriptExecutionResult executeScript(ScriptType scriptType, String scriptContent, String[] arguments, 
                                       ScriptConfiguration configuration) throws Exception;
    
    /**
     * 注册脚本执行器
     *
     * @param executor 脚本执行器
     */
    void registerExecutor(ScriptExecutor executor);
    
    /**
     * 注销脚本执行器
     *
     * @param executor 脚本执行器
     */
    void unregisterExecutor(ScriptExecutor executor);
    
    /**
     * 获取所有已注册的脚本执行器
     *
     * @return 脚本执行器列表
     */
    List<ScriptExecutor> getRegisteredExecutors();
    
    /**
     * 获取支持指定操作系统和脚本类型的执行器
     *
     * @param os 操作系统
     * @param scriptType 脚本类型
     * @return 支持的执行器，如果未找到则返回null
     */
    ScriptExecutor getExecutor(OperatingSystem os, ScriptType scriptType);
    
    /**
     * 获取当前操作系统
     *
     * @return 当前操作系统
     */
    OperatingSystem getCurrentOperatingSystem();
    
    /**
     * 检测脚本类型
     *
     * @param scriptPath 脚本文件路径
     * @return 脚本类型，如果未找到则返回null
     */
    ScriptType detectScriptType(String scriptPath);
    
    /**
     * 检测脚本类型（通过文件名）
     *
     * @param fileName 文件名
     * @return 脚本类型，如果未找到则返回null
     */
    ScriptType detectScriptTypeByFileName(String fileName);
    
    /**
     * 检查脚本是否可以被执行
     *
     * @param scriptPath 脚本文件路径
     * @return 是否可以执行
     */
    boolean canExecute(String scriptPath);
    
    /**
     * 检查脚本是否可以被执行
     *
     * @param scriptPath 脚本文件路径
     * @param arguments 执行参数
     * @return 是否可以执行
     */
    boolean canExecute(String scriptPath, String[] arguments);
    
    /**
     * 获取脚本执行器描述
     *
     * @param executor 脚本执行器
     * @return 描述信息
     */
    String getExecutorDescription(ScriptExecutor executor);
    
    /**
     * 初始化脚本管理器
     *
     * @throws Exception 初始化异常
     */
    void initialize() throws Exception;
    
    /**
     * 销毁脚本管理器
     *
     * @throws Exception 销毁异常
     */
    void destroy() throws Exception;
}
