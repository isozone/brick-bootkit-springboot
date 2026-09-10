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

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.MessageType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 远程信封总线：本地 {@link EnvelopeBus} + 远程 {@link EnvelopeTransport} 双管道。
 * <p>
 * 职责：
 * <ul>
 *   <li><b>上行</b>（设备→业务）：远端 adapter 通过 transport 发布信封 → RemoteEnvelopeBus 收到后 →
 *       发布到本地 EnvelopeBus → 业务插件收到</li>
 *   <li><b>下行</b>（业务→设备）：业务插件通过本地 EnvelopeBus 发送 request/command →
 *       RemoteEnvelopeBus 拦截 → 通过 transport 发布到远端 → 远端 adapter 收到</li>
 * </ul>
 * <p>
 * 防环机制：transport 收到的消息发布到本地 bus 时，不触发再次 transport 发送。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class RemoteEnvelopeBus {

    /** 本地信封总线（进程内 pub/sub） */
    private final EnvelopeBus localBus;

    /** 远程传输层（跨进程 pub/sub） */
    private final EnvelopeTransport transport;

    /** 防环标记：transport 收到的消息不再转发回 transport */
    private final AtomicBoolean propagating = new AtomicBoolean(false);

    /** 是否已启动 */
    private final AtomicBoolean started = new AtomicBoolean(false);

    public RemoteEnvelopeBus(EnvelopeBus localBus, EnvelopeTransport transport) {
        this.localBus = localBus;
        this.transport = transport;
    }

    /**
     * 启动双向桥接。
     * <p>
     * 上行：transport 收到远端消息 → 发布到 localBus
     * 下行：localBus 收到下行消息 → 通过 transport 发布到远端
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        transport.start(this::onTransportMessage);
        subscribeLocalBusDownstream();
    }

    /**
     * 停止双向桥接。
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        transport.stop();
    }

    /**
     * 是否已启动。
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * 获取本地信封总线。
     */
    public EnvelopeBus getLocalBus() {
        return localBus;
    }

    /**
     * 获取远程传输层。
     */
    public EnvelopeTransport getTransport() {
        return transport;
    }

    // ==================== 上行：transport → localBus ====================

    private void onTransportMessage(BrickEnvelope envelope) {
        if (propagating.get()) {
            return;
        }
        propagating.set(true);
        try {
            localBus.publish(envelope);
        } catch (Exception e) {
            // 上行桥接失败，忽略单条消息，不中断传输
        } finally {
            propagating.set(false);
        }
    }

    // ==================== 下行：localBus → transport ====================

    private void subscribeLocalBusDownstream() {
        localBus.subscribe("*", "*", envelope -> {
            if (propagating.get()) {
                return;
            }
            if (!isDownstreamMessage(envelope)) {
                return;
            }
            propagating.set(true);
            try {
                transport.publish(envelope);
            } catch (Exception e) {
                // 下行桥接失败，忽略单条消息，不中断传输
            } finally {
                propagating.set(false);
            }
        });
    }

    /**
     * 判断是否为下行消息（业务→设备方向）。
     * 下行 = request（读值请求）或 command（写值/执行命令）。
     * 上行 = event（设备上报）或 reply（应答）。
     */
    private static boolean isDownstreamMessage(BrickEnvelope envelope) {
        MessageType type = envelope.getType();
        return type == MessageType.REQUEST || type == MessageType.COMMAND;
    }
}