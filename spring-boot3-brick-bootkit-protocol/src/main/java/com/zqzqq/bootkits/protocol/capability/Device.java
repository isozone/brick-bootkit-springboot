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


package com.zqzqq.bootkits.protocol.capability;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备抽象：能力树的容器。
 * <p>
 * 设备接入后抽象为一棵能力树（point/command/event），业务层只对能力树对话。
 * 具体能力由 adapter 在运行时声明（注册 DeviceRegistry），不进协议规范。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 逻辑设备标识（寻址第一层） */
    private String deviceId;

    /** 设备类型（与 adapter 声明的 deviceType 对应，如 modbus-thermostat） */
    private String deviceType;

    /** 设备名称 */
    private String name;

    /** 能力集合：capabilityId → Capability */
    private final Map<String, Capability> capabilities = new LinkedHashMap<>();

    public Device() {
    }

    public Device(String deviceId, String deviceType, String name) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.name = name;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 注册一个能力。
     */
    public void addCapability(Capability capability) {
        Objects.requireNonNull(capability, "capability 不能为空");
        capabilities.put(capability.getId(), capability);
    }

    /**
     * 获取某个能力。
     *
     * @return 能力，不存在返回 null
     */
    public Capability getCapability(String capabilityId) {
        return capabilities.get(capabilityId);
    }

    /**
     * 按类型过滤能力。
     */
    public Collection<Capability> getCapabilities(CapabilityType type) {
        if (type == null) {
            return Collections.unmodifiableCollection(capabilities.values());
        }
        return capabilities.values().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 全部能力（只读视图）。
     */
    public Collection<Capability> getCapabilities() {
        return Collections.unmodifiableCollection(capabilities.values());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Device)) {
            return false;
        }
        Device device = (Device) o;
        return Objects.equals(deviceId, device.deviceId)
                && Objects.equals(deviceType, device.deviceType)
                && Objects.equals(name, device.name)
                && Objects.equals(capabilities, device.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, deviceType, name, capabilities);
    }

    @Override
    public String toString() {
        return "Device{"
                + "deviceId='" + deviceId + '\''
                + ", deviceType='" + deviceType + '\''
                + ", name='" + name + '\''
                + ", capabilities=" + capabilities
                + '}';
    }
}