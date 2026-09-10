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


package com.zqzqq.bootkits.protocol.message;

import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;

import java.util.UUID;

/**
 * 信封工厂：基于一个设备 + 能力，快速构造四类消息与生命周期事件。
 * <p>
 * 业务层只对 deviceId + capabilityId 对话，屏蔽 adapter 存在。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public final class BrickMessages {

    private BrickMessages() {
    }

    /**
     * 生成全局唯一消息ID。
     */
    public static String newMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构造 request（同步读值，期待 reply）。
     */
    public static BrickEnvelope request(String deviceId, String capabilityId, Object payload) {
        return base(deviceId, capabilityId)
                .type(MessageType.REQUEST)
                .messageId(newMessageId())
                .payload(payload)
                .build();
    }

    /**
     * 构造 request，允许覆盖 qos。
     */
    public static BrickEnvelope request(String deviceId, String capabilityId, Object payload, QosLevel qos) {
        return base(deviceId, capabilityId)
                .type(MessageType.REQUEST)
                .messageId(newMessageId())
                .qos(qos)
                .payload(payload)
                .build();
    }

    /**
     * 构造 command（执行动作 / 写值，期待 reply）。
     */
    public static BrickEnvelope command(String deviceId, String capabilityId, Object payload) {
        return base(deviceId, capabilityId)
                .type(MessageType.COMMAND)
                .messageId(newMessageId())
                .payload(payload)
                .build();
    }

    /**
     * 构造 command，允许覆盖 qos。
     */
    public static BrickEnvelope command(String deviceId, String capabilityId, Object payload, QosLevel qos) {
        return base(deviceId, capabilityId)
                .type(MessageType.COMMAND)
                .messageId(newMessageId())
                .qos(qos)
                .payload(payload)
                .build();
    }

    /**
     * 构造 event（主动上报，无强制回包）。
     */
    public static BrickEnvelope event(String deviceId, String capabilityId, Object payload) {
        return base(deviceId, capabilityId)
                .type(MessageType.EVENT)
                .messageId(newMessageId())
                .payload(payload)
                .build();
    }

    /**
     * 构造 reply（应答 request/command）。
     *
     * @param request       被应答的原消息
     * @param replyPayload  应答载荷
     */
    public static BrickEnvelope reply(BrickEnvelope request, Object replyPayload) {
        return base(request.getDeviceId(), request.getCapabilityId())
                .type(MessageType.REPLY)
                .messageId(newMessageId())
                .correlationId(request.getMessageId())
                .qos(request.getQos())
                .payload(replyPayload)
                .build();
    }

    /**
     * 构造生命周期事件信封（capabilityId 固定为 "lifecycle"）。
     */
    public static BrickEnvelope lifecycle(String deviceId, DeviceLifecycle lifecycle) {
        return base(deviceId, DeviceLifecycle.LIFECYCLE_CAPABILITY_ID)
                .type(MessageType.EVENT)
                .messageId(newMessageId())
                .payload(lifecycle.getEventCode())
                .build();
    }

    private static BrickEnvelope.Builder base(String deviceId, String capabilityId) {
        return BrickEnvelope.builder()
                .deviceId(deviceId)
                .capabilityId(capabilityId);
    }

    public static Object reply(String s1, String temp, double v, Object o) {
        return null;
    }
}