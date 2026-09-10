package com.zqzqq.bootkits.core.device;

import com.zqzqq.bootkits.protocol.adapter.DeviceAdapter;
import com.zqzqq.bootkits.protocol.adapter.TransportContext;
import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistryHotPlugTest {

    private EnvelopeBus bus;
    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        bus = new EnvelopeBus();
        registry = new DeviceRegistry(bus);
    }

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void shouldReconnectDevice() {
        ReconnectAdapter adapter = new ReconnectAdapter("s1");
        registry.register(adapter, new TransportContext());
        assertTrue(adapter.connected);
        adapter.connected = false;
        assertTrue(registry.reconnect("s1"));
        assertTrue(adapter.connected);
    }

    @Test
    void shouldFailReconnectForUnregisteredDevice() {
        assertFalse(registry.reconnect("nonexistent"));
    }

    @Test
    void shouldHandleReconnectException() {
        FailConnectAdapter adapter = new FailConnectAdapter("s1");
        registry.register(adapter, new TransportContext());
        adapter.failConnect = true;
        assertFalse(registry.reconnect("s1"));
    }

    @Test
    void shouldConcurrentRegisterUnregister() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String deviceId = "device-" + idx;
                    ReconnectAdapter adapter = new ReconnectAdapter(deviceId);
                    registry.register(adapter, new TransportContext());
                    successCount.incrementAndGet();
                    Thread.sleep(10);
                    registry.unregister(deviceId);
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, registry.getDeviceCount());
    }

    @Test
    void shouldHotPlugWhilePublishing() throws Exception {
        List<BrickEnvelope> received = Collections.synchronizedList(new ArrayList<>());
        bus.subscribe("*", "temp", received::add);

        ReconnectAdapter adapter = new ReconnectAdapter("s1");
        registry.register(adapter, new TransportContext());

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        bus.publish(BrickMessages.event("s1", "temp", j));
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(received.size() > 0);
    }

    @Test
    void shouldStartAndStopHealthCheck() throws Exception {
        ReconnectAdapter adapter = new ReconnectAdapter("s1");
        registry.register(adapter, new TransportContext());
        adapter.connected = false;

        registry.startHealthCheck(100);
        Thread.sleep(300);
        assertTrue(adapter.connected);

        registry.stopHealthCheck();
    }

    @Test
    void shouldNotStartHealthCheckTwice() {
        registry.startHealthCheck(1000);
        registry.startHealthCheck(1000);
        registry.stopHealthCheck();
    }

    @Test
    void shouldShutdownStopsHealthCheck() {
        registry.startHealthCheck(1000);
        registry.shutdown();
    }

    private static class ReconnectAdapter implements DeviceAdapter {
        private final String deviceId;
        private EnvelopeBus bus;
        private volatile boolean connected;
        private final Device device;

        ReconnectAdapter(String deviceId) {
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
        @Override public Object toDevice(BrickEnvelope e) { return null; }
        @Override public void onFromDevice(Object raw, TransportContext ctx) {}
    }

    private static class FailConnectAdapter implements DeviceAdapter {
        private final String deviceId;
        private volatile boolean failConnect;
        private volatile boolean connected;
        private final Device device;

        FailConnectAdapter(String deviceId) {
            this.deviceId = deviceId;
            this.device = new Device(deviceId, "stub", deviceId);
        }

        @Override public String getDeviceType() { return deviceId; }
        @Override public String getTransport() { return "stub"; }
        @Override public void connect(EnvelopeBus bus, TransportContext ctx) {
            if (failConnect) {
                throw new RuntimeException("连接失败");
            }
            this.connected = true;
        }
        @Override public void disconnect() { this.connected = false; }
        @Override public boolean isConnected() { return connected; }
        @Override public Device getDevice() { return device; }
        @Override public Object toDevice(BrickEnvelope e) { return null; }
        @Override public void onFromDevice(Object raw, TransportContext ctx) {}
    }
}