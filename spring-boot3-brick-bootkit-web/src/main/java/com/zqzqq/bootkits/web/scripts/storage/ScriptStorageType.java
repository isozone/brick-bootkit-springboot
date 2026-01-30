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