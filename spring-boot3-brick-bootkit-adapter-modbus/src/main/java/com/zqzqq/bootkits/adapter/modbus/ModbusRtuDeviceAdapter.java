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

import com.zqzqq.bootkits.protocol.adapter.AbstractDeclarativeDeviceAdapter;
import com.zqzqq.bootkits.protocol.adapter.AdapterConfig;
import com.zqzqq.bootkits.protocol.adapter.AdapterMapping;
import com.zqzqq.bootkits.protocol.adapter.JacksonAdapterConfigLoader;
import com.zqzqq.bootkits.protocol.adapter.TransportContext;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Modbus RTU adapter 参考实现：将 Modbus RTU 传输翻译为 Brick Protocol 语义。
 * <p>
 * 声明式映射示例（brick-adapter.json）：
 * <pre>
 * {
 *   "deviceType": "modbus-thermostat",
 *   "transport": "modbus",
 *   "mappings": [
 *     { "capability": "temp",  "type": "point", "access": "read", "register": 40001, "scale": 0.1 },
 *     { "capability": "relay", "type": "point", "access": "read-write", "register": 48001 },
 *     { "capability": "reset", "type": "command", "register": 40011, "writeValue": 1 }
 *   ]
 * }
 * </pre>
 * <p>
 * 传输上下文参数：
 * <ul>
 *   <li>{@code slaveAddress} — Modbus 从站地址（默认 1）</li>
 *   <li>{@code portName} — 串口号（如 COM1、/dev/ttyUSB0）</li>
 *   <li>{@code baudRate} — 波特率（默认 9600）</li>
 * </ul>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class ModbusRtuDeviceAdapter extends AbstractDeclarativeDeviceAdapter {

    private static final Logger log = LoggerFactory.getLogger(ModbusRtuDeviceAdapter.class);

    private static final int DEFAULT_SLAVE_ADDRESS = 1;
    private static final int DEFAULT_BAUD_RATE = 9600;

    private final ModbusTransport transport;
    private int slaveAddress = DEFAULT_SLAVE_ADDRESS;

    public ModbusRtuDeviceAdapter(AdapterConfig config) {
        super(config);
        this.transport = new ModbusTransport();
    }

    public ModbusRtuDeviceAdapter(String resourcePath) {
        super(resourcePath);
        this.transport = new ModbusTransport();
    }

    /**
     * 允许注入外部传输层（测试用）。
     */
    public ModbusRtuDeviceAdapter(AdapterConfig config, ModbusTransport transport) {
        super(config);
        this.transport = transport;
    }

    @Override
    protected void doConnect(TransportContext context) {
        slaveAddress = context.getInt("slaveAddress", DEFAULT_SLAVE_ADDRESS);
        String portName = context.getString("portName");
        int baudRate = context.getInt("baudRate", DEFAULT_BAUD_RATE);

        if (portName == null || portName.isEmpty()) {
            throw new IllegalStateException("传输上下文缺少 portName");
        }
        transport.open(portName, baudRate);
        log.info("Modbus RTU 连接成功: port={}, baud={}, slave={}", portName, baudRate, slaveAddress);
    }

    @Override
    protected void doDisconnect() {
        transport.close();
    }

    @Override
    protected Object doRead(AdapterMapping mapping) {
        int register = mapping.getIntOption("register", 0);
        double scale = mapping.getDoubleOption("scale", 1.0);
        byte[] request = ModbusFrameUtil.buildReadHoldingRegistersRequest(slaveAddress, register, 1);
        try {
            byte[] response = transport.sendAndReceive(request, 256);
            if (response == null) {
                throw new IllegalStateException("Modbus 读取超时: register=" + register);
            }
            int[] values = ModbusFrameUtil.parseReadHoldingRegistersResponse(response);
            int rawValue = values[0];
            return scale != 1.0 ? rawValue * scale : rawValue;
        } catch (IOException e) {
            throw new IllegalStateException("Modbus 读取失败: register=" + register, e);
        }
    }

    @Override
    protected void doWrite(AdapterMapping mapping, Object value) {
        int register = mapping.getIntOption("register", 0);
        Object writeValueObj = mapping.getOption("writeValue");
        int writeValue;
        if (writeValueObj != null) {
            writeValue = toInt(writeValueObj);
        } else {
            writeValue = toInt(value);
        }
        byte[] request = ModbusFrameUtil.buildWriteSingleRegisterRequest(slaveAddress, register, writeValue);
        try {
            byte[] response = transport.sendAndReceive(request, 256);
            if (response == null) {
                throw new IllegalStateException("Modbus 写入超时: register=" + register);
            }
            log.debug("Modbus 写入成功: register={}, value={}", register, writeValue);
        } catch (IOException e) {
            throw new IllegalStateException("Modbus 写入失败: register=" + register, e);
        }
    }

    @Override
    protected BrickEnvelope doTranslateFromDevice(Object rawFrame, TransportContext context) {
        if (!(rawFrame instanceof byte[])) {
            return null;
        }
        byte[] frame = (byte[]) rawFrame;
        if (!ModbusFrameUtil.verifyCrc(frame)) {
            log.warn("Modbus 帧 CRC 校验失败");
            return null;
        }
        int slave = ModbusFrameUtil.getSlaveAddress(frame);
        if (slave != slaveAddress) {
            return null;
        }
        byte funcCode = ModbusFrameUtil.getFunctionCode(frame);
        if (funcCode == ModbusFrameUtil.FUNC_READ_HOLDING_REGISTERS) {
            int[] values = ModbusFrameUtil.parseReadHoldingRegistersResponse(frame);
            if (values.length > 0) {
                return BrickMessages.event(config.getDeviceType(), "temp", values[0]);
            }
        }
        return null;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("无法转换为整数: " + value);
    }
}