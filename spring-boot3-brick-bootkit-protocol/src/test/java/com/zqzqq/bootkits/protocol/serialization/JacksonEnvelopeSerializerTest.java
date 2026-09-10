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


package com.zqzqq.bootkits.protocol.serialization;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import com.zqzqq.bootkits.protocol.message.QosLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link JacksonEnvelopeSerializer} 测试。
 */
class JacksonEnvelopeSerializerTest {

    @Test
    void roundTripShouldPreserveEnvelope() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();

        BrickEnvelope original = BrickMessages.request("sensor-01", "temp", "read");
        byte[] bytes = serializer.serialize(original);
        BrickEnvelope restored = serializer.deserialize(bytes);

        assertEquals(original.getMessageId(), restored.getMessageId());
        assertEquals(original.getDeviceId(), restored.getDeviceId());
        assertEquals(original.getCapabilityId(), restored.getCapabilityId());
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getQos(), restored.getQos());
        assertEquals(original.getVersion(), restored.getVersion());
    }

    @Test
    void wireFormatShouldUseLowercaseTypeAndNumericQos() throws Exception {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();

        BrickEnvelope envelope = BrickEnvelope.builder()
                .type(MessageType.EVENT)
                .messageId("m1")
                .deviceId("sensor-01")
                .capabilityId("temp")
                .qos(QosLevel.AT_LEAST_ONCE)
                .payload(23.5)
                .build();

        String json = new String(serializer.serialize(envelope), java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"type\":\"event\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"qos\":1"));
    }

    @Test
    void replyWireFormatShouldCarryCorrelationId() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();

        BrickEnvelope request = BrickMessages.request("sensor-01", "temp", null);
        BrickEnvelope reply = BrickMessages.reply(request, 23.5);
        BrickEnvelope restored = serializer.deserialize(serializer.serialize(reply));

        assertEquals(MessageType.REPLY, restored.getType());
        assertEquals(request.getMessageId(), restored.getCorrelationId());
    }
}