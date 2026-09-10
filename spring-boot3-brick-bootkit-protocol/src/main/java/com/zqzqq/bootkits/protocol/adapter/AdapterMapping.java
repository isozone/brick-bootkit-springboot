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

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 声明式映射单条记录：一个能力 → 传输细节的翻译字典。
 * <p>
 * 能力标识（capabilityId）由业务定义；传输细节（register、scale 等）由 adapter 定义，
 * 存放在 {@link #options} 中——这是"传输痕迹"被允许存在的唯一位置（协议宪法边界）。
 * <p>
 * JSON 示例：
 * <pre>
 * { "capability": "temp", "type": "point", "access": "read", "register": 40001, "scale": 0.1 }
 * </pre>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class AdapterMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 能力标识（业务层名称，如 temp、relay） */
    private String capability;

    /** 能力类型：point | command | event */
    private String type;

    /** 能力访问方式：read | read-write（仅 point 类型使用） */
    private String access;

    /** 传输专属字段（register、scale、writeValue 等——不同协议选项不同） */
    private Map<String, Object> options;

    public AdapterMapping() {
        this.options = new LinkedHashMap<>();
    }

    public AdapterMapping(String capability, String type, String access) {
        this.capability = capability;
        this.type = type;
        this.access = access;
        this.options = new LinkedHashMap<>();
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options != null ? options : new LinkedHashMap<>();
    }

    /**
     * Jackson 反序列化时，未知字段自动进入 options Map。
     * 这使得 register、scale、writeValue 等传输专属字段被正确捕获。
     */
    @JsonAnySetter
    public void setOption(String key, Object value) {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        options.put(key, value);
    }

    /**
     * 获取某个传输选项值。
     */
    public Object getOption(String key) {
        return options.get(key);
    }

    /**
     * 获取某个传输选项值，不存在时返回默认值。
     */
    public Object getOption(String key, Object defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    /**
     * 获取某个传输选项值并转为整数（常用于寄存器地址）。
     */
    public int getIntOption(String key, int defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取某个传输选项值并转为浮点数（常用于缩放系数）。
     */
    public double getDoubleOption(String key, double defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取 CapabilityAccess 枚举值。
     */
    public CapabilityAccess getCapabilityAccess() {
        return CapabilityAccess.parse(access);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdapterMapping)) {
            return false;
        }
        AdapterMapping that = (AdapterMapping) o;
        return Objects.equals(capability, that.capability)
                && Objects.equals(type, that.type)
                && Objects.equals(access, that.access)
                && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capability, type, access, options);
    }

    @Override
    public String toString() {
        return "AdapterMapping{"
                + "capability='" + capability + '\''
                + ", type='" + type + '\''
                + ", access='" + access + '\''
                + ", options=" + options
                + '}';
    }
}