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


package com.zqzqq.bootkits.core.eventbus;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.bus.EnvelopeListener;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EnvelopeBus ↔ PluginEventBus 双向桥接。
 * <p>
 * 职责：
 * <ul>
 *   <li>上行：adapter 通过 EnvelopeBus 发布的信封 → 转为 PluginEvent(DEVICE_EVENT) → PluginEventBus</li>
 *   <li>下行：业务插件通过 PluginEventBus 发布 DEVICE_EVENT → 转为 BrickEnvelope → EnvelopeBus</li>
 * </ul>
 * <p>
 * 防环机制：通过 {@link #propagating} 线程局部变量防止双向循环传播。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class EnvelopeBusBridge {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeBusBridge.class);

    private static final String SOURCE_ADAPTER = "device-adapter-bridge";

    /** 防止双向循环传播 */
    private static final ThreadLocal<Boolean> PROPAGATING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final EnvelopeBus envelopeBus;
    private final PluginEventBus pluginEventBus;

    private EnvelopeBus.Subscription envelopeSubscription;

    public EnvelopeBusBridge(EnvelopeBus envelopeBus, PluginEventBus pluginEventBus) {
        this.envelopeBus = envelopeBus;
        this.pluginEventBus = pluginEventBus;
    }

    /**
     * 启动双向桥接。
     */
    public void start() {
        subscribeEnvelopeToPlugin();
        subscribePluginToEnvelope();
        log.info("EnvelopeBus ↔ PluginEventBus 双向桥接已启动");
    }

    /**
     * 停止桥接。
     */
    public void stop() {
        if (envelopeSubscription != null) {
            envelopeBus.unsubscribe(envelopeSubscription);
            envelopeSubscription = null;
        }
        log.info("EnvelopeBus ↔ PluginEventBus 桥接已停止");
    }

    // ==================== 上行：EnvelopeBus → PluginEventBus ====================

    private void subscribeEnvelopeToPlugin() {
        envelopeSubscription = envelopeBus.subscribe("*", "*", this::propagateToPluginEventBus);
    }

    private void propagateToPluginEventBus(BrickEnvelope envelope) {
        if (PROPAGATING.get()) {
            return;
        }
        if (isLifecycleMessage(envelope)) {
            return;
        }
        PROPAGATING.set(true);
        try {
            PluginEvent event = new PluginEvent(PluginEvent.EventType.DEVICE_EVENT, SOURCE_ADAPTER);
            event.put("envelope.type", envelope.getType() != null ? envelope.getType().name() : null);
            event.put("envelope.deviceId", envelope.getDeviceId());
            event.put("envelope.capabilityId", envelope.getCapabilityId());
            event.put("envelope.messageId", envelope.getMessageId());
            event.put("envelope.correlationId", envelope.getCorrelationId());
            event.put("envelope.payload", envelope.getPayload());
            event.put("envelope.timestamp", envelope.getTimestamp());
            event.put("envelope.qos", envelope.getQos() != null ? envelope.getQos().getCode() : null);
            event.put("envelope.from", envelope.getFrom());
            pluginEventBus.publish(event);
            log.debug("EnvelopeBus → PluginEventBus: {} on {}:{}", envelope.getType(),
                    envelope.getDeviceId(), envelope.getCapabilityId());
        } catch (Exception e) {
            log.error("EnvelopeBus → PluginEventBus 桥接失败", e);
        } finally {
            PROPAGATING.remove();
        }
    }

    // ==================== 下行：PluginEventBus → EnvelopeBus ====================

    private void subscribePluginToEnvelope() {
        pluginEventBus.registerListener(SOURCE_ADAPTER, new EnvelopeBridgeEventListener());
    }

    private void propagateToEnvelopeBus(PluginEvent event) {
        if (PROPAGATING.get()) {
            return;
        }
        PROPAGATING.set(true);
        try {
            String typeStr = event.get("envelope.type");
            String deviceId = event.get("envelope.deviceId");
            String capabilityId = event.get("envelope.capabilityId");
            if (typeStr == null || deviceId == null || capabilityId == null) {
                return;
            }
            BrickEnvelope envelope = BrickEnvelope.builder()
                    .type(com.zqzqq.bootkits.protocol.message.MessageType.valueOf(typeStr))
                    .deviceId(deviceId)
                    .capabilityId(capabilityId)
                    .messageId(event.get("envelope.messageId"))
                    .correlationId(event.get("envelope.correlationId"))
                    .payload(event.get("envelope.payload"))
                    .qos(intToQos(event.get("envelope.qos")))
                    .from(event.get("envelope.from"))
                    .build();
            envelopeBus.publish(envelope);
            log.debug("PluginEventBus → EnvelopeBus: {} on {}:{}", typeStr, deviceId, capabilityId);
        } catch (Exception e) {
            log.error("PluginEventBus → EnvelopeBus 桥接失败", e);
        } finally {
            PROPAGATING.remove();
        }
    }

    // ==================== 工具方法 ====================

    private static boolean isLifecycleMessage(BrickEnvelope envelope) {
        return com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle.LIFECYCLE_CAPABILITY_ID
                .equals(envelope.getCapabilityId());
    }

    private static com.zqzqq.bootkits.protocol.message.QosLevel intToQos(Object value) {
        if (value instanceof Number) {
            return com.zqzqq.bootkits.protocol.message.QosLevel.fromCode(((Number) value).intValue());
        }
        return com.zqzqq.bootkits.protocol.message.QosLevel.FIRE_FORGET;
    }

    /**
     * PluginEventBus 监听器：接收 DEVICE_EVENT 并桥接到 EnvelopeBus。
     */
    private class EnvelopeBridgeEventListener implements PluginEventListener {
        @Override
        public void onEvent(PluginEvent event) {
            if (event.getType() == PluginEvent.EventType.DEVICE_EVENT) {
                propagateToEnvelopeBus(event);
            }
        }

        @Override
        public int priority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean supportsType(PluginEvent.EventType type) {
            return type == PluginEvent.EventType.DEVICE_EVENT;
        }
    }
}