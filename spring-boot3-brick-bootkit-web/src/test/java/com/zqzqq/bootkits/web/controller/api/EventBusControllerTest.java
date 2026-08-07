package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.EventBusWebService;
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
 * 事件总线 Controller 单元测试。
 */
@DisplayName("EventBusController Test")
class EventBusControllerTest {

    private EventBusWebService eventBusWebService;
    private PluginWebAuthorizationService authorizationService;
    private EventBusController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        eventBusWebService = mock(EventBusWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<EventBusWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(eventBusWebService);
        controller = new EventBusController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取事件统计")
    void statsShouldReturnData() {
        Map<String, Integer> stats = Collections.singletonMap("PLUGIN_STARTED.demo-plugin", 3);
        when(eventBusWebService.getEventCounts()).thenReturn(stats);

        ApiResult<Map<String, Integer>> result = controller.stats();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("PLUGIN_STARTED.demo-plugin", 3);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("获取事件类型列表")
    void typesShouldReturnList() {
        when(eventBusWebService.getEventTypes()).thenReturn(Collections.singletonList("PLUGIN_STARTED"));

        ApiResult<List<String>> result = controller.types();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("PLUGIN_STARTED");
    }

    @Test
    @DisplayName("获取最近事件流")
    void recentShouldReturnList() {
        EventBusWebService.RecentEvent event = new EventBusWebService.RecentEvent(
                "PLUGIN_STARTED", "demo-plugin", null, 1000L, true);
        when(eventBusWebService.getRecentEvents(50)).thenReturn(Collections.singletonList(event));

        ApiResult<List<EventBusWebService.RecentEvent>> result = controller.recent(50);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getType()).isEqualTo("PLUGIN_STARTED");
        assertThat(result.getData().get(0).isBroadcast()).isTrue();
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void statsShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.stats())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(eventBusWebService);
    }
}
