package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lua脚本执行器
 * 用于执行Lua脚本，使用Luaj库实现
 *
 * @author starBlues
 * @since 4.0.1
 */
public class LuaExecutor extends AbstractScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(LuaExecutor.class);
    
    private static final String[] LUA_COMMANDS = {
        "lua", "lua5.4", "lua5.3", "lua5.2", "lua5.1", "luajit"
    };
    
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 3;
    private static final long AVAILABILITY_CHECK_TIMEOUT_SECONDS = 2;
    
    private String luaPath;
    private String luaVersion;
    
    /**
     * 构造函数，使用默认Lua
     */
    public LuaExecutor() {
        this.luaPath = findDefaultLua();
        if (this.luaPath != null) {
            this.luaVersion = detectLuaVersion();
        }
    }
    
    /**
     * 构造函数
     *
     * @param luaPath 指定Lua路径
     */
    public LuaExecutor(String luaPath) {
        this.luaPath = luaPath;
        this.luaVersion = detectLuaVersion();
        
        if (this.luaPath == null || this.luaPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Lua路径不能为空");
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
            ScriptType.LUA,
            ScriptType.SHELL
        };
    }

    @Override
    protected ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                             ScriptConfiguration configuration, LocalDateTime startTime) throws Exception {
        // 检查Lua环境
        if (!isLuaAvailable()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Lua环境不可用: " + luaPath, null);
        }
        
        // 检查脚本文件
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Lua脚本文件不存在: " + scriptFile.getAbsolutePath(), null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "Lua脚本文件不可读: " + scriptFile.getAbsolutePath(), null);
        }
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(scriptFile, arguments, configuration);
            
            // 设置Lua特定的环境变量
            processBuilder.environment().put("LUA_PATH", scriptFile.getAbsolutePath());
            processBuilder.environment().put("LUA_SCRIPT", scriptFile.getAbsolutePath());
            processBuilder.environment().put("LUA_CPATH", scriptFile.getParent());
            
            return executeProcess(processBuilder, configuration, startTime, scriptFile);
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "Lua脚本执行失败 (Lua: %s, 版本: %s, 脚本: %s): %s",
                luaPath, luaVersion, scriptFile.getName(), e.getMessage()
            );
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.FAILED,
                errorMessage, e);
        }
    }

    @Override
    protected void buildCommand(List<String> command, File scriptFile, String[] arguments) {
        // 使用Lua执行脚本
        command.add(luaPath);
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
     * 查找默认Lua
     *
     * @return 默认Lua路径
     */
    private String findDefaultLua() {
        for (String luaCommand : LUA_COMMANDS) {
            if (isLuaAvailable(luaCommand)) {
                return luaCommand;
            }
        }
        return null;
    }

    /**
     * 检查指定Lua命令是否可用
     *
     * @param luaCommand Lua命令
     * @return 是否可用
     */
    private boolean isLuaAvailable(String luaCommand) {
        try {
            Process process = new ProcessBuilder(luaCommand, "-v").start();
            boolean completed = process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Failed to check Lua availability: {}", luaCommand, e);
            return false;
        }
    }

    /**
     * 检测Lua版本
     *
     * @return Lua版本字符串
     */
    private String detectLuaVersion() {
        if (luaPath == null) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(luaPath, "-v").start();
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
            log.debug("Failed to detect Lua version: {}", luaPath, e);
            return "Unknown";
        }
    }

    /**
     * 检查Lua是否可用
     *
     * @return 是否可用
     */
    public boolean isLuaAvailable() {
        return luaPath != null && isLuaAvailable(luaPath);
    }

    /**
     * 获取Lua路径
     *
     * @return Lua路径
     */
    public String getLuaPath() {
        return luaPath;
    }

    /**
     * 设置Lua路径
     *
     * @param luaPath Lua路径
     */
    public void setLuaPath(String luaPath) {
        this.luaPath = luaPath;
        this.luaVersion = detectLuaVersion();
    }

    /**
     * 获取Lua版本
     *
     * @return Lua版本
     */
    public String getLuaVersion() {
        return luaVersion;
    }

    /**
     * 列出所有可用的Lua解释器
     *
     * @return 可用的Lua解释器列表
     */
    public static List<String> listAvailableLuaInterpreters() {
        List<String> available = new ArrayList<>();
        for (String luaCommand : LUA_COMMANDS) {
            try {
                Process process = new ProcessBuilder(luaCommand, "-v").start();
                boolean completed = process.waitFor(AVAILABILITY_CHECK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (completed && process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        available.add(luaCommand + " - " + (version != null ? version.trim() : "Unknown"));
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
     * 获取Lua安装建议
     *
     * @param operatingSystem 当前操作系统
     * @return 安装建议列表
     */
    public static List<String> getLuaInstallationSuggestions(OperatingSystem operatingSystem) {
        List<String> suggestions = new ArrayList<>();
        
        suggestions.add("=== Lua安装建议 ===");
        
        if (operatingSystem == OperatingSystem.WINDOWS) {
            suggestions.add("1. 从 https://www.lua.org/download.html 下载预编译版本");
            suggestions.add("2. 使用 Chocolatey: choco install lua");
            suggestions.add("3. 使用 Scoop: scoop install lua");
            suggestions.add("4. 从 https://luamacro.com/ 下载 Lua for Windows");
        } else if (operatingSystem == OperatingSystem.LINUX) {
            suggestions.add("1. Ubuntu/Debian: sudo apt install lua5.4 lua5.4-dev");
            suggestions.add("2. CentOS/RHEL: sudo yum install lua lua-devel");
            suggestions.add("3. Fedora: sudo dnf install lua lua-devel");
            suggestions.add("4. Arch Linux: sudo pacman -S lua");
        } else if (operatingSystem == OperatingSystem.MACOS) {
            suggestions.add("1. 使用 Homebrew: brew install lua");
            suggestions.add("2. 使用 MacPorts: sudo port install lua");
            suggestions.add("3. 从 https://www.lua.org/download.html 下载源码编译");
        } else {
            suggestions.add("1. 从 https://www.lua.org/download.html 下载并安装Lua");
            suggestions.add("2. 确保Lua已添加到系统PATH环境变量中");
        }
        
        suggestions.add("\n=== 验证安装 ===");
        suggestions.add("安装完成后，在命令行中运行以下命令验证:");
        suggestions.add("  lua -v");
        suggestions.add("  which lua");
        
        return suggestions;
    }

    @Override
    public String toString() {
        return String.format("LuaExecutor{luaPath='%s', luaVersion='%s', supportedOS=%s, supportedTypes=%s}",
                luaPath, luaVersion,
                java.util.Arrays.toString(getSupportedOperatingSystems()),
                java.util.Arrays.toString(getSupportedScriptTypes()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LuaExecutor that = (LuaExecutor) obj;
        return java.util.Objects.equals(luaPath, that.luaPath);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(luaPath);
    }
}
