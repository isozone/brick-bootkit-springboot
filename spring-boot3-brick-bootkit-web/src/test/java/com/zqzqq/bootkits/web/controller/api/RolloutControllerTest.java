package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.RolloutWebService;
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
 * 灰度发布 Controller 单元测试。
 */
@DisplayName("RolloutController Test")
class RolloutControllerTest {

    private RolloutWebService rolloutWebService;
    private PluginWebAuthorizationService authorizationService;
    private RolloutController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        rolloutWebService = mock(RolloutWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<RolloutWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(rolloutWebService);
        controller = new RolloutController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取灰度发布配置")
    void configShouldReturnData() {
        Map<String, Object> config = Collections.singletonMap("mode", "GRAY");
        when(rolloutWebService.getRolloutConfig()).thenReturn(config);

        ApiResult<Map<String, Object>> result = controller.config();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("mode")).isEqualTo("GRAY");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("获取灰度探针列表")
    void probesShouldReturnList() {
        when(rolloutWebService.getProbeNames()).thenReturn(Collections.singletonList("smoke-probe"));

        ApiResult<List<String>> result = controller.probes();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("smoke-probe");
    }

    @Test
    @DisplayName("模拟灰度决策")
    void checkShouldReturnDecision() {
        RolloutWebService.RolloutDecision decision = new RolloutWebService.RolloutDecision("plugin-a");
        decision.setPassed(true);
        when(rolloutWebService.checkPlugin("plugin-a")).thenReturn(decision);

        ApiResult<RolloutWebService.RolloutDecision> result = controller.check("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isPassed()).isTrue();
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void configShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.config())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(rolloutWebService);
    }
}
