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

import java.util.List;

/**
 * 优化建议
 * 针对性能问题给出的具体优化建议
 */
public class OptimizationRecommendation {
    
    private final Priority priority;
    private final String code;
    private final String title;
    private final String description;
    private final Category category;
    private final List<String> actionItems;
    
    public OptimizationRecommendation(Priority priority, String code, String title,
                                   String description, Category category,
                                   List<String> actionItems) {
        this.priority = priority;
        this.code = code;
        this.title = title;
        this.description = description;
        this.category = category;
        this.actionItems = actionItems;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public List<String> getActionItems() {
        return actionItems;
    }
    
    /**
     * 优先级
     */
    public enum Priority {
        CRITICAL("紧急", 4),
        HIGH("高", 3),
        MEDIUM("中", 2),
        LOW("低", 1);
        
        private final String description;
        private final int level;
        
        Priority(String description, int level) {
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
    
    /**
     * 优化类别
     */
    public enum Category {
        MEMORY("内存优化"),
        THREAD("线程优化"),
        CPU("CPU优化"),
        NETWORK("网络优化"),
        EFFICIENCY("效率优化"),
        CODE_QUALITY("代码质量"),
        ARCHITECTURE("架构优化");
        
        private final String description;
        
        Category(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format("OptimizationRecommendation{priority=%s, category=%s, title='%s', description='%s'}",
                priority, category, title, description);
    }
}