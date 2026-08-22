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


package com.zqzqq.bootkits.distributed.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 泛化调用参数与返回值的 JSON 序列化工具。
 * <p>
 * 由于远端插件方法签名是任意的，把入参/出参统一用 JSON 表示，并携带类型信息，
 * 由对端根据类型信息反序列化。为保持宿主与执行节点类型契约一致，优先使用
 * 共享类型（接口）或其简单二进制/JSON 表示。
 */
public final class PayloadCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PayloadCodec() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public static String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("序列化调用参数失败: " + value.getClass().getName(), e);
        }
    }

    /**
     * 根据类型信息将 JSON 反序列化为对象（使用调用线程上下文类加载器解析类型）。
     */
    public static Object fromJson(String json, String typeName) {
        return fromJson(json, typeName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 根据类型信息将 JSON 反序列化为对象，并指定用于解析类型的类加载器。
     * <p>执行节点应传入目标服务实例的插件类加载器，确保插件专属的接口/参数类型可被加载。</p>
     */
    public static Object fromJson(String json, String typeName, ClassLoader loader) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Class<?> type = resolveType(typeName, loader);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("反序列化调用结果失败, 类型: " + typeName, e);
        }
    }

    /**
     * 反序列化参数列表（与类型列表一一对应，使用调用线程上下文类加载器）。
     */
    public static Object[] fromJsonArray(List<String> jsonValues, List<String> typeNames) {
        return fromJsonArray(jsonValues, typeNames, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 反序列化参数列表，并指定用于解析类型的类加载器（执行节点侧应传插件类加载器）。
     */
    public static Object[] fromJsonArray(List<String> jsonValues, List<String> typeNames, ClassLoader loader) {
        if (jsonValues == null) {
            return new Object[0];
        }
        Object[] args = new Object[jsonValues.size()];
        for (int i = 0; i < jsonValues.size(); i++) {
            args[i] = fromJson(jsonValues.get(i), typeNames.get(i), loader);
        }
        return args;
    }

    /**
     * 根据全限定类名解析类型；对基本类型做装箱映射。优先使用传入的类加载器。
     */
    private static Class<?> resolveType(String typeName, ClassLoader loader) throws ClassNotFoundException {
        switch (typeName) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            default:
                ClassLoader effective = loader != null ? loader : Thread.currentThread().getContextClassLoader();
                if (effective != null) {
                    try {
                        return Class.forName(typeName, false, effective);
                    } catch (ClassNotFoundException ignored) {
                        // 插件类加载器不可见时回退到调用线程 TCCL / 系统加载器
                    }
                }
                return Class.forName(typeName);
        }
    }

    /**
     * 尽量解析返回值类型；失败返回 Object。
     */
    public static Class<?> resolveReturnType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return Object.class;
        }
        try {
            return resolveType(typeName, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    /**
     * 供扩展使用的 JavaType 推断。
     */
    public static JavaType resolveJavaType(String typeName, ObjectMapper mapper) throws ClassNotFoundException {
        return mapper.constructType(resolveType(typeName, Thread.currentThread().getContextClassLoader()));
    }
}