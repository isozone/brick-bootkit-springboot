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


package com.zqzqq.bootkits.protocol.capability;

/**
 * 能力类型。协议只定义抽象的"能力能被如何表达"，
 * 具体能力能力（temperature/switch...）由 adapter 在运行时声明，不进协议规范。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public enum CapabilityType {

    /**
     * 点：状态/值，可读或可读可写（如 temp、relay）
     */
    POINT,

    /**
     * 命令：一次性动作，非状态（如 reset）
     */
    COMMAND,

    /**
     * 事件：主动上报通知（如 overheat）
     */
    EVENT
}