package com.zqzqq.bootkits.integration.doctor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Doctor report for host/plugin environment.
 */
public class PluginDoctorReport implements Serializable {

    private final long generatedAt;
    private final boolean enabled;
    private final String mainPackage;
    private final List<String> pluginRoots;
    private final int pluginCount;
    private final int startedPluginCount;
    private final int errorCount;
    private final int warningCount;
    private final String overallStatus;
    private final String summary;
    private final List<Item> items;

    public PluginDoctorReport(long generatedAt,
                              boolean enabled,
                              String mainPackage,
                              List<String> pluginRoots,
                              int pluginCount,
                              int startedPluginCount,
                              int errorCount,
                              int warningCount,
                              String overallStatus,
                              String summary,
                              List<Item> items) {
        this.generatedAt = generatedAt;
        this.enabled = enabled;
        this.mainPackage = mainPackage;
        this.pluginRoots = pluginRoots == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(pluginRoots));
        this.pluginCount = pluginCount;
        this.startedPluginCount = startedPluginCount;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.overallStatus = overallStatus;
        this.summary = summary;
        this.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(items));
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMainPackage() {
        return mainPackage;
    }

    public List<String> getPluginRoots() {
        return pluginRoots;
    }

    public int getPluginCount() {
        return pluginCount;
    }

    public int getStartedPluginCount() {
        return startedPluginCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public String getSummary() {
        return summary;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item implements Serializable {
        private final String code;
        private final int errorCode;
        private final String severity;
        private final String message;
        private final String suggestion;
        private final String docPath;
        private final String docAnchor;

        public Item(String code,
                    int errorCode,
                    String severity,
                    String message,
                    String suggestion,
                    String docPath,
                    String docAnchor) {
            this.code = code;
            this.errorCode = errorCode;
            this.severity = severity;
            this.message = message;
            this.suggestion = suggestion;
            this.docPath = docPath;
            this.docAnchor = docAnchor;
        }

        public String getCode() {
            return code;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public String getDocPath() {
            return docPath;
        }

        public String getDocAnchor() {
            return docAnchor;
        }
    }
}
