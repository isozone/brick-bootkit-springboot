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


package com.zqzqq.bootkits.protocol.lifecycle;

/**
 * 设备生命周期语义。
 * <p>
 * 协议只定义这几个标准状态事件，重连/心跳的真实含义由传输层（adapter）解释，
 * 协议层只管"设备状态变了这件事被表达"。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public enum DeviceLifecycle {

    /**
     * 设备上线
     */
    ONLINE("device.online"),

    /**
     * 设备离线
     */
    OFFLINE("device.offline"),

    /**
     * 心跳
     */
    HEARTBEAT("device.heartbeat");

    /** 生命周期事件的 capabilityId 占位 */
    public static final String LIFECYCLE_CAPABILITY_ID = "lifecycle";

    private final String eventCode;

    DeviceLifecycle(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventCode() {
        return eventCode;
    }
}