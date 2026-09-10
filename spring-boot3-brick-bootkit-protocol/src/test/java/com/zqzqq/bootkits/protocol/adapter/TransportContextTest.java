package com.zqzqq.bootkits.protocol.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportContextTest {

    @Test
    void shouldStoreAndRetrieveStringValues() {
        TransportContext ctx = new TransportContext();
        ctx.put("portName", "COM1");
        assertEquals("COM1", ctx.getString("portName"));
        assertEquals("COM1", ctx.get("portName"));
    }

    @Test
    void shouldStoreAndRetrieveIntValues() {
        TransportContext ctx = new TransportContext();
        ctx.put("baudRate", 9600);
        assertEquals(Integer.valueOf(9600), ctx.getInt("baudRate"));
        assertEquals(9600, ctx.getInt("baudRate", 0));
    }

    @Test
    void shouldReturnDefaultForMissingKeys() {
        TransportContext ctx = new TransportContext();
        assertEquals(42, ctx.getInt("missing", 42));
        assertEquals("default", ctx.getString("missing", "default"));
        assertNull(ctx.getInt("missing"));
        assertNull(ctx.getString("missing"));
    }

    @Test
    void shouldHandleEmptyContext() {
        TransportContext ctx = new TransportContext();
        assertTrue(ctx.isEmpty());
        assertEquals(0, ctx.size());
    }

    @Test
    void shouldSupportBulkConstruction() {
        TransportContext ctx = new TransportContext(java.util.Map.of("a", 1, "b", "two"));
        assertEquals(2, ctx.size());
        assertFalse(ctx.isEmpty());
        assertTrue(ctx.has("a"));
        assertFalse(ctx.has("c"));
    }

    @Test
    void shouldSupportChainedPut() {
        TransportContext ctx = new TransportContext();
        TransportContext result = ctx.put("x", 1).put("y", 2);
        assertSame(result, ctx);
        assertEquals(2, ctx.size());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        TransportContext a = new TransportContext(java.util.Map.of("k", "v"));
        TransportContext b = new TransportContext(java.util.Map.of("k", "v"));
        TransportContext c = new TransportContext(java.util.Map.of("k", "other"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void shouldImplementToString() {
        TransportContext ctx = new TransportContext();
        ctx.put("key", "val");
        assertTrue(ctx.toString().contains("key"));
    }
}