package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutMode;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbeResult;
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
 * 灰度发布 Web 服务单元测试。
 */
@DisplayName("RolloutWebService Test")
class RolloutWebServiceTest {

    private IntegrationConfiguration configuration;
    private PluginManager pluginManager;
    private PluginRolloutProbe probe;
    private RolloutWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        configuration = mock(IntegrationConfiguration.class);
        pluginManager = mock(PluginManager.class);
        probe = mock(PluginRolloutProbe.class);
        when(probe.getName()).thenReturn("smoke-probe");

        ObjectProvider<IntegrationConfiguration> cProvider = mock(ObjectProvider.class);
        when(cProvider.getIfAvailable()).thenReturn(configuration);
        ObjectProvider<PluginManager> pmProvider = mock(ObjectProvider.class);
        when(pmProvider.getIfAvailable()).thenReturn(pluginManager);

        service = new RolloutWebService(cProvider, pmProvider, Collections.singletonList(probe));
    }

    @Test
    @DisplayName("获取灰度配置")
    void getRolloutConfigShouldReturnMode() {
        when(configuration.pluginRolloutMode()).thenReturn(PluginRolloutMode.GRAY);
        when(configuration.pluginRolloutAutoStart()).thenReturn(true);
        when(configuration.pluginRolloutRollbackOnFailure()).thenReturn(true);

        Map<String, Object> config = service.getRolloutConfig();

        assertThat(config.get("mode")).isEqualTo("GRAY");
        assertThat(config.get("probeCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("获取探针名称列表")
    void getProbeNamesShouldReturnList() {
        List<String> names = service.getProbeNames();

        assertThat(names).containsExactly("smoke-probe");
    }

    @Test
    @DisplayName("模拟灰度决策：全部通过")
    void checkPluginShouldPassWhenAllProbesPass() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginManager.getPlugin("plugin-a")).thenReturn(pluginInfo);
        when(probe.probe("plugin-a", pluginInfo))
                .thenReturn(PluginRolloutProbeResult.pass("ok"));

        RolloutWebService.RolloutDecision decision = service.checkPlugin("plugin-a");

        assertThat(decision.isPassed()).isTrue();
        assertThat(decision.getProbes()).hasSize(1);
        assertThat(decision.getProbes().get(0).isPassed()).isTrue();
    }

    @Test
    @DisplayName("模拟灰度决策：探针拒绝")
    void checkPluginShouldFailWhenProbeRejects() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginManager.getPlugin("plugin-a")).thenReturn(pluginInfo);
        when(probe.probe("plugin-a", pluginInfo))
                .thenReturn(PluginRolloutProbeResult.reject("版本不兼容"));

        RolloutWebService.RolloutDecision decision = service.checkPlugin("plugin-a");

        assertThat(decision.isPassed()).isFalse();
        assertThat(decision.getProbes().get(0).getMessage()).isEqualTo("版本不兼容");
    }

    @Test
    @DisplayName("模拟灰度决策：插件不存在抛出异常")
    void checkPluginShouldFailWhenPluginMissing() {
        when(pluginManager.getPlugin("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.checkPlugin("missing"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("配置缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenConfigurationMissing() {
        ObjectProvider<IntegrationConfiguration> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        RolloutWebService emptyService = new RolloutWebService(emptyProvider,
                mock(ObjectProvider.class), Collections.emptyList());

        assertThatThrownBy(emptyService::getRolloutConfig)
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不可用");
    }
}
