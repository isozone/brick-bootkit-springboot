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

import java.io.Serializable;
import java.util.Objects;

/**
 * 能力抽象基类。
 * <p>
 * 具体能力（温度/湿度/开关...）是运行时由 adapter 声明出来的数据，
 * 不进协议规范——协议只保证"任何能力都能被表达"。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public abstract class Capability implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 能力唯一标识（如 temp、relay、reset） */
    private String id;

    /** 能力名称（人类可读，如 "温度"） */
    private String name;

    /** 能力描述 */
    private String description;

    protected Capability() {
    }

    protected Capability(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 能力类型
     */
    public abstract CapabilityType getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Capability)) {
            return false;
        }
        Capability that = (Capability) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Capability{"
                + "type=" + getType()
                + ", id='" + id + '\''
                + ", name='" + name + '\''
                + ", description='" + description + '\''
                + '}';
    }
}