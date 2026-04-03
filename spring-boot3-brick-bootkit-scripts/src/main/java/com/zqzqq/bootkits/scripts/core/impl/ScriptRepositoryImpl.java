package com.zqzqq.bootkits.scripts.core.impl;

import com.zqzqq.bootkits.scripts.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 脚本仓库默认实现
 * 提供完整的脚本版本控制、依赖管理和部署更新功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptRepositoryImpl implements ScriptRepository {
    
    private static final Logger log = LoggerFactory.getLogger(ScriptRepositoryImpl.class);
    private final Map<String, ScriptInfo> scripts = new ConcurrentHashMap<>();
    private final Map<String, List<ScriptVersion>> versionHistory = new ConcurrentHashMap<>();
    private final Map<String, List<ScriptDependency>> dependencies = new ConcurrentHashMap<>();
    private final Path repositoryPath;
    private final Path versionsPath;
    private final Path dependenciesPath;
    
    /**
     * 构造函数
     *
     * @param repositoryPath 仓库根路径
     */
    public ScriptRepositoryImpl(String repositoryPath) {
        this.repositoryPath = Paths.get(repositoryPath);
        this.versionsPath = this.repositoryPath.resolve("versions");
        this.dependenciesPath = this.repositoryPath.resolve("dependencies");
        
        initializeRepository();
    }
    
    /**
     * 初始化仓库
     */
    private void initializeRepository() {
        try {
            Files.createDirectories(repositoryPath);
            Files.createDirectories(versionsPath);
            Files.createDirectories(dependenciesPath);
            
            // 加载现有脚本
            loadScripts();
        } catch (IOException e) {
            throw new RuntimeException("初始化脚本仓库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件加载脚本
     */
    private void loadScripts() throws IOException {
        if (!Files.exists(repositoryPath)) {
            return;
        }
        
        Files.walk(repositoryPath)
            .filter(Files::isRegularFile)
            .filter(path -> ScriptType.fromFileName(path.getFileName().toString()) != null)
            .forEach(path -> {
                try {
                    String content = Files.readString(path, StandardCharsets.UTF_8);
                    String fileName = path.getFileName().toString();
                    ScriptType type = ScriptType.fromFileName(fileName);
                    
                    if (type != null) {
                        ScriptInfo scriptInfo = new ScriptInfo(
                            fileName, content, type, "1.0.0", 
                            "从文件导入", List.of("imported"), "system"
                        );
                        scripts.put(fileName, scriptInfo);
                    }
                } catch (IOException e) {
                    // 记录错误但继续加载其他文件
                    log.error("加载脚本文件失败: {}, 错误: {}", path, e.getMessage(), e);
                }
            });
    }
    
    @Override
    public RepositoryResult registerScript(ScriptInfo scriptInfo) {
        try {
            if (scriptInfo == null) {
                return RepositoryResult.failure("脚本信息不能为空", null, null);
            }
            
            String scriptName = scriptInfo.getName();
            if (scriptName == null || scriptName.trim().isEmpty()) {
                return RepositoryResult.failure("脚本名称不能为空", null, null);
            }
            
            if (scripts.containsKey(scriptName)) {
                return RepositoryResult.failure("脚本已存在: " + scriptName, null, scriptName);
            }
            
            // 保存脚本文件
            saveScriptFile(scriptInfo);
            
            // 注册到仓库
            scripts.put(scriptName, scriptInfo);
            
            // 初始化版本历史
            ScriptVersion version = new ScriptVersion(
                scriptInfo.getVersion(), scriptInfo.getContent(), 
                scriptInfo.getAuthor(), "初始版本"
            );
            versionHistory.put(scriptName, new ArrayList<>(List.of(version)));
            
            return RepositoryResult.success("脚本注册成功: " + scriptName, scriptName, scriptInfo);
            
        } catch (Exception e) {
            return RepositoryResult.failure("脚本注册失败: " + e.getMessage(), e, scriptInfo.getName());
        }
    }
    
    @Override
    public Optional<ScriptInfo> getScript(String scriptName) {
        return Optional.ofNullable(scripts.get(scriptName));
    }
    
    @Override
    public List<ScriptInfo> getAllScripts() {
        return new ArrayList<>(scripts.values());
    }
    
    @Override
    public RepositoryResult deleteScript(String scriptName) {
        try {
            if (!scripts.containsKey(scriptName)) {
                return RepositoryResult.failure("脚本不存在: " + scriptName, null, scriptName);
            }
            
            // 删除脚本文件
            deleteScriptFile(scriptName);
            
            // 从仓库移除
            scripts.remove(scriptName);
            versionHistory.remove(scriptName);
            dependencies.remove(scriptName);
            
            return RepositoryResult.success("脚本删除成功: " + scriptName, scriptName);
            
        } catch (Exception e) {
            return RepositoryResult.failure("脚本删除失败: " + e.getMessage(), e, scriptName);
        }
    }
    
    @Override
    public RepositoryResult updateScript(String scriptName, String scriptContent) {
        try {
            ScriptInfo existingScript = scripts.get(scriptName);
            if (existingScript == null) {
                return RepositoryResult.failure("脚本不存在: " + scriptName, null, scriptName);
            }
            
            // 创建新版本
            String newVersion = incrementVersion(existingScript.getVersion());
            ScriptVersion newVersionInfo = new ScriptVersion(
                newVersion, scriptContent, "system", "脚本更新"
            );
            
            // 更新版本历史
            versionHistory.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(newVersionInfo);
            
            // 更新脚本信息
            ScriptInfo updatedScript = new ScriptInfo(
                scriptName, scriptContent, existingScript.getType(), newVersion,
                existingScript.getDescription(), existingScript.getTags(), 
                existingScript.getAuthor()
            );
            scripts.put(scriptName, updatedScript);
            
            // 保存更新后的脚本文件
            saveScriptFile(updatedScript);
            
            return RepositoryResult.success("脚本更新成功: " + scriptName, scriptName, updatedScript);
            
        } catch (Exception e) {
            return RepositoryResult.failure("脚本更新失败: " + e.getMessage(), e, scriptName);
        }
    }
    
    @Override
    public List<ScriptVersion> getVersionHistory(String scriptName) {
        return versionHistory.getOrDefault(scriptName, new ArrayList<>());
    }
    
    @Override
    public RepositoryResult addDependency(String scriptName, ScriptDependency dependency) {
        try {
            if (!scripts.containsKey(scriptName)) {
                return RepositoryResult.failure("脚本不存在: " + scriptName, null, scriptName);
            }
            
            if (dependency == null) {
                return RepositoryResult.failure("依赖信息不能为空", null, scriptName);
            }
            
            List<ScriptDependency> scriptDependencies = dependencies.computeIfAbsent(scriptName, k -> new ArrayList<>());
            
            // 检查是否已存在同名依赖
            boolean exists = scriptDependencies.stream()
                .anyMatch(d -> d.getName().equals(dependency.getName()));
            
            if (exists) {
                return RepositoryResult.failure("依赖已存在: " + dependency.getName(), null, scriptName);
            }
            
            scriptDependencies.add(dependency);
            saveDependencies(scriptName, scriptDependencies);
            
            return RepositoryResult.success("依赖添加成功: " + dependency.getName(), scriptName, dependency);
            
        } catch (Exception e) {
            return RepositoryResult.failure("依赖添加失败: " + e.getMessage(), e, scriptName);
        }
    }
    
    @Override
    public List<ScriptDependency> getDependencies(String scriptName) {
        return dependencies.getOrDefault(scriptName, new ArrayList<>());
    }
    
    @Override
    public DependencyCheckResult checkDependencies(String scriptName) {
        try {
            List<ScriptDependency> scriptDependencies = dependencies.getOrDefault(scriptName, new ArrayList<>());
            
            if (scriptDependencies.isEmpty()) {
                return DependencyCheckResult.allSatisfied();
            }
            
            List<String> missing = new ArrayList<>();
            List<String> conflicts = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            for (ScriptDependency dep : scriptDependencies) {
                boolean found = false;
                
                switch (dep.getType()) {
                    case SCRIPT:
                        // 检查脚本依赖
                        found = scripts.containsKey(dep.getName());
                        break;
                    case LIBRARY:
                        // 检查库依赖 (简化实现)
                        found = checkLibraryDependency(dep);
                        break;
                    case SYSTEM:
                        // 检查系统依赖
                        found = checkSystemDependency(dep);
                        break;
                    case MODULE:
                        // 检查模块依赖
                        found = checkModuleDependency(dep);
                        break;
                }
                
                if (!found && dep.isRequired()) {
                    missing.add(dep.getName() + " (版本: " + dep.getVersion() + ")");
                } else if (!found && !dep.isRequired()) {
                    warnings.add("可选依赖不可用: " + dep.getName());
                }
            }
            
            return new DependencyCheckResult(missing.isEmpty(), missing, conflicts, warnings);
            
        } catch (Exception e) {
            return DependencyCheckResult.hasIssues(List.of("依赖检查失败: " + e.getMessage()), 
                                                 List.of(), List.of());
        }
    }
    
    @Override
    public RepositoryResult deployScript(String scriptName, String version, String targetPath) {
        try {
            ScriptInfo script = scripts.get(scriptName);
            if (script == null) {
                return RepositoryResult.failure("脚本不存在: " + scriptName, null, scriptName);
            }
            
            // 检查依赖
            DependencyCheckResult depCheck = checkDependencies(scriptName);
            if (!depCheck.isAllSatisfied()) {
                return RepositoryResult.failure("依赖检查失败: " + depCheck, null, scriptName);
            }
            
            // 部署脚本
            Path targetFilePath = Paths.get(targetPath, scriptName);
            Files.writeString(targetFilePath, script.getContent(), StandardCharsets.UTF_8);
            
            // 设置执行权限 (Unix系统)
            if (System.getProperty("os.name").toLowerCase().contains("nix") || 
                System.getProperty("os.name").toLowerCase().contains("nux")) {
                targetFilePath.toFile().setExecutable(true);
            }
            
            return RepositoryResult.success("脚本部署成功: " + scriptName + " -> " + targetPath, 
                                          scriptName, targetFilePath.toString());
            
        } catch (Exception e) {
            return RepositoryResult.failure("脚本部署失败: " + e.getMessage(), e, scriptName);
        }
    }
    
    @Override
    public RepositoryResult updateToLatest(String scriptName, String targetPath) {
        try {
            List<ScriptVersion> versions = getVersionHistory(scriptName);
            if (versions.isEmpty()) {
                return RepositoryResult.failure("脚本版本历史为空: " + scriptName, null, scriptName);
            }
            
            // 获取最新版本
            ScriptVersion latestVersion = versions.get(versions.size() - 1);
            
            return deployScript(scriptName, latestVersion.getVersion(), targetPath);
            
        } catch (Exception e) {
            return RepositoryResult.failure("脚本更新失败: " + e.getMessage(), e, scriptName);
        }
    }
    
    /**
     * 保存脚本文件
     */
    private void saveScriptFile(ScriptInfo scriptInfo) throws IOException {
        Path scriptPath = repositoryPath.resolve(scriptInfo.getName());
        Files.writeString(scriptPath, scriptInfo.getContent(), StandardCharsets.UTF_8);
    }
    
    /**
     * 删除脚本文件
     */
    private void deleteScriptFile(String scriptName) throws IOException {
        Path scriptPath = repositoryPath.resolve(scriptName);
        Files.deleteIfExists(scriptPath);
    }
    
    /**
     * 保存依赖信息
     */
    private void saveDependencies(String scriptName, List<ScriptDependency> deps) throws IOException {
        Path depPath = dependenciesPath.resolve(scriptName + ".dep");
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(depPath))) {
            oos.writeObject(deps);
        }
    }
    
    /**
     * 检查库依赖
     */
    private boolean checkLibraryDependency(ScriptDependency dep) {
        // 简化实现：检查常用库
        List<String> commonLibs = List.of("numpy", "pandas", "requests", "flask", "django");
        return commonLibs.contains(dep.getName().toLowerCase());
    }
    
    /**
     * 检查系统依赖
     */
    private boolean checkSystemDependency(ScriptDependency dep) {
        try {
            Process process = new ProcessBuilder(dep.getName(), "--version").start();
            boolean completed = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("System dependency check failed for: {}", dep.getName(), e);
            return false;
        }
    }
    
    /**
     * 检查模块依赖
     */
    private boolean checkModuleDependency(ScriptDependency dep) {
        // 简化实现：检查Python模块
        try {
            Process process = new ProcessBuilder("python3", "-c", "import " + dep.getName()).start();
            boolean completed = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Module dependency check failed for: {}", dep.getName(), e);
            return false;
        }
    }
    
    /**
     * 增加版本号
     */
    private String incrementVersion(String currentVersion) {
        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 3) {
                int patch = Integer.parseInt(parts[2]) + 1;
                return parts[0] + "." + parts[1] + "." + patch;
            }
        } catch (Exception e) {
            log.warn("Failed to parse version: {}", currentVersion, e);
        }
        return "1.0.1"; // 默认新版本
    }
}
