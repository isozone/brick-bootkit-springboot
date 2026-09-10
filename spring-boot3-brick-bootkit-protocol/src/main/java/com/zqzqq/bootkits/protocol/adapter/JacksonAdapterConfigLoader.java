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


package com.zqzqq.bootkits.protocol.adapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Jackson 的声明式映射加载器。
 * <p>
 * 从 {@code brick-adapter.json} 解析 {@link AdapterConfig}。
 * Jackson 为 provided/optional 依赖：运行时宿主（Spring Boot）负责提供。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class JacksonAdapterConfigLoader implements AdapterConfigLoader {

    private final ObjectMapper objectMapper;

    public JacksonAdapterConfigLoader() {
        this(createDefaultObjectMapper());
    }

    public JacksonAdapterConfigLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AdapterConfig load(String resourcePath) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }
        if (is == null) {
            throw new IllegalArgumentException("找不到 brick-adapter 资源: " + resourcePath);
        }
        try {
            return load(is);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 brick-adapter 资源失败: " + resourcePath, e);
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public AdapterConfig load(InputStream inputStream) {
        try {
            AdapterConfig config = objectMapper.readValue(inputStream, AdapterConfig.class);
            validate(config);
            return config;
        } catch (IOException e) {
            throw new IllegalArgumentException("解析 brick-adapter.json 失败", e);
        }
    }

    private void validate(AdapterConfig config) {
        if (config.getDeviceType() == null || config.getDeviceType().isEmpty()) {
            throw new IllegalArgumentException("deviceType 不能为空");
        }
        if (config.getTransport() == null || config.getTransport().isEmpty()) {
            throw new IllegalArgumentException("transport 不能为空");
        }
        if (config.getMappings() == null) {
            throw new IllegalArgumentException("mappings 不能为空");
        }
        for (AdapterMapping mapping : config.getMappings()) {
            if (mapping.getCapability() == null || mapping.getCapability().isEmpty()) {
                throw new IllegalArgumentException("映射的 capability 不能为空");
            }
            if (mapping.getType() == null || mapping.getType().isEmpty()) {
                throw new IllegalArgumentException("映射的 type 不能为空: " + mapping.getCapability());
            }
        }
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}