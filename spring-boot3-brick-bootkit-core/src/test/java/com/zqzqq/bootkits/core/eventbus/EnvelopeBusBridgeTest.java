package com.zqzqq.bootkits.core.eventbus;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import com.zqzqq.bootkits.protocol.message.QosLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EnvelopeBusBridgeTest {

    private EnvelopeBus envelopeBus;
    private PluginEventBus pluginEventBus;
    private EnvelopeBusBridge bridge;

    @BeforeEach
    void setUp() {
        envelopeBus = new EnvelopeBus();
        pluginEventBus = new PluginEventBus();
        bridge = new EnvelopeBusBridge(envelopeBus, pluginEventBus);
        bridge.start();
    }

    @AfterEach
    void tearDown() {
        bridge.stop();
        pluginEventBus.shutdown();
    }

    @Test
    void shouldBridgeEnvelopeToPluginEvent() throws Exception {
        List<PluginEvent> received = new ArrayList<>();
        pluginEventBus.registerListener("test-listener", new PluginEventListener() {
            @Override
            public void onEvent(PluginEvent event) {
                received.add(event);
            }
            @Override
            public boolean supportsType(PluginEvent.EventType type) {
                return type == PluginEvent.EventType.DEVICE_EVENT;
            }
        });

        BrickEnvelope envelope = BrickMessages.event("sensor-01", "temp", 25.5);
        envelopeBus.publish(envelope);

        Thread.sleep(100);
        assertFalse(received.isEmpty());
        PluginEvent event = received.get(0);
        assertEquals(PluginEvent.EventType.DEVICE_EVENT, event.getType());
        assertEquals("sensor-01", event.get("envelope.deviceId"));
        assertEquals("temp", event.get("envelope.capabilityId"));
        assertEquals("EVENT", event.get("envelope.type"));
        assertEquals(25.5, event.get("envelope.payload"));
    }

    @Test
    void shouldBridgePluginEventToEnvelope() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<BrickEnvelope> received = new ArrayList<>();
        envelopeBus.subscribe("sensor-02", "relay", envelope -> {
            received.add(envelope);
            latch.countDown();
        });

        PluginEvent event = new PluginEvent(PluginEvent.EventType.DEVICE_EVENT, "test-plugin");
        event.put("envelope.type", "COMMAND");
        event.put("envelope.deviceId", "sensor-02");
        event.put("envelope.capabilityId", "relay");
        event.put("envelope.messageId", "msg-001");
        event.put("envelope.payload", true);
        pluginEventBus.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(received.isEmpty());
        BrickEnvelope envelope = received.get(0);
        assertEquals(MessageType.COMMAND, envelope.getType());
        assertEquals("sensor-02", envelope.getDeviceId());
        assertEquals("relay", envelope.getCapabilityId());
        assertEquals(true, envelope.getPayload());
    }

    @Test
    void shouldNotBridgeLifecycleMessages() throws Exception {
        List<PluginEvent> received = new ArrayList<>();
        pluginEventBus.registerListener("test", event -> received.add(event));

        envelopeBus.publish(BrickMessages.lifecycle("sensor-01", com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle.ONLINE));
        Thread.sleep(100);
        assertTrue(received.isEmpty());
    }

    @Test
    void shouldPreventCircularPropagation() throws Exception {
        List<PluginEvent> pluginReceived = new ArrayList<>();
        pluginEventBus.registerListener("test", event -> pluginReceived.add(event));

        BrickEnvelope envelope = BrickMessages.event("s1", "temp", 10);
        envelopeBus.publish(envelope);
        Thread.sleep(100);
        assertEquals(1, pluginReceived.size());
    }

    @Test
    void shouldBridgeQosLevel() throws Exception {
        List<PluginEvent> received = new ArrayList<>();
        pluginEventBus.registerListener("test", event -> received.add(event));

        BrickEnvelope envelope = BrickMessages.request("s1", "temp", null, QosLevel.AT_LEAST_ONCE);
        envelopeBus.publish(envelope);
        Thread.sleep(100);
        assertFalse(received.isEmpty());
        assertEquals(Integer.valueOf(1), received.get(0).get("envelope.qos"));
    }

    @Test
    void shouldHandleIncompletePluginEvent() throws Exception {
        PluginEvent event = new PluginEvent(PluginEvent.EventType.DEVICE_EVENT, "test");
        event.put("envelope.type", "EVENT");
        // missing deviceId and capabilityId
        pluginEventBus.publish(event);
        Thread.sleep(100);
    }

    @Test
    void shouldStopWithoutError() {
        bridge.stop();
        bridge.stop();
    }
}