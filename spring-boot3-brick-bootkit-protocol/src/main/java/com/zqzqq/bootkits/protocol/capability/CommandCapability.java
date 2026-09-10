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
 * 命令能力：一次性动作，非状态（如 reset、reboot）。
 * <p>
 * 对应信封中的 {@code type=command/reply} 交互。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class CommandCapability extends Capability {

    private static final long serialVersionUID = 1L;

    public CommandCapability() {
    }

    public CommandCapability(String id, String name, String description) {
        super(id, name, description);
    }

    @Override
    public CapabilityType getType() {
        return CapabilityType.COMMAND;
    }
}