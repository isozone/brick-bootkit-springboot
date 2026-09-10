package com.zqzqq.bootkits.protocol.adapter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdapterMappingTest {

    @Test
    void shouldCreateDefaultMapping() {
        AdapterMapping m = new AdapterMapping();
        assertNull(m.getCapability());
        assertNull(m.getType());
        assertNotNull(m.getOptions());
        assertTrue(m.getOptions().isEmpty());
    }

    @Test
    void shouldCreateMappingWithFields() {
        AdapterMapping m = new AdapterMapping("temp", "point", "read");
        assertEquals("temp", m.getCapability());
        assertEquals("point", m.getType());
        assertEquals("read", m.getAccess());
    }

    @Test
    void shouldStoreAndGetOptions() {
        AdapterMapping m = new AdapterMapping();
        m.getOptions().put("register", 40001);
        m.getOptions().put("scale", 0.1);
        assertEquals(40001, m.getIntOption("register", 0));
        assertEquals(0.1, m.getDoubleOption("scale", 1.0), 0.001);
    }

    @Test
    void shouldReturnDefaultForMissingOptions() {
        AdapterMapping m = new AdapterMapping();
        assertEquals(99, m.getIntOption("missing", 99));
        assertEquals(1.5, m.getDoubleOption("missing", 1.5), 0.001);
    }

    @Test
    void shouldParseStringOptionsAsInt() {
        AdapterMapping m = new AdapterMapping();
        m.getOptions().put("register", "40001");
        assertEquals(40001, m.getIntOption("register", 0));
    }

    @Test
    void shouldParseStringOptionsAsDouble() {
        AdapterMapping m = new AdapterMapping();
        m.getOptions().put("scale", "0.1");
        assertEquals(0.1, m.getDoubleOption("scale", 1.0), 0.001);
    }

    @Test
    void shouldReturnInvalidNumberAsDefault() {
        AdapterMapping m = new AdapterMapping();
        m.getOptions().put("register", "not-a-number");
        assertEquals(42, m.getIntOption("register", 42));
        m.getOptions().put("scale", "not-a-number");
        assertEquals(1.0, m.getDoubleOption("scale", 1.0), 0.001);
    }

    @Test
    void shouldParseCapabilityAccess() {
        AdapterMapping m = new AdapterMapping();
        m.setAccess("read");
        assertEquals(CapabilityAccess.READ, m.getCapabilityAccess());
        m.setAccess("read-write");
        assertEquals(CapabilityAccess.READ_WRITE, m.getCapabilityAccess());
    }

    @Test
    void shouldSetOptions() {
        AdapterMapping m = new AdapterMapping();
        m.setOptions(null);
        assertNotNull(m.getOptions());
        m.setOptions(Map.of("k", "v"));
        assertEquals("v", m.getOption("k"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        AdapterMapping a = new AdapterMapping("temp", "point", "read");
        AdapterMapping b = new AdapterMapping("temp", "point", "read");
        AdapterMapping c = new AdapterMapping("relay", "point", "read");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void shouldImplementToString() {
        AdapterMapping m = new AdapterMapping("temp", "point", "read");
        assertTrue(m.toString().contains("temp"));
    }
}