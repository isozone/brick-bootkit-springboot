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
 * Brick Protocol 消息类型。
 * <p>
 * 一种信封装四种消息，方向由类型隐含：
 * <ul>
 *     <li>{@link #REQUEST}  下行：同步读值，期待 {@link #REPLY} 回包</li>
 *     <li>{@link #REPLY}    上行：request/command 的应答，用 correlationId 关联</li>
 *     <li>{@link #COMMAND}  下行：告诉设备"执行/写值"，期待 {@link #REPLY} 回包</li>
 *     <li>{@link #EVENT}    上行：设备主动上报，单向下行无回包（或按订阅分发）</li>
 * </ul>
 * <p>
 * 该枚举为封闭集合，新语义靠组合而非新增类型（协议宪法）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public enum MessageType {

    /**
     * 同步读值请求（下行），期待 reply
     */
    REQUEST,

    /**
     * 应答（上行），correlationId 关联被应答消息
     */
    REPLY,

    /**
     * 指令（下行）：执行动作或写值，期待 reply
     */
    COMMAND,

    /**
     * 事件（上行）：主动上报，无强制回包
     */
    EVENT;

    /**
     * 是否为下行类型（发起方 → 目标方）
     */
    public boolean isDownward() {
        return this == REQUEST || this == COMMAND;
    }

    /**
     * 是否为上行类型（目标方 → 发起方）
     */
    public boolean isUpward() {
        return this == REPLY || this == EVENT;
    }

    /**
     * 该类型是否期待对应答（reply）。
     */
    public boolean expectsReply() {
        return this == REQUEST || this == COMMAND;
    }

    /**
     * wire 形态：小写字符串（与协议文档 JSON 示例一致）。
     */
    @JsonValue
    public String wireName() {
        return name().toLowerCase();
    }

    /**
     * 从 wire 形态解析。
     */
    @JsonCreator
    public static MessageType fromWire(String wire) {
        if (wire == null) {
            return null;
        }
        return MessageType.valueOf(wire.trim().toUpperCase());
    }
}