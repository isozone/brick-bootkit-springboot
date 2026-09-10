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

import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.integration.AdoptionLevel;
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

/**
 * doctor 对渐进式接入级别的识别测试。
 */
class PluginDoctorAdoptionTest {

    @TempDir
    Path tempDir;

    private IntegrationConfiguration baseConfiguration() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);
        Path uploadTemp = tempDir.resolve("upload-temp");
        Files.createDirectories(uploadTemp);

        IntegrationConfiguration configuration = mock(IntegrationConfiguration.class);
        when(configuration.enable()).thenReturn(true);
        when(configuration.mainPackage()).thenReturn("com.example.demo");
        when(configuration.pluginPath()).thenReturn(List.of(pluginRoot.toString()));
        when(configuration.uploadTempPath()).thenReturn(uploadTemp.toString());
        when(configuration.adoptionLevel()).thenReturn(AdoptionLevel.ACTIVE);
        return configuration;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PluginManager> emptyManagerProvider() {
        ObjectProvider<PluginManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @Test
    void shouldWarnWhenRunningInShadowLevel() throws Exception {
        IntegrationConfiguration configuration = baseConfiguration();
        when(configuration.adoptionLevel()).thenReturn(AdoptionLevel.SHADOW);

        PluginDoctorReport report = new PluginDoctorService(configuration, emptyManagerProvider()).diagnose();

        assertThat(report.getItems()).extracting(PluginDoctorReport.Item::getCode)
                .contains("ADOPTION_LEVEL_SHADOW");
        assertThat(report.getItems()).filteredOn(item -> "ADOPTION_LEVEL_SHADOW".equals(item.getCode()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getSeverity()).isEqualTo("WARN");
                    assertThat(item.getSuggestion()).contains("autoLoadPlugins");
                    assertThat(item.getDocAnchor()).isEqualTo("adoption");
                });
        assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldWarnWhenRunningInObserveLevel() throws Exception {
        IntegrationConfiguration configuration = baseConfiguration();
        when(configuration.adoptionLevel()).thenReturn(AdoptionLevel.OBSERVE);

        PluginDoctorReport report = new PluginDoctorService(configuration, emptyManagerProvider()).diagnose();

        assertThat(report.getItems()).extracting(PluginDoctorReport.Item::getCode)
                .contains("ADOPTION_LEVEL_OBSERVE");
        assertThat(report.getItems()).filteredOn(item -> "ADOPTION_LEVEL_OBSERVE".equals(item.getCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getSuggestion()).contains("autoStartPlugins"));
    }

    @Test
    void shouldNotReportAdoptionIssueInActiveLevel() throws Exception {
        IntegrationConfiguration configuration = baseConfiguration();

        PluginDoctorReport report = new PluginDoctorService(configuration, emptyManagerProvider()).diagnose();

        assertThat(report.getItems()).extracting(PluginDoctorReport.Item::getCode)
                .doesNotContain("ADOPTION_LEVEL_SHADOW", "ADOPTION_LEVEL_OBSERVE", "ADOPTION_LEVEL_ACTIVE");
    }

    @Test
    void shouldTolerateNullAdoptionLevelFromLegacyConfiguration() throws Exception {
        IntegrationConfiguration configuration = baseConfiguration();
        when(configuration.adoptionLevel()).thenReturn(null);

        PluginDoctorReport report = new PluginDoctorService(configuration, emptyManagerProvider()).diagnose();

        assertThat(report.getItems()).extracting(PluginDoctorReport.Item::getCode)
                .doesNotContain("ADOPTION_LEVEL_SHADOW", "ADOPTION_LEVEL_OBSERVE");
    }
}
