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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 传输层上下文：携带传输侧元数据（串口号、从站地址、MQTT 主题等）。
 * <p>
 * 传输上下文是 adapter 与传输层之间的契约，禁止进入语义信封（协议宪法）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class TransportContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> attributes;

    public TransportContext() {
        this.attributes = new LinkedHashMap<>();
    }

    public TransportContext(Map<String, Object> attributes) {
        this.attributes = new LinkedHashMap<>(attributes);
    }

    /**
     * 获取传输属性（只读视图）。
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * 获取某个传输属性。
     */
    public Object get(String key) {
        return attributes.get(key);
    }

    /**
     * 获取某个传输属性，不存在时返回默认值。
     */
    public Object getOrDefault(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串类型的传输属性。
     */
    public String getString(String key) {
        Object value = attributes.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取字符串类型的传输属性，不存在时返回默认值。
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数类型的传输属性。
     */
    public Integer getInt(String key) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return null;
    }

    /**
     * 获取整数类型的传输属性，不存在时返回默认值。
     */
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置传输属性。
     */
    public TransportContext put(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * 是否包含某个属性。
     */
    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 属性数量。
     */
    public int size() {
        return attributes.size();
    }

    /**
     * 是否为空。
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransportContext)) {
            return false;
        }
        TransportContext that = (TransportContext) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public String toString() {
        return "TransportContext{" + "attributes=" + attributes + '}';
    }
}