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


package com.zqzqq.bootkits.web.scripts.storage;

/**
 * 脚本存储类型
 * 
 * @author brick-bootkit
 */
public enum ScriptStorageType {
    
    /**
     * 文件存储（默认）
     */
    FILE("file", "文件存储"),
    
    /**
     * 数据库存储（可选）
     */
    JDBC("jdbc", "数据库存储"),
    
    /**
     * 自定义存储
     */
    CUSTOM("custom", "自定义存储");
    
    private final String code;
    private final String description;
    
    ScriptStorageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ScriptStorageType fromCode(String code) {
        for (ScriptStorageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return FILE; // 默认返回文件存储
    }
}