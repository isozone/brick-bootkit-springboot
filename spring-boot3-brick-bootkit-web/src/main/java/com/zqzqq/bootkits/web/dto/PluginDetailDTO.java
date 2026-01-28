package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 插件详情 DTO
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDetailDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件状态
     */
    private String state;
    
    /**
     * 状态描述
     */
    private String stateDescription;
    
    /**
     * 插件路径
     */
    private String pluginPath;
    
    /**
     * 启动时间
     */
    private String startTime;
    
    /**
     * 停止时间
     */
    private String stopTime;
    
    /**
     * 插件主类
     */
    private String mainClass;
    
    /**
     * 插件类型
     */
    private String pluginType;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 依赖插件列表
     */
    private List<DependencyPluginDTO> dependentPlugins;
    
    /**
     * 扩展信息
     */
    private Map<String, Object> extensionInfo;
    
    /**
     * 依赖插件 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyPluginDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String pluginId;
        private String pluginName;
        private String version;
    }
}
