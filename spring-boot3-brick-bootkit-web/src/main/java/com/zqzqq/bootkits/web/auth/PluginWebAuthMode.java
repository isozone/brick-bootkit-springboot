package com.zqzqq.bootkits.web.auth;

/**
 * Plugin web authorization mode.
 */
public enum PluginWebAuthMode {
    /**
     * Disable authorization checks in web module.
     */
    DISABLED,
    /**
     * Delegate authorization to host application's authorizer.
     * If no custom authorizer exists, fallback authorizer is used.
     */
    DELEGATE,
    /**
     * Delegate authorization to host application and require custom authorizer.
     * Startup fails if fallback authorizer is used.
     */
    STRICT;

    public static PluginWebAuthMode from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DELEGATE;
        }
        String normalized = value.trim().toUpperCase();
        for (PluginWebAuthMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return DELEGATE;
    }
}

