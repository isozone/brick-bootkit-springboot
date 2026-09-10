package com.zqzqq.bootkits.protocol.adapter;

import com.zqzqq.bootkits.protocol.message.QosLevel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AdapterConfigTest {

    @Test
    void shouldCreateDefaultConfig() {
        AdapterConfig config = new AdapterConfig();
        assertNull(config.getDeviceType());
        assertNull(config.getTransport());
        assertEquals(QosLevel.FIRE_FORGET.getCode(), config.getDefaultQos());
        assertNotNull(config.getMappings());
        assertTrue(config.getMappings().isEmpty());
    }

    @Test
    void shouldFindMappingByCapabilityId() {
        AdapterConfig config = new AdapterConfig();
        config.setDeviceType("thermostat");
        config.setTransport("modbus");
        config.setMappings(Arrays.asList(
                new AdapterMapping("temp", "point", "read"),
                new AdapterMapping("relay", "point", "read-write"),
                new AdapterMapping("reset", "command", null)
        ));
        assertNotNull(config.findMapping("temp"));
        assertNotNull(config.findMapping("relay"));
        assertNotNull(config.findMapping("reset"));
        assertNull(config.findMapping("missing"));
    }

    @Test
    void shouldReturnNullForNullCapabilityId() {
        AdapterConfig config = new AdapterConfig();
        config.setMappings(Collections.singletonList(new AdapterMapping("temp", "point", "read")));
        assertNull(config.findMapping(null));
    }

    @Test
    void shouldReturnNullForNullMappings() {
        AdapterConfig config = new AdapterConfig();
        config.setMappings(null);
        assertNull(config.findMapping("temp"));
    }

    @Test
    void shouldConvertDefaultQosToLevel() {
        AdapterConfig config = new AdapterConfig();
        config.setDefaultQos(0);
        assertEquals(QosLevel.FIRE_FORGET, config.getDefaultQosLevel());
        config.setDefaultQos(1);
        assertEquals(QosLevel.AT_LEAST_ONCE, config.getDefaultQosLevel());
    }

    @Test
    void shouldSetMappingsNull() {
        AdapterConfig config = new AdapterConfig();
        config.setMappings(null);
        assertNotNull(config.getMappings());
        assertTrue(config.getMappings().isEmpty());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        AdapterConfig a = new AdapterConfig();
        a.setDeviceType("t");
        a.setTransport("m");
        AdapterConfig b = new AdapterConfig();
        b.setDeviceType("t");
        b.setTransport("m");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldImplementToString() {
        AdapterConfig config = new AdapterConfig();
        config.setDeviceType("t");
        assertTrue(config.toString().contains("t"));
    }
}