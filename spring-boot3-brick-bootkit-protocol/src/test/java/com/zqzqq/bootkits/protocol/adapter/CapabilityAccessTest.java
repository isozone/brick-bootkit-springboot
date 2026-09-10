package com.zqzqq.bootkits.protocol.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityAccessTest {

    @Test
    void shouldParseReadAccess() {
        assertEquals(CapabilityAccess.READ, CapabilityAccess.parse("read"));
        assertEquals(CapabilityAccess.READ, CapabilityAccess.parse("r"));
    }

    @Test
    void shouldParseReadWriteAccess() {
        assertEquals(CapabilityAccess.READ_WRITE, CapabilityAccess.parse("read-write"));
        assertEquals(CapabilityAccess.READ_WRITE, CapabilityAccess.parse("read_write"));
        assertEquals(CapabilityAccess.READ_WRITE, CapabilityAccess.parse("rw"));
    }

    @Test
    void shouldReturnNullForNull() {
        assertNull(CapabilityAccess.parse(null));
    }

    @Test
    void shouldThrowOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> CapabilityAccess.parse("write"));
    }

    @Test
    void shouldReportWritable() {
        assertFalse(CapabilityAccess.READ.isWritable());
        assertTrue(CapabilityAccess.READ_WRITE.isWritable());
    }

    @Test
    void shouldHandleCaseInsensitive() {
        assertEquals(CapabilityAccess.READ, CapabilityAccess.parse("READ"));
        assertEquals(CapabilityAccess.READ_WRITE, CapabilityAccess.parse("Read-Write"));
    }

    @Test
    void shouldHandleWhitespace() {
        assertEquals(CapabilityAccess.READ, CapabilityAccess.parse("  read  "));
    }
}