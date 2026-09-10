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

import java.io.Serializable;

/**
 * 能力访问方式（仅点能力使用）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public enum CapabilityAccess implements Serializable {

    /** 只读 */
    READ,

    /** 可读可写 */
    READ_WRITE;

    /**
     * 是否可写。
     */
    public boolean isWritable() {
        return this == READ_WRITE;
    }

    /**
     * 从配置字符串解析（read / read-write / rw）。
     */
    public static CapabilityAccess parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        switch (normalized) {
            case "read":
            case "r":
                return READ;
            case "read-write":
            case "read_write":
            case "rw":
                return READ_WRITE;
            default:
                throw new IllegalArgumentException("未知的能力访问方式: " + value);
        }
    }
}