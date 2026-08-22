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
