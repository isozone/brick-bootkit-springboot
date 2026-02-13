package com.zqzqq.bootkits.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Brick Web 配置属性
 * 
 * @author brick-bootkit
 */
@Data
@ConfigurationProperties(prefix = "plugin.web")
public class BrickWebProperties {
    
    /**
     * 是否启用 Brick Web
     */
    private boolean enabled = true;
    
    /**
     * 是否启用 UI 界面
     */
    private boolean enableUI = true;
    
    /**
     * API 前缀
     */
    private String apiPrefix = "/plugins-web/api";
    
    /**
     * 页面路径前缀
     */
    private String pagePrefix = "/plugins-web";
    
    /**
     * 监控刷新间隔（秒）
     */
    private int monitorRefreshInterval = 5;
    
    /**
     * API 文档标题
     */
    private String apiTitle = "Brick BootKit Web API";
    
    // ========== 以下属性从 IntegrationConfiguration 自动读取 ==========
    
    /**
     * 插件路径列表（从 IntegrationConfiguration 自动获取）
     */
    private List<String> pluginPaths;
    
    /**
     * 插件上传临时目录（从 IntegrationConfiguration 自动获取）
     */
    private String uploadTempPath;
    
    /**
     * 插件备份目录（从 IntegrationConfiguration 自动获取）
     */
    private String backupPath;
    
    /**
     * 插件 REST 接口前缀（从 IntegrationConfiguration 自动获取）
     */
    private String pluginRestPathPrefix;
}
