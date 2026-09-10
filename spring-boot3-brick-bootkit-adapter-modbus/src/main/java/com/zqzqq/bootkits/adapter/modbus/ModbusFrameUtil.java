/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.adapter.modbus;

import java.io.Serializable;
import java.util.Objects;

/**
 * Modbus RTU 帧工具：CRC16 校验 + 帧构造/解析。
 * <p>
 * RTU 帧格式：[从站地址(1)] [功能码(1)] [数据(N)] [CRC16(2)]
 * <p>
 * 本类为纯工具类，无外部依赖，可独立测试。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public final class ModbusFrameUtil {

    /** 功能码：读保持寄存器 */
    public static final byte FUNC_READ_HOLDING_REGISTERS = 0x03;

    /** 功能码：写单个寄存器 */
    public static final byte FUNC_WRITE_SINGLE_REGISTER = 0x06;

    /** 功能码：写单个线圈 */
    public static final byte FUNC_WRITE_SINGLE_COIL = 0x05;

    /** 异或标志：功能码最高位为 1 表示异常响应 */
    public static final byte EXCEPTION_FLAG = (byte) 0x80;

    /** CRC16 查表 */
    private static final int[] CRC_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = crc >>> 1;
                }
            }
            CRC_TABLE[i] = crc;
        }
    }

    private ModbusFrameUtil() {
    }

    /**
     * 计算 CRC16（Modbus 专用多项式 0xA001）。
     *
     * @param data 数据
     * @return CRC16 值（unsigned int）
     */
    public static int crc16(byte[] data) {
        return crc16(data, 0, data.length);
    }

    /**
     * 计算 CRC16（指定范围）。
     *
     * @param data   数据
     * @param offset 起始偏移
     * @param length 长度
     * @return CRC16 值
     */
    public static int crc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ data[i]) & 0xFF];
        }
        return crc;
    }

    /**
     * 构造读保持寄存器请求帧。
     *
     * @param slaveAddress 从站地址（1-247）
     * @param register     起始寄存器地址
     * @param count        寄存器数量
     * @return 完整 RTU 帧（含 CRC）
     */
    public static byte[] buildReadHoldingRegistersRequest(int slaveAddress, int register, int count) {
        byte[] frame = new byte[8];
        frame[0] = (byte) slaveAddress;
        frame[1] = FUNC_READ_HOLDING_REGISTERS;
        frame[2] = (byte) ((register >> 8) & 0xFF);
        frame[3] = (byte) (register & 0xFF);
        frame[4] = (byte) ((count >> 8) & 0xFF);
        frame[5] = (byte) (count & 0xFF);
        appendCrc(frame, 6);
        return frame;
    }

    /**
     * 构造写单个寄存器请求帧。
     *
     * @param slaveAddress 从站地址
     * @param register     寄存器地址
     * @param value        写入值
     * @return 完整 RTU 帧
     */
    public static byte[] buildWriteSingleRegisterRequest(int slaveAddress, int register, int value) {
        byte[] frame = new byte[8];
        frame[0] = (byte) slaveAddress;
        frame[1] = FUNC_WRITE_SINGLE_REGISTER;
        frame[2] = (byte) ((register >> 8) & 0xFF);
        frame[3] = (byte) (register & 0xFF);
        frame[4] = (byte) ((value >> 8) & 0xFF);
        frame[5] = (byte) (value & 0xFF);
        appendCrc(frame, 6);
        return frame;
    }

    /**
     * 构造写单个线圈请求帧。
     *
     * @param slaveAddress 从站地址
     * @param coil         线圈地址
     * @param value        true=ON(0xFF00), false=OFF(0x0000)
     * @return 完整 RTU 帧
     */
    public static byte[] buildWriteSingleCoilRequest(int slaveAddress, int coil, boolean value) {
        byte[] frame = new byte[8];
        frame[0] = (byte) slaveAddress;
        frame[1] = FUNC_WRITE_SINGLE_COIL;
        frame[2] = (byte) ((coil >> 8) & 0xFF);
        frame[3] = (byte) (coil & 0xFF);
        if (value) {
            frame[4] = (byte) 0xFF;
            frame[5] = 0x00;
        } else {
            frame[4] = 0x00;
            frame[5] = 0x00;
        }
        appendCrc(frame, 6);
        return frame;
    }

    /**
     * 校验响应帧的 CRC。
     *
     * @param frame 完整响应帧（含 CRC）
     * @return true 如果 CRC 正确
     */
    public static boolean verifyCrc(byte[] frame) {
        if (frame == null || frame.length < 4) {
            return false;
        }
        int dataCrc = crc16(frame, 0, frame.length - 2);
        int frameCrc = (frame[frame.length - 2] & 0xFF) | ((frame[frame.length - 1] & 0xFF) << 8);
        return dataCrc == frameCrc;
    }

    /**
     * 解析读保持寄存器响应：提取寄存器值数组。
     *
     * @param frame 完整响应帧
     * @return 寄存器值数组
     * @throws IllegalArgumentException 帧格式错误或异常响应
     */
    public static int[] parseReadHoldingRegistersResponse(byte[] frame) {
        validateResponse(frame, FUNC_READ_HOLDING_REGISTERS);
        int byteCount = frame[2] & 0xFF;
        int registerCount = byteCount / 2;
        int[] values = new int[registerCount];
        for (int i = 0; i < registerCount; i++) {
            int offset = 3 + i * 2;
            values[i] = ((frame[offset] & 0xFF) << 8) | (frame[offset + 1] & 0xFF);
        }
        return values;
    }

    /**
     * 从响应帧中提取从站地址。
     */
    public static int getSlaveAddress(byte[] frame) {
        return frame[0] & 0xFF;
    }

    /**
     * 从响应帧中提取功能码。
     */
    public static byte getFunctionCode(byte[] frame) {
        return frame[1];
    }

    /**
     * 判断是否为异常响应。
     */
    public static boolean isException(byte[] frame) {
        return (frame[1] & EXCEPTION_FLAG) != 0;
    }

    /**
     * 获取异常码（仅异常响应有效）。
     */
    public static byte getExceptionCode(byte[] frame) {
        return frame[2];
    }

    private static void validateResponse(byte[] frame, byte expectedFunc) {
        if (frame == null || frame.length < 5) {
            throw new IllegalArgumentException("响应帧太短");
        }
        if (!verifyCrc(frame)) {
            throw new IllegalArgumentException("CRC 校验失败");
        }
        if (isException(frame)) {
            throw new ModbusException(getSlaveAddress(frame), getExceptionCode(frame));
        }
        if (frame[1] != expectedFunc) {
            throw new IllegalArgumentException("功能码不匹配: 期望 0x"
                    + Integer.toHexString(expectedFunc & 0xFF)
                    + "，实际 0x" + Integer.toHexString(frame[1] & 0xFF));
        }
    }

    private static void appendCrc(byte[] frame, int dataLength) {
        int crc = crc16(frame, 0, dataLength);
        frame[dataLength] = (byte) (crc & 0xFF);
        frame[dataLength + 1] = (byte) ((crc >> 8) & 0xFF);
    }

    /**
     * Modbus 异常响应。
     */
    public static class ModbusException extends RuntimeException {
        private final int slaveAddress;
        private final byte exceptionCode;

        public ModbusException(int slaveAddress, byte exceptionCode) {
            super("Modbus 异常: slave=" + slaveAddress + ", code=0x" + Integer.toHexString(exceptionCode & 0xFF));
            this.slaveAddress = slaveAddress;
            this.exceptionCode = exceptionCode;
        }

        public int getSlaveAddress() {
            return slaveAddress;
        }

        public byte getExceptionCode() {
            return exceptionCode;
        }
    }
}