package com.zqzqq.bootkits.protocol.adapter;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JacksonAdapterConfigLoaderTest {

    private final JacksonAdapterConfigLoader loader = new JacksonAdapterConfigLoader();

    @Test
    void shouldLoadValidConfig() {
        String json = "{"
                + "\"deviceType\": \"modbus-thermostat\","
                + "\"transport\": \"modbus\","
                + "\"defaultQos\": 1,"
                + "\"mappings\": ["
                + "  {\"capability\": \"temp\", \"type\": \"point\", \"access\": \"read\", \"register\": 40001, \"scale\": 0.1},"
                + "  {\"capability\": \"relay\", \"type\": \"point\", \"access\": \"read-write\", \"register\": 48001},"
                + "  {\"capability\": \"reset\", \"type\": \"command\", \"register\": 40011, \"writeValue\": 1}"
                + "]"
                + "}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        AdapterConfig config = loader.load(is);

        assertEquals("modbus-thermostat", config.getDeviceType());
        assertEquals("modbus", config.getTransport());
        assertEquals(1, config.getDefaultQos());
        assertEquals(3, config.getMappings().size());

        AdapterMapping temp = config.findMapping("temp");
        assertNotNull(temp);
        assertEquals("point", temp.getType());
        assertEquals("read", temp.getAccess());
        assertEquals(40001, temp.getIntOption("register", 0));
        assertEquals(0.1, temp.getDoubleOption("scale", 1.0), 0.001);

        AdapterMapping relay = config.findMapping("relay");
        assertNotNull(relay);
        assertEquals("read-write", relay.getAccess());

        AdapterMapping reset = config.findMapping("reset");
        assertNotNull(reset);
        assertEquals("command", reset.getType());
        assertEquals(1, reset.getIntOption("writeValue", 0));
    }

    @Test
    void shouldRejectEmptyDeviceType() {
        String json = "{\"deviceType\": \"\", \"transport\": \"modbus\", \"mappings\": []}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(is));
    }

    @Test
    void shouldRejectEmptyTransport() {
        String json = "{\"deviceType\": \"t\", \"transport\": \"\", \"mappings\": []}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(is));
    }

    @Test
    void shouldRejectMappingWithoutCapability() {
        String json = "{\"deviceType\": \"t\", \"transport\": \"m\", \"mappings\": [{\"type\": \"point\"}]}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(is));
    }

    @Test
    void shouldRejectMappingWithoutType() {
        String json = "{\"deviceType\": \"t\", \"transport\": \"m\", \"mappings\": [{\"capability\": \"c\"}]}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(is));
    }

    @Test
    void shouldRejectInvalidJson() {
        InputStream is = new ByteArrayInputStream("not json".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(is));
    }

    @Test
    void shouldRejectMissingResource() {
        assertThrows(IllegalArgumentException.class, () -> loader.load("nonexistent.json"));
    }

    @Test
    void shouldLoadFromClasspath() {
        AdapterConfig config = loader.load("brick-adapter-test.json");
        assertNotNull(config);
        assertEquals("test-device", config.getDeviceType());
    }
}