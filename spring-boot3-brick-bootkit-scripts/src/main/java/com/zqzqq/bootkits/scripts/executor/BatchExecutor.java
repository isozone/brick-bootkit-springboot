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


package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Windows批处理脚本执行器
 * 用于执行Windows系统上的批处理脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class BatchExecutor extends AbstractScriptExecutor {
    
    private static final String CMD_EXE = "cmd.exe";
    private static final String CMD_ARGUMENT = "/c";
    private static final String DEFAULT_BATCH_COMMANDS[] = {
        "cmd.exe", "C:\\Windows\\System32\\cmd.exe", "C:\\Windows\\SysWOW64\\cmd.exe"
    };
    
    private String cmdPath;
    
    /**
     * 构造函数，使用默认cmd
     */
    public BatchExecutor() {
        this.cmdPath = findDefaultCmd();
    }
    
    /**
     * 构造函数
     *
     * @param cmdPath 指定cmd路径
     */
    public BatchExecutor(String cmdPath) {
        this.cmdPath = cmdPath;
    }
    
    @Override
    public OperatingSystem[] getSupportedOperatingSystems() {
        return new OperatingSystem[] {
            OperatingSystem.WINDOWS
        };
    }
    
    @Override
    public ScriptType[] getSupportedScriptTypes() {
        return new ScriptType[] {
            ScriptType.BATCH,
            ScriptType.PYTHON,
            ScriptType.JAVASCRIPT
        };
    }
    
    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用cmd.exe执行批处理脚本
        command.add(cmdPath);
        command.add(CMD_ARGUMENT);
        command.add(scriptFile.getAbsolutePath());
        
        // 添加参数
        if (arguments != null) {
            for (String arg : arguments) {
                if (arg != null && !arg.trim().isEmpty()) {
                    command.add(arg);
                }
            }
        }
    }
    
    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查cmd.exe是否存在
        if (cmdPath == null || cmdPath.trim().isEmpty()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Windows CMD未找到", null);
        }
        
        File cmdFile = new File(cmdPath);
        if (!cmdFile.exists() || !cmdFile.canExecute()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Windows CMD不可执行: " + cmdPath, null);
        }
        
        // 检查脚本文件是否存在且可读
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "批处理脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "批处理脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
        
        // 设置Windows特定的环境变量
        processBuilder.environment().put("SCRIPT_PATH", scriptFile.getAbsolutePath());
        processBuilder.environment().put("SCRIPT_DIR", scriptFile.getParent());
        processBuilder.environment().put("SCRIPT_NAME", scriptFile.getName());
        
        return executeProcess(processBuilder, configuration, startTime, scriptFile);
    }
    
    /**
     * 查找默认cmd.exe
     *
     * @return 默认cmd路径
     */
    private String findDefaultCmd() {
        for (String cmdCommand : DEFAULT_BATCH_COMMANDS) {
            File cmdFile = new File(cmdCommand);
            if (cmdFile.exists() && cmdFile.canExecute()) {
                return cmdCommand;
            }
        }
        
        // 尝试使用系统PATH中的cmd
        try {
            Process process = new ProcessBuilder("where", "cmd.exe").start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        return CMD_EXE;
    }
    
    /**
     * 获取cmd路径
     *
     * @return cmd路径
     */
    public String getCmdPath() {
        return cmdPath;
    }
    
    /**
     * 设置cmd路径
     *
     * @param cmdPath cmd路径
     */
    public void setCmdPath(String cmdPath) {
        this.cmdPath = cmdPath;
    }
    
    /**
     * 检查cmd是否可用
     *
     * @return 是否可用
     */
    public boolean isCmdAvailable() {
        if (cmdPath == null || cmdPath.trim().isEmpty()) {
            return false;
        }
        
        File cmdFile = new File(cmdPath);
        return cmdFile.exists() && cmdFile.canExecute();
    }
    
    /**
     * 测试批处理功能
     *
     * @return 测试结果
     */
    public boolean testBatchFunctionality() {
        try {
            Process process = new ProcessBuilder(cmdPath, CMD_ARGUMENT, "echo", "test").start();
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return String.format("BatchExecutor{cmdPath='%s', supportedOS=%s, supportedTypes=%s}",
                cmdPath, 
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
}
