package com.zqzqq.bootkits.web.scripts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.ScriptTypeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ScriptTypeSerializer;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 脚本模板DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptTemplateDTO {
    
    private String templateId;
    
    private String templateName;
    
    private String displayName;
    
    private String description;
    
    private String category;
    
    private List<String> tags;
    
    @JsonSerialize(using = ScriptTypeSerializer.class)
    @JsonDeserialize(using = ScriptTypeDeserializer.class)
    private ScriptType scriptType;
    
    private String templateContent;
    
    private List<TemplateParameter> parameters;
    
    private List<TemplateParameter> defaultValues;
    
    private Long usageCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateParameter {
        private String name;
        private String type;
        private String description;
        private Boolean required;
        private Object defaultValue;
        private String validationRule;
    }
}