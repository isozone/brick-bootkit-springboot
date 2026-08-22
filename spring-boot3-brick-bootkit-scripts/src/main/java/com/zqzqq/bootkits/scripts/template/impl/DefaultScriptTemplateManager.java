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


package com.zqzqq.bootkits.scripts.template.impl;

import com.zqzqq.bootkits.scripts.core.*;
import com.zqzqq.bootkits.scripts.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 脚本模板管理器默认实现
 * 提供完整的模板管理功能，包括存储、检索、更新和实例化
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptTemplateManager implements ScriptTemplateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptTemplateManager.class);
    
    // 模板存储
    private final Map<String, ScriptTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, List<String>> categoryTemplates = new ConcurrentHashMap<>();
    private final Map<ScriptType, List<String>> typeTemplates = new ConcurrentHashMap<>();
    
    // 使用统计
    private final AtomicLong totalUsages = new AtomicLong(0);
    private final Map<String, Long> templateUsageStats = new ConcurrentHashMap<>();
    
    // 模板引擎
    private final TemplateEngine templateEngine;
    
    // ID生成器
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /**
     * 构造函数
     */
    public DefaultScriptTemplateManager() {
        this.templateEngine = new DefaultTemplateEngine();
        initializeDefaultTemplates();
    }
    
    /**
     * 构造函数
     *
     * @param templateEngine 模板引擎
     */
    public DefaultScriptTemplateManager(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine != null ? templateEngine : new DefaultTemplateEngine();
        initializeDefaultTemplates();
    }
    
    @Override
    public TemplateCreationResult createTemplate(ScriptTemplate template) {
        if (template == null) {
            return TemplateCreationResult.failure("模板对象不能为空");
        }
        
        try {
            // 验证模板
            TemplateValidationResult validation = validateTemplate(template);
            if (!validation.isValid()) {
                return TemplateCreationResult.failure("模板验证失败: " + validation.getMessage());
            }
            
            // 生成模板ID
            String templateId = generateTemplateId(template);
            if (templates.containsKey(templateId)) {
                return TemplateCreationResult.failure("模板ID已存在: " + templateId);
            }
            
            // 设置模板属性
            template.setTemplateId(templateId);
            template.setCreatedTime(LocalDateTime.now());
            template.setLastModifiedTime(LocalDateTime.now());
            template.setUsageCount(0);
            
            // 存储模板
            templates.put(templateId, template);
            
            // 更新索引
            updateIndexes(template);
            
            logger.info("模板创建成功: {} - {}", templateId, template.getName());
            return TemplateCreationResult.success("模板创建成功", templateId);
            
        } catch (Exception e) {
            logger.error("模板创建失败", e);
            return TemplateCreationResult.failure("模板创建失败: " + e.getMessage());
        }
    }
    
    @Override
    public ScriptTemplate getTemplate(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return null;
        }
        
        ScriptTemplate template = templates.get(templateId);
        if (template != null && template.isActive()) {
            return template;
        }
        
        return null;
    }
    
    @Override
    public List<ScriptTemplate> getAllTemplates() {
        return templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .sorted(Comparator.comparing(ScriptTemplate::getName))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ScriptTemplate> queryTemplates(TemplateQueryCriteria criteria) {
        if (criteria == null) {
            return getAllTemplates();
        }
        
        List<ScriptTemplate> filtered = templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .filter(template -> matchesCriteria(template, criteria))
                .collect(Collectors.toList());
        
        // 排序
        if (criteria.getSortBy() != null) {
            Comparator<ScriptTemplate> comparator = createSortComparator(criteria.getSortBy());
            if (criteria.getSortOrder() == TemplateQueryCriteria.SortOrder.DESC) {
                comparator = comparator.reversed();
            }
            filtered.sort(comparator);
        }
        
        // 分页
        int fromIndex = Math.min(criteria.getOffset(), filtered.size());
        int toIndex = Math.min(criteria.getOffset() + criteria.getLimit(), filtered.size());
        
        return filtered.subList(fromIndex, toIndex);
    }
    
    @Override
    public TemplateUpdateResult updateTemplate(ScriptTemplate template) {
        if (template == null || template.getTemplateId() == null) {
            return TemplateUpdateResult.failure("模板对象或模板ID不能为空");
        }
        
        try {
            String templateId = template.getTemplateId();
            ScriptTemplate existingTemplate = templates.get(templateId);
            
            if (existingTemplate == null) {
                return TemplateUpdateResult.failure("模板不存在: " + templateId);
            }
            
            // 验证模板
            TemplateValidationResult validation = validateTemplate(template);
            if (!validation.isValid()) {
                return TemplateUpdateResult.failure("模板验证失败: " + validation.getMessage());
            }
            
            // 更新索引
            removeFromIndexes(existingTemplate);
            
            // 更新模板属性
            template.setLastModifiedTime(LocalDateTime.now());
            if (template.getCreatedTime() == null) {
                template.setCreatedTime(existingTemplate.getCreatedTime());
            }
            if (template.getUsageCount() == 0) {
                template.setUsageCount(existingTemplate.getUsageCount());
            }
            
            // 存储更新后的模板
            templates.put(templateId, template);
            
            // 更新索引
            updateIndexes(template);
            
            logger.info("模板更新成功: {}", templateId);
            return TemplateUpdateResult.success("模板更新成功", template.getLastModifiedTime());
            
        } catch (Exception e) {
            logger.error("模板更新失败", e);
            return TemplateUpdateResult.failure("模板更新失败: " + e.getMessage());
        }
    }
    
    @Override
    public TemplateDeletionResult deleteTemplate(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return TemplateDeletionResult.failure("模板ID不能为空");
        }
        
        try {
            ScriptTemplate template = templates.get(templateId);
            if (template == null) {
                return TemplateDeletionResult.failure("模板不存在: " + templateId);
            }
            
            // 软删除 - 标记为非活跃
            template.setActive(false);
            template.setLastModifiedTime(LocalDateTime.now());
            
            // 从索引中移除
            removeFromIndexes(template);
            
            logger.info("模板删除成功: {}", templateId);
            return TemplateDeletionResult.success("模板删除成功", templateId);
            
        } catch (Exception e) {
            logger.error("模板删除失败", e);
            return TemplateDeletionResult.failure("模板删除失败: " + e.getMessage());
        }
    }
    
    @Override
    public TemplateBatchDeletionResult batchDeleteTemplates(List<String> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return TemplateBatchDeletionResult.failure("模板ID列表不能为空");
        }
        
        int deletedCount = 0;
        int failedCount = 0;
        List<String> failedTemplateIds = new ArrayList<>();
        
        for (String templateId : templateIds) {
            try {
                TemplateDeletionResult result = deleteTemplate(templateId);
                if (result.isSuccess()) {
                    deletedCount++;
                } else {
                    failedCount++;
                    failedTemplateIds.add(templateId);
                }
            } catch (Exception e) {
                failedCount++;
                failedTemplateIds.add(templateId);
                logger.warn("删除模板失败: {}", templateId, e);
            }
        }
        
        logger.info("批量删除完成: 成功={}, 失败={}", deletedCount, failedCount);
        return TemplateBatchDeletionResult.success(deletedCount, failedCount, failedTemplateIds);
    }
    
    @Override
    public ScriptTemplateInstantiationResult instantiateTemplate(String templateId, Map<String, Object> variables, String scriptName) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return ScriptTemplateInstantiationResult.failure("模板ID不能为空");
        }
        
        try {
            ScriptTemplate template = getTemplate(templateId);
            if (template == null) {
                return ScriptTemplateInstantiationResult.failure("模板不存在: " + templateId);
            }
            
            // 验证变量
            Map<String, Object> validatedVariables = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            
            for (ScriptTemplate.TemplateVariable varDef : template.getVariables()) {
                String varName = varDef.getName();
                Object value = variables != null ? variables.get(varName) : null;
                
                ScriptTemplate.ValidationResult validation = template.validateVariable(varName, value);
                if (!validation.isValid()) {
                    return ScriptTemplateInstantiationResult.failure("变量验证失败: " + validation.getMessage());
                }
                
                // 使用校正后的值
                Object finalValue = validation.getCorrectedValue() != null ? 
                    validation.getCorrectedValue() : value;
                validatedVariables.put(varName, finalValue);
                
                if (validation.getMessage() != null && !validation.getMessage().isEmpty()) {
                    warnings.add(varName + ": " + validation.getMessage());
                }
            }
            
            // 渲染模板
            TemplateEngine.RenderResult renderResult = templateEngine.renderTemplate(
                template.getScriptContent(), validatedVariables, template);
            
            if (!renderResult.isSuccess()) {
                return ScriptTemplateInstantiationResult.failure("模板渲染失败: " + 
                    String.join(", ", renderResult.getErrors()));
            }
            
            // 更新使用统计
            template.incrementUsage();
            templateUsageStats.merge(templateId, 1L, Long::sum);
            totalUsages.incrementAndGet();
            
            logger.info("模板实例化成功: {} -> {}", templateId, scriptName);
            return ScriptTemplateInstantiationResult.success(
                renderResult.getRenderedContent(), scriptName, 
                renderResult.getUsedVariables(), warnings);
            
        } catch (Exception e) {
            logger.error("模板实例化失败", e);
            return ScriptTemplateInstantiationResult.failure("模板实例化失败: " + e.getMessage());
        }
    }
    
    @Override
    public TemplateValidationResult validateTemplate(ScriptTemplate template) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (template == null) {
            errors.add("模板对象不能为空");
            return TemplateValidationResult.failure("模板验证失败", errors);
        }
        
        // 验证基本字段
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            errors.add("模板名称不能为空");
        }
        
        if (template.getScriptContent() == null || template.getScriptContent().trim().isEmpty()) {
            errors.add("脚本内容不能为空");
        }
        
        if (template.getScriptType() == null) {
            errors.add("脚本类型不能为空");
        }
        
        // 验证变量定义
        for (ScriptTemplate.TemplateVariable varDef : template.getVariables()) {
            if (varDef.getName() == null || varDef.getName().trim().isEmpty()) {
                errors.add("变量名称不能为空");
            }
            
            if (varDef.getType() == null) {
                errors.add("变量类型不能为空: " + varDef.getName());
            }
            
            // 验证允许的值列表
            if (varDef.getAllowedValues() != null) {
                for (String allowedValue : varDef.getAllowedValues()) {
                    if (allowedValue == null || allowedValue.trim().isEmpty()) {
                        errors.add("变量 '" + varDef.getName() + "' 的允许值不能为空");
                    }
                }
            }
        }
        
        // 验证脚本内容
        if (template.getScriptContent() != null) {
            TemplateValidationResult contentValidation = templateEngine.validateTemplateContent(
                template.getScriptContent(), template);
            
            if (!contentValidation.isValid()) {
                errors.addAll(contentValidation.getErrors());
            }
            warnings.addAll(contentValidation.getWarnings());
        }
        
        if (!errors.isEmpty()) {
            return TemplateValidationResult.failure("模板验证失败", errors);
        }
        
        return TemplateValidationResult.success();
    }
    
    @Override
    public TemplateCopyResult copyTemplate(String sourceTemplateId, String newTemplateId, String newName) {
        if (sourceTemplateId == null || sourceTemplateId.trim().isEmpty()) {
            return TemplateCopyResult.failure("源模板ID不能为空");
        }
        
        if (newTemplateId == null || newTemplateId.trim().isEmpty()) {
            return TemplateCopyResult.failure("新模板ID不能为空");
        }
        
        try {
            ScriptTemplate sourceTemplate = getTemplate(sourceTemplateId);
            if (sourceTemplate == null) {
                return TemplateCopyResult.failure("源模板不存在: " + sourceTemplateId);
            }
            
            if (templates.containsKey(newTemplateId)) {
                return TemplateCopyResult.failure("新模板ID已存在: " + newTemplateId);
            }
            
            // 创建副本
            ScriptTemplate copiedTemplate = new ScriptTemplate();
            copiedTemplate.setName(newName != null ? newName : sourceTemplate.getName() + " (副本)");
            copiedTemplate.setDescription(sourceTemplate.getDescription());
            copiedTemplate.setScriptContent(sourceTemplate.getScriptContent());
            copiedTemplate.setScriptType(sourceTemplate.getScriptType());
            copiedTemplate.setCategory(sourceTemplate.getCategory());
            copiedTemplate.setTags(sourceTemplate.getTags());
            copiedTemplate.setAuthor(sourceTemplate.getAuthor());
            copiedTemplate.setVersion(sourceTemplate.getVersion());
            copiedTemplate.setMetadata(new HashMap<>(sourceTemplate.getMetadata()));
            
            // 复制变量定义
            for (ScriptTemplate.TemplateVariable varDef : sourceTemplate.getVariables()) {
                ScriptTemplate.TemplateVariable copiedVar = new ScriptTemplate.TemplateVariable();
                copiedVar.setName(varDef.getName());
                copiedVar.setType(varDef.getType());
                copiedVar.setDescription(varDef.getDescription());
                copiedVar.setDefaultValue(varDef.getDefaultValue());
                copiedVar.setRequired(varDef.isRequired());
                copiedVar.setPattern(varDef.getPattern());
                copiedVar.setAllowedValues(new ArrayList<>(varDef.getAllowedValues()));
                copiedTemplate.addVariable(copiedVar);
            }
            
            // 创建新模板
            TemplateCreationResult creationResult = createTemplate(copiedTemplate);
            if (!creationResult.isSuccess()) {
                return TemplateCopyResult.failure("复制模板失败: " + creationResult.getMessage());
            }
            
            logger.info("模板复制成功: {} -> {}", sourceTemplateId, newTemplateId);
            return TemplateCopyResult.success(newTemplateId, copiedTemplate);
            
        } catch (Exception e) {
            logger.error("模板复制失败", e);
            return TemplateCopyResult.failure("模板复制失败: " + e.getMessage());
        }
    }
    
    @Override
    public TemplateMoveResult moveTemplateToCategory(String templateId, String newCategory) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return TemplateMoveResult.failure("模板ID不能为空");
        }
        
        try {
            ScriptTemplate template = getTemplate(templateId);
            if (template == null) {
                return TemplateMoveResult.failure("模板不存在: " + templateId);
            }
            
            String oldCategory = template.getCategory();
            template.setCategory(newCategory);
            template.setLastModifiedTime(LocalDateTime.now());
            
            // 更新索引
            removeFromIndexes(template);
            updateIndexes(template);
            
            logger.info("模板分类移动成功: {} {} -> {}", templateId, oldCategory, newCategory);
            return TemplateMoveResult.success(templateId, newCategory);
            
        } catch (Exception e) {
            logger.error("模板分类移动失败", e);
            return TemplateMoveResult.failure("模板分类移动失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<ScriptTemplate> searchTemplates(String keyword, boolean searchInContent) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTemplates();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        return templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .filter(template -> {
                    // 搜索名称
                    if (template.getName() != null && template.getName().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    // 搜索描述
                    if (template.getDescription() != null && 
                        template.getDescription().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    // 搜索分类
                    if (template.getCategory() != null && 
                        template.getCategory().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    // 搜索标签
                    if (template.getTags() != null && 
                        template.getTags().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    // 搜索作者
                    if (template.getAuthor() != null && 
                        template.getAuthor().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    // 搜索脚本内容
                    if (searchInContent && template.getScriptContent() != null && 
                        template.getScriptContent().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    
                    return false;
                })
                .sorted(Comparator.comparing(ScriptTemplate::getName))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ScriptTemplate> getPopularTemplates(int limit) {
        return templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .sorted(Comparator.comparing(ScriptTemplate::getUsageCount).reversed()
                        .thenComparing(ScriptTemplate::getName))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ScriptTemplate> getRecentlyModifiedTemplates(int limit) {
        return templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .sorted(Comparator.comparing(ScriptTemplate::getLastModifiedTime).reversed()
                        .thenComparing(ScriptTemplate::getName))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public String exportTemplates(List<String> templateIds, String format) {
        if (templateIds == null || templateIds.isEmpty()) {
            return "";
        }
        
        if ("json".equalsIgnoreCase(format)) {
            return exportAsJson(templateIds);
        } else if ("xml".equalsIgnoreCase(format)) {
            return exportAsXml(templateIds);
        } else {
            return exportAsJson(templateIds); // 默认使用JSON格式
        }
    }
    
    @Override
    public TemplateImportResult importTemplates(String content, String format, boolean overwrite) {
        if (content == null || content.trim().isEmpty()) {
            return TemplateImportResult.failure("导入内容不能为空");
        }
        
        try {
            List<ScriptTemplate> templatesToImport;
            
            if ("json".equalsIgnoreCase(format)) {
                templatesToImport = importFromJson(content);
            } else if ("xml".equalsIgnoreCase(format)) {
                templatesToImport = importFromXml(content);
            } else {
                templatesToImport = importFromJson(content); // 默认使用JSON格式
            }
            
            int importedCount = 0;
            int updatedCount = 0;
            int failedCount = 0;
            List<String> failedTemplateIds = new ArrayList<>();
            List<String> importedTemplateIds = new ArrayList<>();
            
            for (ScriptTemplate template : templatesToImport) {
                try {
                    String templateId = template.getTemplateId();
                    if (templateId == null) {
                        templateId = generateTemplateId(template);
                        template.setTemplateId(templateId);
                    }
                    
                    if (templates.containsKey(templateId) && !overwrite) {
                        // 跳过已存在的模板
                        continue;
                    }
                    
                    TemplateCreationResult result = createTemplate(template);
                    if (result.isSuccess()) {
                        if (templates.containsKey(templateId)) {
                            updatedCount++;
                        } else {
                            importedCount++;
                        }
                        importedTemplateIds.add(templateId);
                    } else {
                        failedCount++;
                        failedTemplateIds.add(templateId);
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    logger.warn("导入模板失败", e);
                }
            }
            
            logger.info("模板导入完成: 导入={}, 更新={}, 失败={}", importedCount, updatedCount, failedCount);
            return TemplateImportResult.success(importedCount, updatedCount, failedCount, failedTemplateIds, importedTemplateIds);
            
        } catch (Exception e) {
            logger.error("模板导入失败", e);
            return TemplateImportResult.failure("模板导入失败: " + e.getMessage());
        }
    }
    
    @Override
    public TemplateStatistics getStatistics() {
        int totalTemplates = (int) templates.values().stream().filter(ScriptTemplate::isActive).count();
        int activeTemplates = totalTemplates;
        int inactiveTemplates = (int) templates.values().stream().filter(t -> !t.isActive()).count();
        
        Map<String, Integer> templatesByCategory = templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                    ScriptTemplate::getCategory,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ));
        
        Map<String, Integer> templatesByType = templates.values().stream()
                .filter(ScriptTemplate::isActive)
                .collect(Collectors.groupingBy(
                    t -> t.getScriptType().name(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ));
        
        Map<String, Integer> mostUsedTemplates = templateUsageStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ));
        
        LocalDateTime lastActivityTime = templates.values().stream()
                .map(ScriptTemplate::getLastModifiedTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        return new TemplateStatistics(
            totalTemplates,
            activeTemplates,
            inactiveTemplates,
            Integer.parseInt(totalUsages.get()+""),
            templatesByCategory,
            templatesByType,
            mostUsedTemplates,
            lastActivityTime
        );
    }
    
    // 私有方法
    
    private void initializeDefaultTemplates() {
        // 创建一些示例模板
        ScriptTemplate helloTemplate = new ScriptTemplate("hello-template", "Hello World模板", "生成Hello World脚本", 
            "echo \"Hello ${name:World}!\"", ScriptType.SHELL);
        helloTemplate.addVariable(new ScriptTemplate.TemplateVariable("name", ScriptTemplate.VariableType.STRING, "姓名", "World", false));
        helloTemplate.setCategory("示例");
        helloTemplate.setAuthor("系统");
        createTemplate(helloTemplate);
        
        ScriptTemplate dateTemplate = new ScriptTemplate("date-template", "日期脚本模板", "生成显示当前日期的脚本", 
            "#!/bin/bash\necho \"当前日期: $(date '+%Y-%m-%d %H:%M:%S')\"\necho \"时区: $(date '+%Z')\"", ScriptType.SHELL);
        dateTemplate.setCategory("系统");
        dateTemplate.setAuthor("系统");
        createTemplate(dateTemplate);
        
        logger.info("默认模板初始化完成");
    }
    
    private String generateTemplateId(ScriptTemplate template) {
        String baseId = template.getName().toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        return baseId + "-" + idGenerator.getAndIncrement();
    }
    
    private boolean matchesCriteria(ScriptTemplate template, TemplateQueryCriteria criteria) {
        // 名称模式匹配
        if (criteria.hasNamePattern() && !matchesPattern(template.getName(), criteria.getNamePattern())) {
            return false;
        }
        
        // 描述模式匹配
        if (criteria.hasDescriptionPattern() && !matchesPattern(template.getDescription(), criteria.getDescriptionPattern())) {
            return false;
        }
        
        // 分类匹配
        if (criteria.hasCategory() && !criteria.getCategory().equals(template.getCategory())) {
            return false;
        }
        
        // 标签匹配
        if (criteria.hasTags()) {
            Set<String> templateTags = parseTags(template.getTags());
            if (!templateTags.containsAll(criteria.getTags())) {
                return false;
            }
        }
        
        // 作者匹配
        if (criteria.hasAuthor() && !criteria.getAuthor().equals(template.getAuthor())) {
            return false;
        }
        
        // 脚本类型匹配
        if (criteria.hasScriptType() && !criteria.getScriptType().equals(template.getScriptType().name())) {
            return false;
        }
        
        // 活跃状态匹配
        if (criteria.getIsActive() != null && criteria.getIsActive() != template.isActive()) {
            return false;
        }
        
        // 使用次数范围匹配
        if (criteria.hasUsageCountRange()) {
            long usageCount = template.getUsageCount();
            if (criteria.getMinUsageCount() != null && usageCount < criteria.getMinUsageCount()) {
                return false;
            }
            if (criteria.getMaxUsageCount() != null && usageCount > criteria.getMaxUsageCount()) {
                return false;
            }
        }
        
        // 创建时间范围匹配
        if (criteria.hasCreatedTimeRange()) {
            LocalDateTime createdTime = template.getCreatedTime();
            if (criteria.getCreatedAfter() != null && createdTime.isBefore(criteria.getCreatedAfter())) {
                return false;
            }
            if (criteria.getCreatedBefore() != null && createdTime.isAfter(criteria.getCreatedBefore())) {
                return false;
            }
        }
        
        // 修改时间范围匹配
        if (criteria.hasModifiedTimeRange()) {
            LocalDateTime modifiedTime = template.getLastModifiedTime();
            if (criteria.getModifiedAfter() != null && modifiedTime.isBefore(criteria.getModifiedAfter())) {
                return false;
            }
            if (criteria.getModifiedBefore() != null && modifiedTime.isAfter(criteria.getModifiedBefore())) {
                return false;
            }
        }
        
        // 模板ID列表匹配
        if (criteria.hasTemplateIds() && !criteria.getTemplateIds().contains(template.getTemplateId())) {
            return false;
        }
        
        return true;
    }
    
    private boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        
        // 简单的通配符匹配
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return text.matches("(?i)" + regex);
    }
    
    private Set<String> parseTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
    
    private Comparator<ScriptTemplate> createSortComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "name": return Comparator.comparing(ScriptTemplate::getName);
            case "createdtime": return Comparator.comparing(ScriptTemplate::getCreatedTime);
            case "lastmodifiedtime": return Comparator.comparing(ScriptTemplate::getLastModifiedTime);
            case "usagecount": return Comparator.comparing(ScriptTemplate::getUsageCount);
            case "category": return Comparator.comparing(ScriptTemplate::getCategory);
            case "scripttype": return Comparator.comparing(t -> t.getScriptType().name());
            default: return Comparator.comparing(ScriptTemplate::getName);
        }
    }
    
    private void updateIndexes(ScriptTemplate template) {
        // 更新分类索引
        if (template.getCategory() != null) {
            categoryTemplates.computeIfAbsent(template.getCategory(), k -> new ArrayList<>()).add(template.getTemplateId());
        }
        
        // 更新类型索引
        typeTemplates.computeIfAbsent(template.getScriptType(), k -> new ArrayList<>()).add(template.getTemplateId());
    }
    
    private void removeFromIndexes(ScriptTemplate template) {
        // 从分类索引中移除
        if (template.getCategory() != null) {
            List<String> categoryList = categoryTemplates.get(template.getCategory());
            if (categoryList != null) {
                categoryList.remove(template.getTemplateId());
                if (categoryList.isEmpty()) {
                    categoryTemplates.remove(template.getCategory());
                }
            }
        }
        
        // 从类型索引中移除
        List<String> typeList = typeTemplates.get(template.getScriptType());
        if (typeList != null) {
            typeList.remove(template.getTemplateId());
            if (typeList.isEmpty()) {
                typeTemplates.remove(template.getScriptType());
            }
        }
    }
    
    private String exportAsJson(List<String> templateIds) {
        List<ScriptTemplate> templatesToExport = templateIds.stream()
                .map(templates::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // 这里应该使用JSON库如Jackson或Gson进行序列化
        // 简化实现，返回基本格式
        StringBuilder json = new StringBuilder("{\n  \"templates\": [\n");
        
        for (int i = 0; i < templatesToExport.size(); i++) {
            ScriptTemplate template = templatesToExport.get(i);
            json.append("    {\n");
            json.append("      \"templateId\": \"").append(template.getTemplateId()).append("\",\n");
            json.append("      \"name\": \"").append(escapeJson(template.getName())).append("\",\n");
            json.append("      \"description\": \"").append(escapeJson(template.getDescription())).append("\",\n");
            json.append("      \"scriptContent\": \"").append(escapeJson(template.getScriptContent())).append("\",\n");
            json.append("      \"scriptType\": \"").append(template.getScriptType()).append("\",\n");
            json.append("      \"category\": \"").append(escapeJson(template.getCategory())).append("\",\n");
            json.append("      \"tags\": \"").append(escapeJson(template.getTags())).append("\",\n");
            json.append("      \"author\": \"").append(escapeJson(template.getAuthor())).append("\",\n");
            json.append("      \"version\": \"").append(escapeJson(template.getVersion())).append("\"\n");
            json.append("    }");
            if (i < templatesToExport.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString();
    }
    
    private String exportAsXml(List<String> templateIds) {
        // 简化实现，返回基本XML格式
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<templates>\n");
        
        for (String templateId : templateIds) {
            ScriptTemplate template = templates.get(templateId);
            if (template != null) {
                xml.append("  <template>\n");
                xml.append("    <templateId>").append(escapeXml(template.getTemplateId())).append("</templateId>\n");
                xml.append("    <name>").append(escapeXml(template.getName())).append("</name>\n");
                xml.append("    <description>").append(escapeXml(template.getDescription())).append("</description>\n");
                xml.append("    <scriptContent><![CDATA[").append(template.getScriptContent()).append("]]></scriptContent>\n");
                xml.append("    <scriptType>").append(template.getScriptType()).append("</scriptType>\n");
                xml.append("    <category>").append(escapeXml(template.getCategory())).append("</category>\n");
                xml.append("    <tags>").append(escapeXml(template.getTags())).append("</tags>\n");
                xml.append("    <author>").append(escapeXml(template.getAuthor())).append("</author>\n");
                xml.append("    <version>").append(escapeXml(template.getVersion())).append("</version>\n");
                xml.append("  </template>\n");
            }
        }
        
        xml.append("</templates>");
        return xml.toString();
    }
    
    private List<ScriptTemplate> importFromJson(String content) {
        // 简化实现，实际应该使用JSON库解析
        List<ScriptTemplate> templates = new ArrayList<>();
        
        try {
            // 这里应该使用JSON解析库
            // 简化处理，返回空列表
            logger.info("JSON导入功能需要JSON库支持");
        } catch (Exception e) {
            logger.error("JSON导入失败", e);
        }
        
        return templates;
    }
    
    private List<ScriptTemplate> importFromXml(String content) {
        // 简化实现，实际应该使用XML解析库
        List<ScriptTemplate> templates = new ArrayList<>();
        
        try {
            // 这里应该使用XML解析库
            // 简化处理，返回空列表
            logger.info("XML导入功能需要XML库支持");
        } catch (Exception e) {
            logger.error("XML导入失败", e);
        }
        
        return templates;
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\"", "&quot;")
                 .replace("'", "&apos;");
    }
}