package com.zqzqq.bootkits.integration.rollout;

/**
 * Probe result for gray rollout.
 */
public final class PluginRolloutProbeResult {

    private final boolean passed;
    private final String message;

    private PluginRolloutProbeResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public static PluginRolloutProbeResult pass(String message) {
        return new PluginRolloutProbeResult(true, message);
    }

    public static PluginRolloutProbeResult reject(String message) {
        return new PluginRolloutProbeResult(false, message);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }
}
