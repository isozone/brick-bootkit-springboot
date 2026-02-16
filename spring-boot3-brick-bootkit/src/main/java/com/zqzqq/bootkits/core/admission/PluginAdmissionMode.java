package com.zqzqq.bootkits.core.admission;

/**
 * Admission mode for plugin lifecycle validation.
 */
public enum PluginAdmissionMode {

    OFF,
    WARN,
    ENFORCE;

    public static PluginAdmissionMode fromText(String value, PluginAdmissionMode defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        for (PluginAdmissionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return defaultValue;
    }
}
