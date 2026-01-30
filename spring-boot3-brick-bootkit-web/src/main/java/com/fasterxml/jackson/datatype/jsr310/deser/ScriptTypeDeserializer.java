package com.fasterxml.jackson.datatype.jsr310.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.zqzqq.bootkits.scripts.core.ScriptType;

import java.io.IOException;

/**
 * ScriptType 反序列化器
 * 将字符串转换为 ScriptType 枚举
 * 
 * @author brick-bootkit
 */
public class ScriptTypeDeserializer extends JsonDeserializer<ScriptType> {
    
    @Override
    public ScriptType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ScriptType.fromTypeName(value);
    }
}
