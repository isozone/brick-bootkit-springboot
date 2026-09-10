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


package com.zqzqq.bootkits.protocol.transport;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;

/**
 * 跨进程传输层 SPI：将信封投递到远端（MQTT/gRPC/Kafka/Redis 等）。
 * <p>
 * 本接口是协议模块对"传输层"的唯一抽象，零依赖、零实现。
 * 具体传输由 adapter 或 gateway 在各自模块中实现。
 * <p>
 * 传输层职责：
 * <ul>
 *   <li>publish：将信封编码后发送到远端（fire-and-forget 或 ack）</li>
 *   <li>subscribe：监听远端消息并回调 {@link TransportListener}</li>
 *   <li>start/stop：管理传输连接生命周期</li>
 * </ul>
 * <p>
 * 传输层禁止感知信封语义（协议宪法）——它只负责搬运字节。
 * 信封的 device/capability 寻址由上层（EnvelopeBus / RemoteEnvelopeBus）负责。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public interface EnvelopeTransport {

    /**
     * 启动传输连接。
     *
     * @param listener 消息回调
     */
    void start(TransportListener listener);

    /**
     * 停止传输连接。
     */
    void stop();

    /**
     * 是否已连接。
     */
    boolean isConnected();

    /**
     * 发布一条信封到远端。
     *
     * @param envelope 语义信封
     */
    void publish(BrickEnvelope envelope);

    /**
     * 传输层消息回调。
     */
    interface TransportListener {
        /**
         * 收到远端消息时回调。
         *
         * @param envelope 解码后的语义信封
         */
        void onMessage(BrickEnvelope envelope);
    }
}