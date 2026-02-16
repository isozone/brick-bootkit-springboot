package com.zqzqq.bootkits.core.admission;

/**
 * Admission check SPI.
 */
public interface PluginAdmissionCheck {

    /**
     * Stable check name used in logs.
     */
    String getName();

    /**
     * Execute admission check.
     */
    PluginAdmissionDecision check(PluginAdmissionContext context);
}
