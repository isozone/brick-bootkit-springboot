package com.zqzqq.bootkits.protocol.adapter;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.capability.CapabilityType;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractDeclarativeDeviceAdapterTest {

    private EnvelopeBus bus;
    private StubAdapter adapter;

    @BeforeEach
    void setUp() {
        bus = new EnvelopeBus();
        AdapterConfig config = new AdapterConfig();
        config.setDeviceType("test-device");
        config.setTransport("test");
        config.setMappings(List.of(
                new AdapterMapping("temp", "point", "read"),
                new AdapterMapping("relay", "point", "read-write"),
                new AdapterMapping("reset", "command", null),
                new AdapterMapping("alert", "event", null)
        ));
        adapter = new StubAdapter(config);
    }

    @Test
    void shouldBuildDeviceFromConfig() {
        adapter.connect(bus, new TransportContext());
        Device device = adapter.getDevice();
        assertNotNull(device);
        assertEquals("test-device", device.getDeviceId());
        assertEquals(4, device.getCapabilities().size());
        assertNotNull(device.getCapability("temp"));
        assertEquals(CapabilityType.POINT, device.getCapability("temp").getType());
        assertNotNull(device.getCapability("reset"));
        assertEquals(CapabilityType.COMMAND, device.getCapability("reset").getType());
        assertNotNull(device.getCapability("alert"));
        assertEquals(CapabilityType.EVENT, device.getCapability("alert").getType());
    }

    @Test
    void shouldSetWritableFlag() {
        adapter.connect(bus, new TransportContext());
        Device device = adapter.getDevice();
        var temp = (com.zqzqq.bootkits.protocol.capability.PointCapability) device.getCapability("temp");
        assertFalse(temp.isWritable());
        var relay = (com.zqzqq.bootkits.protocol.capability.PointCapability) device.getCapability("relay");
        assertTrue(relay.isWritable());
    }

    @Test
    void shouldTranslateReadRequest() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope request = BrickMessages.request("test-device", "temp", null);
        Object result = adapter.toDevice(request);
        assertEquals(42, result);
        assertEquals(1, adapter.readCount);
    }

    @Test
    void shouldTranslateWriteCommand() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope cmd = BrickMessages.command("test-device", "relay", true);
        Object result = adapter.toDevice(cmd);
        assertEquals("ok", result);
        assertEquals(1, adapter.writeCount);
        assertTrue(adapter.lastWriteValue instanceof Boolean);
    }

    @Test
    void shouldRejectUnknownCapability() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope request = BrickMessages.request("test-device", "nonexistent", null);
        assertThrows(IllegalArgumentException.class, () -> adapter.toDevice(request));
    }

    @Test
    void shouldRejectUnsupportedMessageType() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope event = BrickMessages.event("test-device", "alert", "fire");
        assertThrows(IllegalArgumentException.class, () -> adapter.toDevice(event));
    }

    @Test
    void shouldPublishLifecycleOnConnect() {
        List<BrickEnvelope> lifecycleEvents = new ArrayList<>();
        bus.subscribe("*", "lifecycle", lifecycleEvents::add);
        adapter.connect(bus, new TransportContext());
        assertEquals(1, lifecycleEvents.size());
        assertEquals(DeviceLifecycle.ONLINE.getEventCode(), lifecycleEvents.get(0).getPayload());
    }

    @Test
    void shouldPublishLifecycleOnDisconnect() {
        adapter.connect(bus, new TransportContext());
        List<BrickEnvelope> lifecycleEvents = new ArrayList<>();
        bus.subscribe("*", "lifecycle", lifecycleEvents::add);
        adapter.disconnect();
        assertEquals(1, lifecycleEvents.size());
        assertEquals(DeviceLifecycle.OFFLINE.getEventCode(), lifecycleEvents.get(0).getPayload());
        assertFalse(adapter.isConnected());
    }

    @Test
    void shouldNotDisconnectTwice() {
        adapter.connect(bus, new TransportContext());
        adapter.disconnect();
        assertEquals(1, adapter.disconnectCount);
        adapter.disconnect();
        assertEquals(1, adapter.disconnectCount);
    }

    @Test
    void shouldRouteBusMessagesToAdapter() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope request = BrickMessages.request("test-device", "temp", null);
        BrickEnvelope reply = bus.request(request, 2, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(reply);
        assertEquals(MessageType.REPLY, reply.getType());
        assertEquals(42, reply.getPayload());
    }

    @Test
    void shouldIgnoreMessagesForOtherDevices() {
        adapter.connect(bus, new TransportContext());
        BrickEnvelope request = BrickMessages.request("other-device", "temp", null);
        BrickEnvelope reply = bus.request(request, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertNull(reply);
        assertEquals(0, adapter.readCount);
    }

    @Test
    void shouldHandleNullTranslateResult() {
        adapter.connect(bus, new TransportContext());
        adapter.nullResult = true;
        BrickEnvelope request = BrickMessages.request("test-device", "temp", null);
        BrickEnvelope reply = bus.request(request, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertNull(reply);
    }

    @Test
    void shouldGetDeviceTypeAndTransport() {
        assertEquals("test-device", adapter.getDeviceType());
        assertEquals("test", adapter.getTransport());
    }

    @Test
    void shouldReturnNullDeviceBeforeConnect() {
        assertNull(adapter.getDevice());
    }

    @Test
    void shouldHandleResourcePathConstructor() {
        AdapterConfig config = new AdapterConfig();
        config.setDeviceType("t");
        config.setTransport("t");
        config.setMappings(List.of());
        StubAdapter a = new StubAdapter(config);
        assertEquals("t", a.getDeviceType());
    }

    private static class StubAdapter extends AbstractDeclarativeDeviceAdapter {
        int readCount;
        int writeCount;
        int disconnectCount;
        Object lastWriteValue;
        boolean nullResult;

        StubAdapter(AdapterConfig config) {
            super(config);
        }

        @Override
        protected void doConnect(TransportContext context) {
        }

        @Override
        protected void doDisconnect() {
            disconnectCount++;
        }

        @Override
        protected Object doRead(AdapterMapping mapping) {
            readCount++;
            return nullResult ? null : 42;
        }

        @Override
        protected void doWrite(AdapterMapping mapping, Object value) {
            writeCount++;
            lastWriteValue = value;
        }

        @Override
        protected BrickEnvelope doTranslateFromDevice(Object rawFrame, TransportContext context) {
            return null;
        }
    }
}