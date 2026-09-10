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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link EnvelopeBus} 校验与异步路径覆盖。
 */
class EnvelopeBusValidationTest {

    @Test
    void nullPublishRejected() {
        EnvelopeBus bus = new EnvelopeBus();
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }

    @Test
    void wrongTypeForRequestRejected() {
        EnvelopeBus bus = new EnvelopeBus();
        BrickEnvelope event = BrickMessages.event("sensor-01", "temp", null);
        assertThrows(IllegalArgumentException.class,
                () -> bus.request(event, 1, TimeUnit.SECONDS));
    }

    @Test
    void wrongTypeForCommandRejected() {
        EnvelopeBus bus = new EnvelopeBus();
        BrickEnvelope event = BrickMessages.event("sensor-01", "temp", null);
        assertThrows(IllegalArgumentException.class,
                () -> bus.command(event, 1, TimeUnit.SECONDS));
    }

    @Test
    void wrongTypeForSendAsyncRejected() {
        EnvelopeBus bus = new EnvelopeBus();
        BrickEnvelope event = BrickMessages.event("sensor-01", "temp", null);
        assertThrows(IllegalArgumentException.class,
                () -> bus.sendAsync(event, 1, TimeUnit.SECONDS));
    }

    @Test
    void sendAsyncCompletesWithReply() throws Exception {
        EnvelopeBus bus = new EnvelopeBus();
        bus.subscribe("sensor-01", "temp", request -> {
            bus.reply(BrickMessages.reply(request, 42));
        });

        CompletableFuture<BrickEnvelope> future =
                bus.sendAsync(BrickMessages.request("sensor-01", "temp", null), 1, TimeUnit.SECONDS);
        BrickEnvelope reply = future.get(1, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(42, reply.getPayload());
    }

    @Test
    void sendAsyncTimesOut() {
        EnvelopeBus bus = new EnvelopeBus();

        CompletableFuture<BrickEnvelope> future =
                bus.sendAsync(BrickMessages.request("sensor-01", "temp", null), 100, TimeUnit.MILLISECONDS);
        assertNull(future.join());
    }

    @Test
    void replyOrphanCorrelationIgnored() {
        EnvelopeBus bus = new EnvelopeBus();
        BrickEnvelope reply = BrickMessages.reply(
                BrickMessages.request("sensor-01", "temp", null), "ok");
        bus.reply(reply); // 无待回包方，安全忽略
    }

    @Test
    void nullListenerRejected() {
        EnvelopeBus bus = new EnvelopeBus();
        assertThrows(NullPointerException.class, () -> bus.subscribe("sensor-01", "temp", null));
    }

    @Test
    void dupPublishToAllWildcards() {
        EnvelopeBus bus = new EnvelopeBus();
        AtomicInteger count = new AtomicInteger();
        bus.subscribe("*", "*", e -> count.incrementAndGet());
        bus.publish(BrickMessages.event("sensor-01", "temp", 1.0));
        assertEquals(1, count.get());
    }
}