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
import com.zqzqq.bootkits.protocol.capability.CapabilityType;
import com.zqzqq.bootkits.protocol.capability.CommandCapability;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.capability.EventCapability;
import com.zqzqq.bootkits.protocol.capability.PointCapability;
import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.MessageType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 声明式适配器模板基类：从 {@link AdapterConfig} 自动构建能力树 + 分发请求。
 * <p>
 * 子类只需实现模板方法（传输连接/读值/写值/发命令），其余由本类处理：
 * <ul>
 *   <li>{@link #doConnect(TransportContext)} — 建立传输连接</li>
 *   <li>{@link #doDisconnect()} — 断开传输连接</li>
 *   <li>{@link #doRead(AdapterMapping)} — 读取传输值</li>
 *   <li>{@link #doWrite(AdapterMapping, Object)} — 写入传输值</li>
 *   <li>{@link #doCommand(AdapterMapping, Object)} — 执行传输命令</li>
 *   <li>{@link #doTranslateFromDevice(Object, TransportContext)} — 传输帧 → 语义信封</li>
 * </ul>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public abstract class AbstractDeclarativeDeviceAdapter implements DeviceAdapter {

    protected final AdapterConfig config;
    protected EnvelopeBus bus;
    protected Device device;
    protected TransportContext transportContext;
    protected final AtomicBoolean connected = new AtomicBoolean(false);

    protected AbstractDeclarativeDeviceAdapter(AdapterConfig config) {
        this.config = config;
    }

    protected AbstractDeclarativeDeviceAdapter(String resourcePath) {
        this(loadConfig(resourcePath));
    }

    @Override
    public String getDeviceType() {
        return config.getDeviceType();
    }

    @Override
    public String getTransport() {
        return config.getTransport();
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public void connect(EnvelopeBus bus, TransportContext context) {
        this.bus = bus;
        this.transportContext = context;
        this.device = buildDevice();
        doConnect(context);
        subscribeToBus();
        connected.set(true);
        publishLifecycle(DeviceLifecycle.ONLINE);
    }

    @Override
    public void disconnect() {
        if (!connected.compareAndSet(true, false)) {
            return;
        }
        publishLifecycle(DeviceLifecycle.OFFLINE);
        doDisconnect();
    }

    @Override
    public Object toDevice(BrickEnvelope envelope) {
        String capabilityId = envelope.getCapabilityId();
        AdapterMapping mapping = config.findMapping(capabilityId);
        if (mapping == null) {
            throw new IllegalArgumentException("未找到能力映射: " + capabilityId);
        }
        switch (envelope.getType()) {
            case REQUEST:
                return doRead(mapping);
            case COMMAND:
                doWrite(mapping, envelope.getPayload());
                return "ok";
            default:
                throw new IllegalArgumentException("toDevice 不支持的消息类型: " + envelope.getType());
        }
    }

    @Override
    public void onFromDevice(Object rawFrame, TransportContext context) {
        BrickEnvelope envelope = doTranslateFromDevice(rawFrame, context);
        if (envelope != null && bus != null) {
            bus.publish(envelope);
        }
    }

    protected Device buildDevice() {
        Device d = new Device(config.getDeviceType(), config.getDeviceType(), config.getDeviceType());
        for (AdapterMapping mapping : config.getMappings()) {
            CapabilityType type = parseCapabilityType(mapping.getType());
            switch (type) {
                case POINT:
                    CapabilityAccess access = mapping.getCapabilityAccess();
                    boolean writable = access != null && access.isWritable();
                    d.addCapability(new PointCapability(
                            mapping.getCapability(),
                            mapping.getCapability(),
                            mapping.getCapability(),
                            writable));
                    break;
                case COMMAND:
                    d.addCapability(new CommandCapability(
                            mapping.getCapability(),
                            mapping.getCapability(),
                            mapping.getCapability()));
                    break;
                case EVENT:
                    d.addCapability(new EventCapability(
                            mapping.getCapability(),
                            mapping.getCapability(),
                            mapping.getCapability()));
                    break;
                default:
                    break;
            }
        }
        return d;
    }

    protected AdapterMapping findMapping(String capabilityId) {
        return config.findMapping(capabilityId);
    }

    private void subscribeToBus() {
        bus.subscribe("*", "*", envelope -> {
            if (!connected.get()) {
                return;
            }
            if (!config.getDeviceType().equals(envelope.getDeviceId())) {
                return;
            }
            if (DeviceLifecycle.LIFECYCLE_CAPABILITY_ID.equals(envelope.getCapabilityId())) {
                return;
            }
            handleBusMessage(envelope);
        });
    }

    private void handleBusMessage(BrickEnvelope envelope) {
        if (envelope.getType() == MessageType.REQUEST) {
            Object result = doRead(config.findMapping(envelope.getCapabilityId()));
            if (result != null) {
                bus.reply(BrickMessages.reply(envelope, result));
            }
        } else if (envelope.getType() == MessageType.COMMAND) {
            doWrite(config.findMapping(envelope.getCapabilityId()), envelope.getPayload());
            bus.reply(BrickMessages.reply(envelope, "ok"));
        }
    }

    private void publishLifecycle(DeviceLifecycle lifecycle) {
        if (bus != null && device != null) {
            bus.publish(BrickMessages.lifecycle(device.getDeviceId(), lifecycle));
        }
    }

    private static CapabilityType parseCapabilityType(String type) {
        if (type == null) {
            return CapabilityType.POINT;
        }
        switch (type.trim().toLowerCase()) {
            case "point":
                return CapabilityType.POINT;
            case "command":
                return CapabilityType.COMMAND;
            case "event":
                return CapabilityType.EVENT;
            default:
                throw new IllegalArgumentException("未知的能力类型: " + type);
        }
    }

    private static AdapterConfig loadConfig(String resourcePath) {
        try {
            JacksonAdapterConfigLoader loader = new JacksonAdapterConfigLoader();
            return loader.load(resourcePath);
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 adapter 配置: " + resourcePath, e);
        }
    }

    // ==================== 模板方法 ====================

    /**
     * 建立传输连接（模板方法）。
     *
     * @param context 传输上下文（串口号、从站地址等）
     */
    protected abstract void doConnect(TransportContext context);

    /**
     * 断开传输连接（模板方法）。
     */
    protected abstract void doDisconnect();

    /**
     * 读取传输值（模板方法）。
     *
     * @param mapping 映射定义
     * @return 读取到的值
     */
    protected abstract Object doRead(AdapterMapping mapping);

    /**
     * 写入传输值（模板方法）。
     *
     * @param mapping 映射定义
     * @param value   要写入的值
     */
    protected abstract void doWrite(AdapterMapping mapping, Object value);

    /**
     * 执行传输命令（可选覆盖，默认委托 doWrite）。
     *
     * @param mapping 映射定义
     * @param payload 命令载荷
     */
    protected void doCommand(AdapterMapping mapping, Object payload) {
        doWrite(mapping, payload);
    }

    /**
     * 传输帧 → 语义信封（模板方法）。
     *
     * @param rawFrame 原始传输帧
     * @param context  传输上下文
     * @return 语义信封，返回 null 表示不处理
     */
    protected abstract BrickEnvelope doTranslateFromDevice(Object rawFrame, TransportContext context);
}