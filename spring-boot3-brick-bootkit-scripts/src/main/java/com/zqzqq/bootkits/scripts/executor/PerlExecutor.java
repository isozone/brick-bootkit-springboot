package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Perl脚本执行器
 * 用于执行Perl脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class PerlExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(PerlExecutor.class);
    
    private static final String[] PERL_COMMANDS = {
        "perl", "perl5", "perl5.34", "perl5.32", "perl5.30"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    
    private String perlPath;
    private String perlVersion;
    
    /**
     * 构造函数，使用默认Perl
     */
    public PerlExecutor() {
        this.perlPath = findDefaultPerl();
        if (this.perlPath != null) {
            this.perlVersion = detectPerlVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param perlPath 指定Perl路径
     */
    public PerlExecutor(String perlPath) {
        this.perlPath = perlPath;
        this.perlVersion = detectPerlVersion();
        
        if (this.perlPath == null || this.perlPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Perl路径不能为空");
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
            ScriptType.PERL,
            ScriptType.SHELL
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Perl环境
        if (!isPerlAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Perl环境不可用: " + perlPath, null);
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Perl脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Perl脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Perl特定的环境变量
            processBuilder.environment().put("PERL_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("PERL_SCRIPT", scriptFile.getAbsolutePath());
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "Perl脚本执行失败 (Perl: %s, 版本: %s, 脚本: %s): %s",
                perlPath, perlVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Perl执行脚本
        command.add(perlPath);
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
     * 查找默认Perl
     *
     * @return 默认Perl路径
     */
    private String findDefaultPerl() {
        for (String perlCommand : PERL_COMMANDS) {
            if (isPerlAvailable(perlCommand)) {
                return perlCommand;
            }
        }
        return null;
    }

    /**
     * 检查指定Perl命令是否可用
     *
     * @param perlCommand Perl命令
     * @return 是否可用
     */
    private boolean isPerlAvailable(String perlCommand) {
        try {
            Process process = new ProcessBuilder(perlCommand, "--version").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Perl availability: {}", perlCommand, e);
            return false;
        }
    }

    /**
     * 检测Perl版本
     *
     * @return Perl版本字符串
     */
    private String detectPerlVersion() {
        if (perlPath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(perlPath, "--version").start();
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
            log.debug("Failed to detect Perl version: {}", perlPath, e);
            return "Unknown";
        }
    }

    /**
     * 检查Perl是否可用
     *
     * @return 是否可用
     */
    public boolean isPerlAvailable() {
        return perlPath != null && isPerlAvailable(perlPath);
    }

    /**
     * 获取Perl路径
     *
     * @return Perl路径
     */
    public String getPerlPath() {
        return perlPath;
    }

    /**
     * 设置Perl路径
     *
     * @param perlPath Perl路径
     */
    public void setPerlPath(String perlPath) {
        this.perlPath = perlPath;
        this.perlVersion = detectPerlVersion();
    }

    /**
     * 获取Perl版本
     *
     * @return Perl版本
     */
    public String getPerlVersion() {
        return perlVersion;
    }

    /**
     * 列出所有可用的Perl解释器
     *
     * @return 可用的Perl解释器列表
     */
    public static List<String> listAvailablePerlInterpreters() {
        List<String> available = new ArrayList<>();
        for (String perlCommand : PERL_COMMANDS) {
            try {
                Process process = new ProcessBuilder(perlCommand, "--version").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(perlCommand + " - " + (version != null ? version.trim() : "Unknown"));
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
     * 获取Perl安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getPerlInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Perl安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://strawberryperl.com/ 下载并安装Strawberry Perl");
            suggestions.add("2. 使用 Chocolatey: choco install strawberryperl");
            suggestions.add("3. 使用 Scoop: scoop install perl");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: sudo apt install perl");
            suggestions.add("2. CentOS/RHEL: sudo yum install perl");
            suggestions.add("3. Fedora: sudo dnf install perl");
            suggestions.add("4. Arch Linux: sudo pacman -S perl");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. Perl通常预装在macOS中");
            suggestions.add("2. 使用 Homebrew: brew install perl");
            suggestions.add("3. 使用 MacPorts: sudo port install perl");
        } else {
            suggestions.add("1. 从 https://www.perl.org/ 下载并安装Perl");
            suggestions.add("2. 确保Perl已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  perl --version");
        suggestions.add("  which perl");
        
        return suggestions;
    }

    @Override
    public String toString() {
        return String.format("PerlExecutor{perlPath='%s', perlVersion='%s', supportedOS=%s, supportedTypes=%s}",
                perlPath, perlVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PerlExecutor that = (PerlExecutor) obj;
        return java.util.Objects.equals(perlPath, that.perlPath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(perlPath);
    }
}