package com.zqzqq.bootkits.web.scripts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脚本依赖DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDependencyDTO {
    
    /**
     * 依赖名称
     */
    private String name;
    
    /**
     * 依赖版本
     */
    private String version;
    
    /**
     * 依赖类型
     */
    private String type;
    
    /**
     * 是否必需
     */
    private Boolean required;
    
    /**
     * 描述
     */
    private String description;
}