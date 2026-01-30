package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptTemplateDTO;
import com.zqzqq.bootkits.web.scripts.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模板Controller
 * 提供脚本模板的管理和使用功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/brick-web/api/v1/scripts/templates")
@RequiredArgsConstructor
public class TemplateController {
    
    private final TemplateService templateService;
    
    /**
     * 获取所有模板
     */
    @GetMapping
    public ResponseEntity<List<ScriptTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }
    
    /**
     * 获取模板
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<ScriptTemplateDTO> getTemplate(@PathVariable String templateId) {
        return templateService.getTemplate(templateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据模板名称获取模板
     */
    @GetMapping("/by-name/{templateName}")
    public ResponseEntity<ScriptTemplateDTO> getTemplateByName(@PathVariable String templateName) {
        return templateService.getTemplateByName(templateName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据分类获取模板
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ScriptTemplateDTO>> getTemplatesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(templateService.getTemplatesByCategory(category));
    }
    
    /**
     * 根据标签获取模板
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<ScriptTemplateDTO>> getTemplatesByTag(@PathVariable String tag) {
        return ResponseEntity.ok(templateService.getTemplatesByTag(tag));
    }
    
    /**
     * 获取热门模板
     */
    @GetMapping("/popular")
    public ResponseEntity<List<ScriptTemplateDTO>> getPopularTemplates(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(templateService.getPopularTemplates(limit));
    }
    
    /**
     * 获取最新模板
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ScriptTemplateDTO>> getRecentTemplates(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(templateService.getRecentTemplates(limit));
    }
    
    /**
     * 搜索模板
     */
    @GetMapping("/search")
    public ResponseEntity<List<ScriptTemplateDTO>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(templateService.searchTemplates(keyword));
    }
    
    /**
     * 创建模板
     */
    @PostMapping
    public ResponseEntity<ScriptTemplateDTO> createTemplate(@RequestBody ScriptTemplateDTO template) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(template));
    }
    
    /**
     * 更新模板
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<ScriptTemplateDTO> updateTemplate(@PathVariable String templateId,
                                                             @RequestBody ScriptTemplateDTO template) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, template));
    }
    
    /**
     * 删除模板
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String templateId) {
        boolean deleted = templateService.deleteTemplate(templateId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * 使用模板创建脚本
     */
    @PostMapping("/{templateId}/create-script")
    public ResponseEntity<ScriptInfoDTO> createScriptFromTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        String scriptName = (String) request.get("scriptName");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createScriptFromTemplate(templateId, scriptName, parameters));
    }
    
    /**
     * 验证模板参数
     */
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<Map<String, Object>> validateTemplateParameters(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> parameters) {
        boolean valid = templateService.validateTemplateParameters(templateId, parameters);
        return ResponseEntity.ok(Map.of("valid", valid, "templateId", templateId));
    }
    
    /**
     * 克隆模板
     */
    @PostMapping("/{templateId}/clone")
    public ResponseEntity<ScriptTemplateDTO> cloneTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, String> request) {
        String newTemplateName = request.get("newTemplateName");
        String newCategory = request.get("newCategory");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.cloneTemplate(templateId, newTemplateName, newCategory));
    }
}