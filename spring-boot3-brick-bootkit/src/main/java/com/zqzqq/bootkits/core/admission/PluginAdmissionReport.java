package com.zqzqq.bootkits.core.admission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated admission report.
 */
public final class PluginAdmissionReport {

    private final List<String> warnings = new ArrayList<>();
    private final List<String> rejections = new ArrayList<>();

    public void addWarning(String warning) {
        if (warning != null && !warning.isEmpty()) {
            warnings.add(warning);
        }
    }

    public void addRejection(String rejection) {
        if (rejection != null && !rejection.isEmpty()) {
            rejections.add(rejection);
        }
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getRejections() {
        return Collections.unmodifiableList(rejections);
    }

    public boolean hasRejections() {
        return !rejections.isEmpty();
    }

    public boolean isClean() {
        return warnings.isEmpty() && rejections.isEmpty();
    }
}
