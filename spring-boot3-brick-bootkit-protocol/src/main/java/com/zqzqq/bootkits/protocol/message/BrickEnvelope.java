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

import java.io.Serializable;
import java.util.Objects;

/**
 * Brick Protocol 消息信封。
 * <p>
 * 信封只装元数据（给谁、什么类型、什么时候、要不要回），业务值放在 {@link #payload}。
 * 信封与载荷分离：协议演进改信封，业务数据不动；业务变化，信封照旧。
 * <p>
 * 寻址是逻辑的，与传输无关：地址只能是 {@code deviceId + capabilityId}，
 * 任何传输痕迹（modbus 地址 / MQTT 主题 / 特征 UUID）禁止出现在信封中（协议宪法）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class BrickEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 语义协议自身版本，用于兼容性判定 */
    private String version;

    /** 消息类型：request | reply | event | command */
    private MessageType type;

    /** 全局唯一消息ID，reply 靠 correlationId 关联 */
    private String messageId;

    /** 关联ID，仅 reply 使用，回填被应答消息的 messageId */
    private String correlationId;

    /** 逻辑设备标识，寻址第一层 */
    private String deviceId;

    /** 逻辑能力标识，寻址第二层 */
    private String capabilityId;

    /** 消息产生时间（epoch 毫秒，UTC） */
    private long timestamp;

    /** 可靠性等级 */
    private QosLevel qos;

    /** 发件人，跨进程路由回程用 */
    private String from;

    /** 业务载荷，与信封分离 */
    private Object payload;

    public BrickEnvelope() {
    }

    private BrickEnvelope(Builder builder) {
        this.version = builder.version;
        this.type = builder.type;
        this.messageId = builder.messageId;
        this.correlationId = builder.correlationId;
        this.deviceId = builder.deviceId;
        this.capabilityId = builder.capabilityId;
        this.timestamp = builder.timestamp;
        this.qos = builder.qos;
        this.from = builder.from;
        this.payload = builder.payload;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public QosLevel getQos() {
        return qos;
    }

    public void setQos(QosLevel qos) {
        this.qos = qos;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BrickEnvelope)) {
            return false;
        }
        BrickEnvelope that = (BrickEnvelope) o;
        return timestamp == that.timestamp
                && Objects.equals(version, that.version)
                && type == that.type
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(deviceId, that.deviceId)
                && Objects.equals(capabilityId, that.capabilityId)
                && qos == that.qos
                && Objects.equals(from, that.from)
                && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, type, messageId, correlationId,
                deviceId, capabilityId, timestamp, qos, from, payload);
    }

    @Override
    public String toString() {
        return "BrickEnvelope{"
                + "version='" + version + '\''
                + ", type=" + type
                + ", messageId='" + messageId + '\''
                + ", correlationId='" + correlationId + '\''
                + ", deviceId='" + deviceId + '\''
                + ", capabilityId='" + capabilityId + '\''
                + ", timestamp=" + timestamp
                + ", qos=" + qos
                + ", from='" + from + '\''
                + ", payload=" + payload
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * BrickEnvelope 构建器。
     */
    public static final class Builder {
        private String version = "1.0";
        private MessageType type;
        private String messageId;
        private String correlationId;
        private String deviceId;
        private String capabilityId;
        private long timestamp;
        private QosLevel qos = QosLevel.FIRE_FORGET;
        private String from;
        private Object payload;

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder capabilityId(String capabilityId) {
            this.capabilityId = capabilityId;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder qos(QosLevel qos) {
            this.qos = qos;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public BrickEnvelope build() {
            if (type == null) {
                throw new IllegalStateException("type 不能为空");
            }
            if (deviceId == null || deviceId.isEmpty()) {
                throw new IllegalStateException("deviceId 不能为空");
            }
            if (capabilityId == null || capabilityId.isEmpty()) {
                throw new IllegalStateException("capabilityId 不能为空");
            }
            if (messageId == null || messageId.isEmpty()) {
                throw new IllegalStateException("messageId 不能为空");
            }
            if (timestamp <= 0) {
                timestamp = System.currentTimeMillis();
            }
            if ((type == MessageType.REPLY) && correlationId == null) {
                throw new IllegalStateException("reply 消息必须携带 correlationId");
            }
            return new BrickEnvelope(this);
        }
    }
}