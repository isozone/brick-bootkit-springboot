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
