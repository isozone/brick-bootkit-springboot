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
 * Shell脚本执行器
 * 用于执行Unix/Linux/Mac系统上的shell脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ShellExecutor extends AbstractScriptExecutor {
    
    private static final String[] DEFAULT_SHELL_COMMANDS = {
        "/bin/bash", "/bin/sh", "/usr/bin/bash", "/usr/bin/sh", 
        "/usr/local/bin/bash", "/usr/local/bin/sh"
    };
    
    private String shellPath;
    
    /**
     * 构造函数，使用默认shell
     */
    public ShellExecutor() {
        this.shellPath = findDefaultShell();
    }
    
    /**
     * 构造函数
     *
     * @param shellPath 指定shell路径
     */
    public ShellExecutor(String shellPath) {
        this.shellPath = shellPath;
    }
    
    @Override
    public OperatingSystem[] getSupportedOperatingSystems() {
        return new OperatingSystem[] {
            OperatingSystem.LINUX,
            OperatingSystem.MACOS,
            OperatingSystem.UNIX
        };
    }
    
    @Override
    public ScriptType[] getSupportedScriptTypes() {
        return new ScriptType[] {
            ScriptType.SHELL,
            ScriptType.PYTHON,
            ScriptType.JAVASCRIPT
        };
    }
    
    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用shell执行脚本
        command.add(shellPath);
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
        // 检查shell是否存在
        if (shellPath == null || shellPath.trim().isEmpty()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Shell解释器未找到", null);
        }
        
        File shellFile = new File(shellPath);
        if (!shellFile.exists() || !shellFile.canExecute()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Shell解释器不可执行: " + shellPath, null);
        }
        
        // 检查脚本文件权限
        if (!scriptFile.canExecute()) {
            // 尝试添加执行权限
            try {
                scriptFile.setExecutable(true);
            } catch (SecurityException e) {
                return ScriptExecutionResult.failed(
                    ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                    "无法设置脚本文件执行权限: " + e.getMessage(), e);
            }
        }
        
        ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
        
        // 设置脚本文件的执行权限
        processBuilder.environment().put("SCRIPT_PATH", scriptFile.getAbsolutePath());
        processBuilder.environment().put("SCRIPT_DIR", scriptFile.getParent());
        
        return executeProcess(processBuilder, configuration, startTime, scriptFile);
    }
    
    /**
     * 查找默认shell
     *
     * @return 默认shell路径
     */
    private String findDefaultShell() {
        for (String shellCommand : DEFAULT_SHELL_COMMANDS) {
            File shellFile = new File(shellCommand);
            if (shellFile.exists() && shellFile.canExecute()) {
                return shellCommand;
            }
        }
        
        // 如果预定义的shell都找不到，尝试从PATH中获取
        String pathShell = System.getenv("SHELL");
        if (pathShell != null && !pathShell.trim().isEmpty()) {
            File shellFile = new File(pathShell);
            if (shellFile.exists() && shellFile.canExecute()) {
                return pathShell;
            }
        }
        
        return null;
    }
    
    /**
     * 获取shell路径
     *
     * @return shell路径
     */
    public String getShellPath() {
        return shellPath;
    }
    
    /**
     * 设置shell路径
     *
     * @param shellPath shell路径
     */
    public void setShellPath(String shellPath) {
        this.shellPath = shellPath;
    }
    
    /**
     * 检查shell是否可用
     *
     * @return 是否可用
     */
    public boolean isShellAvailable() {
        if (shellPath == null || shellPath.trim().isEmpty()) {
            return false;
        }
        
        File shellFile = new File(shellPath);
        return shellFile.exists() && shellFile.canExecute();
    }
    
    @Override
    public String toString() {
        return String.format("ShellExecutor{shellPath='%s', supportedOS=%s, supportedTypes=%s}",
                shellPath, 
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ShellExecutor that = (ShellExecutor) obj;
        return java.util.Objects.equals(shellPath, that.shellPath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(shellPath);
    }
}
