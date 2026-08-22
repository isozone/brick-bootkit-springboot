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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginAdmissionPipelineTest {

    @Test
    void enforceModeShouldReject() {
        PluginAdmissionPipeline pipeline = new PluginAdmissionPipeline(
                PluginAdmissionMode.ENFORCE,
                java.util.List.of(new PluginAdmissionCheck() {
                    @Override
                    public String getName() {
                        return "test-check";
                    }

                    @Override
                    public PluginAdmissionDecision check(PluginAdmissionContext context) {
                        return PluginAdmissionDecision.reject("denied");
                    }
                })
        );

        assertThatThrownBy(() -> pipeline.evaluate(new PluginAdmissionContext(
                PluginAdmissionOperation.INSTALL, null, null)))
                .hasMessageContaining("Plugin admission rejected");
    }

    @Test
    void warnModeShouldNotThrow() {
        PluginAdmissionPipeline pipeline = new PluginAdmissionPipeline(
                PluginAdmissionMode.WARN,
                java.util.List.of(new PluginAdmissionCheck() {
                    @Override
                    public String getName() {
                        return "warn-check";
                    }

                    @Override
                    public PluginAdmissionDecision check(PluginAdmissionContext context) {
                        return PluginAdmissionDecision.reject("warn only");
                    }
                })
        );

        PluginAdmissionReport report = pipeline.evaluate(new PluginAdmissionContext(
                PluginAdmissionOperation.START, null, null));
        assertThat(report.hasRejections()).isTrue();
    }
}
