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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Node.js脚本执行器
 * 用于执行Node.js脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class NodeJsExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(NodeJsExecutor.class);
    
    private static final String[] NODE_COMMANDS = {
        "node", "nodejs", "node20", "node18", "node16"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    
    private String nodePath;
    private String nodeVersion;
    
    /**
     * 构造函数，使用默认Node.js
     */
    public NodeJsExecutor() {
        this.nodePath = findDefaultNode();
        if (this.nodePath != null) {
            this.nodeVersion = detectNodeVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param nodePath 指定Node.js路径
     */
    public NodeJsExecutor(String nodePath) {
        this.nodePath = nodePath;
        this.nodeVersion = detectNodeVersion();
        
        if (this.nodePath == null || this.nodePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Node.js路径不能为空");
        }
    }

    @Override
    public OperatingSystem[] getSupportedOperatingSystems() {
        return new OperatingSystem[] {
            OperatingSystem.WINDOWS,
            OperatingSystem.LINUX,
            OperatingSystem.MACOS,
            OperatingSystem.UNIX,
            OperatingSystem.UNKNOWN
        };
    }

    @Override
    public ScriptType[] getSupportedScriptTypes() {
        return new ScriptType[] {
            ScriptType.NODEJS,
            ScriptType.JAVASCRIPT,
            ScriptType.SHELL
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Node.js环境
        if (!isNodeAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Node.js环境不可用: " + nodePath, null);
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Node.js脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Node.js脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Node.js特定的环境变量
            processBuilder.environment().put("NODE_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("NODE_SCRIPT", scriptFile.getAbsolutePath());
            processBuilder.environment().put("NODE_ENV", "production");
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "Node.js脚本执行失败 (Node.js: %s, 版本: %s, 脚本: %s): %s",
                nodePath, nodeVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Node.js执行脚本
        command.add(nodePath);
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

    /**
     * 查找默认Node.js
     *
     * @return 默认Node.js路径
     */
    private String findDefaultNode() {
        for (String nodeCommand : NODE_COMMANDS) {
            if (isNodeAvailable(nodeCommand)) {
                return nodeCommand;
            }
        }
        return null;
    }

    /**
     * 检查指定Node.js命令是否可用
     *
     * @param nodeCommand Node.js命令
     * @return 是否可用
     */
    private boolean isNodeAvailable(String nodeCommand) {
        try {
            Process process = new ProcessBuilder(nodeCommand, "--version").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Node.js availability: {}", nodeCommand, e);
            return false;
        }
    }

    /**
     * 检测Node.js版本
     *
     * @return Node.js版本字符串
     */
    private String detectNodeVersion() {
        if (nodePath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(nodePath, "--version").start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    return "Unknown";
                }
                return version != null ? version.trim() : "Unknown";
            }
        } catch (Exception e) {
            log.debug("Failed to detect Node.js version: {}", nodePath, e);
            return "Unknown";
        }
    }

    /**
     * 检查Node.js是否可用
     *
     * @return 是否可用
     */
    public boolean isNodeAvailable() {
        return nodePath != null && isNodeAvailable(nodePath);
    }

    /**
     * 获取Node.js路径
     *
     * @return Node.js路径
     */
    public String getNodePath() {
        return nodePath;
    }

    /**
     * 设置Node.js路径
     *
     * @param nodePath Node.js路径
     */
    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
        this.nodeVersion = detectNodeVersion();
    }

    /**
     * 获取Node.js版本
     *
     * @return Node.js版本
     */
    public String getNodeVersion() {
        return nodeVersion;
    }

    /**
     * 列出所有可用的Node.js解释器
     *
     * @return 可用的Node.js解释器列表
     */
    public static List<String> listAvailableNodeInterpreters() {
        List<String> available = new ArrayList<>();
        for (String nodeCommand : NODE_COMMANDS) {
            try {
                Process process = new ProcessBuilder(nodeCommand, "--version").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(nodeCommand + " - " + (version != null ? version.trim() : "Unknown"));
                    }
                }
                if (!completed) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
        return available;
    }

    /**
     * 获取Node.js安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getNodeInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Node.js安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://nodejs.org/ 下载并安装LTS版本");
            suggestions.add("2. 使用 Chocolatey: choco install nodejs");
            suggestions.add("3. 使用 Scoop: scoop install nodejs");
            suggestions.add("4. 使用 winget: winget install OpenJS.NodeJS");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash - && sudo apt-get install -y nodejs");
            suggestions.add("2. CentOS/RHEL: curl -fsSL https://rpm.nodesource.com/setup_lts.x | sudo bash - && sudo yum install nodejs npm");
            suggestions.add("3. Fedora: sudo dnf install nodejs npm");
            suggestions.add("4. Arch Linux: sudo pacman -S nodejs npm");
            suggestions.add("5. 使用nvm: curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. 使用 Homebrew: brew install node");
            suggestions.add("2. 使用 MacPorts: sudo port install nodejs");
            suggestions.add("3. 从 https://nodejs.org/ 下载安装包");
            suggestions.add("4. 使用nvm: curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash");
        } else {
            suggestions.add("1. 从 https://nodejs.org/ 下载并安装Node.js");
            suggestions.add("2. 确保Node.js已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  node --version");
        suggestions.add("  npm --version");
        suggestions.add("  which node");
        
        return suggestions;
    }

    @Override
    public String toString() {
        return String.format("NodeJsExecutor{nodePath='%s', nodeVersion='%s', supportedOS=%s, supportedTypes=%s}",
                nodePath, nodeVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NodeJsExecutor that = (NodeJsExecutor) obj;
        return java.util.Objects.equals(nodePath, that.nodePath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(nodePath);
    }
}