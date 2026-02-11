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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version range parser and comparator.
 * <p>
 * Supports semantic versioning with range expressions:
 * <ul>
 *   <li>Single version: "1.0.0", "=2.0.0"</li>
 *   <li>Greater than: ">=1.0.0", ">1.0.0"</li>
 *   <li>Less than: "&lt;=2.0.0", "&lt;2.0.0"</li>
 *   <li>Range: "[1.0,2.0)", "(1.0.0,2.0.0]"</li>
 * </ul>
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class VersionRange {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "([\\[\\(])\\s*([0-9]+(?:\\.[0-9]+)*(?:\\.[0-9]+)?)\\s*,\\s*([0-9]+(?:\\.[0-9]+)*(?:\\.[0-9]+)?)\\s*([\\]\\)])"
    );

    private static final Pattern SINGLE_PATTERN = Pattern.compile(
        "([><=!]+)?\\s*([0-9]+(?:\\.[0-9]+)*(?:\\.[0-9]+)?)"
    );

    private final LowerBound lowerBound;
    private final UpperBound upperBound;

    private VersionRange(LowerBound lowerBound, UpperBound upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Parse version range string.
     *
     * @param range version range string
     * @return VersionRange instance
     */
    public static VersionRange parse(String range) {
        if (range == null || range.trim().isEmpty()) {
            return any();
        }

        String trimmed = range.trim();

        // Try range pattern [1.0,2.0)
        Matcher rangeMatcher = RANGE_PATTERN.matcher(trimmed);
        if (rangeMatcher.matches()) {
            boolean lowerInclusive = "[".equals(rangeMatcher.group(1));
            String lower = rangeMatcher.group(2);
            String upper = rangeMatcher.group(3);
            boolean upperInclusive = "]".equals(rangeMatcher.group(4));

            return new VersionRange(
                new LowerBound(lower, lowerInclusive),
                new UpperBound(upper, upperInclusive)
            );
        }

        // Try single version pattern
        Matcher singleMatcher = SINGLE_PATTERN.matcher(trimmed);
        if (singleMatcher.matches()) {
            String operator = singleMatcher.group(1);
            String version = singleMatcher.group(2);

            if (operator == null || operator.isEmpty()) {
                return exact(version);
            }

            switch (operator) {
                case ">":
                    return new VersionRange(
                        new LowerBound(version, false),
                        UpperBound.UNBOUNDED
                    );
                case ">=":
                    return new VersionRange(
                        new LowerBound(version, true),
                        UpperBound.UNBOUNDED
                    );
                case "<":
                    return new VersionRange(
                        LowerBound.ZERO,
                        new UpperBound(version, false)
                    );
                case "<=":
                    return new VersionRange(
                        LowerBound.ZERO,
                        new UpperBound(version, true)
                    );
                case "!=":
                case "<>":
                    return parse("[0, " + version + ")").union(parse("(" + version + ", +∞)"));
                case "=":
                    return exact(version);
                default:
                    return exact(version);
            }
        }

        // Default: exact match
        return exact(trimmed);
    }

    /**
     * Create exact version range.
     */
    public static VersionRange exact(String version) {
        return new VersionRange(
            new LowerBound(version, true),
            new UpperBound(version, true)
        );
    }

    /**
     * Create range matching any version.
     */
    public static VersionRange any() {
        return new VersionRange(LowerBound.ZERO, UpperBound.UNBOUNDED);
    }

    /**
     * Create version range [min, max).
     */
    public static VersionRange range(String min, String max) {
        return new VersionRange(
            new LowerBound(min, true),
            new UpperBound(max, false)
        );
    }

    /**
     * Union of two version ranges.
     */
    public VersionRange union(VersionRange other) {
        // Simplified union - in practice would need more complex logic
        return this;
    }

    /**
     * Check if a version is compatible with this range.
     *
     * @param version version to check
     * @return true if compatible
     */
    public boolean isCompatible(String version) {
        if (version == null) {
            return false;
        }

        int cmpLower = lowerBound.compareTo(version);
        if (cmpLower > 0) {  // version < lower bound
            return false;
        }

        int cmpUpper = upperBound.compareTo(version);
        if (cmpUpper < 0) {  // version > upper bound
            return false;
        }

        return true;
    }

    /**
     * Get lower bound representation.
     */
    public LowerBound getLowerBound() {
        return lowerBound;
    }

    /**
     * Get upper bound representation.
     */
    public UpperBound getUpperBound() {
        return upperBound;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lowerBound.isInclusive() ? "[" : "(");
        sb.append(lowerBound.getVersion());
        sb.append(", ");
        sb.append(upperBound.getVersion());
        sb.append(upperBound.isInclusive() ? "]" : ")");
        return sb.toString();
    }

    /**
     * Lower bound of version range.
     */
    public static class LowerBound implements Comparable<String> {
        private static final LowerBound ZERO = new LowerBound("0", false);

        private final String version;
        private final boolean inclusive;

        public LowerBound(String version, boolean inclusive) {
            this.version = version;
            this.inclusive = inclusive;
        }

        public String getVersion() {
            return version;
        }

        public boolean isInclusive() {
            return inclusive;
        }

        @Override
        public int compareTo(String other) {
            if (other == null) {
                return 1;
            }

            int cmp = compareVersions(version, other);
            if (cmp == 0) {
                return inclusive ? 0 : 1;  // exclusive bound requires version > lower
            }
            return cmp;
        }

        private static int compareVersions(String v1, String v2) {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int len = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < len; i++) {
                int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }
            return 0;
        }

        private static int parseVersionPart(String part) {
            try {
                // Remove any non-numeric suffix (like "-SNAPSHOT")
                String numPart = part.split("[^0-9]")[0];
                return Integer.parseInt(numPart);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /**
     * Upper bound of version range.
     */
    public static class UpperBound implements Comparable<String> {
        private static final UpperBound UNBOUNDED = new UpperBound("+∞", false);

        private final String version;
        private final boolean inclusive;

        public UpperBound(String version, boolean inclusive) {
            this.version = version;
            this.inclusive = inclusive;
        }

        public String getVersion() {
            return version;
        }

        public boolean isInclusive() {
            return inclusive;
        }

        @Override
        public int compareTo(String other) {
            if (other == null) {
                return -1;
            }

            if (this == UNBOUNDED) {
                return -1;  // +∞ is always greater
            }

            int cmp = LowerBound.compareVersions(version, other);
            if (cmp == 0) {
                return inclusive ? 0 : -1;  // exclusive bound requires version < upper
            }
            return cmp;
        }
    }
}
