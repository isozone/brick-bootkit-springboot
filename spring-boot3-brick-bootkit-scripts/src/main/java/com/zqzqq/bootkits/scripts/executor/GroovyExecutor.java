package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Groovy脚本执行器
 * 用于执行Groovy脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class GroovyExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(GroovyExecutor.class);
    
    private static final String[] GROOVY_COMMANDS = {
        "groovy", "groovy3", "groovy2.5"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    
    private String groovyPath;
    private String groovyVersion;
    
    /**
     * 构造函数，使用默认Groovy
     */
    public GroovyExecutor() {
        this.groovyPath = findDefaultGroovy();
        if (this.groovyPath != null) {
            this.groovyVersion = detectGroovyVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param groovyPath 指定Groovy路径
     */
    public GroovyExecutor(String groovyPath) {
        this.groovyPath = groovyPath;
        this.groovyVersion = detectGroovyVersion();
        
        if (this.groovyPath == null || this.groovyPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Groovy路径不能为空");
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
            ScriptType.GROOVY,
            ScriptType.SHELL
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Groovy环境
        if (!isGroovyAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Groovy环境不可用: " + groovyPath, null);
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Groovy脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Groovy脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Groovy特定的环境变量
            processBuilder.environment().put("GROOVY_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("GROOVY_SCRIPT", scriptFile.getAbsolutePath());
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "Groovy脚本执行失败 (Groovy: %s, 版本: %s, 脚本: %s): %s",
                groovyPath, groovyVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Groovy执行脚本
        command.add(groovyPath);
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
     * 查找默认Groovy
     *
     * @return 默认Groovy路径
     */
    private String findDefaultGroovy() {
        for (String groovyCommand : GROOVY_COMMANDS) {
            if (isGroovyAvailable(groovyCommand)) {
                return groovyCommand;
            }
        }
        return null;
    }

    /**
     * 检查指定Groovy命令是否可用
     *
     * @param groovyCommand Groovy命令
     * @return 是否可用
     */
    private boolean isGroovyAvailable(String groovyCommand) {
        try {
            Process process = new ProcessBuilder(groovyCommand, "--version").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Groovy availability: {}", groovyCommand, e);
            return false;
        }
    }

    /**
     * 检测Groovy版本
     *
     * @return Groovy版本字符串
     */
    private String detectGroovyVersion() {
        if (groovyPath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(groovyPath, "--version").start();
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
            log.debug("Failed to detect Groovy version: {}", groovyPath, e);
            return "Unknown";
        }
    }

    /**
     * 检查Groovy是否可用
     *
     * @return 是否可用
     */
    public boolean isGroovyAvailable() {
        return groovyPath != null && isGroovyAvailable(groovyPath);
    }

    /**
     * 获取Groovy路径
     *
     * @return Groovy路径
     */
    public String getGroovyPath() {
        return groovyPath;
    }

    /**
     * 设置Groovy路径
     *
     * @param groovyPath Groovy路径
     */
    public void setGroovyPath(String groovyPath) {
        this.groovyPath = groovyPath;
        this.groovyVersion = detectGroovyVersion();
    }

    /**
     * 获取Groovy版本
     *
     * @return Groovy版本
     */
    public String getGroovyVersion() {
        return groovyVersion;
    }

    /**
     * 列出所有可用的Groovy解释器
     *
     * @return 可用的Groovy解释器列表
     */
    public static List<String> listAvailableGroovyInterpreters() {
        List<String> available = new ArrayList<>();
        for (String groovyCommand : GROOVY_COMMANDS) {
            try {
                Process process = new ProcessBuilder(groovyCommand, "--version").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(groovyCommand + " - " + (version != null ? version.trim() : "Unknown"));
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
     * 获取Groovy安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getGroovyInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Groovy安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://groovy.apache.org/download.html 下载Binary Distribution");
            suggestions.add("2. 解压并设置GROOVY_HOME环境变量");
            suggestions.add("3. 将%GROOVY_HOME%/bin添加到PATH");
            suggestions.add("4. 使用 Chocolatey: choco install groovy");
            suggestions.add("5. 使用 Scoop: scoop install groovy");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: sudo apt install groovy");
            suggestions.add("2. CentOS/RHEL: sudo yum install groovy");
            suggestions.add("3. Fedora: sudo dnf install groovy");
            suggestions.add("4. Arch Linux: sudo pacman -S groovy");
            suggestions.add("5. 使用SDKMAN: curl -s \"https://get.sdkman.io\" | bash");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. 使用 Homebrew: brew install groovy");
            suggestions.add("2. 使用 MacPorts: sudo port install groovy");
            suggestions.add("3. 使用SDKMAN: curl -s \"https://get.sdkman.io\" | bash");
            suggestions.add("4. 从 https://groovy.apache.org/download.html 下载");
        } else {
            suggestions.add("1. 从 https://groovy.apache.org/download.html 下载并安装Groovy");
            suggestions.add("2. 设置GROOVY_HOME环境变量");
            suggestions.add("3. 确保Groovy已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  groovy --version");
        suggestions.add("  which groovy");
        
        return suggestions;
    }

    @Override
    public String toString() {
        return String.format("GroovyExecutor{groovyPath='%s', groovyVersion='%s', supportedOS=%s, supportedTypes=%s}",
                groovyPath, groovyVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroovyExecutor that = (GroovyExecutor) obj;
        return java.util.Objects.equals(groovyPath, that.groovyPath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(groovyPath);
    }
}