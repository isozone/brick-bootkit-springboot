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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 消息可靠性等级（QoS）。
 * <p>
 * 语义协议仅定义可靠性语义，具体的投递机制由传输层（adapter）实现。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public enum QosLevel {

    /**
     * fire-and-forget：最多一次投递，丢失不做补偿
     */
    FIRE_FORGET(0),

    /**
     * at-least-once：至少一次投递，传输层负责重试直到收到 ack
     */
    AT_LEAST_ONCE(1);

    private final int code;

    QosLevel(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 按协议编码值解析 QoS。
     *
     * @param code 0 或 1
     * @return 对应的 QoS 等级
     * @throws IllegalArgumentException 编码非法时
     */
    public static QosLevel fromCode(int code) {
        for (QosLevel qos : values()) {
            if (qos.code == code) {
                return qos;
            }
        }
        throw new IllegalArgumentException("未知的 QoS 编码: " + code);
    }

    /**
     * wire 形态：数值编码（0/1，与协议文档 JSON 示例一致）。
     */
    @JsonValue
    public int wireCode() {
        return code;
    }

    /**
     * 从 wire 形态解析。
     */
    @JsonCreator
    public static QosLevel fromWire(int code) {
        return fromCode(code);
    }
}