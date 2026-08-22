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


package com.zqzqq.bootkits.scripts.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 脚本仓库接口
 * 提供脚本的版本控制、依赖管理和部署更新功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptRepository {
    
    /**
     * 注册脚本到仓库
     *
     * @param scriptInfo 脚本信息
     * @return 注册结果
     */
    RepositoryResult registerScript(ScriptInfo scriptInfo);
    
    /**
     * 获取脚本信息
     *
     * @param scriptName 脚本名称
     * @return 脚本信息
     */
    Optional<ScriptInfo> getScript(String scriptName);
    
    /**
     * 获取所有脚本
     *
     * @return 脚本列表
     */
    List<ScriptInfo> getAllScripts();
    
    /**
     * 删除脚本
     *
     * @param scriptName 脚本名称
     * @return 删除结果
     */
    RepositoryResult deleteScript(String scriptName);
    
    /**
     * 更新脚本
     *
     * @param scriptName 脚本名称
     * @param scriptContent 脚本内容
     * @return 更新结果
     */
    RepositoryResult updateScript(String scriptName, String scriptContent);
    
    /**
     * 获取脚本版本历史
     *
     * @param scriptName 脚本名称
     * @return 版本历史
     */
    List<ScriptVersion> getVersionHistory(String scriptName);
    
    /**
     * 添加脚本依赖
     *
     * @param scriptName 脚本名称
     * @param dependency 依赖信息
     * @return 添加结果
     */
    RepositoryResult addDependency(String scriptName, ScriptDependency dependency);
    
    /**
     * 获取脚本依赖
     *
     * @param scriptName 脚本名称
     * @return 依赖列表
     */
    List<ScriptDependency> getDependencies(String scriptName);
    
    /**
     * 检查脚本依赖是否满足
     *
     * @param scriptName 脚本名称
     * @return 检查结果
     */
    DependencyCheckResult checkDependencies(String scriptName);
    
    /**
     * 部署脚本
     *
     * @param scriptName 脚本名称
     * @param version 版本号
     * @param targetPath 目标路径
     * @return 部署结果
     */
    RepositoryResult deployScript(String scriptName, String version, String targetPath);
    
    /**
     * 更新脚本到最新版本
     *
     * @param scriptName 脚本名称
     * @param targetPath 目标路径
     * @return 更新结果
     */
    RepositoryResult updateToLatest(String scriptName, String targetPath);
    
    /**
     * 脚本信息类
     */
    class ScriptInfo {
        private final String name;
        private final String content;
        private final ScriptType type;
        private final String version;
        private final LocalDateTime createdAt;
        private final LocalDateTime lastModified;
        private final String description;
        private final List<String> tags;
        private final String author;
        private final long size;
        
        public ScriptInfo(String name, String content, ScriptType type, String version,
                         String description, List<String> tags, String author) {
            this.name = name;
            this.content = content;
            this.type = type;
            this.version = version;
            this.createdAt = LocalDateTime.now();
            this.lastModified = LocalDateTime.now();
            this.description = description;
            this.tags = tags != null ? tags : List.of();
            this.author = author;
            this.size = content != null ? content.length() : 0;
        }
        
        // Getters
        public String getName() { return name; }
        public String getContent() { return content; }
        public ScriptType getType() { return type; }
        public String getVersion() { return version; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastModified() { return lastModified; }
        public String getDescription() { return description; }
        public List<String> getTags() { return tags; }
        public String getAuthor() { return author; }
        public long getSize() { return size; }
        
        @Override
        public String toString() {
            return String.format("ScriptInfo{name='%s', type=%s, version='%s', author='%s', size=%d}",
                    name, type, version, author, size);
        }
    }
    
    /**
     * 脚本版本信息
     */
    class ScriptVersion {
        private final String version;
        private final String content;
        private final LocalDateTime createdAt;
        private final String author;
        private final String changeLog;
        private final long size;
        
        public ScriptVersion(String version, String content, String author, String changeLog) {
            this.version = version;
            this.content = content;
            this.createdAt = LocalDateTime.now();
            this.author = author;
            this.changeLog = changeLog;
            this.size = content != null ? content.length() : 0;
        }
        
        // Getters
        public String getVersion() { return version; }
        public String getContent() { return content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getAuthor() { return author; }
        public String getChangeLog() { return changeLog; }
        public long getSize() { return size; }
        
        @Override
        public String toString() {
            return String.format("ScriptVersion{version='%s', author='%s', size=%d}",
                    version, author, size);
        }
    }
    
    /**
     * 脚本依赖信息
     */
    class ScriptDependency {
        private final String name;
        private final String version;
        private final DependencyType type;
        private final boolean required;
        private final String description;
        
        public ScriptDependency(String name, String version, DependencyType type, boolean required, String description) {
            this.name = name;
            this.version = version;
            this.type = type;
            this.required = required;
            this.description = description;
        }
        
        // Getters
        public String getName() { return name; }
        public String getVersion() { return version; }
        public DependencyType getType() { return type; }
        public boolean isRequired() { return required; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("ScriptDependency{name='%s', version='%s', type=%s, required=%s}",
                    name, version, type, required);
        }
    }
    
    /**
     * 依赖类型
     */
    enum DependencyType {
        SCRIPT,      // 脚本依赖
        LIBRARY,     // 库依赖
        SYSTEM,      // 系统依赖
        MODULE       // 模块依赖
    }
    
    /**
     * 仓库操作结果
     */
    class RepositoryResult {
        private final boolean success;
        private final String message;
        private final Exception exception;
        private final String scriptName;
        private final Object data;
        
        public RepositoryResult(boolean success, String message, Exception exception, String scriptName, Object data) {
            this.success = success;
            this.message = message;
            this.exception = exception;
            this.scriptName = scriptName;
            this.data = data;
        }
        
        // Factory methods
        public static RepositoryResult success(String message, String scriptName, Object data) {
            return new RepositoryResult(true, message, null, scriptName, data);
        }
        
        public static RepositoryResult success(String message, String scriptName) {
            return success(message, scriptName, null);
        }
        
        public static RepositoryResult failure(String message, Exception exception, String scriptName) {
            return new RepositoryResult(false, message, exception, scriptName, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
        public String getScriptName() { return scriptName; }
        public Object getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("RepositoryResult{success=%s, message='%s', scriptName='%s'}",
                    success, message, scriptName);
        }
    }
    
    /**
     * 依赖检查结果
     */
    class DependencyCheckResult {
        private final boolean allSatisfied;
        private final List<String> missingDependencies;
        private final List<String> versionConflicts;
        private final List<String> warnings;
        
        public DependencyCheckResult(boolean allSatisfied, List<String> missingDependencies,
                                   List<String> versionConflicts, List<String> warnings) {
            this.allSatisfied = allSatisfied;
            this.missingDependencies = missingDependencies != null ? missingDependencies : List.of();
            this.versionConflicts = versionConflicts != null ? versionConflicts : List.of();
            this.warnings = warnings != null ? warnings : List.of();
        }
        
        // Factory methods
        public static DependencyCheckResult allSatisfied() {
            return new DependencyCheckResult(true, List.of(), List.of(), List.of());
        }
        
        public static DependencyCheckResult hasIssues(List<String> missing, List<String> conflicts, List<String> warnings) {
            return new DependencyCheckResult(false, missing, conflicts, warnings);
        }
        
        // Getters
        public boolean isAllSatisfied() { return allSatisfied; }
        public List<String> getMissingDependencies() { return missingDependencies; }
        public List<String> getVersionConflicts() { return versionConflicts; }
        public List<String> getWarnings() { return warnings; }
        
        @Override
        public String toString() {
            return String.format("DependencyCheckResult{allSatisfied=%s, missing=%d, conflicts=%d, warnings=%d}",
                    allSatisfied, missingDependencies.size(), versionConflicts.size(), warnings.size());
        }
    }
}