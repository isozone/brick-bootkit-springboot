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



package com.zqzqq.bootkits.protocol.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;

import java.io.IOException;

/**
 * 基于 Jackson 的 JSON 序列化实现（v1 默认编码）。
 * <p>
 * 该实现依赖 jackson，因此在 protocol 模块中为 optional 提供；
 * 宿主若使用本模块自带的默认序列化，需自行引入 jackson（Spring Boot 应用天然具备）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class JacksonEnvelopeSerializer implements EnvelopeSerializer {

    private final ObjectMapper objectMapper;

    public JacksonEnvelopeSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public JacksonEnvelopeSerializer(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper 不能为空");
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(BrickEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("envelope 不能为空");
        }
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化信封失败", e);
        }
    }

    @Override
    public BrickEnvelope deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes 不能为空");
        }
        try {
            return objectMapper.readValue(bytes, BrickEnvelope.class);
        } catch (IOException e) {
            throw new IllegalStateException("反序列化信封失败", e);
        }
    }
}