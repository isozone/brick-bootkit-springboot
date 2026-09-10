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


package com.zqzqq.bootkits.protocol.bus;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link EnvelopeBus} 测试。
 */
class EnvelopeBusTest {

    @Test
    void publishShouldDeliverToExactMatcher() {
        EnvelopeBus bus = new EnvelopeBus();
        AtomicInteger received = new AtomicInteger();
        bus.subscribe("sensor-01", "temp", e -> received.incrementAndGet());

        bus.publish(BrickMessages.event("sensor-01", "temp", 23.5));
        bus.publish(BrickMessages.event("other-device", "temp", 10.0));

        assertEquals(1, received.get());
    }

    @Test
    void subscribeShouldSupportWildcards() {
        EnvelopeBus bus = new EnvelopeBus();
        AtomicInteger allDevice = new AtomicInteger();
        AtomicInteger allCapability = new AtomicInteger();

        bus.subscribe("*", "temp", e -> allDevice.incrementAndGet());
        bus.subscribe("sensor-01", "*", e -> allCapability.incrementAndGet());

        bus.publish(BrickMessages.event("sensor-01", "temp", 23.5));
        bus.publish(BrickMessages.event("sensor-01", "humidity", 60.0));

        assertEquals(1, allDevice.get());
        assertEquals(2, allCapability.get());
    }

    @Test
    void unsubscribeShouldRemoveListener() {
        EnvelopeBus bus = new EnvelopeBus();
        AtomicInteger received = new AtomicInteger();
        EnvelopeBus.Subscription subscription =
                bus.subscribe("sensor-01", "temp", e -> received.incrementAndGet());

        bus.publish(BrickMessages.event("sensor-01", "temp", 1.0));
        bus.unsubscribe(subscription);
        bus.publish(BrickMessages.event("sensor-01", "temp", 2.0));

        assertEquals(1, received.get());
    }

    @Test
    void requestReplyRoundTrip() throws Exception {
        EnvelopeBus bus = new EnvelopeBus();
        bus.subscribe("sensor-01", "temp", request -> {
            BrickEnvelope reply = BrickMessages.reply(
                    request,
                    new TempResult(23.5, "°C"));
            bus.reply(reply);
        });

        BrickEnvelope reply = bus.request(
                BrickMessages.request("sensor-01", "temp", null),
                1, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals("°C", ((TempResult) reply.getPayload()).getUnit());
    }

    @Test
    void requestShouldTimeoutWhenNoReply() {
        EnvelopeBus bus = new EnvelopeBus();

        BrickEnvelope reply = bus.request(
                BrickMessages.request("sensor-01", "temp", null),
                100, TimeUnit.MILLISECONDS);

        assertNull(reply);
    }

    @Test
    void commandReplyRoundTrip() throws Exception {
        EnvelopeBus bus = new EnvelopeBus();
        bus.subscribe("sensor-01", "relay", command -> {
            BrickEnvelope reply = BrickMessages.reply(command, "on");
            bus.reply(reply);
        });

        BrickEnvelope reply = bus.command(
                BrickMessages.command("sensor-01", "relay", "on"),
                1, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals("on", reply.getPayload());
    }

    /**
     * 载荷示例。
     */
    public static final class TempResult {
        private double value;
        private String unit;

        public TempResult() {
        }

        TempResult(double value, String unit) {
            this.value = value;
            this.unit = unit;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}