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


package com.zqzqq.bootkits.protocol.adapter;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;

/**
 * 设备适配器 SPI：语义 ↔ 传输的翻译桥梁。
 * <p>
 * 本接口定义 adapter 必须实现的方法：
 * <ul>
 *   <li>声明自身设备类型和传输协议</li>
 *   <li>建立/断开传输连接</li>
 *   <li>语义信封 → 传输报文（下行）</li>
 *   <li>传输报文 → 语义信封（上行）</li>
 * </ul>
 * <p>
 * 99% 的场景用声明式映射（{@link AbstractDeclarativeDeviceAdapter}）即可；
 * 仅当私有二进制协议/双向长连接无法用映射覆盖时，才需要命令式实现本接口。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public interface DeviceAdapter {

    /**
     * 设备类型标识（与 AdapterConfig.deviceType 对应）。
     */
    String getDeviceType();

    /**
     * 传输协议标识（如 modbus、mqtt、ble）。
     */
    String getTransport();

    /**
     * 建立传输连接。
     *
     * @param bus  信封总线（adapter 通过 bus 发布上行消息、接收下行消息）
     * @param context 传输上下文（串口号、从站地址等传输侧配置）
     */
    void connect(EnvelopeBus bus, TransportContext context);

    /**
     * 断开传输连接。
     */
    void disconnect();

    /**
     * 是否已连接。
     */
    boolean isConnected();

    /**
     * 获取设备能力树（由 adapter 在连接后构建）。
     */
    Device getDevice();

    /**
     * 语义信封 → 传输报文（下行翻译）。
     *
     * @param envelope 语义信封（request 或 command）
     * @return 传输层可执行的对象（字节数组、传输帧、API 调用参数等）
     */
    Object toDevice(BrickEnvelope envelope);

    /**
     * 传输报文 → 语义信封（上行翻译），并将信封发布到 bus。
     * <p>
     * 当传输层收到设备数据时调用本方法，adapter 负责：
     * 1. 解析传输帧
     * 2. 构造语义信封
     * 3. 通过 bus.publish() 发布到业务层
     *
     * @param rawFrame 传输层原始帧/报文
     * @param context  传输上下文
     */
    void onFromDevice(Object rawFrame, TransportContext context);
}