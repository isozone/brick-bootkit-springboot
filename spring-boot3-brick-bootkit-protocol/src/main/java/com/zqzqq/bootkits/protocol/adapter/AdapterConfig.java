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

import com.zqzqq.bootkits.protocol.message.QosLevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 声明式映射配置：{@code brick-adapter.json} 的完整结构。
 * <p>
 * 一个 AdapterConfig 定义了一种设备类型如何翻译为语义能力树。
 * Adapter 在运行时持有此配置，构建 {@link com.zqzqq.bootkits.protocol.capability.Device}。
 * <p>
 * JSON 示例：
 * <pre>
 * {
 *   "deviceType": "modbus-thermostat",
 *   "transport": "modbus",
 *   "defaultQos": 1,
 *   "mappings": [
 *     { "capability": "temp",  "type": "point",   "access": "read",       "register": 40001, "scale": 0.1 },
 *     { "capability": "relay", "type": "point",   "access": "read-write", "register": 48001 },
 *     { "capability": "reset", "type": "command",                        "register": 40011, "writeValue": 1 }
 *   ]
 * }
 * </pre>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class AdapterConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备类型（全局唯一标识，如 modbus-thermostat） */
    private String deviceType;

    /** 传输协议标识（如 modbus、mqtt、ble） */
    private String transport;

    /** 默认 QoS 等级（0=fire-forget，1=at-least-once） */
    private int defaultQos;

    /** 能力映射列表 */
    private List<AdapterMapping> mappings;

    public AdapterConfig() {
        this.mappings = new ArrayList<>();
        this.defaultQos = QosLevel.FIRE_FORGET.getCode();
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public int getDefaultQos() {
        return defaultQos;
    }

    public void setDefaultQos(int defaultQos) {
        this.defaultQos = defaultQos;
    }

    public QosLevel getDefaultQosLevel() {
        return QosLevel.fromCode(defaultQos);
    }

    public List<AdapterMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<AdapterMapping> mappings) {
        this.mappings = mappings != null ? mappings : new ArrayList<>();
    }

    /**
     * 按能力 ID 查找映射。
     *
     * @return 映射，未找到返回 null
     */
    public AdapterMapping findMapping(String capabilityId) {
        if (capabilityId == null || mappings == null) {
            return null;
        }
        for (AdapterMapping mapping : mappings) {
            if (capabilityId.equals(mapping.getCapability())) {
                return mapping;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdapterConfig)) {
            return false;
        }
        AdapterConfig that = (AdapterConfig) o;
        return defaultQos == that.defaultQos
                && Objects.equals(deviceType, that.deviceType)
                && Objects.equals(transport, that.transport)
                && Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceType, transport, defaultQos, mappings);
    }

    @Override
    public String toString() {
        return "AdapterConfig{"
                + "deviceType='" + deviceType + '\''
                + ", transport='" + transport + '\''
                + ", defaultQos=" + defaultQos
                + ", mappings=" + mappings
                + '}';
    }
}