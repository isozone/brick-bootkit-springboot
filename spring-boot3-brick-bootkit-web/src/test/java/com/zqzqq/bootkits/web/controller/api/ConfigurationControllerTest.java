package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.config.PluginConfiguration;
import com.zqzqq.bootkits.core.config.PluginConfigurationStatistics;
import com.zqzqq.bootkits.core.config.PluginConfigurationVersion;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.ConfigurationWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 插件配置热更新 Controller 单元测试。
 */
@DisplayName("ConfigurationController Test")
class ConfigurationControllerTest {

    private ConfigurationWebService configurationWebService;
    private PluginWebAuthorizationService authorizationService;
    private ConfigurationController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        configurationWebService = mock(ConfigurationWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<ConfigurationWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(configurationWebService);
        controller = new ConfigurationController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取配置统计")
    void statisticsShouldReturnData() {
        PluginConfigurationStatistics stats = new PluginConfigurationStatistics(1, 3, 1, 1);
        when(configurationWebService.getStatistics()).thenReturn(stats);

        ApiResult<PluginConfigurationStatistics> result = controller.statistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalConfigurations()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取所有插件配置")
    void allConfigurationsShouldReturnMap() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(configurationWebService.getAllConfigurations())
                .thenReturn(Collections.singletonMap("plugin-a", config));

        ApiResult<Map<String, PluginConfiguration>> result = controller.allConfigurations();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKey("plugin-a");
    }

    @Test
    @DisplayName("获取指定插件配置")
    void getConfigurationShouldReturnConfig() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(configurationWebService.getConfiguration("plugin-a")).thenReturn(config);

        ApiResult<PluginConfiguration> result = controller.getConfiguration("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getPluginId()).isEqualTo("plugin-a");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");
    }

    @Test
    @DisplayName("热更新插件配置")
    void updateConfigurationShouldReturnUpdated() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "2.0.0");
        when(configurationWebService.updateConfiguration(eq("plugin-a"), any(), any()))
                .thenReturn(config);

        ConfigurationController.UpdateRequest request = new ConfigurationController.UpdateRequest();
        request.setConfiguration(new PluginConfiguration("plugin-a", "1.0.0"));
        request.setVersionDescription("web update");

        ApiResult<PluginConfiguration> result = controller.updateConfiguration("plugin-a", request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getVersion()).isEqualTo("2.0.0");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_INSTALL, "plugin-a");
    }

    @Test
    @DisplayName("获取配置版本历史")
    void versionsShouldReturnList() {
        when(configurationWebService.getConfigurationVersions("plugin-a"))
                .thenReturn(Collections.emptyList());

        ApiResult<List<PluginConfigurationVersion>> result = controller.versions("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("回滚配置到指定版本")
    void rollbackShouldInvokeService() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(configurationWebService.rollbackToVersion("plugin-a", "v1")).thenReturn(config);

        ConfigurationController.RollbackRequest request = new ConfigurationController.RollbackRequest();
        request.setVersionId("v1");

        ApiResult<PluginConfiguration> result = controller.rollback("plugin-a", request);

        assertThat(result.isSuccess()).isTrue();
        verify(configurationWebService).rollbackToVersion("plugin-a", "v1");
    }

    @Test
    @DisplayName("删除插件配置")
    void removeShouldInvokeService() {
        ApiResult<Void> result = controller.remove("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        verify(configurationWebService).removeConfiguration("plugin-a");
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void statisticsShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.statistics())
                .isInstanceOf(PluginWebAuthorizationException.class);
    }
}
