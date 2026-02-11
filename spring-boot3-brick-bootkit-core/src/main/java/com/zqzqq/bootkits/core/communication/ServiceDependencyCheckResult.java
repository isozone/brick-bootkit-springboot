/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of service dependency check.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class ServiceDependencyCheckResult {

    private final String pluginId;
    private final boolean satisfied;
    private final List<DependencyInfo> satisfiedDependencies;
    private final List<DependencyInfo> unsatisfiedDependencies;
    private final List<DependencyWarning> warnings;

    private ServiceDependencyCheckResult(
        String pluginId,
        boolean satisfied,
        List<DependencyInfo> satisfiedDependencies,
        List<DependencyInfo> unsatisfiedDependencies,
        List<DependencyWarning> warnings
    ) {
        this.pluginId = pluginId;
        this.satisfied = satisfied;
        this.satisfiedDependencies = Collections.unmodifiableList(satisfiedDependencies);
        this.unsatisfiedDependencies = Collections.unmodifiableList(unsatisfiedDependencies);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /**
     * Create a successful check result.
     */
    public static ServiceDependencyCheckResult success(String pluginId) {
        return new Builder(pluginId)
            .satisfied(true)
            .build();
    }

    /**
     * Create a failed check result.
     */
    public static ServiceDependencyCheckResult failure(
        String pluginId,
        List<DependencyInfo> unsatisfied
    ) {
        return new Builder(pluginId)
            .satisfied(false)
            .unsatisfiedDependencies(unsatisfied)
            .build();
    }

    public String getPluginId() {
        return pluginId;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public List<DependencyInfo> getSatisfiedDependencies() {
        return satisfiedDependencies;
    }

    public List<DependencyInfo> getUnsatisfiedDependencies() {
        return unsatisfiedDependencies;
    }

    public List<DependencyWarning> getWarnings() {
        return warnings;
    }

    /**
     * Dependency information.
     */
    public static class DependencyInfo {
        private final Class<?> interfaceClass;
        private final String requiredVersionRange;
        private final String actualVersion;  // null if not found
        private final boolean satisfied;
        private final String reason;

        public DependencyInfo(
            Class<?> interfaceClass,
            String requiredVersionRange,
            String actualVersion,
            boolean satisfied,
            String reason
        ) {
            this.interfaceClass = interfaceClass;
            this.requiredVersionRange = requiredVersionRange;
            this.actualVersion = actualVersion;
            this.satisfied = satisfied;
            this.reason = reason;
        }

        public Class<?> getInterfaceClass() {
            return interfaceClass;
        }

        public String getRequiredVersionRange() {
            return requiredVersionRange;
        }

        public String getActualVersion() {
            return actualVersion;
        }

        public boolean isSatisfied() {
            return satisfied;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "DependencyInfo{" +
                "interfaceClass=" + interfaceClass.getName() +
                ", requiredVersionRange='" + requiredVersionRange + '\'' +
                ", actualVersion='" + actualVersion + '\'' +
                ", satisfied=" + satisfied +
                ", reason='" + reason + '\'' +
                '}';
        }
    }

    /**
     * Dependency warning (non-fatal).
     */
    public static class DependencyWarning {
        private final Class<?> interfaceClass;
        private final String message;
        private final WarningType type;

        public DependencyWarning(
            Class<?> interfaceClass,
            String message,
            WarningType type
        ) {
            this.interfaceClass = interfaceClass;
            this.message = message;
            this.type = type;
        }

        public Class<?> getInterfaceClass() {
            return interfaceClass;
        }

        public String getMessage() {
            return message;
        }

        public WarningType getType() {
            return type;
        }

        public enum WarningType {
            VERSION_MISMATCH,
            DEPRECATED,
            PERFORMANCE,
            SECURITY
        }
    }

    /**
     * Builder for ServiceDependencyCheckResult.
     */
    public static class Builder {
        private final String pluginId;
        private boolean satisfied = true;
        private List<DependencyInfo> satisfiedDependencies = new ArrayList<>();
        private List<DependencyInfo> unsatisfiedDependencies = new ArrayList<>();
        private List<DependencyWarning> warnings = new ArrayList<>();

        public Builder(String pluginId) {
            this.pluginId = Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        }

        public Builder satisfied(boolean satisfied) {
            this.satisfied = satisfied;
            return this;
        }

        public Builder satisfiedDependencies(List<DependencyInfo> dependencies) {
            this.satisfiedDependencies = dependencies != null ? dependencies : new ArrayList<>();
            return this;
        }

        public Builder unsatisfiedDependencies(List<DependencyInfo> dependencies) {
            this.unsatisfiedDependencies = dependencies != null ? dependencies : new ArrayList<>();
            this.satisfied = this.unsatisfiedDependencies.isEmpty();
            return this;
        }

        public Builder addSatisfiedDependency(DependencyInfo dependency) {
            this.satisfiedDependencies.add(dependency);
            return this;
        }

        public Builder addUnsatisfiedDependency(DependencyInfo dependency) {
            this.unsatisfiedDependencies.add(dependency);
            this.satisfied = false;
            return this;
        }

        public Builder warnings(List<DependencyWarning> warnings) {
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            return this;
        }

        public Builder addWarning(DependencyWarning warning) {
            this.warnings.add(warning);
            return this;
        }

        public ServiceDependencyCheckResult build() {
            return new ServiceDependencyCheckResult(
                pluginId,
                satisfied,
                satisfiedDependencies,
                unsatisfiedDependencies,
                warnings
            );
        }
    }
}
