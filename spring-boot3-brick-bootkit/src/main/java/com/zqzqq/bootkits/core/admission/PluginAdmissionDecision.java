package com.zqzqq.bootkits.core.admission;

/**
 * Admission check result.
 */
public final class PluginAdmissionDecision {

    public enum Level {
        PASS,
        WARN,
        REJECT
    }

    private final Level level;
    private final String message;

    private PluginAdmissionDecision(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    public static PluginAdmissionDecision pass(String message) {
        return new PluginAdmissionDecision(Level.PASS, message);
    }

    public static PluginAdmissionDecision warn(String message) {
        return new PluginAdmissionDecision(Level.WARN, message);
    }

    public static PluginAdmissionDecision reject(String message) {
        return new PluginAdmissionDecision(Level.REJECT, message);
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRejected() {
        return level == Level.REJECT;
    }
}
