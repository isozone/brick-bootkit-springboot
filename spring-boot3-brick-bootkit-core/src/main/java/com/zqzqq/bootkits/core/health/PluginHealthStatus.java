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


package com.zqzqq.bootkits.core.health;

/**
 * 插件健康状态枚举
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public enum PluginHealthStatus {
    
    /**
     * 健康状态 - 插件正常运行
     */
    HEALTHY("healthy", "插件运行正常"),
    
    /**
     * 警告状态 - 插件基本正常但存在潜在问题
     */
    WARNING("warning", "插件存在潜在问题"),
    
    /**
     * 危险状态 - 插件存在问题但不严重
     */
    CRITICAL("critical", "插件存在严重问题"),
    
    /**
     * 死亡状态 - 插件已无法正常运行
     */
    DEAD("dead", "插件已无法正常运行"),
    
    /**
     * 未知状态 - 无法确定插件状态
     */
    UNKNOWN("unknown", "插件状态未知");

    private final String status;
    private final String description;

    PluginHealthStatus(String status, String description) {
        this.status = status;
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否为正常运行状态
     */
    public boolean isHealthy() {
        return this == HEALTHY;
    }

    /**
     * 检查是否需要自动恢复
     */
    public boolean needsAutoRecovery() {
        return this == CRITICAL || this == DEAD;
    }

    /**
     * 检查是否需要人工干预
     */
    public boolean needsManualIntervention() {
        return this == DEAD || this == UNKNOWN;
    }

    /**
     * 获取严重程度级别（数值越大越严重）
     */
    public int getSeverityLevel() {
        switch (this) {
            case HEALTHY: return 0;
            case WARNING: return 1;
            case CRITICAL: return 2;
            case DEAD: return 3;
            case UNKNOWN: return 2; // 未知状态按中等严重程度处理
            default: return 2;
        }
    }

    @Override
    public String toString() {
        return "PluginHealthStatus{" +
                "status='" + status + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}