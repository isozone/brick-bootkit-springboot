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



package com.zqzqq.bootkits.spring.environment;

import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.ObjectValueUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * map 类型的配置信息提供者
 *
 * @author starBlues
 * @version 3.0.3
 */
public class MapEnvironmentProvider implements EnvironmentProvider{

    private final String prefix;
    private final Map<String, Object> source;

    public MapEnvironmentProvider(Map<String, Object> source){
        this(null, source);
    }

    public MapEnvironmentProvider(String prefix, Map<String, Object> source) {
        if(prefix == null){
            this.prefix = "";
        } else {
            this.prefix = prefix;
        }
        if(ObjectUtils.isEmpty(source)){
            this.source = Collections.emptyMap();
        } else {
            this.source = source;
        }
    }

    public static String resolveKey(String prefix, String name){
        String key = name.replace(prefix, "");
        int i = key.indexOf(".");
        if(i > -1){
            key = key.substring(i + 1);
        }
        return key;
    }

    @Override
    public Object getValue(String name) {
        String key = prefix + name;
        return source.get(key);
    }

    @Override
    public String getString(String name) {
        return ObjectValueUtils.getString(getValue(name));
    }

    @Override
    public Integer getInteger(String name) {
        return ObjectValueUtils.getInteger(getValue(name));
    }

    @Override
    public Long getLong(String name) {
        return ObjectValueUtils.getLong(getValue(name));
    }

    @Override
    public Double getDouble(String name) {
        return ObjectValueUtils.getDouble(getValue(name));
    }

    @Override
    public Float getFloat(String name) {
        return ObjectValueUtils.getFloat(getValue(name));
    }

    @Override
    public Boolean getBoolean(String name) {
        return ObjectValueUtils.getBoolean(getValue(name));
    }

    @Override
    public EnvironmentProvider getByPrefix(String prefix) {
        if(ObjectUtils.isEmpty(prefix)){
            return new EmptyEnvironmentProvider();
        }
        Map<String, Object> collect = new LinkedHashMap<>();
        source.forEach((k,v)->{
            if(k.startsWith(prefix)){
                collect.put(resolveKey(prefix, k), v);
            }
        });
        return new MapEnvironmentProvider(collect);
    }

    @Override
    public void forEach(BiConsumer<String, Object> action) {
        source.forEach(action);
    }

}

