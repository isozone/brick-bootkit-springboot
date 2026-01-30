package com.fasterxml.jackson.datatype.jsr310.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zqzqq.bootkits.scripts.core.ScriptType;

import java.io.IOException;

/**
 * ScriptType 序列化器
 * 将 ScriptType 枚举转换为字符串
 * 
 * @author brick-bootkit
 */
public class ScriptTypeSerializer extends JsonSerializer<ScriptType> {
    
    @Override
    public void serialize(ScriptType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.name());
        }
    }
}
