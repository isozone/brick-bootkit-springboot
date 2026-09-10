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

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Modbus RTU 串口传输层：基于 jSerialComm 的串口读写。
 * <p>
 * 传输层只负责"字节进字节出"，不做任何协议翻译（协议翻译由 ModbusRtuDeviceAdapter 承担）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class ModbusTransport {

    private static final Logger log = LoggerFactory.getLogger(ModbusTransport.class);

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * 打开串口。
     *
     * @param portName 串口名（如 COM1、/dev/ttyUSB0）
     * @param baudRate 波特率
     * @param dataBits 数据位（默认 8）
     * @param stopBits 停止位（默认 1）
     * @param parity   校验位（默认 NONE）
     */
    public void open(String portName, int baudRate, int dataBits, int stopBits, int parity) {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("无法打开串口: " + portName);
        }
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
        log.info("串口已打开: {} (baud={}, dataBits={}, stopBits={}, parity={})",
                portName, baudRate, dataBits, stopBits, parity);
    }

    /**
     * 使用默认参数打开串口（8N1）。
     */
    public void open(String portName, int baudRate) {
        open(portName, baudRate, 8, 1, SerialPort.NO_PARITY);
    }

    /**
     * 发送数据。
     *
     * @param data 待发送的字节数组
     * @throws IOException 发送失败
     */
    public void send(byte[] data) throws IOException {
        if (outputStream == null) {
            throw new IllegalStateException("串口未打开");
        }
        outputStream.write(data);
        outputStream.flush();
        log.debug("串口发送: {} bytes", data.length);
    }

    /**
     * 接收数据（阻塞读取，超时返回 null）。
     *
     * @param maxLength 最大读取长度
     * @return 读取到的字节数组，超时返回 null
     * @throws IOException 读取失败
     */
    public byte[] receive(int maxLength) throws IOException {
        if (inputStream == null) {
            throw new IllegalStateException("串口未打开");
        }
        byte[] buffer = new byte[maxLength];
        int totalRead = 0;
        long deadline = System.currentTimeMillis() + 2000;
        while (totalRead < maxLength && System.currentTimeMillis() < deadline) {
            int available = inputStream.available();
            if (available > 0) {
                int toRead = Math.min(available, maxLength - totalRead);
                int read = inputStream.read(buffer, totalRead, toRead);
                if (read > 0) {
                    totalRead += read;
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (totalRead == 0) {
            return null;
        }
        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        log.debug("串口接收: {} bytes", totalRead);
        return result;
    }

    /**
     * 发送请求并接收响应（半双工典型流程）。
     *
     * @param request  请求帧
     * @param responseMaxLen 响应最大长度
     * @return 响应帧
     * @throws IOException 通信失败
     */
    public byte[] sendAndReceive(byte[] request, int responseMaxLen) throws IOException {
        send(request);
        return receive(responseMaxLen);
    }

    /**
     * 关闭串口。
     */
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            log.info("串口已关闭: {}", serialPort.getSystemPortName());
        }
        inputStream = null;
        outputStream = null;
    }

    /**
     * 串口是否已打开。
     */
    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }
}