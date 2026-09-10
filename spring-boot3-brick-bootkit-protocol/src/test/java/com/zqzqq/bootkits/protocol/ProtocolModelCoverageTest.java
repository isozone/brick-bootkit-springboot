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


package com.zqzqq.bootkits.protocol;

import com.zqzqq.bootkits.protocol.capability.Capability;
import com.zqzqq.bootkits.protocol.capability.CapabilityType;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.capability.EventCapability;
import com.zqzqq.bootkits.protocol.capability.PointCapability;
import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import com.zqzqq.bootkits.protocol.message.QosLevel;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 补充覆盖：枚举、POJO 等值语义、异常分支。
 */
class ProtocolModelCoverageTest {

    @Test
    void qosCodeRoundTrip() {
        assertEquals(0, QosLevel.FIRE_FORGET.getCode());
        assertEquals(1, QosLevel.AT_LEAST_ONCE.getCode());
        assertEquals(QosLevel.FIRE_FORGET, QosLevel.fromCode(0));
        assertEquals(QosLevel.AT_LEAST_ONCE, QosLevel.fromWire(1));
        assertThrows(IllegalArgumentException.class, () -> QosLevel.fromCode(9));
    }

    @Test
    void messageTypeEnumsComplete() {
        assertEquals(4, MessageType.values().length);
        assertTrue(MessageType.valueOf("REQUEST") == MessageType.REQUEST);
        assertNull(MessageType.fromWire(null));
    }

    @Test
    void capabilityEqualsAndHashCode() {
        PointCapability a = new PointCapability("temp", "温度", "x", false);
        PointCapability b = new PointCapability("temp", "温度", "y", false);
        PointCapability c = new PointCapability("other", "温度", "x", false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertNotNull(a.toString());
        assertEquals(CapabilityType.POINT, a.getType());
        assertEquals("temp", a.getId());
        assertEquals("温度", a.getName());
        assertEquals("x", a.getDescription());

        EventCapability other = new EventCapability("e", "事件", "d");
        assertEquals(CapabilityType.EVENT, other.getType());
        assertNotNull(other.toString());
    }

    @Test
    void deviceEqualsAndToString() {
        Device a = new Device("sensor-01", "modbus-thermostat", "温控");
        Device b = new Device("sensor-01", "modbus-thermostat", "温控");
        Device c = new Device("other", "modbus-thermostat", "温控");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));

        a.addCapability(new PointCapability("temp", "温度", null, false));
        Collection<Capability> all = a.getCapabilities(null);
        assertEquals(1, all.size());
        assertNotNull(a.toString());
        assertEquals("sensor-01", a.getDeviceId());
        assertEquals("modbus-thermostat", a.getDeviceType());
        assertEquals("温控", a.getName());
    }

    @Test
    void builderWithQosAndCorrelation() {
        BrickEnvelope envelope = BrickMessages.command("sensor-01", "relay", "on", QosLevel.AT_LEAST_ONCE);
        assertEquals(QosLevel.AT_LEAST_ONCE, envelope.getQos());
        assertEquals(MessageType.COMMAND, envelope.getType());

        BrickEnvelope req = BrickMessages.request("sensor-01", "temp", "read", QosLevel.FIRE_FORGET);
        assertEquals(QosLevel.FIRE_FORGET, req.getQos());
        assertEquals(MessageType.REQUEST, req.getType());
    }

    @Test
    void envelopeSettersAndValueSemantics() {
        BrickEnvelope a = BrickEnvelope.builder()
                .type(MessageType.EVENT)
                .messageId("m1")
                .deviceId("d")
                .capabilityId("c")
                .timestamp(1000L)
                .build();
        a.setVersion("1.0");
        a.setFrom("adapter");
        a.setPayload("p");
        a.setCorrelationId(null);
        a.setTimestamp(1000L);

        BrickEnvelope b = BrickEnvelope.builder()
                .type(MessageType.EVENT)
                .messageId("m1")
                .deviceId("d")
                .capabilityId("c")
                .timestamp(1000L)
                .build();
        b.setVersion("1.0");
        b.setFrom("adapter");
        b.setPayload("p");
        b.setTimestamp(1000L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("m1"));

        a.setMessageId("m2");
        assertFalse(a.equals(b));
    }

    @Test
    void lifecycleEnumsComplete() {
        assertEquals(3, DeviceLifecycle.values().length);
        assertEquals("device.online", DeviceLifecycle.ONLINE.getEventCode());
        assertEquals("device.offline", DeviceLifecycle.OFFLINE.getEventCode());
        assertEquals("device.heartbeat", DeviceLifecycle.HEARTBEAT.getEventCode());
        assertEquals("lifecycle", DeviceLifecycle.LIFECYCLE_CAPABILITY_ID);
    }
}