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


package com.zqzqq.bootkits.scripts.template;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 脚本模板管理器接口
 * 提供模板的创建、存储、检索、更新和删除功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptTemplateManager {
    
    /**
     * 创建模板
     *
     * @param template 模板对象
     * @return 创建结果
     */
    TemplateCreationResult createTemplate(ScriptTemplate template);
    
    /**
     * 根据ID获取模板
     *
     * @param templateId 模板ID
     * @return 模板对象
     */
    ScriptTemplate getTemplate(String templateId);
    
    /**
     * 获取所有模板
     *
     * @return 模板列表
     */
    List<ScriptTemplate> getAllTemplates();
    
    /**
     * 根据条件查询模板
     *
     * @param criteria 查询条件
     * @return 匹配的模板列表
     */
    List<ScriptTemplate> queryTemplates(TemplateQueryCriteria criteria);
    
    /**
     * 更新模板
     *
     * @param template 模板对象
     * @return 更新结果
     */
    TemplateUpdateResult updateTemplate(ScriptTemplate template);
    
    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 删除结果
     */
    TemplateDeletionResult deleteTemplate(String templateId);
    
    /**
     * 批量删除模板
     *
     * @param templateIds 模板ID列表
     * @return 删除结果
     */
    TemplateBatchDeletionResult batchDeleteTemplates(List<String> templateIds);
    
    /**
     * 从模板创建脚本实例
     *
     * @param templateId 模板ID
     * @param variables 变量值映射
     * @param scriptName 生成的脚本名称
     * @return 生成的脚本内容
     */
    ScriptTemplateInstantiationResult instantiateTemplate(String templateId, Map<String, Object> variables, String scriptName);
    
    /**
     * 验证模板
     *
     * @param template 模板对象
     * @return 验证结果
     */
    TemplateValidationResult validateTemplate(ScriptTemplate template);
    
    /**
     * 复制模板
     *
     * @param sourceTemplateId 源模板ID
     * @param newTemplateId 新模板ID
     * @param newName 新模板名称
     * @return 复制结果
     */
    TemplateCopyResult copyTemplate(String sourceTemplateId, String newTemplateId, String newName);
    
    /**
     * 移动模板到分类
     *
     * @param templateId 模板ID
     * @param newCategory 新分类
     * @return 移动结果
     */
    TemplateMoveResult moveTemplateToCategory(String templateId, String newCategory);
    
    /**
     * 搜索模板
     *
     * @param keyword 搜索关键词
     * @param searchInContent 是否在内容中搜索
     * @return 匹配的模板列表
     */
    List<ScriptTemplate> searchTemplates(String keyword, boolean searchInContent);
    
    /**
     * 获取热门模板
     *
     * @param limit 数量限制
     * @return 热门模板列表
     */
    List<ScriptTemplate> getPopularTemplates(int limit);
    
    /**
     * 获取最近修改的模板
     *
     * @param limit 数量限制
     * @return 最近修改的模板列表
     */
    List<ScriptTemplate> getRecentlyModifiedTemplates(int limit);
    
    /**
     * 导出模板
     *
     * @param templateIds 模板ID列表
     * @param format 导出格式 (JSON, XML)
     * @return 导出的内容
     */
    String exportTemplates(List<String> templateIds, String format);
    
    /**
     * 导入模板
     *
     * @param content 导入内容
     * @param format 导入格式 (JSON, XML)
     * @param overwrite 是否覆盖已存在的模板
     * @return 导入结果
     */
    TemplateImportResult importTemplates(String content, String format, boolean overwrite);
    
    /**
     * 获取模板统计信息
     *
     * @return 统计信息
     */
    TemplateStatistics getStatistics();
    
    // 结果类
    
    /**
     * 模板创建结果
     */
    class TemplateCreationResult {
        private boolean success;
        private String message;
        private String templateId;
        
        public TemplateCreationResult() {}
        
        public TemplateCreationResult(boolean success, String message, String templateId) {
            this.success = success;
            this.message = message;
            this.templateId = templateId;
        }
        
        public static TemplateCreationResult success(String message, String templateId) {
            return new TemplateCreationResult(true, message, templateId);
        }
        
        public static TemplateCreationResult failure(String message) {
            return new TemplateCreationResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
    }
    
    /**
     * 模板更新结果
     */
    class TemplateUpdateResult {
        private boolean success;
        private String message;
        private LocalDateTime lastModifiedTime;
        
        public TemplateUpdateResult() {}
        
        public TemplateUpdateResult(boolean success, String message, LocalDateTime lastModifiedTime) {
            this.success = success;
            this.message = message;
            this.lastModifiedTime = lastModifiedTime;
        }
        
        public static TemplateUpdateResult success(String message, LocalDateTime lastModifiedTime) {
            return new TemplateUpdateResult(true, message, lastModifiedTime);
        }
        
        public static TemplateUpdateResult failure(String message) {
            return new TemplateUpdateResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    }
    
    /**
     * 模板删除结果
     */
    class TemplateDeletionResult {
        private boolean success;
        private String message;
        private String deletedTemplateId;
        
        public TemplateDeletionResult() {}
        
        public TemplateDeletionResult(boolean success, String message, String deletedTemplateId) {
            this.success = success;
            this.message = message;
            this.deletedTemplateId = deletedTemplateId;
        }
        
        public static TemplateDeletionResult success(String message, String deletedTemplateId) {
            return new TemplateDeletionResult(true, message, deletedTemplateId);
        }
        
        public static TemplateDeletionResult failure(String message) {
            return new TemplateDeletionResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDeletedTemplateId() { return deletedTemplateId; }
    }
    
    /**
     * 批量删除结果
     */
    class TemplateBatchDeletionResult {
        private boolean success;
        private String message;
        private int deletedCount;
        private int failedCount;
        private List<String> failedTemplateIds;
        
        public TemplateBatchDeletionResult() {}
        
        public TemplateBatchDeletionResult(boolean success, String message, int deletedCount, int failedCount, List<String> failedTemplateIds) {
            this.success = success;
            this.message = message;
            this.deletedCount = deletedCount;
            this.failedCount = failedCount;
            this.failedTemplateIds = failedTemplateIds;
        }
        
        public static TemplateBatchDeletionResult success(int deletedCount, int failedCount, List<String> failedTemplateIds) {
            return new TemplateBatchDeletionResult(true, "批量删除完成", deletedCount, failedCount, failedTemplateIds);
        }
        
        public static TemplateBatchDeletionResult failure(String message) {
            return new TemplateBatchDeletionResult(false, message, 0, 0, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getDeletedCount() { return deletedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getFailedTemplateIds() { return failedTemplateIds; }
    }
    
    /**
     * 模板实例化结果
     */
    class ScriptTemplateInstantiationResult {
        private boolean success;
        private String message;
        private String scriptContent;
        private String scriptName;
        private Map<String, Object> appliedVariables;
        private List<String> warnings;
        
        public ScriptTemplateInstantiationResult() {}
        
        public ScriptTemplateInstantiationResult(boolean success, String message, String scriptContent, String scriptName, 
                                               Map<String, Object> appliedVariables, List<String> warnings) {
            this.success = success;
            this.message = message;
            this.scriptContent = scriptContent;
            this.scriptName = scriptName;
            this.appliedVariables = appliedVariables;
            this.warnings = warnings;
        }
        
        public static ScriptTemplateInstantiationResult success(String scriptContent, String scriptName, 
                                                              Map<String, Object> appliedVariables, List<String> warnings) {
            return new ScriptTemplateInstantiationResult(true, "模板实例化成功", scriptContent, scriptName, appliedVariables, warnings);
        }
        
        public static ScriptTemplateInstantiationResult failure(String message) {
            return new ScriptTemplateInstantiationResult(false, message, null, null, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getScriptContent() { return scriptContent; }
        public String getScriptName() { return scriptName; }
        public Map<String, Object> getAppliedVariables() { return appliedVariables; }
        public List<String> getWarnings() { return warnings; }
    }
    
    /**
     * 模板验证结果
     */
    class TemplateValidationResult {
        private boolean valid;
        private String message;
        private List<String> errors;
        private List<String> warnings;
        
        public TemplateValidationResult() {}
        
        public TemplateValidationResult(boolean valid, String message, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public static TemplateValidationResult success() {
            return new TemplateValidationResult(true, "模板验证通过", null, null);
        }
        
        public static TemplateValidationResult failure(String message, List<String> errors) {
            return new TemplateValidationResult(false, message, errors, null);
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
    
    /**
     * 模板复制结果
     */
    class TemplateCopyResult {
        private boolean success;
        private String message;
        private String newTemplateId;
        private ScriptTemplate copiedTemplate;
        
        public TemplateCopyResult() {}
        
        public TemplateCopyResult(boolean success, String message, String newTemplateId, ScriptTemplate copiedTemplate) {
            this.success = success;
            this.message = message;
            this.newTemplateId = newTemplateId;
            this.copiedTemplate = copiedTemplate;
        }
        
        public static TemplateCopyResult success(String newTemplateId, ScriptTemplate copiedTemplate) {
            return new TemplateCopyResult(true, "模板复制成功", newTemplateId, copiedTemplate);
        }
        
        public static TemplateCopyResult failure(String message) {
            return new TemplateCopyResult(false, message, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getNewTemplateId() { return newTemplateId; }
        public ScriptTemplate getCopiedTemplate() { return copiedTemplate; }
    }
    
    /**
     * 模板移动结果
     */
    class TemplateMoveResult {
        private boolean success;
        private String message;
        private String templateId;
        private String newCategory;
        
        public TemplateMoveResult() {}
        
        public TemplateMoveResult(boolean success, String message, String templateId, String newCategory) {
            this.success = success;
            this.message = message;
            this.templateId = templateId;
            this.newCategory = newCategory;
        }
        
        public static TemplateMoveResult success(String templateId, String newCategory) {
            return new TemplateMoveResult(true, "模板移动成功", templateId, newCategory);
        }
        
        public static TemplateMoveResult failure(String message) {
            return new TemplateMoveResult(false, message, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getTemplateId() { return templateId; }
        public String getNewCategory() { return newCategory; }
    }
    
    /**
     * 模板导入结果
     */
    class TemplateImportResult {
        private boolean success;
        private String message;
        private int importedCount;
        private int updatedCount;
        private int failedCount;
        private List<String> failedTemplateIds;
        private List<String> importedTemplateIds;
        
        public TemplateImportResult() {}
        
        public TemplateImportResult(boolean success, String message, int importedCount, int updatedCount, 
                                  int failedCount, List<String> failedTemplateIds, List<String> importedTemplateIds) {
            this.success = success;
            this.message = message;
            this.importedCount = importedCount;
            this.updatedCount = updatedCount;
            this.failedCount = failedCount;
            this.failedTemplateIds = failedTemplateIds;
            this.importedTemplateIds = importedTemplateIds;
        }
        
        public static TemplateImportResult success(int importedCount, int updatedCount, int failedCount, 
                                                 List<String> failedTemplateIds, List<String> importedTemplateIds) {
            return new TemplateImportResult(true, "模板导入成功", importedCount, updatedCount, failedCount, failedTemplateIds, importedTemplateIds);
        }
        
        public static TemplateImportResult failure(String message) {
            return new TemplateImportResult(false, message, 0, 0, 0, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getImportedCount() { return importedCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getFailedTemplateIds() { return failedTemplateIds; }
        public List<String> getImportedTemplateIds() { return importedTemplateIds; }
    }
    
    /**
     * 模板统计信息
     */
    class TemplateStatistics {
        private int totalTemplates;
        private int activeTemplates;
        private int inactiveTemplates;
        private int totalUsages;
        private Map<String, Integer> templatesByCategory;
        private Map<String, Integer> templatesByType;
        private Map<String, Integer> mostUsedTemplates;
        private LocalDateTime lastActivityTime;
        
        public TemplateStatistics() {}
        
        public TemplateStatistics(int totalTemplates, int activeTemplates, int inactiveTemplates, int totalUsages,
                                Map<String, Integer> templatesByCategory, Map<String, Integer> templatesByType,
                                Map<String, Integer> mostUsedTemplates, LocalDateTime lastActivityTime) {
            this.totalTemplates = totalTemplates;
            this.activeTemplates = activeTemplates;
            this.inactiveTemplates = inactiveTemplates;
            this.totalUsages = totalUsages;
            this.templatesByCategory = templatesByCategory;
            this.templatesByType = templatesByType;
            this.mostUsedTemplates = mostUsedTemplates;
            this.lastActivityTime = lastActivityTime;
        }
        
        public int getTotalTemplates() { return totalTemplates; }
        public int getActiveTemplates() { return activeTemplates; }
        public int getInactiveTemplates() { return inactiveTemplates; }
        public int getTotalUsages() { return totalUsages; }
        public Map<String, Integer> getTemplatesByCategory() { return templatesByCategory; }
        public Map<String, Integer> getTemplatesByType() { return templatesByType; }
        public Map<String, Integer> getMostUsedTemplates() { return mostUsedTemplates; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
    }
}