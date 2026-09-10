package com.zqzqq.bootkits.transport.mqtt;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.serialization.JacksonEnvelopeSerializer;
import com.zqzqq.bootkits.protocol.transport.EnvelopeTransport;
import com.zqzqq.bootkits.protocol.transport.RemoteEnvelopeBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MqttTransportIntegrationTest {

    private JacksonEnvelopeSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonEnvelopeSerializer();
    }

    @Test
    void shouldSerializeEnvelopeToMqttPayload() {
        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);
        byte[] payload = serializer.serialize(envelope);

        assertNotNull(payload);
        assertTrue(payload.length > 0);

        String json = new String(payload, StandardCharsets.UTF_8);
        assertTrue(json.contains("sensor-01"));
        assertTrue(json.contains("temp"));
        assertTrue(json.contains("EVENT"));
    }

    @Test
    void shouldDeserializeMqttPayloadToEnvelope() {
        BrickEnvelope original = BrickMessages.command("relay-01", "switch", true);
        byte[] payload = serializer.serialize(original);

        BrickEnvelope restored = serializer.deserialize(payload);
        assertEquals("relay-01", restored.getDeviceId());
        assertEquals("switch", restored.getCapabilityId());
        assertEquals(true, restored.getPayload());
    }

    @Test
    void shouldRoundTripAllMessageTypes() {
        List<BrickEnvelope> originals = List.of(
                BrickMessages.request("s1", "temp", null),
                BrickMessages.reply("s1", "temp", 25.5, null),
                BrickMessages.event("s1", "temp", 25.5),
                BrickMessages.command("s1", "relay", true),
                BrickMessages.lifecycle("s1", com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle.ONLINE)
        );

        for (BrickEnvelope original : originals) {
            byte[] payload = serializer.serialize(original);
            BrickEnvelope restored = serializer.deserialize(payload);
            assertEquals(original.getType(), restored.getType());
            assertEquals(original.getDeviceId(), restored.getDeviceId());
            assertEquals(original.getCapabilityId(), restored.getCapabilityId());
        }
    }

    @Test
    void shouldRoundTripQosLevel() {
        BrickEnvelope atMost = BrickMessages.request("s1", "temp", null, QosLevel.AT_MOST_ONCE);
        BrickEnvelope atLeast = BrickMessages.request("s1", "temp", null, QosLevel.AT_LEAST_ONCE);

        assertEquals(0, serializer.deserialize(serializer.serialize(atMost)).getQos().ordinal());
        assertEquals(1, serializer.deserialize(serializer.serialize(atLeast)).getQos().ordinal());
    }

    @Test
    void shouldTopicContainDeviceAndCapability() {
        MqttTransportConfig config = new MqttTransportConfig("tcp://localhost:1883", "test");
        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);

        String topic = config.buildTopic(envelope.getDeviceId(), envelope.getCapabilityId());
        assertEquals("brick/sensor-01/temp", topic);
    }

    @Test
    void shouldRemoteEnvelopeBusWorkWithMockTransport() throws Exception {
        EnvelopeBus localBus = new EnvelopeBus();
        MockTransport transport = new MockTransport();
        RemoteEnvelopeBus remoteBus = new RemoteEnvelopeBus(localBus, transport);
        remoteBus.start();

        List<BrickEnvelope> received = new ArrayList<>();
        localBus.subscribe("sensor-01", "temp", received::add);

        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);
        transport.simulateIncoming(envelope);

        Thread.sleep(100);
        assertEquals(1, received.size());
        assertEquals("sensor-01", received.get(0).getDeviceId());

        remoteBus.stop();
    }

    @Test
    void shouldRemoteEnvelopeBusBridgeDownstream() throws Exception {
        EnvelopeBus localBus = new EnvelopeBus();
        MockTransport transport = new MockTransport();
        RemoteEnvelopeBus remoteBus = new RemoteEnvelopeBus(localBus, transport);
        remoteBus.start();

        CountDownLatch latch = new CountDownLatch(1);
        transport.setOutgoingHandler(e -> latch.countDown());

        localBus.publish(BrickMessages.request("sensor-01", "temp", null));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(transport.getLastPublished());
        remoteBus.stop();
    }

    static class MockTransport implements EnvelopeTransport {
        private volatile boolean connected = false;
        private volatile TransportListener listener;
        private volatile java.util.function.Consumer<BrickEnvelope> outgoingHandler;
        private BrickEnvelope lastPublished;

        @Override
        public void start(TransportListener listener) {
            this.listener = listener;
            this.connected = true;
        }

        @Override
        public void stop() { this.connected = false; }

        @Override
        public boolean isConnected() { return connected; }

        @Override
        public void publish(BrickEnvelope envelope) {
            lastPublished = envelope;
            if (outgoingHandler != null) outgoingHandler.accept(envelope);
        }

        void simulateIncoming(BrickEnvelope envelope) {
            if (listener != null) listener.onMessage(envelope);
        }

        BrickEnvelope getLastPublished() { return lastPublished; }

        void setOutgoingHandler(java.util.function.Consumer<BrickEnvelope> handler) {
            this.outgoingHandler = handler;
        }
    }
}