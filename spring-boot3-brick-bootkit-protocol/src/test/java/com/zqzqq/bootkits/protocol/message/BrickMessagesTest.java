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


package com.zqzqq.bootkits.protocol.message;

import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link BrickMessages} 测试。
 */
class BrickMessagesTest {

    @Test
    void requestShouldBeRequestType() {
        BrickEnvelope envelope = BrickMessages.request("sensor-01", "temp", "read");
        assertEquals(MessageType.REQUEST, envelope.getType());
        assertEquals("sensor-01", envelope.getDeviceId());
        assertEquals("temp", envelope.getCapabilityId());
        assertNull(envelope.getCorrelationId());
    }

    @Test
    void commandShouldBeCommandType() {
        BrickEnvelope envelope = BrickMessages.command("sensor-01", "relay", "on");
        assertEquals(MessageType.COMMAND, envelope.getType());
    }

    @Test
    void eventShouldBeEventType() {
        BrickEnvelope envelope = BrickMessages.event("sensor-01", "overheat", "high");
        assertEquals(MessageType.EVENT, envelope.getType());
    }

    @Test
    void replyShouldCorrelate() {
        BrickEnvelope request = BrickMessages.request("sensor-01", "temp", null);
        BrickEnvelope reply = BrickMessages.reply(request, 23.5d);
        assertEquals(MessageType.REPLY, reply.getType());
        assertEquals(request.getMessageId(), reply.getCorrelationId());
    }

    @Test
    void lifecycleShouldUseStandardCapabilityId() {
        BrickEnvelope online = BrickMessages.lifecycle("sensor-01", DeviceLifecycle.ONLINE);
        assertEquals(MessageType.EVENT, online.getType());
        assertEquals("lifecycle", online.getCapabilityId());
        assertEquals(DeviceLifecycle.ONLINE.getEventCode(), online.getPayload());
        assertNotNull(BrickMessages.newMessageId());
    }
}