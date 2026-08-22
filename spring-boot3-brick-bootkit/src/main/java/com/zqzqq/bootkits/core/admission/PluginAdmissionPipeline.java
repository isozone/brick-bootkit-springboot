/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.core.admission;

import com.zqzqq.bootkits.core.exception.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Admission pipeline with mode-based enforcement.
 */
public class PluginAdmissionPipeline {

    private static final Logger log = LoggerFactory.getLogger(PluginAdmissionPipeline.class);

    private final PluginAdmissionMode mode;
    private final List<PluginAdmissionCheck> checks;

    public PluginAdmissionPipeline(PluginAdmissionMode mode, List<PluginAdmissionCheck> checks) {
        this.mode = mode == null ? PluginAdmissionMode.WARN : mode;
        this.checks = new ArrayList<>();
        if (checks != null) {
            this.checks.addAll(checks);
        }
        this.checks.removeIf(Objects::isNull);
        this.checks.sort(Comparator.comparing(PluginAdmissionCheck::getName));
    }

    public PluginAdmissionMode getMode() {
        return mode;
    }

    public PluginAdmissionReport evaluate(PluginAdmissionContext context) {
        PluginAdmissionReport report = new PluginAdmissionReport();
        if (mode == PluginAdmissionMode.OFF) {
            return report;
        }

        for (PluginAdmissionCheck check : checks) {
            PluginAdmissionDecision decision = check.check(context);
            if (decision == null || decision.getLevel() == PluginAdmissionDecision.Level.PASS) {
                continue;
            }
            String issue = "check=" + check.getName() + ", op=" + context.getOperation()
                    + ", plugin=" + context.getPluginId() + ", detail=" + decision.getMessage();
            if (decision.getLevel() == PluginAdmissionDecision.Level.WARN) {
                report.addWarning(issue);
                continue;
            }
            report.addRejection(issue);
        }

        if (!report.getWarnings().isEmpty()) {
            for (String warning : report.getWarnings()) {
                log.warn("Plugin admission warning: {}", warning);
            }
        }

        if (report.hasRejections()) {
            for (String rejection : report.getRejections()) {
                log.warn("Plugin admission rejection: {}", rejection);
            }
            if (mode == PluginAdmissionMode.ENFORCE) {
                throw new PluginException("Plugin admission rejected: " + report.getRejections());
            }
        }

        return report;
    }
}
