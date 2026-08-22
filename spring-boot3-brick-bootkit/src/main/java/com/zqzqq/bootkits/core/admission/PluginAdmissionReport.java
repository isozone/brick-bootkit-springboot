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
