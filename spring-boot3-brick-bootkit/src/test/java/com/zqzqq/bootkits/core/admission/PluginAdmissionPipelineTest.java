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
