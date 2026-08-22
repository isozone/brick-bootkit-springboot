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
import java.util.ArrayList;
import java.util.List;

/**
 * 可执行文件执行器
 * 用于执行系统可执行文件
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ExecutableExecutor extends AbstractScriptExecutor {
    
    private List<String> executablePaths = new ArrayList<>();
    
    /**
     * 构造函数，使用默认路径
     */
    public ExecutableExecutor() {
        initializeDefaultPaths();
    }
    
    /**
     * 构造函数
     *
     * @param executablePaths 可执行文件路径列表
     */
    public ExecutableExecutor(List<String> executablePaths) {
        this.executablePaths = new ArrayList<>(executablePaths);
    }
    
    /**
     * 添加可执行文件路径
     *
     * @param path 路径
     */
    public void addExecutablePath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            this.executablePaths.add(path.trim());
        }
    }
    
    /**
     * 设置可执行文件路径列表
     *
     * @param paths 路径列表
     */
    public void setExecutablePaths(List<String> paths) {
        this.executablePaths = new ArrayList<>(paths);
    }
    
    /**
     * 获取可执行文件路径列表
     *
     * @return 路径列表
     */
    public List<String> getExecutablePaths() {
        return new ArrayList<>(executablePaths);
    }
    
    @Override
    public OperatingSystem[] getSupportedOperatingSystems() {
        return new OperatingSystem[] {
            OperatingSystem.LINUX,
            OperatingSystem.MACOS,
            OperatingSystem.WINDOWS,
            OperatingSystem.UNIX,
            OperatingSystem.UNKNOWN
        };
    }
    
    @Override
    public ScriptType[] getSupportedScriptTypes() {
        return new ScriptType[] {
            ScriptType.EXECUTABLE
        };
    }
    
    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 直接执行可执行文件
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
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "可执行文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "可执行文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        // 检查文件权限
        if (!OperatingSystem.isWindows()) {
            if (!scriptFile.canExecute()) {
                // 尝试添加执行权限
                try {
                    scriptFile.setExecutable(true);
                } catch (SecurityException e) {
                    return ScriptExecutionResult.failed(
                        ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                        "无法设置可执行文件执行权限: " + e.getMessage(), e);
                }
            }
        }
        
        ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
        
        // 设置环境变量
        processBuilder.environment().put("EXECUTABLE_PATH", scriptFile.getAbsolutePath());
        processBuilder.environment().put("EXECUTABLE_NAME", scriptFile.getName());
        
        return executeProcess(processBuilder, configuration, startTime, scriptFile);
    }
    
    /**
     * 初始化默认路径
     */
    private void initializeDefaultPaths() {
        // 添加系统PATH中的常见路径
        String systemPath = System.getenv("PATH");
        if (systemPath != null && !systemPath.trim().isEmpty()) {
            String[] paths = systemPath.split(File.pathSeparator);
            for (String path : paths) {
                if (path != null && !path.trim().isEmpty()) {
                    executablePaths.add(path.trim());
                }
            }
        }
        
        // 添加其他常见路径
        if (OperatingSystem.isWindows()) {
            executablePaths.add("C:\\Windows\\System32");
            executablePaths.add("C:\\Windows");
            executablePaths.add("C:\\Windows\\SysWOW64");
        } else {
            executablePaths.add("/usr/bin");
            executablePaths.add("/bin");
            executablePaths.add("/usr/local/bin");
        }
    }
    
    /**
     * 从指定路径搜索可执行文件
     *
     * @param fileName 文件名
     * @return 可执行文件路径，如果未找到则返回null
     */
    public String findExecutable(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        
        String executableName = OperatingSystem.isWindows() ? 
            fileName + ".exe" : fileName;
        
        // 首先尝试直接执行（可能在PATH中）
        File directFile = new File(fileName);
        if (directFile.exists() && directFile.canExecute()) {
            return directFile.getAbsolutePath();
        }
        
        // 从配置的路径中搜索
        for (String path : executablePaths) {
            File executableFile = new File(path, executableName);
            if (executableFile.exists() && executableFile.canExecute()) {
                return executableFile.getAbsolutePath();
            }
        }
        
        return null;
    }
    
    /**
     * 检查文件是否为可执行文件
     *
     * @param file 文件
     * @return 是否可执行
     */
    public boolean isExecutable(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        // 检查文件权限
        if (!OperatingSystem.isWindows()) {
            return file.canExecute() || file.getName().endsWith(".sh") || 
                   file.getName().endsWith(".py") || file.getName().endsWith(".pl");
        } else {
            return file.canRead() && (file.getName().endsWith(".exe") || 
                   file.getName().endsWith(".bat") || file.getName().endsWith(".cmd"));
        }
    }
    
    /**
     * 验证可执行文件路径
     *
     * @param path 路径
     * @return 是否有效
     */
    public boolean validateExecutablePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && file.canRead();
    }
    
    @Override
    public String toString() {
        return String.format("ExecutableExecutor{paths=%d, supportedOS=%s, supportedTypes=%s}",
                executablePaths.size(),
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
}