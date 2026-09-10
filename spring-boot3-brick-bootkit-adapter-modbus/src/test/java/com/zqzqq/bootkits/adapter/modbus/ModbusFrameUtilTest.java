package com.zqzqq.bootkits.adapter.modbus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusFrameUtilTest {

    @Test
    void shouldCalculateCrc16() {
        byte[] data = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01};
        int crc = ModbusFrameUtil.crc16(data);
        assertTrue(crc >= 0 && crc <= 0xFFFF);
    }

    @Test
    void shouldBuildReadHoldingRegistersRequest() {
        byte[] frame = ModbusFrameUtil.buildReadHoldingRegistersRequest(1, 40001, 1);
        assertEquals(8, frame.length);
        assertEquals(0x01, frame[0] & 0xFF);
        assertEquals(ModbusFrameUtil.FUNC_READ_HOLDING_REGISTERS, frame[1]);
        assertEquals(0x9C, frame[2] & 0xFF);
        assertEquals(0x41, frame[3] & 0xFF);
        assertEquals(0x00, frame[4] & 0xFF);
        assertEquals(0x01, frame[5] & 0xFF);
        assertTrue(ModbusFrameUtil.verifyCrc(frame));
    }

    @Test
    void shouldBuildWriteSingleRegisterRequest() {
        byte[] frame = ModbusFrameUtil.buildWriteSingleRegisterRequest(1, 48001, 255);
        assertEquals(8, frame.length);
        assertEquals(0x01, frame[0] & 0xFF);
        assertEquals(ModbusFrameUtil.FUNC_WRITE_SINGLE_REGISTER, frame[1]);
        assertEquals(0xBB, frame[2] & 0xFF);
        assertEquals(0x81, frame[3] & 0xFF);
        assertEquals(0x00, frame[4] & 0xFF);
        assertEquals(0xFF, frame[5] & 0xFF);
        assertTrue(ModbusFrameUtil.verifyCrc(frame));
    }

    @Test
    void shouldBuildWriteSingleCoilRequest() {
        byte[] onFrame = ModbusFrameUtil.buildWriteSingleCoilRequest(1, 100, true);
        assertEquals(0xFF, onFrame[4] & 0xFF);
        assertEquals(0x00, onFrame[5] & 0xFF);
        assertTrue(ModbusFrameUtil.verifyCrc(onFrame));

        byte[] offFrame = ModbusFrameUtil.buildWriteSingleCoilRequest(1, 100, false);
        assertEquals(0x00, offFrame[4] & 0xFF);
        assertEquals(0x00, offFrame[5] & 0xFF);
        assertTrue(ModbusFrameUtil.verifyCrc(offFrame));
    }

    @Test
    void shouldVerifyCrc() {
        byte[] frame = ModbusFrameUtil.buildReadHoldingRegistersRequest(1, 40001, 1);
        assertTrue(ModbusFrameUtil.verifyCrc(frame));
        frame[0] = 0x02;
        assertFalse(ModbusFrameUtil.verifyCrc(frame));
    }

    @Test
    void shouldRejectNullFrameForCrc() {
        assertFalse(ModbusFrameUtil.verifyCrc(null));
        assertFalse(ModbusFrameUtil.verifyCrc(new byte[]{0x01}));
    }

    @Test
    void shouldParseReadHoldingRegistersResponse() {
        byte[] frame = buildReadResponse(1, new int[]{256});
        int[] values = ModbusFrameUtil.parseReadHoldingRegistersResponse(frame);
        assertEquals(1, values.length);
        assertEquals(256, values[0]);
    }

    @Test
    void shouldParseReadResponseMultipleRegisters() {
        byte[] frame = buildReadResponse(1, new int[]{100, 200, 300});
        int[] values = ModbusFrameUtil.parseReadHoldingRegistersResponse(frame);
        assertEquals(3, values.length);
        assertEquals(100, values[0]);
        assertEquals(200, values[1]);
        assertEquals(300, values[2]);
    }

    @Test
    void shouldGetSlaveAddress() {
        byte[] frame = ModbusFrameUtil.buildReadHoldingRegistersRequest(5, 0, 1);
        assertEquals(5, ModbusFrameUtil.getSlaveAddress(frame));
    }

    @Test
    void shouldGetFunctionCode() {
        byte[] frame = ModbusFrameUtil.buildReadHoldingRegistersRequest(1, 0, 1);
        assertEquals(ModbusFrameUtil.FUNC_READ_HOLDING_REGISTERS, ModbusFrameUtil.getFunctionCode(frame));
    }

    @Test
    void shouldDetectExceptionResponse() {
        byte[] frame = {0x01, (byte) 0x83, 0x02, 0x00, 0x00};
        int dataCrc = ModbusFrameUtil.crc16(frame, 0, 3);
        frame[3] = (byte) (dataCrc & 0xFF);
        frame[4] = (byte) ((dataCrc >> 8) & 0xFF);
        assertTrue(ModbusFrameUtil.isException(frame));
        assertEquals(0x02, ModbusFrameUtil.getExceptionCode(frame));
    }

    @Test
    void shouldThrowModbusExceptionOnExceptionResponse() {
        byte[] frame = {0x01, (byte) 0x83, 0x02, 0x00, 0x00};
        int dataCrc = ModbusFrameUtil.crc16(frame, 0, 3);
        frame[3] = (byte) (dataCrc & 0xFF);
        frame[4] = (byte) ((dataCrc >> 8) & 0xFF);
        assertThrows(ModbusFrameUtil.ModbusException.class,
                () -> ModbusFrameUtil.parseReadHoldingRegistersResponse(frame));
    }

    @Test
    void shouldCalculateCrcRange() {
        byte[] data = {0x01, 0x03, 0x02, 0x01, 0x00};
        int fullCrc = ModbusFrameUtil.crc16(data);
        int partialCrc = ModbusFrameUtil.crc16(data, 0, 3);
        assertNotEquals(fullCrc, partialCrc);
    }

    private byte[] buildReadResponse(int slave, int[] values) {
        int byteCount = values.length * 2;
        byte[] frame = new byte[5 + byteCount];
        frame[0] = (byte) slave;
        frame[1] = ModbusFrameUtil.FUNC_READ_HOLDING_REGISTERS;
        frame[2] = (byte) byteCount;
        for (int i = 0; i < values.length; i++) {
            frame[3 + i * 2] = (byte) ((values[i] >> 8) & 0xFF);
            frame[4 + i * 2] = (byte) (values[i] & 0xFF);
        }
        int crc = ModbusFrameUtil.crc16(frame, 0, frame.length - 2);
        frame[frame.length - 2] = (byte) (crc & 0xFF);
        frame[frame.length - 1] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }
}