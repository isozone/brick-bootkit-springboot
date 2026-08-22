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
 * Ruby脚本执行器
 * 用于执行Ruby脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class RubyExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(RubyExecutor.class);
    
    private static final String[] RUBY_COMMANDS = {
        "ruby", "ruby3.0", "ruby2.7", "ruby2.6"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    
    private String rubyPath;
    private String rubyVersion;
    
    /**
     * 构造函数，使用默认Ruby
     */
    public RubyExecutor() {
        this.rubyPath = findDefaultRuby();
        if (this.rubyPath != null) {
            this.rubyVersion = detectRubyVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param rubyPath 指定Ruby路径
     */
    public RubyExecutor(String rubyPath) {
        this.rubyPath = rubyPath;
        this.rubyVersion = detectRubyVersion();
        
        if (this.rubyPath == null || this.rubyPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Ruby路径不能为空");
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
            ScriptType.RUBY,
            ScriptType.SHELL
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Ruby环境
        if (!isRubyAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Ruby环境不可用: " + rubyPath, null);
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Ruby脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Ruby脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Ruby特定的环境变量
            processBuilder.environment().put("RUBY_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("RUBY_SCRIPT", scriptFile.getAbsolutePath());
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "Ruby脚本执行失败 (Ruby: %s, 版本: %s, 脚本: %s): %s",
                rubyPath, rubyVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Ruby执行脚本
        command.add(rubyPath);
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
     * 查找默认Ruby
     *
     * @return 默认Ruby路径
     */
    private String findDefaultRuby() {
        for (String rubyCommand : RUBY_COMMANDS) {
            if (isRubyAvailable(rubyCommand)) {
                return rubyCommand;
            }
        }
        return null;
    }

    /**
     * 检查指定Ruby命令是否可用
     *
     * @param rubyCommand Ruby命令
     * @return 是否可用
     */
    private boolean isRubyAvailable(String rubyCommand) {
        try {
            Process process = new ProcessBuilder(rubyCommand, "--version").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Ruby availability: {}", rubyCommand, e);
            return false;
        }
    }

    /**
     * 检测Ruby版本
     *
     * @return Ruby版本字符串
     */
    private String detectRubyVersion() {
        if (rubyPath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(rubyPath, "--version").start();
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
            log.debug("Failed to detect Ruby version: {}", rubyPath, e);
            return "Unknown";
        }
    }

    /**
     * 检查Ruby是否可用
     *
     * @return 是否可用
     */
    public boolean isRubyAvailable() {
        return rubyPath != null && isRubyAvailable(rubyPath);
    }

    /**
     * 获取Ruby路径
     *
     * @return Ruby路径
     */
    public String getRubyPath() {
        return rubyPath;
    }

    /**
     * 设置Ruby路径
     *
     * @param rubyPath Ruby路径
     */
    public void setRubyPath(String rubyPath) {
        this.rubyPath = rubyPath;
        this.rubyVersion = detectRubyVersion();
    }

    /**
     * 获取Ruby版本
     *
     * @return Ruby版本
     */
    public String getRubyVersion() {
        return rubyVersion;
    }

    /**
     * 列出所有可用的Ruby解释器
     *
     * @return 可用的Ruby解释器列表
     */
    public static List<String> listAvailableRubyInterpreters() {
        List<String> available = new ArrayList<>();
        for (String rubyCommand : RUBY_COMMANDS) {
            try {
                Process process = new ProcessBuilder(rubyCommand, "--version").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(rubyCommand + " - " + (version != null ? version.trim() : "Unknown"));
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
     * 获取Ruby安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getRubyInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Ruby安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://rubyinstaller.org/ 下载并安装RubyInstaller");
            suggestions.add("2. 使用 Chocolatey: choco install ruby");
            suggestions.add("3. 使用 Scoop: scoop install ruby");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: sudo apt install ruby-full");
            suggestions.add("2. CentOS/RHEL: sudo yum install ruby");
            suggestions.add("3. Fedora: sudo dnf install ruby");
            suggestions.add("4. Arch Linux: sudo pacman -S ruby");
            suggestions.add("5. 使用Rbenv: curl -fsSL https://github.com/rbenv/rbenv-installer/raw/main/bin/rbenv-installer | bash");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. 使用 Homebrew: brew install ruby");
            suggestions.add("2. 使用 MacPorts: sudo port install ruby");
            suggestions.add("3. 使用Rbenv: curl -fsSL https://github.com/rbenv/rbenv-installer/raw/main/bin/rbenv-installer | bash");
        } else {
            suggestions.add("1. 从 https://www.ruby-lang.org/ 下载并安装Ruby");
            suggestions.add("2. 确保Ruby已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  ruby --version");
        suggestions.add("  which ruby");
        
        return suggestions;
    }

    @Override
    public String toString() {
        return String.format("RubyExecutor{rubyPath='%s', rubyVersion='%s', supportedOS=%s, supportedTypes=%s}",
                rubyPath, rubyVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RubyExecutor that = (RubyExecutor) obj;
        return java.util.Objects.equals(rubyPath, that.rubyPath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(rubyPath);
    }
}