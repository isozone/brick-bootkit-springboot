package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptTemplateDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 脚本模板服务
 * 提供脚本模板的管理和使用功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    
    private final ScriptStorage scriptStorage;
    
    /**
     * 创建模板
     */
    public ScriptTemplateDTO createTemplate(ScriptTemplateDTO template) {
        log.info("Creating template: {}", template.getTemplateName());
        
        if (template.getTemplateId() == null) {
            template.setTemplateId(java.util.UUID.randomUUID().toString());
        }
        template.setCreatedAt(LocalDateTime.now());
        if (template.getUsageCount() == null) {
            template.setUsageCount(0L);
        }
        
        try {
            scriptStorage.saveTemplate(template);
        } catch (StorageException e) {
            log.error("Failed to save template", e);
        }
        return template;
    }
    
    /**
     * 获取模板
     */
    public Optional<ScriptTemplateDTO> getTemplate(String templateId) {
        try {
            return scriptStorage.getTemplate(templateId);
        } catch (StorageException e) {
            log.error("Failed to get template", e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据模板名称获取模板
     */
    public Optional<ScriptTemplateDTO> getTemplateByName(String templateName) {
        try {
            return scriptStorage.getAllTemplates().stream()
                    .filter(t -> templateName.equals(t.getTemplateName()))
                    .findFirst();
        } catch (StorageException e) {
            log.error("Failed to get template by name", e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有模板
     */
    public List<ScriptTemplateDTO> getAllTemplates() {
        try {
            return scriptStorage.getAllTemplates();
        } catch (StorageException e) {
            log.error("Failed to get all templates", e);
            return List.of();
        }
    }
    
    /**
     * 根据分类获取模板
     */
    public List<ScriptTemplateDTO> getTemplatesByCategory(String category) {
        return getAllTemplates().stream()
                .filter(t -> category.equals(t.getCategory()))
                .toList();
    }
    
    /**
     * 根据标签获取模板
     */
    public List<ScriptTemplateDTO> getTemplatesByTag(String tag) {
        return getAllTemplates().stream()
                .filter(t -> t.getTags() != null && t.getTags().contains(tag))
                .toList();
    }
    
    /**
     * 获取热门模板
     */
    public List<ScriptTemplateDTO> getPopularTemplates(int limit) {
        return getAllTemplates().stream()
                .sorted((t1, t2) -> Long.compare(t2.getUsageCount() != null ? t2.getUsageCount() : 0, 
                                                   t1.getUsageCount() != null ? t1.getUsageCount() : 0))
                .limit(limit)
                .toList();
    }
    
    /**
     * 获取最新模板
     */
    public List<ScriptTemplateDTO> getRecentTemplates(int limit) {
        return getAllTemplates().stream()
                .sorted((t1, t2) -> {
                    LocalDateTime time1 = t1.getCreatedAt() != null ? t1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime time2 = t2.getCreatedAt() != null ? t2.getCreatedAt() : LocalDateTime.MIN;
                    return time2.compareTo(time1);
                })
                .limit(limit)
                .toList();
    }
    
    /**
     * 更新模板
     */
    public ScriptTemplateDTO updateTemplate(String templateId, ScriptTemplateDTO updatedTemplate) {
        log.info("Updating template: {}", templateId);
        
        Optional<ScriptTemplateDTO> existingOpt = getTemplate(templateId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        
        ScriptTemplateDTO existing = existingOpt.get();
        existing.setDescription(updatedTemplate.getDescription());
        existing.setCategory(updatedTemplate.getCategory());
        existing.setTags(updatedTemplate.getTags());
        existing.setParameters(updatedTemplate.getParameters());
        existing.setDefaultValues(updatedTemplate.getDefaultValues());
        existing.setTemplateContent(updatedTemplate.getTemplateContent());
        
        try {
            scriptStorage.saveTemplate(existing);
        } catch (StorageException e) {
            log.error("Failed to save template", e);
        }
        return existing;
    }
    
    /**
     * 删除模板
     */
    public boolean deleteTemplate(String templateId) {
        log.info("Deleting template: {}", templateId);
        try {
            scriptStorage.deleteTemplate(templateId);
            return true;
        } catch (StorageException e) {
            log.error("Failed to delete template", e);
            return false;
        }
    }
    
    /**
     * 使用模板创建脚本
     */
    public ScriptInfoDTO createScriptFromTemplate(String templateId, String scriptName, 
                                                   Map<String, Object> parameters) {
        log.info("Creating script from template: {}, scriptName: {}", templateId, scriptName);
        
        ScriptTemplateDTO template = getTemplate(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        // 解析模板
        String content = template.getTemplateContent();
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                content = content.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        
        // 创建脚本
        ScriptInfoDTO script = new ScriptInfoDTO();
        script.setScriptName(scriptName);
        script.setDescription("Created from template: " + template.getTemplateName());
        script.setCreatedAt(LocalDateTime.now());
        script.setUpdatedAt(LocalDateTime.now());
        
        try {
            scriptStorage.saveScriptInfo(script);
            scriptStorage.saveScriptContent(scriptName, content);
        } catch (StorageException e) {
            log.error("Failed to save script", e);
        }
        
        return script;
    }
    
    /**
     * 验证模板参数
     */
    public boolean validateTemplateParameters(String templateId, Map<String, Object> parameters) {
        ScriptTemplateDTO template = getTemplate(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        if (template.getParameters() == null || template.getParameters().isEmpty()) {
            return true;
        }
        
        // 检查必填参数
        for (ScriptTemplateDTO.TemplateParameter param : template.getParameters()) {
            if (param.getRequired() != null && param.getRequired() && 
                (parameters == null || !parameters.containsKey(param.getName()))) {
                log.warn("Missing required parameter: {}", param.getName());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 搜索模板
     */
    public List<ScriptTemplateDTO> searchTemplates(String keyword) {
        return getAllTemplates().stream()
                .filter(t -> (t.getTemplateName() != null && t.getTemplateName().contains(keyword)) ||
                            (t.getDescription() != null && t.getDescription().contains(keyword)) ||
                            (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.contains(keyword))))
                .toList();
    }
    
    /**
     * 克隆模板
     */
    public ScriptTemplateDTO cloneTemplate(String templateId, String newTemplateName, String newCategory) {
        ScriptTemplateDTO source = getTemplate(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        ScriptTemplateDTO cloned = new ScriptTemplateDTO();
        cloned.setTemplateId(java.util.UUID.randomUUID().toString());
        cloned.setTemplateName(newTemplateName);
        cloned.setDisplayName(source.getDisplayName() != null ? source.getDisplayName() + " (Copy)" : newTemplateName + " (Copy)");
        cloned.setDescription(source.getDescription());
        cloned.setCategory(newCategory != null ? newCategory : source.getCategory());
        cloned.setTags(source.getTags());
        cloned.setScriptType(source.getScriptType());
        cloned.setParameters(source.getParameters());
        cloned.setDefaultValues(source.getDefaultValues());
        cloned.setTemplateContent(source.getTemplateContent());
        cloned.setCreatedAt(LocalDateTime.now());
        cloned.setUsageCount(0L);
        
        try {
            scriptStorage.saveTemplate(cloned);
        } catch (StorageException e) {
            log.error("Failed to save template", e);
        }
        return cloned;
    }
}