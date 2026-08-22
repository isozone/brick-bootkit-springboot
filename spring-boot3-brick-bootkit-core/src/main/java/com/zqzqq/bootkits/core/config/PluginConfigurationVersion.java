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

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 插件配置版本
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class PluginConfigurationVersion {
    
    /**
     * 版本ID
     */
    private String versionId;
    
    /**
     * 配置内容
     */
    private PluginConfiguration configuration;
    
    /**
     * 创建时间戳
     */
    private long timestamp;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 变更原因
     */
    private String changeReason;
    
    /**
     * 变更用户
     */
    private String changedBy;
    
    /**
     * 版本标签
     */
    private String tag;
    
    public PluginConfigurationVersion() {
        this.timestamp = System.currentTimeMillis();
        this.createdAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp), 
                ZoneId.systemDefault());
    }
    
    public PluginConfigurationVersion(String versionId, PluginConfiguration configuration, 
                                    long timestamp, String changeReason) {
        this.versionId = versionId;
        this.configuration = configuration.copy(); // 创建副本避免引用问题
        this.timestamp = timestamp;
        this.createdAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp), 
                ZoneId.systemDefault());
        this.changeReason = changeReason;
    }
    
    /**
     * 获取版本描述
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Version: ").append(versionId);
        if (tag != null) {
            desc.append(" (").append(tag).append(")");
        }
        if (changeReason != null) {
            desc.append(" - ").append(changeReason);
        }
        if (changedBy != null) {
            desc.append(" by ").append(changedBy);
        }
        return desc.toString();
    }
    
    /**
     * 检查版本是否有效
     */
    public boolean isValid() {
        return versionId != null && configuration != null && configuration.isValid();
    }
    
    /**
     * 获取版本年龄（毫秒）
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * 检查版本是否过期
     */
    public boolean isExpired(long maxAgeMillis) {
        return getAge() > maxAgeMillis;
    }

    /**
     * 获取版本ID（public方法，确保兼容性）
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * 获取配置（public方法，确保兼容性）
     */
    public PluginConfiguration getConfiguration() {
        return configuration;
    }
    
    // Getters and Setters
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public void setConfiguration(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}