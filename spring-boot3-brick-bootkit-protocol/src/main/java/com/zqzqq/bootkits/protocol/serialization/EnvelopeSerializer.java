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


package com.zqzqq.bootkits.protocol.serialization;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;

/**
 * 信封序列化器 SPI。
 * <p>
 * 协议是 schema 不是编码：同一信封可换 JSON/CBOR/protobuf 不换语义。
 * v1 默认使用 {@link JacksonEnvelopeSerializer}（JSON）。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public interface EnvelopeSerializer {

    /**
     * 将信封编码为字节。
     *
     * @param envelope 消息信封
     * @return 编码后的字节
     */
    byte[] serialize(BrickEnvelope envelope);

    /**
     * 将字节解码为信封。
     *
     * @param bytes 编码字节
     * @return 消息信封
     */
    BrickEnvelope deserialize(byte[] bytes);
}