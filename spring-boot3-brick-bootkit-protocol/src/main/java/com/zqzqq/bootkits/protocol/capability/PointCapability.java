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
 * 点能力：状态/值，可读或可读可写（如 temp、relay）。
 * <p>
 * 对应信封中的 {@code type=request/reply} 交互。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class PointCapability extends Capability {

    private static final long serialVersionUID = 1L;

    /** 是否可写。false 表示只读点 */
    private boolean writable;

    public PointCapability() {
    }

    public PointCapability(String id, String name, String description, boolean writable) {
        super(id, name, description);
        this.writable = writable;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    @Override
    public CapabilityType getType() {
        return CapabilityType.POINT;
    }
}