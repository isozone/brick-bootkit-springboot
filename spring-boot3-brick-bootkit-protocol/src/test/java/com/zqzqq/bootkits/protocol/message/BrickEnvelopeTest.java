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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BrickEnvelope} 测试。
 */
class BrickEnvelopeTest {

    @Test
    void builderShouldCompleteTimestampWhenMissing() {
        BrickEnvelope envelope = BrickEnvelope.builder()
                .type(MessageType.EVENT)
                .messageId("m1")
                .deviceId("sensor-01")
                .capabilityId("temp")
                .payload(new Payload("23.5"))
                .build();

        assertEquals("1.0", envelope.getVersion());
        assertEquals(MessageType.EVENT, envelope.getType());
        assertTrue(envelope.getTimestamp() > 0);
    }

    @Test
    void replyMustCarryCorrelationId() {
        assertThrows(IllegalStateException.class, () -> BrickEnvelope.builder()
                .type(MessageType.REPLY)
                .messageId("m1")
                .deviceId("sensor-01")
                .capabilityId("temp")
                .build());
    }

    @Test
    void messageIdIsRequired() {
        assertThrows(IllegalStateException.class, () -> BrickEnvelope.builder()
                .type(MessageType.REQUEST)
                .deviceId("sensor-01")
                .capabilityId("temp")
                .build());
    }

    @Test
    void typeDirectionFlags() {
        assertTrue(MessageType.REQUEST.isDownward());
        assertTrue(MessageType.COMMAND.isDownward());
        assertTrue(MessageType.REPLY.isUpward());
        assertTrue(MessageType.EVENT.isUpward());
        assertTrue(MessageType.REQUEST.expectsReply());
        assertTrue(MessageType.COMMAND.expectsReply());
        assertFalse(MessageType.EVENT.expectsReply());
    }

    @Test
    void wireNameRoundTrip() {
        assertEquals("request", MessageType.REQUEST.wireName());
        assertEquals(MessageType.REQUEST, MessageType.fromWire("request"));
        assertEquals(MessageType.REPLY, MessageType.fromWire("REPLY"));
    }

    /**
     * 载荷示例。
     */
    static final class Payload {
        private String value;

        Payload() {
        }

        Payload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}