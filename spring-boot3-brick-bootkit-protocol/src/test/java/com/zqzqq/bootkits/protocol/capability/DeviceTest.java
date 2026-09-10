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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Device} / {@link Capability} 测试。
 */
class DeviceTest {

    @Test
    void deviceShouldHostCapabilityTree() {
        Device device = new Device("sensor-01", "modbus-thermostat", "温控传感器");

        PointCapability temp = new PointCapability("temp", "温度", "环境温度", false);
        PointCapability relay = new PointCapability("relay", "继电器", "开关", true);
        CommandCapability reset = new CommandCapability("reset", "复位", "重启设备");
        EventCapability overheat = new EventCapability("overheat", "过热", "过热告警");

        device.addCapability(temp);
        device.addCapability(relay);
        device.addCapability(reset);
        device.addCapability(overheat);

        assertEquals(4, device.getCapabilities().size());
        assertEquals(CapabilityType.POINT, device.getCapability("temp").getType());
        assertEquals(2, device.getCapabilities(CapabilityType.POINT).size());
        assertEquals(1, device.getCapabilities(CapabilityType.COMMAND).size());
        assertEquals(1, device.getCapabilities(CapabilityType.EVENT).size());
        assertFalse(temp.isWritable());
        assertTrue(relay.isWritable());
        assertNull(device.getCapability("nonexistent"));
    }
}