package com.zqzqq.bootkits.integration.rollout;

/**
 * Rollout mode for plugin upgrades.
 */
public enum PluginRolloutMode {
    DIRECT,
    GRAY;

    public static PluginRolloutMode fromText(String value, PluginRolloutMode defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        for (PluginRolloutMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return defaultValue;
    }
}
