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


package com.zqzqq.bootkits.scripts.core;

/**
 * 脚本类型枚举
 * 定义了支持的脚本类型
 *
 * @author starBlues
 * @since 4.0.1
 */
public enum ScriptType {
    
    /**
     * Shell脚本（Linux/Mac）
     */
    SHELL(".sh", "shell"),
    
    /**
     * Windows批处理脚本
     */
    BATCH(".bat", "batch", ".cmd"),
    
    /**
     * PowerShell脚本
     */
    POWERSHELL(".ps1", "powershell"),
    
    /**
     * Lua脚本
     */
    LUA(".lua", "lua"),
    
    /**
     * Python脚本
     */
    PYTHON(".py", "python"),
    
    /**
     * Ruby脚本
     */
    RUBY(".rb", "ruby"),
    
    /**
     * Perl脚本
     */
    PERL(".pl", "perl"),
    
    /**
     * JavaScript脚本
     */
    JAVASCRIPT(".js", "javascript"),
    
    /**
     * Node.js脚本
     */
    NODEJS(".js", "nodejs"),
    
    /**
     * Groovy脚本
     */
    GROOVY(".groovy", "groovy"),
    
    /**
     * 可执行文件
     */
    EXECUTABLE("", "executable");
    
    private final String extension;
    private final String type;
    private final String[] alternativeExtensions;
    
    ScriptType(String extension, String type, String... alternativeExtensions) {
        this.extension = extension;
        this.type = type;
        this.alternativeExtensions = alternativeExtensions == null ? new String[0] : alternativeExtensions;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public String getType() {
        return type;
    }

    public boolean matchesFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        if (matchesExtension(lowerFileName, extension)) {
            return true;
        }

        for (String alternativeExtension : alternativeExtensions) {
            if (matchesExtension(lowerFileName, alternativeExtension)) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * 根据文件扩展名获取脚本类型
     *
     * @param fileName 文件名
     * @return 脚本类型，如果未找到则返回null
     */
    public static ScriptType fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        for (ScriptType type : values()) {
            if (type.matchesFileName(fileName)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * 根据脚本类型名称获取脚本类型
     *
     * @param typeName 类型名称
     * @return 脚本类型，如果未找到则返回null
     */
    public static ScriptType fromTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        
        String lowerTypeName = typeName.toLowerCase();
        for (ScriptType type : values()) {
            if (type.type.toLowerCase().equals(lowerTypeName)) {
                return type;
            }
        }
        
        return null;
    }

    private boolean matchesExtension(String lowerFileName, String extension) {
        return extension != null
            && !extension.isEmpty()
            && lowerFileName.endsWith(extension.toLowerCase());
    }
}
