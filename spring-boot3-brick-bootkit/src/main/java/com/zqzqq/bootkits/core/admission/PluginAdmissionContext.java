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


package com.zqzqq.bootkits.core.admission;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Admission check context.
 */
public final class PluginAdmissionContext {

    private final PluginAdmissionOperation operation;
    private final InsidePluginDescriptor descriptor;
    private final Path pluginPath;

    public PluginAdmissionContext(PluginAdmissionOperation operation,
                                  InsidePluginDescriptor descriptor,
                                  Path pluginPath) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.descriptor = descriptor;
        this.pluginPath = pluginPath;
    }

    public PluginAdmissionOperation getOperation() {
        return operation;
    }

    public InsidePluginDescriptor getDescriptor() {
        return descriptor;
    }

    public Path getPluginPath() {
        return pluginPath;
    }

    public String getPluginId() {
        return descriptor == null ? null : descriptor.getPluginId();
    }
}
