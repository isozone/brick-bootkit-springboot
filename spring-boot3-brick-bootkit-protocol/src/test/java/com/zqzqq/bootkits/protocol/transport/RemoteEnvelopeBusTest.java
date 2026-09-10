package com.zqzqq.bootkits.protocol.transport;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RemoteEnvelopeBusTest {

    private EnvelopeBus localBus;
    private MockTransport transport;
    private RemoteEnvelopeBus remoteBus;

    @BeforeEach
    void setUp() {
        localBus = new EnvelopeBus();
        transport = new MockTransport();
        remoteBus = new RemoteEnvelopeBus(localBus, transport);
        remoteBus.start();
    }

    @AfterEach
    void tearDown() {
        remoteBus.stop();
    }

    @Test
    void shouldStartAndStop() {
        assertTrue(remoteBus.isStarted());
        remoteBus.stop();
        assertFalse(remoteBus.isStarted());
    }

    @Test
    void shouldNotStartTwice() {
        remoteBus.start();
        assertTrue(remoteBus.isStarted());
    }

    @Test
    void shouldNotStopTwice() {
        remoteBus.stop();
        remoteBus.stop();
    }

    @Test
    void shouldBridgeUpstream() throws Exception {
        List<BrickEnvelope> received = new ArrayList<>();
        localBus.subscribe("sensor-01", "temp", received::add);

        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);
        transport.simulateIncoming(envelope);

        Thread.sleep(100);
        assertEquals(1, received.size());
        assertEquals(25.5, received.get(0).getPayload());
    }

    @Test
    void shouldBridgeDownstream() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        transport.setOutgoingHandler(envelope -> {
            latch.countDown();
        });

        BrickEnvelope envelope = BrickMessages.request("sensor-01", "temp", null);
        localBus.publish(envelope);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(transport.getLastPublished());
        assertEquals(MessageType.REQUEST, transport.getLastPublished().getType());
    }

    @Test
    void shouldBridgeDownstreamCommand() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        transport.setOutgoingHandler(envelope -> latch.countDown());

        BrickEnvelope envelope = BrickMessages.command("sensor-01", "relay", true);
        localBus.publish(envelope);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(MessageType.COMMAND, transport.getLastPublished().getType());
        assertEquals(true, transport.getLastPublished().getPayload());
    }

    @Test
    void shouldNotBridgeUpstreamEventsToTransport() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        transport.setOutgoingHandler(envelope -> latch.countDown());

        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);
        transport.simulateIncoming(envelope);

        assertFalse(latch.await(300, TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldProvideLocalBusAccess() {
        assertNotNull(remoteBus.getLocalBus());
        assertSame(localBus, remoteBus.getLocalBus());
    }

    @Test
    void shouldProvideTransportAccess() {
        assertNotNull(remoteBus.getTransport());
        assertSame(transport, remoteBus.getTransport());
    }

    @Test
    void shouldHandleTransportException() {
        transport.setFailPublish(true);
        remoteBus.stop();
        remoteBus = new RemoteEnvelopeBus(localBus, transport);
        remoteBus.start();

        BrickEnvelope envelope = BrickMessages.request("s1", "temp", null);
        assertDoesNotThrow(() -> localBus.publish(envelope));
    }

    @Test
    void shouldHandleMultipleUpstreamMessages() throws Exception {
        List<BrickEnvelope> received = new ArrayList<>();
        localBus.subscribe("*", "*", received::add);

        for (int i = 0; i < 5; i++) {
            transport.simulateIncoming(BrickMessages.event("s1", "temp", i));
        }

        Thread.sleep(200);
        assertEquals(5, received.size());
    }

    static class MockTransport implements EnvelopeTransport {
        private final List<BrickEnvelope> published = new ArrayList<>();
        private volatile boolean connected = false;
        private volatile boolean failPublish = false;
        private volatile TransportListener listener;
        private volatile java.util.function.Consumer<BrickEnvelope> outgoingHandler;

        @Override
        public void start(TransportListener listener) {
            this.listener = listener;
            this.connected = true;
        }

        @Override
        public void stop() {
            this.connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void publish(BrickEnvelope envelope) {
            if (failPublish) throw new RuntimeException("publish failed");
            published.add(envelope);
            if (outgoingHandler != null) outgoingHandler.accept(envelope);
        }

        void simulateIncoming(BrickEnvelope envelope) {
            if (listener != null) listener.onMessage(envelope);
        }

        BrickEnvelope getLastPublished() {
            return published.isEmpty() ? null : published.get(published.size() - 1);
        }

        void setOutgoingHandler(java.util.function.Consumer<BrickEnvelope> handler) {
            this.outgoingHandler = handler;
        }

        void setFailPublish(boolean fail) {
            this.failPublish = fail;
        }
    }
}