package com.zqzqq.bootkits.web.scripts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 脚本管理存储配置属性
 * 
 * @author brick-bootkit
 */
@Data
@ConfigurationProperties(prefix = "brick.scripts.storage")
public class ScriptStorageProperties {
    
    /**
     * 存储类型：file（默认）、jdbc、custom
     */
    private StorageType type = StorageType.FILE;
    
    /**
     * 自定义存储Bean名称（当type为custom时使用）
     */
    private String customBeanName;
    
    /**
     * 文件存储配置
     */
    private FileStorage file = new FileStorage();
    
    /**
     * 数据库存储配置
     */
    private JdbcStorage jdbc = new JdbcStorage();
    
    /**
     * 存储类型枚举
     */
    public enum StorageType {
        FILE, JDBC, CUSTOM
    }
    
    @Data
    public static class FileStorage {
        /**
         * 数据存储路径
         */
        private String dataPath = "./brick-scripts-data";
        
        /**
         * 日志保留天数
         */
        private Integer logRetentionDays = 30;
        
        /**
         * 最大日志文件大小（MB）
         */
        private Integer maxLogSizeMb = 100;
        
        /**
         * 自动清理过期数据
         */
        private Boolean autoCleanup = true;
    }
    
    @Data
    public static class JdbcStorage {
        /**
         * 数据存储路径（用于存储脚本文件和日志）
         */
        private String dataPath = "./brick-scripts-data";
        
        /**
         * 表前缀
         */
        private String tablePrefix = "brick_";
        
        /**
         * 是否自动创建表
         */
        private Boolean autoCreateTables = false;
    }
}