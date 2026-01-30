package com.zqzqq.bootkits.web.scripts.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 脚本执行请求DTO
 */
@Data
public class ExecuteRequest {
    
    /**
     * 脚本类型
     */
    private String scriptType;
    
    /**
     * 脚本内容
     */
    private String scriptContent;
    
    /**
     * 脚本名称（可选，用于记录）
     */
    private String scriptName;
    
    /**
     * 执行参数
     */
    private List<Map<String, String>> params;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;
}
