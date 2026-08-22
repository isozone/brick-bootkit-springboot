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


package com.zqzqq.bootkits.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件配置管理属性
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@ConfigurationProperties(prefix = "plugin.configuration")
public class PluginConfigurationProperties {
    
    /**
     * 是否启用配置管理
     */
    private boolean enabled = true;
    
    /**
     * 配置文件目录
     */
    private String configDirectory = "config/plugins";
    
    /**
     * 是否启用热重载
     */
    private boolean hotReloadEnabled = true;
    
    /**
     * 热重载延迟（毫秒）
     */
    private long hotReloadDelay = 1000;
    
    /**
     * 是否启用持久化
     */
    private boolean persistenceEnabled = true;
    
    /**
     * 每个插件最大版本数
     */
    private int maxVersionsPerPlugin = 10;
    
    /**
     * 配置缓存大小
     */
    private int cacheSize = 100;
    
    /**
     * 配置验证是否启用
     */
    private boolean validationEnabled = true;
    
    /**
     * 是否启用配置加密
     */
    private boolean encryptionEnabled = false;
    
    /**
     * 加密密钥
     */
    private String encryptionKey;
    
    /**
     * 配置备份是否启用
     */
    private boolean backupEnabled = true;
    
    /**
     * 备份目录
     */
    private String backupDirectory = "backup/plugins";
    
    /**
     * 备份保留天数
     */
    private int backupRetentionDays = 30;
    
    /**
     * 获取最大版本历史数量（兼容方法）
     */
    public int getMaxVersionHistory() {
        return maxVersionsPerPlugin;
    }

    /**
     * 获取配置目录
     */
    public String getConfigDirectory() {
        return configDirectory;
    }

    public void setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }

    public void setHotReloadEnabled(boolean hotReloadEnabled) {
        this.hotReloadEnabled = hotReloadEnabled;
    }

    public long getHotReloadDelay() {
        return hotReloadDelay;
    }

    public void setHotReloadDelay(long hotReloadDelay) {
        this.hotReloadDelay = hotReloadDelay;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    public int getMaxVersionsPerPlugin() {
        return maxVersionsPerPlugin;
    }

    public void setMaxVersionsPerPlugin(int maxVersionsPerPlugin) {
        this.maxVersionsPerPlugin = maxVersionsPerPlugin;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public void setBackupEnabled(boolean backupEnabled) {
        this.backupEnabled = backupEnabled;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public int getBackupRetentionDays() {
        return backupRetentionDays;
    }

    public void setBackupRetentionDays(int backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }
}