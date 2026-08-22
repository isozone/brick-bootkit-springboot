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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorReport;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorService;
import com.zqzqq.bootkits.web.dto.MonitorOverviewDTO;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorWebServiceTest {

    @Test
    void shouldExposeDoctorSummaryInOverview() {
        PluginDoctorService doctorService = mock(PluginDoctorService.class);
        PluginDoctorReport report = new PluginDoctorReport(
                System.currentTimeMillis(),
                true,
                "com.example.demo",
                List.of("plugins"),
                1,
                1,
                0,
                1,
                "WARN",
                "doctor=WARN",
                List.of(new PluginDoctorReport.Item(
                        "NO_PLUGINS_FOUND",
                        1020,
                        "WARN",
                        "未发现插件",
                        "检查 plugin.pluginPath",
                        "/troubleshooting",
                        "common-errors"))
        );
        when(doctorService.diagnose()).thenReturn(report);

        @SuppressWarnings("unchecked")
        ObjectProvider<PluginManager> pluginManagerProvider = mock(ObjectProvider.class);
        when(pluginManagerProvider.getIfAvailable()).thenReturn(null);

        @SuppressWarnings("unchecked")
        ObjectProvider<PluginDoctorService> doctorProvider = mock(ObjectProvider.class);
        when(doctorProvider.getIfAvailable()).thenReturn(doctorService);

        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        MonitorWebService service = new MonitorWebService(pluginManagerProvider, doctorProvider, meterRegistry);

        MonitorOverviewDTO overview = service.getOverview();

        assertThat(overview.getDoctorSummary()).isNotNull();
        assertThat(overview.getDoctorSummary().getOverallStatus()).isEqualTo("WARN");
        assertThat(overview.getDoctorSummary().getTopMessages()).contains("未发现插件");
    }
}
