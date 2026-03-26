package com.zqzqq.bootkits.integration.doctor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginDoctorExportFormatterTest {

    @Test
    void shouldRenderReadableTextReport() {
        PluginDoctorReport report = new PluginDoctorReport(
                0L,
                true,
                "com.example.demo",
                List.of("plugins"),
                1,
                0,
                1,
                1,
                "WARN",
                "doctor=WARN",
                List.of(new PluginDoctorReport.Item(
                        "NO_PLUGINS_FOUND",
                        1020,
                        "WARN",
                        "当前未发现任何插件",
                        "检查 plugin.pluginPath",
                        "/troubleshooting",
                        "common-errors"))
        );

        String text = PluginDoctorExportFormatter.toText(report);

        assertThat(text).contains("Brick BootKit Doctor Report");
        assertThat(text).contains("doctor=WARN");
        assertThat(text).contains("NO_PLUGINS_FOUND");
        assertThat(text).contains("/troubleshooting#common-errors");
    }
}
