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


package com.zqzqq.bootkits.core.performance;

/**
 * 性能问题
 * 表示插件在运行过程中发现的问题
 */
public class PerformanceIssue {
    
    private final Severity severity;
    private final String code;
    private final String description;
    private final String pluginId;
    
    public PerformanceIssue(Severity severity, String code, String description, String pluginId) {
        this.severity = severity;
        this.code = code;
        this.description = description;
        this.pluginId = pluginId;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 问题严重程度
     */
    public enum Severity {
        CRITICAL("关键问题", 3),
        WARNING("警告", 2),
        INFO("信息", 1);
        
        private final String description;
        private final int level;
        
        Severity(String description, int level) {
            this.description = description;
            this.level = level;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceIssue{severity=%s, code='%s', description='%s', pluginId='%s'}",
                severity, code, description, pluginId);
    }
}