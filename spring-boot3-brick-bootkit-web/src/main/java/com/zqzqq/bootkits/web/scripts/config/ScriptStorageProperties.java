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


package com.zqzqq.bootkits.web.scripts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 脚本管理存储配置属性
 * 
 * @author brick-bootkit
 */
@Data
@ConfigurationProperties(prefix = "plugin.scripts.storage")
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