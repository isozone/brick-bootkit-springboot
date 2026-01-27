package com.zqzqq.bootkits.scripts.core.impl;

import com.zqzqq.bootkits.scripts.core.*;
import com.zqzqq.bootkits.scripts.executor.*;
import com.zqzqq.bootkits.scripts.utils.OSDetectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 脚本管理器默认实现
 * 提供完整的脚本执行和管理功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptManager implements ScriptManager {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultScriptManager.class);
    private final Map<String, ScriptExecutor> executorMap = new ConcurrentHashMap<>();
    private final List<String> scriptPaths = new CopyOnWriteArrayList<>();
    private volatile boolean initialized = false;
    
    /**
     * 构造函数
     */
    public DefaultScriptManager() {
        initializeDefaultExecutors();
    }
    
    /**
     * 构造函数
     *
     * @param scriptPaths 脚本搜索路径列表
     */
    public DefaultScriptManager(List<String> scriptPaths) {
        this.scriptPaths.addAll(scriptPaths);
        initializeDefaultExecutors();
    }
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        // 验证系统环境
        OSDetectionUtils.SystemValidationResult validation = OSDetectionUtils.validateSystem();
        if (validation.hasErrors()) {
            throw new IllegalStateException("系统环境验证失败: " + validation.getErrors());
        }
        
        // 注册默认执行器
        registerDefaultExecutors();
        
        // 初始化脚本路径
        initializeScriptPaths();
        
        initialized = true;
    }
    
    @Override
    public void destroy() throws Exception {
        // 清理资源
        executorMap.clear();
        scriptPaths.clear();
        initialized = false;
    }
    
    @Override
    public ScriptExecutionResult executeScript(String scriptPath) throws Exception {
        return executeScript(scriptPath, new String[0], ScriptConfiguration.defaultConfiguration());
    }
    
    @Override
    public ScriptExecutionResult executeScript(String scriptPath, String... arguments) throws Exception {
        return executeScript(scriptPath, arguments, ScriptConfiguration.defaultConfiguration());
    }
    
    @Override
    public ScriptExecutionResult executeScript(String scriptPath, ScriptConfiguration configuration) throws Exception {
        return executeScript(scriptPath, new String[0], configuration);
    }
    
    @Override
    public ScriptExecutionResult executeScript(String scriptPath, String[] arguments, ScriptConfiguration configuration) throws Exception {
        ensureInitialized();
        
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本路径不能为空", null);
        }
        
        // 查找脚本文件
        File scriptFile = findScriptFile(scriptPath);
        if (scriptFile == null || !scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本文件不存在: " + scriptPath, null);
        }
        
        // 检测脚本类型
        ScriptType scriptType = detectScriptType(scriptPath);
        if (scriptType == null) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "无法识别的脚本类型: " + scriptPath, null);
        }
        
        // 获取执行器
        ScriptExecutor executor = getExecutor(getCurrentOperatingSystem(), scriptType);
        if (executor == null) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "没有找到适合的执行器: " + scriptType + " on " + getCurrentOperatingSystem(), null);
        }
        
        // 执行脚本
        return executor.execute(scriptFile.getAbsolutePath(), arguments, configuration);
    }
    
    @Override
    public ScriptExecutionResult executeScript(ScriptType scriptType, String scriptContent, String[] arguments, 
                                             ScriptConfiguration configuration) throws Exception {
        ensureInitialized();
        
        if (scriptType == null) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本类型不能为空", null);
        }
        
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本内容不能为空", null);
        }
        
        // 创建临时脚本文件
        File tempScriptFile = createTempScriptFile(scriptType, scriptContent);
        if (tempScriptFile == null) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "无法创建临时脚本文件", null);
        }
        
        try {
            // 执行脚本
            ScriptExecutionResult result = executeScript(tempScriptFile.getAbsolutePath(), arguments, configuration);
            return result;
        } finally {
            // 清理临时文件
            if (tempScriptFile.exists()) {
                tempScriptFile.delete();
            }
        }
    }
    
    @Override
    public void registerExecutor(ScriptExecutor executor) {
        if (executor == null) {
            return;
        }
        
        String executorKey = generateExecutorKey(executor);
        executorMap.put(executorKey, executor);
    }
    
    @Override
    public void unregisterExecutor(ScriptExecutor executor) {
        if (executor == null) {
            return;
        }
        
        String executorKey = generateExecutorKey(executor);
        executorMap.remove(executorKey);
    }
    
    @Override
    public List<ScriptExecutor> getRegisteredExecutors() {
        return new ArrayList<>(executorMap.values());
    }
    
    @Override
    public ScriptExecutor getExecutor(OperatingSystem os, ScriptType scriptType) {
        for (ScriptExecutor executor : executorMap.values()) {
            if (executor.supports(os, scriptType)) {
                return executor;
            }
        }
        return null;
    }
    
    @Override
    public OperatingSystem getCurrentOperatingSystem() {
        return OperatingSystem.detectCurrentOS();
    }
    
    @Override
    public ScriptType detectScriptType(String scriptPath) {
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            return null;
        }
        
        // 根据文件扩展名检测
        ScriptType scriptType = ScriptType.fromFileName(scriptPath);
        if (scriptType != null) {
            return scriptType;
        }
        
        // 根据文件内容检测（简单检测）
        File scriptFile = new File(scriptPath);
        if (scriptFile.exists() && scriptFile.canRead()) {
            try {
                String content = readFileHeader(scriptFile, 200);
                if (content != null) {
                    if (content.contains("#!/bin/bash") || content.contains("#!/bin/sh") || 
                        content.contains("#!/usr/bin/bash") || content.contains("#!/usr/bin/sh")) {
                        return ScriptType.SHELL;
                    } else if (content.contains("@echo off") || content.contains("echo ")) {
                        return ScriptType.BATCH;
                    } else if (content.contains("#!/usr/bin/env python") || 
                              content.contains("import ") || content.contains("def ")) {
                        return ScriptType.PYTHON;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read script file header: {}", scriptPath, e);
            }
        }
        
        return null;
    }
    
    @Override
    public ScriptType detectScriptTypeByFileName(String fileName) {
        return ScriptType.fromFileName(fileName);
    }
    
    @Override
    public boolean canExecute(String scriptPath) {
        return canExecute(scriptPath, new String[0]);
    }
    
    @Override
    public boolean canExecute(String scriptPath, String[] arguments) {
        try {
            ScriptType scriptType = detectScriptType(scriptPath);
            if (scriptType == null) {
                return false;
            }
            
            ScriptExecutor executor = getExecutor(getCurrentOperatingSystem(), scriptType);
            if (executor == null) {
                return false;
            }
            
            File scriptFile = findScriptFile(scriptPath);
            return scriptFile != null && scriptFile.exists();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getExecutorDescription(ScriptExecutor executor) {
        if (executor == null) {
            return "null executor";
        }
        
        OperatingSystem[] supportedOS = executor.getSupportedOperatingSystems();
        ScriptType[] supportedTypes = executor.getSupportedScriptTypes();
        
        return String.format("Executor[OS=%s, Types=%s]", 
            Arrays.toString(supportedOS), 
            Arrays.toString(supportedTypes));
    }
    
    /**
     * 添加脚本搜索路径
     *
     * @param path 路径
     */
    public void addScriptPath(String path) {
        if (path != null && !path.trim().isEmpty() && !scriptPaths.contains(path)) {
            scriptPaths.add(path.trim());
        }
    }
    
    /**
     * 移除脚本搜索路径
     *
     * @param path 路径
     */
    public void removeScriptPath(String path) {
        scriptPaths.remove(path);
    }
    
    /**
     * 获取所有脚本搜索路径
     *
     * @return 路径列表
     */
    public List<String> getScriptPaths() {
        return new ArrayList<>(scriptPaths);
    }
    
    /**
     * 初始化默认执行器
     */
    private void initializeDefaultExecutors() {
        // 将在initialize方法中注册
    }
    
    /**
     * 注册默认执行器
     */
    private void registerDefaultExecutors() {
        // 注册Shell执行器
        if (OperatingSystem.isLinux() || OperatingSystem.isMacOS() || OperatingSystem.isUnix()) {
            registerExecutor(new ShellExecutor());
        }
        
        // 注册Windows批处理执行器
        if (OperatingSystem.isWindows()) {
            registerExecutor(new BatchExecutor());
        }
        
        // 注册PowerShell执行器
        PowerShellExecutor powerShellExecutor = new PowerShellExecutor();
        if (powerShellExecutor.isPowerShellAvailable()) {
            registerExecutor(powerShellExecutor);
        }
        
        // 注册Python执行器
        PythonExecutor pythonExecutor = new PythonExecutor();
        if (pythonExecutor.isPythonAvailable()) {
            registerExecutor(pythonExecutor);
        }
        
        // 注册Lua执行器
        LuaExecutor luaExecutor = new LuaExecutor();
        if (luaExecutor.isLuaAvailable()) {
            registerExecutor(luaExecutor);
        }
        
        // 注册Ruby执行器
        RubyExecutor rubyExecutor = new RubyExecutor();
        if (rubyExecutor.isRubyAvailable()) {
            registerExecutor(rubyExecutor);
        }
        
        // 注册Perl执行器
        PerlExecutor perlExecutor = new PerlExecutor();
        if (perlExecutor.isPerlAvailable()) {
            registerExecutor(perlExecutor);
        }
        
        // 注册Node.js执行器
        NodeJsExecutor nodeJsExecutor = new NodeJsExecutor();
        if (nodeJsExecutor.isNodeAvailable()) {
            registerExecutor(nodeJsExecutor);
        }
        
        // 注册Groovy执行器
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        if (groovyExecutor.isGroovyAvailable()) {
            registerExecutor(groovyExecutor);
        }
        
        // 注册可执行文件执行器
        registerExecutor(new ExecutableExecutor());
    }
    
    /**
     * 初始化脚本路径
     */
    private void initializeScriptPaths() {
        // 添加当前工作目录
        addScriptPath(System.getProperty("user.dir"));
        
        // 添加用户主目录
        addScriptPath(System.getProperty("user.home"));
        
        // 添加临时目录
        addScriptPath(System.getProperty("java.io.tmpdir"));
        
        // 从配置文件加载路径（如果存在）
        loadScriptPathsFromConfig();
    }
    
    /**
     * 从配置文件加载脚本路径
     */
    private void loadScriptPathsFromConfig() {
        try (InputStream configStream = getClass().getClassLoader().getResourceAsStream("config/scripts.properties")) {
            if (configStream != null) {
                java.util.Properties config = new java.util.Properties();
                config.load(configStream);
                
                // 加载数组格式的路径
                loadArrayProperty(config, "script.paths");
            }
        } catch (Exception e) {
            // 忽略配置文件加载错误，使用默认路径
            log.warn("无法加载配置文件，使用默认路径", e);
        }
    }
    
    /**
     * 加载数组格式的属性
     */
    private void loadArrayProperty(java.util.Properties props, String baseKey) {
        // 首先尝试加载索引数组格式 (script.paths[0], script.paths[1], etc.)
        for (int i = 0; i < 10; i++) { // 限制最大索引
            String key = baseKey + "[" + i + "]";
            String value = props.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                addScriptPath(value.trim());
            }
        }
        
        // 如果没有索引数组，尝试加载逗号分隔格式
        if (!scriptPaths.isEmpty()) {
            String commaSeparated = props.getProperty(baseKey);
            if (commaSeparated != null) {
                String[] paths = commaSeparated.split(",");
                for (String path : paths) {
                    String trimmedPath = path.trim();
                    if (!trimmedPath.isEmpty()) {
                        addScriptPath(trimmedPath);
                    }
                }
            }
        }
    }
    
    /**
     * 生成执行器键
     *
     * @param executor 执行器
     * @return 执行器键
     */
    private String generateExecutorKey(ScriptExecutor executor) {
        return executor.getClass().getSimpleName() + "_" + 
               Arrays.toString(executor.getSupportedOperatingSystems()) + "_" + 
               Arrays.toString(executor.getSupportedScriptTypes());
    }
    
    /**
     * 查找脚本文件
     *
     * @param scriptPath 脚本路径
     * @return 脚本文件
     */
    private File findScriptFile(String scriptPath) {
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            return null;
        }
        
        // 直接路径
        File scriptFile = new File(scriptPath);
        if (scriptFile.exists()) {
            return scriptFile;
        }
        
        // 在配置的路径中搜索
        for (String searchPath : scriptPaths) {
            File searchFile = new File(searchPath, scriptPath);
            if (searchFile.exists()) {
                return searchFile;
            }
        }
        
        return null;
    }
    
    /**
     * 创建临时脚本文件
     *
     * @param scriptType 脚本类型
     * @param content 脚本内容
     * @return 临时文件
     */
    private File createTempScriptFile(ScriptType scriptType, String content) {
        try {
            String prefix = "temp_script_";
            String suffix = scriptType.getExtension();
            if (suffix.isEmpty()) {
                suffix = ".tmp";
            }
            
            File tempFile = File.createTempFile(prefix, suffix);
            
            // 写入内容
            java.nio.file.Files.write(tempFile.toPath(), content.getBytes("UTF-8"));
            
            // 设置执行权限（Unix系统）
            if (!OperatingSystem.isWindows()) {
                tempFile.setExecutable(true);
            }
            
            return tempFile;
        } catch (Exception e) {
            log.error("Failed to create temp script file", e);
            return null;
        }
    }
    
    /**
     * 读取文件头部内容
     *
     * @param file 文件
     * @param maxLength 最大长度
     * @return 文件头部内容
     */
    private String readFileHeader(File file, int maxLength) {
        try {
            if (file.length() > maxLength) {
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                    byte[] buffer = new byte[maxLength];
                    raf.read(buffer);
                    return new String(buffer, "UTF-8");
                }
            } else {
                return new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            }
        } catch (Exception e) {
            log.warn("Failed to read file header: {}", file.getName(), e);
            return null;
        }
    }
    
    /**
     * 确保管理器已初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("脚本管理器未初始化，请先调用initialize()方法");
        }
    }
    
    @Override
    public String toString() {
        return String.format("DefaultScriptManager{initialized=%s, executors=%d, scriptPaths=%d}",
                initialized, executorMap.size(), scriptPaths.size());
    }
}
