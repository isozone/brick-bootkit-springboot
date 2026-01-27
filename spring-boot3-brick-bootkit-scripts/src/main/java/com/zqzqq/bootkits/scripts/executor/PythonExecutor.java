package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Python脚本执行器
 * 用于执行Python脚本，支持跨平台
 *
 * @author starBlues
 * @since 4.0.1
 */
public class PythonExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(PythonExecutor.class);
    
    private static final String[] PYTHON_COMMANDS = {
        "python3", "python", "py", "pypy3", "pypy"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    private static final long MODULE_CHECK_TIMEOUT_SECONDS = 3;
    private static final long FUNCTIONALITY_TEST_TIMEOUT_SECONDS = 5;
    private static final long SIMPLE_TEST_TIMEOUT_SECONDS = 2;
    
    private String pythonPath;
    private String pythonVersion;
    
    /**
     * Python环境检查结果
     */
    public static class PythonEnvironmentCheck {
        private boolean available;
        private String errorMessage;
        private Exception exception;
        private List<String> diagnostics;
        
        public PythonEnvironmentCheck(boolean available) {
            this.available = available;
            this.diagnostics = new ArrayList<>();
        }
        
        public PythonEnvironmentCheck(String errorMessage, Exception exception) {
            this.available = false;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.diagnostics = new ArrayList<>();
        }
        
        public boolean isAvailable() { return available; }
        public String getErrorMessage() { return errorMessage; }
        public Exception getException() { return exception; }
        public List<String> getDiagnostics() { return diagnostics; }
        
        public void addDiagnostic(String diagnostic) {
            diagnostics.add(diagnostic);
        }
        
        public String getFullErrorMessage() {
            if (diagnostics.isEmpty()) {
                return errorMessage;
            }
            return errorMessage + "\n诊断信息:\n" + String.join("\n", diagnostics);
        }
    }

    /**
     * 构造函数，使用默认Python
     */
    public PythonExecutor() {
        this.pythonPath = findDefaultPython();
        if (this.pythonPath != null) {
            this.pythonVersion = detectPythonVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param pythonPath 指定Python路径
     */
    public PythonExecutor(String pythonPath) {
        this.pythonPath = pythonPath;
        this.pythonVersion = detectPythonVersion();
        
        if (this.pythonPath == null || this.pythonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Python路径不能为空");
        }
    }
    
    /**
     * 构造函数，自动检测最佳Python解释器
     *
     * @param preferredVersion 首选Python版本 (如 "3.8", "3.9", "3.10")
     */
    public PythonExecutor(String preferredVersion, boolean autoDetect) {
        this.pythonPath = findBestPython(preferredVersion);
        if (this.pythonPath != null) {
            this.pythonVersion = detectPythonVersion();
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
            ScriptType.PYTHON,
            ScriptType.SHELL,
            ScriptType.JAVASCRIPT
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Python环境
        PythonEnvironmentCheck envCheck = checkPythonEnvironment();
        if (!envCheck.isAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Python环境检查失败: " + envCheck.getErrorMessage(),
                envCheck.getException());
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Python脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Python脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Python特定的环境变量
            processBuilder.environment().put("PYTHON_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("PYTHON_SCRIPT", scriptFile.getAbsolutePath());
            processBuilder.environment().put("PYTHONDONTWRITEBYTECODE", "1"); // 禁用.pyc文件生成
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            // 提供更详细的Python执行错误信息
            String errorMessage = String.format(
                "Python脚本执行失败 (Python: %s, 版本: %s, 脚本: %s): %s",
                pythonPath, pythonVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Python执行脚本
        command.add(pythonPath);
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
     * 查找默认Python
     *
     * @return 默认Python路径
     */
    private String findDefaultPython() {
        for (String pythonCommand : PYTHON_COMMANDS) {
            if (isPythonAvailable(pythonCommand)) {
                return pythonCommand;
            }
        }
        return null;
    }
    
    /**
     * 查找最佳Python解释器
     *
     * @param preferredVersion 首选版本
     * @return 最佳Python路径
     */
    private String findBestPython(String preferredVersion) {
        List<String> available = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        
        // 检测所有可用的Python解释器
        for (String pythonCommand : PYTHON_COMMANDS) {
            try {
                Process process = new ProcessBuilder(pythonCommand, "--version").start();
                boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        
                        if (version != null) {
                            available.add(pythonCommand);
                            versions.add(version.trim());
                        }
                    }
                } else {
                    if (!completed) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to find Python interpreter: {}", pythonCommand, e);
            }
        }
        
        if (available.isEmpty()) {
            return null;
        }
        
        // 如果指定了首选版本，优先选择匹配的版本
        if (preferredVersion != null && !preferredVersion.trim().isEmpty()) {
            for (int i = 0; i < available.size(); i++) {
                if (versions.get(i).contains(preferredVersion)) {
                    return available.get(i);
                }
            }
        }
        
        // 否则返回第一个可用的Python解释器
        return available.get(0);
    }

    /**
     * 检查指定Python命令是否可用
     *
     * @param pythonCommand Python命令
     * @return 是否可用
     */
    private boolean isPythonAvailable(String pythonCommand) {
        try {
            Process process = new ProcessBuilder(pythonCommand, "--version").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Python availability: {}", pythonCommand, e);
            return false;
        }
    }

    /**
     * 检测Python版本
     *
     * @return Python版本字符串
     */
    private String detectPythonVersion() {
        if (pythonPath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(pythonPath, "--version").start();
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
            log.debug("Failed to detect Python version: {}", pythonPath, e);
            return "Unknown";
        }
    }

    /**
     * 执行详细的Python环境检查
     *
     * @return Python环境检查结果
     */
    public PythonEnvironmentCheck checkPythonEnvironment() {
        PythonEnvironmentCheck check = new PythonEnvironmentCheck(true);
        
        // 检查Python路径
        if (pythonPath == null || pythonPath.trim().isEmpty()) {
            return new PythonEnvironmentCheck(
                "未找到Python解释器。请安装Python或配置PYTHON_PATH环境变量。",
                new RuntimeException("Python not found"));
        }
        
        // 检查文件存在性和可执行性
        File pythonFile = new File(pythonPath);
        if (!pythonFile.exists()) {
            return new PythonEnvironmentCheck(
                String.format("Python解释器文件不存在: %s", pythonPath),
                new RuntimeException("Python file not found"));
        }
        
        if (!pythonFile.canExecute()) {
            return new PythonEnvironmentCheck(
                String.format("Python解释器不可执行: %s", pythonPath),
                new RuntimeException("Python not executable"));
        }
        
        // 检查Python版本
        try {
            Process process = new ProcessBuilder(pythonPath, "--version").start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!completed) {
                    process.destroyForcibly();
                    return new PythonEnvironmentCheck(
                        "Python版本检查超时: " + pythonPath,
                        new RuntimeException("Python version check timeout"));
                }
                
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    return new PythonEnvironmentCheck(
                        String.format("Python版本检查失败 (退出码: %d): %s", exitCode, pythonPath),
                        new RuntimeException("Python version check failed"));
                }
                
                if (version != null) {
                    pythonVersion = version.trim();
                    check.addDiagnostic("Python版本: " + pythonVersion);
                }
            }
        } catch (Exception e) {
            return new PythonEnvironmentCheck(
                "Python版本检测失败: " + e.getMessage(),
                e);
        }
        
        // 检查Python模块导入功能
        check.addDiagnostic(checkPythonCoreModules());
        
        return check;
    }
    
    /**
     * 检查Python核心模块
     *
     * @return 检查结果
     */
    private String checkPythonCoreModules() {
        if (pythonPath == null) {
            return "无法检查Python模块 (Python路径为空)";
        }
        
        try {
            // 测试多个核心模块
            String testScript = "import sys, os, json, re, collections, itertools; print('All modules OK')";
            Process process = new ProcessBuilder(pythonPath, "-c", testScript).start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                
                // 等待进程完成
                boolean completed = process.waitFor(MODULE_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                
                if (completed && process.exitValue() == 0) {
                    if (output != null && output.contains("All modules OK")) {
                        return "Python核心模块检查: 通过 (sys, os, json, re, collections, itertools)";
                    } else {
                        return "Python核心模块检查: 部分通过 (输出异常)";
                    }
                } else {
                    if (!completed) {
                        process.destroyForcibly();
                    }
                    
                    // 尝试更简单的测试
                    try {
                        Process simpleProcess = new ProcessBuilder(pythonPath, "-c", "print('Simple test')").start();
                        try (java.io.BufferedReader simpleReader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(simpleProcess.getInputStream()))) {
                            String simpleOutput = simpleReader.readLine();
                            boolean simpleCompleted = simpleProcess.waitFor(SIMPLE_TEST_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                            
                            if (simpleCompleted && simpleProcess.exitValue() == 0) {
                                return "Python核心模块检查: 基础功能正常 (简化测试通过)";
                            } else {
                                if (!simpleCompleted) {
                                    simpleProcess.destroyForcibly();
                                }
                                return "Python核心模块检查: 失败 (基础执行异常)";
                            }
                        }
                    } catch (Exception simpleEx) {
                        return "Python核心模块检查: 失败 (基础执行异常: " + simpleEx.getMessage() + ")";
                    }
                }
            }
        } catch (Exception e) {
            return "Python核心模块检查: 失败 (异常: " + e.getMessage() + ")";
        }
    }

    /**
     * 获取Python环境诊断信息
     *
     * @return 诊断信息列表
     */
    public List<String> getPythonEnvironmentDiagnostics() {
        List<String> diagnostics = new ArrayList<>();
        
        // 基本环境信息
        diagnostics.add("=== Python环境诊断 ===");
        diagnostics.add("操作系统: " + System.getProperty("os.name"));
        diagnostics.add("Java版本: " + System.getProperty("java.version"));
        diagnostics.add("当前工作目录: " + System.getProperty("user.dir"));
        
        // PATH环境变量
        String path = System.getenv("PATH");
        if (path != null) {
            diagnostics.add("PATH环境变量: " + path);
        }
        
        // Python相关环境变量
        String pythonPath = System.getenv("PYTHONPATH");
        if (pythonPath != null) {
            diagnostics.add("PYTHONPATH: " + pythonPath);
        }
        
        // 检查所有可能的Python解释器
        diagnostics.add("\n=== 可用的Python解释器 ===");
        List<String> availableInterpreters = listAvailablePythonInterpreters();
        if (availableInterpreters.isEmpty()) {
            diagnostics.add("未找到任何可用的Python解释器");
        } else {
            diagnostics.addAll(availableInterpreters);
        }
        
        return diagnostics;
    }

    /**
     * 获取Python路径
     *
     * @return Python路径
     */
    public String getPythonPath() {
        return pythonPath;
    }

    /**
     * 设置Python路径
     *
     * @param pythonPath Python路径
     */
    public void setPythonPath(String pythonPath) {
        this.pythonPath = pythonPath;
        this.pythonVersion = detectPythonVersion();
    }

    /**
     * 获取Python版本
     *
     * @return Python版本
     */
    public String getPythonVersion() {
        return pythonVersion;
    }

    /**
     * 检查Python是否可用
     *
     * @return 是否可用
     */
    public boolean isPythonAvailable() {
        return pythonPath != null && isPythonAvailable(pythonPath);
    }

    /**
     * 测试Python执行功能
     *
     * @return 测试结果
     */
    public boolean testPythonFunctionality() {
        if (!isPythonAvailable()) {
            return false;
        }
        
        try {
            Process process = new ProcessBuilder(pythonPath, "-c", "print('Python测试成功')").start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                return completed && process.exitValue() == 0 && 
                       output != null && output.contains("Python测试成功");
            }
        } catch (Exception e) {
            log.debug("Failed to test Python functionality", e);
            return false;
        }
    }

    /**
     * 列出所有可用的Python解释器
     *
     * @return 可用的Python解释器列表
     */
    public static List<String> listAvailablePythonInterpreters() {
        List<String> available = new ArrayList<>();
        for (String pythonCommand : PYTHON_COMMANDS) {
            try {
                Process process = new ProcessBuilder(pythonCommand, "--version").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(pythonCommand + " - " + (version != null ? version.trim() : "Unknown"));
                    }
                }
                if (!completed) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                log.debug("Failed to list Python interpreter: {}", pythonCommand, e);
            }
        }
        return available;
    }
    
    /**
     * 获取Python安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getPythonInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Python安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://python.org 下载并安装Python");
            suggestions.add("2. 确保安装时勾选 'Add Python to PATH' 选项");
            suggestions.add("3. 或者使用 Chocolatey: choco install python");
            suggestions.add("4. 或者使用 winget: winget install Python.Python.3");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: sudo apt update && sudo apt install python3 python3-pip");
            suggestions.add("2. CentOS/RHEL: sudo yum install python3 python3-pip");
            suggestions.add("3. Fedora: sudo dnf install python3 python3-pip");
            suggestions.add("4. Arch Linux: sudo pacman -S python python-pip");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. 使用 Homebrew: brew install python3");
            suggestions.add("2. 使用 MacPorts: sudo port install python38");
            suggestions.add("3. 从 https://python.org 下载安装包");
        } else {
            suggestions.add("1. 从 https://python.org 下载并安装Python");
            suggestions.add("2. 确保Python已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  python3 --version");
        suggestions.add("  python --version");
        suggestions.add("  which python3");
        
        return suggestions;
    }
    
    /**
     * 尝试自动修复Python环境
     *
     * @return 修复结果
     */
    public PythonEnvironmentCheck attemptAutoFix() {
        PythonEnvironmentCheck check = checkPythonEnvironment();
        if (check.isAvailable()) {
            return check; // 环境正常，无需修复
        }
        
        // 尝试查找其他Python解释器
        String originalPath = pythonPath;
        for (String pythonCommand : PYTHON_COMMANDS) {
            if (!pythonCommand.equals(originalPath)) {
                try {
                    if (isPythonAvailable(pythonCommand)) {
                        pythonPath = pythonCommand;
                        pythonVersion = detectPythonVersion();
                        
                        // 验证修复后的环境
                        PythonEnvironmentCheck newCheck = checkPythonEnvironment();
                        if (newCheck.isAvailable()) {
                            newCheck.addDiagnostic("已自动切换到备用Python解释器: " + pythonCommand);
                            return newCheck;
                        }
                    }
                } catch (Exception e) {
                    // 忽略其他Python解释器的错误
                }
            }
        }
        
        // 恢复原始路径
        pythonPath = originalPath;
        pythonVersion = detectPythonVersion();
        
        return check;
    }

    @Override
    public String toString() {
        return String.format("PythonExecutor{pythonPath='%s', pythonVersion='%s', supportedOS=%s, supportedTypes=%s}",
                pythonPath, pythonVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
}