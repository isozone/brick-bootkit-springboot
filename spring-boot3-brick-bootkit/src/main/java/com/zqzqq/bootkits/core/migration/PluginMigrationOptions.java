package com.zqzqq.bootkits.core.migration;

/**
 * Runtime options for plugin migration execution.
 */
public class PluginMigrationOptions {

    private final boolean validateChecksum;
    private final boolean continueOnError;

    public PluginMigrationOptions(boolean validateChecksum, boolean continueOnError) {
        this.validateChecksum = validateChecksum;
        this.continueOnError = continueOnError;
    }

    public static PluginMigrationOptions defaults() {
        return new PluginMigrationOptions(true, false);
    }

    public boolean isValidateChecksum() {
        return validateChecksum;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }
}
