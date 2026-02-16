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
