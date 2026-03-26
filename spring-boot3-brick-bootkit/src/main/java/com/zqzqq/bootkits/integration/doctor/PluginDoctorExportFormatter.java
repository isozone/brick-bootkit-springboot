package com.zqzqq.bootkits.integration.doctor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats doctor reports for export.
 */
public final class PluginDoctorExportFormatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private PluginDoctorExportFormatter() {
    }

    public static String toText(PluginDoctorReport report) {
        if (report == null) {
            return "doctor report unavailable";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Brick BootKit Doctor Report").append(System.lineSeparator());
        builder.append("Generated At: ").append(FORMATTER.format(Instant.ofEpochMilli(report.getGeneratedAt())))
                .append(System.lineSeparator());
        builder.append("Status: ").append(report.getOverallStatus()).append(System.lineSeparator());
        builder.append("Summary: ").append(report.getSummary()).append(System.lineSeparator());
        builder.append("Enabled: ").append(report.isEnabled()).append(System.lineSeparator());
        builder.append("Main Package: ").append(report.getMainPackage()).append(System.lineSeparator());
        builder.append("Plugin Roots: ").append(report.getPluginRoots()).append(System.lineSeparator());
        builder.append("Plugins: ").append(report.getPluginCount())
                .append(", Started: ").append(report.getStartedPluginCount()).append(System.lineSeparator());
        builder.append("Errors: ").append(report.getErrorCount())
                .append(", Warnings: ").append(report.getWarningCount()).append(System.lineSeparator());
        builder.append(System.lineSeparator()).append("Items:").append(System.lineSeparator());

        for (PluginDoctorReport.Item item : report.getItems()) {
            builder.append("- [").append(item.getSeverity()).append("] ")
                    .append(item.getCode());
            if (item.getErrorCode() > 0) {
                builder.append(" (").append(item.getErrorCode()).append(")");
            }
            builder.append(": ").append(item.getMessage()).append(System.lineSeparator());
            if (item.getSuggestion() != null) {
                builder.append("  Suggestion: ").append(item.getSuggestion()).append(System.lineSeparator());
            }
            if (item.getDocPath() != null) {
                builder.append("  Docs: ").append(item.getDocPath());
                if (item.getDocAnchor() != null) {
                    builder.append("#").append(item.getDocAnchor());
                }
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }
}
