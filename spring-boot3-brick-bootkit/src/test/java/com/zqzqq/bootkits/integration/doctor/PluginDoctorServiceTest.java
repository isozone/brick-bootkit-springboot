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
