package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.config.PluginConfiguration;
import com.zqzqq.bootkits.core.config.PluginConfigurationManager;
import com.zqzqq.bootkits.core.config.PluginConfigurationStatistics;
import com.zqzqq.bootkits.core.config.PluginConfigurationVersion;
import com.zqzqq.bootkits.core.exception.PluginException;
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
 * 插件配置热更新 Web 服务单元测试。
 */
@DisplayName("ConfigurationWebService Test")
class ConfigurationWebServiceTest {

    private PluginConfigurationManager manager;
    private ConfigurationWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        manager = mock(PluginConfigurationManager.class);
        ObjectProvider<PluginConfigurationManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(manager);
        service = new ConfigurationWebService(provider);
    }

    @Test
    @DisplayName("获取配置统计")
    void getStatisticsShouldReturnData() {
        PluginConfigurationStatistics stats = new PluginConfigurationStatistics(1, 3, 1, 1);
        when(manager.getStatistics()).thenReturn(stats);

        PluginConfigurationStatistics actual = service.getStatistics();

        assertThat(actual.getTotalConfigurations()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取所有插件配置")
    void getAllConfigurationsShouldReturnMap() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(manager.getAllConfigurations()).thenReturn(Collections.singletonMap("plugin-a", config));

        Map<String, PluginConfiguration> actual = service.getAllConfigurations();

        assertThat(actual).containsKey("plugin-a");
    }

    @Test
    @DisplayName("获取指定插件配置")
    void getConfigurationShouldReturnConfig() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(manager.hasConfiguration("plugin-a")).thenReturn(true);
        when(manager.getConfiguration("plugin-a")).thenReturn(config);

        PluginConfiguration actual = service.getConfiguration("plugin-a");

        assertThat(actual.getPluginId()).isEqualTo("plugin-a");
    }

    @Test
    @DisplayName("配置不存在时抛出异常")
    void getConfigurationShouldFailWhenMissing() {
        when(manager.hasConfiguration("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.getConfiguration("missing"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("热更新插件配置")
    void updateConfigurationShouldSetPluginId() {
        PluginConfiguration input = new PluginConfiguration();
        PluginConfiguration updated = new PluginConfiguration("plugin-a", "2.0.0");
        when(manager.getConfiguration("plugin-a")).thenReturn(updated);

        PluginConfiguration actual = service.updateConfiguration("plugin-a", input, "web update");

        assertThat(actual.getVersion()).isEqualTo("2.0.0");
        verify(manager).updateConfiguration(eq("plugin-a"), any(), eq("web update"));
    }

    @Test
    @DisplayName("配置为空时抛出异常")
    void updateConfigurationShouldFailOnNull() {
        assertThatThrownBy(() -> service.updateConfiguration("plugin-a", null, "desc"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("获取配置版本历史")
    void getConfigurationVersionsShouldReturnList() {
        when(manager.getConfigurationVersions("plugin-a")).thenReturn(Collections.emptyList());

        List<PluginConfigurationVersion> actual = service.getConfigurationVersions("plugin-a");

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("回滚配置到指定版本")
    void rollbackToVersionShouldReturnConfig() {
        PluginConfiguration config = new PluginConfiguration("plugin-a", "1.0.0");
        when(manager.getConfiguration("plugin-a")).thenReturn(config);

        PluginConfiguration actual = service.rollbackToVersion("plugin-a", "v1");

        assertThat(actual).isNotNull();
        verify(manager).rollbackToVersion("plugin-a", "v1");
    }

    @Test
    @DisplayName("删除插件配置")
    void removeConfigurationShouldInvokeManager() {
        service.removeConfiguration("plugin-a");

        verify(manager).removeConfiguration("plugin-a");
    }

    @Test
    @DisplayName("配置管理器缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenManagerMissing() {
        ObjectProvider<PluginConfigurationManager> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        ConfigurationWebService emptyService = new ConfigurationWebService(emptyProvider);

        assertThatThrownBy(emptyService::getStatistics)
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
