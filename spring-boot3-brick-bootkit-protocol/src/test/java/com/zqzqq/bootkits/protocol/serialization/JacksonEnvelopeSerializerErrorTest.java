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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link JacksonEnvelopeSerializer} 错误路径覆盖。
 */
class JacksonEnvelopeSerializerErrorTest {

    @Test
    void serializeNullShouldFail() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(null));
    }

    @Test
    void deserializeEmptyShouldFail() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(null));
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(new byte[0]));
    }

    @Test
    void deserializeGarbageShouldFail() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer();
        assertThrows(IllegalStateException.class,
                () -> serializer.deserialize("not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void customObjectMapperSupported() {
        JacksonEnvelopeSerializer serializer = new JacksonEnvelopeSerializer(new ObjectMapper());

        BrickEnvelope original = BrickMessages.event("sensor-01", "temp", 1.0);
        BrickEnvelope restored = serializer.deserialize(serializer.serialize(original));

        assertEquals(original.getMessageId(), restored.getMessageId());
        assertEquals(MessageType.EVENT, restored.getType());
    }

    @Test
    void nullObjectMapperRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JacksonEnvelopeSerializer(null));
    }
}