package com.zqzqq.bootkits.core.device;

import com.zqzqq.bootkits.protocol.adapter.DeviceAdapter;
import com.zqzqq.bootkits.protocol.adapter.TransportContext;
import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistryTest {

    private EnvelopeBus bus;
    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        bus = new EnvelopeBus();
        registry = new DeviceRegistry(bus);
    }

    @Test
    void shouldRegisterDevice() {
        StubAdapter adapter = new StubAdapter("sensor-01");
        registry.register(adapter, new TransportContext());
        assertEquals(1, registry.getDeviceCount());
        assertTrue(registry.isRegistered("sensor-01"));
        assertNotNull(registry.lookup("sensor-01"));
    }

    @Test
    void shouldUnregisterDevice() {
        StubAdapter adapter = new StubAdapter("sensor-01");
        registry.register(adapter, new TransportContext());
        registry.unregister("sensor-01");
        assertEquals(0, registry.getDeviceCount());
        assertFalse(registry.isRegistered("sensor-01"));
        assertFalse(adapter.connected);
    }

    @Test
    void shouldUnregisterNonExistentDevice() {
        registry.unregister("nonexistent");
        assertEquals(0, registry.getDeviceCount());
    }

    @Test
    void shouldGetAllDevices() {
        registry.register(new StubAdapter("a"), new TransportContext());
        registry.register(new StubAdapter("b"), new TransportContext());
        Collection<Device> devices = registry.getAllDevices();
        assertEquals(2, devices.size());
    }

    @Test
    void shouldGetAllDeviceIds() {
        registry.register(new StubAdapter("a"), new TransportContext());
        registry.register(new StubAdapter("b"), new TransportContext());
        Collection<String> ids = registry.getAllDeviceIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("a"));
        assertTrue(ids.contains("b"));
    }

    @Test
    void shouldGetAdapter() {
        StubAdapter adapter = new StubAdapter("sensor-01");
        registry.register(adapter, new TransportContext());
        assertSame(adapter, registry.getAdapter("sensor-01"));
        assertNull(registry.getAdapter("nonexistent"));
    }

    @Test
    void shouldNotifyListeners() {
        List<String> events = new ArrayList<>();
        registry.addListener(new DeviceRegistrationListener() {
            @Override
            public void onDeviceRegistered(Device device, DeviceAdapter adapter) {
                events.add("registered:" + device.getDeviceId());
            }
            @Override
            public void onDeviceUnregistered(Device device, DeviceAdapter adapter) {
                events.add("unregistered:" + device.getDeviceId());
            }
        });
        StubAdapter adapter = new StubAdapter("s1");
        registry.register(adapter, new TransportContext());
        registry.unregister("s1");
        assertEquals(2, events.size());
        assertEquals("registered:s1", events.get(0));
        assertEquals("unregistered:s1", events.get(1));
    }

    @Test
    void shouldHandleListenerException() {
        registry.addListener(new DeviceRegistrationListener() {
            @Override
            public void onDeviceRegistered(Device device, DeviceAdapter adapter) {
                throw new RuntimeException("boom");
            }
            @Override
            public void onDeviceUnregistered(Device device, DeviceAdapter adapter) {
            }
        });
        StubAdapter adapter = new StubAdapter("s1");
        assertDoesNotThrow(() -> registry.register(adapter, new TransportContext()));
    }

    @Test
    void shouldShutdownAllDevices() {
        StubAdapter a = new StubAdapter("a");
        StubAdapter b = new StubAdapter("b");
        registry.register(a, new TransportContext());
        registry.register(b, new TransportContext());
        registry.shutdown();
        assertEquals(0, registry.getDeviceCount());
        assertFalse(a.connected);
        assertFalse(b.connected);
    }

    @Test
    void shouldGetBus() {
        assertSame(bus, registry.getBus());
    }

    @Test
    void shouldRejectNullDeviceFromAdapter() {
        DeviceAdapter adapter = new DeviceAdapter() {
            @Override public String getDeviceType() { return "x"; }
            @Override public String getTransport() { return "x"; }
            @Override public void connect(EnvelopeBus bus, TransportContext ctx) {}
            @Override public void disconnect() {}
            @Override public boolean isConnected() { return false; }
            @Override public Device getDevice() { return null; }
            @Override public Object toDevice(com.zqzqq.bootkits.protocol.message.BrickEnvelope e) { return null; }
            @Override public void onFromDevice(Object raw, TransportContext ctx) {}
        };
        assertThrows(IllegalStateException.class, () -> registry.register(adapter, new TransportContext()));
    }

    @Test
    void shouldReplaceExistingDeviceOnReRegister() {
        StubAdapter old = new StubAdapter("s1");
        StubAdapter newAdapter = new StubAdapter("s1");
        registry.register(old, new TransportContext());
        registry.register(newAdapter, new TransportContext());
        assertEquals(1, registry.getDeviceCount());
        assertFalse(old.connected);
        assertTrue(newAdapter.connected);
    }

    private static class StubAdapter implements DeviceAdapter {
        private final String deviceId;
        private EnvelopeBus bus;
        private boolean connected;
        private final Device device;

        StubAdapter(String deviceId) {
            this.deviceId = deviceId;
            this.device = new Device(deviceId, "stub", deviceId);
        }

        @Override public String getDeviceType() { return deviceId; }
        @Override public String getTransport() { return "stub"; }
        @Override public void connect(EnvelopeBus bus, TransportContext ctx) {
            this.bus = bus;
            this.connected = true;
        }
        @Override public void disconnect() { this.connected = false; }
        @Override public boolean isConnected() { return connected; }
        @Override public Device getDevice() { return device; }
        @Override public Object toDevice(com.zqzqq.bootkits.protocol.message.BrickEnvelope e) { return null; }
        @Override public void onFromDevice(Object raw, TransportContext ctx) {}
    }
}