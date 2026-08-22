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

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginDoctorServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReportErrorsWhenCoreConfigurationIsMissing() {
        IntegrationConfiguration configuration = mock(IntegrationConfiguration.class);
        when(configuration.enable()).thenReturn(true);
        when(configuration.mainPackage()).thenReturn("");
        when(configuration.pluginPath()).thenReturn(List.of());
        when(configuration.uploadTempPath()).thenReturn(tempDir.resolve("upload-temp").toString());

        @SuppressWarnings("unchecked")
        ObjectProvider<PluginManager> provider = mock(ObjectProvider.class);

        PluginDoctorReport report = new PluginDoctorService(configuration, provider).diagnose();

        assertThat(report.getOverallStatus()).isEqualTo("ERROR");
        assertThat(report.getErrorCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.getItems()).extracting(PluginDoctorReport.Item::getCode)
                .contains("MAIN_PACKAGE_MISSING", "PLUGIN_PATH_EMPTY");
        assertThat(report.getItems()).filteredOn(item -> "MAIN_PACKAGE_MISSING".equals(item.getCode()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getErrorCode()).isEqualTo(1012);
                    assertThat(item.getDocPath()).isEqualTo("/troubleshooting");
                    assertThat(item.getDocAnchor()).isEqualTo("common-errors");
                });
    }

    @Test
    void shouldReportHealthySetupWhenPathsAndPluginsAreReady() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);

        IntegrationConfiguration configuration = mock(IntegrationConfiguration.class);
        when(configuration.enable()).thenReturn(true);
        when(configuration.mainPackage()).thenReturn("com.example.demo");
        when(configuration.pluginPath()).thenReturn(List.of(pluginRoot.toString()));
        when(configuration.uploadTempPath()).thenReturn(tempDir.resolve("upload-temp").toString());

        PluginInfo plugin = mock(PluginInfo.class);
        when(plugin.getPluginState()).thenReturn(EnhancedPluginState.STARTED);

        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.getPlugins()).thenReturn(List.of(plugin));

        @SuppressWarnings("unchecked")
        ObjectProvider<PluginManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(pluginManager);

        PluginDoctorReport report = new PluginDoctorService(configuration, provider).diagnose();

        assertThat(report.getOverallStatus()).isEqualTo("OK");
        assertThat(report.getPluginCount()).isEqualTo(1);
        assertThat(report.getStartedPluginCount()).isEqualTo(1);
        assertThat(report.getErrorCount()).isZero();
        assertThat(report.getWarningCount()).isZero();
    }
}
