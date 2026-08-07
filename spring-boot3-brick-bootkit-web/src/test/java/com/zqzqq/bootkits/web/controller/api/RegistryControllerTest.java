package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.communication.RegistryStatistics;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.RegistryWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 服务注册中心 Controller 单元测试。
 */
@DisplayName("RegistryController Test")
class RegistryControllerTest {

    private RegistryWebService registryWebService;
    private PluginWebAuthorizationService authorizationService;
    private RegistryController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        registryWebService = mock(RegistryWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<RegistryWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registryWebService);
        controller = new RegistryController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取注册中心统计")
    void statisticsShouldReturnData() {
        RegistryStatistics stats = new RegistryStatistics(3, 2, 3, 1);
        when(registryWebService.getStatistics()).thenReturn(stats);

        ApiResult<RegistryStatistics> result = controller.statistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalServices()).isEqualTo(3);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("获取所有注册服务（按插件分组）")
    void servicesShouldReturnGroups() {
        RegistryWebService.PluginServiceGroup group =
                new RegistryWebService.PluginServiceGroup("plugin-a", Collections.emptyList());
        when(registryWebService.getServicesGroupedByPlugin()).thenReturn(Collections.singletonList(group));

        ApiResult<List<RegistryWebService.PluginServiceGroup>> result = controller.services();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getPluginId()).isEqualTo("plugin-a");
    }

    @Test
    @DisplayName("获取指定插件的服务列表")
    void servicesByPluginShouldReturnList() {
        when(registryWebService.getServicesByPlugin("plugin-a")).thenReturn(Collections.emptyList());

        ApiResult<List<ServiceDescriptor>> result = controller.servicesByPlugin("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");
    }

    @Test
    @DisplayName("获取已注册插件 ID")
    void pluginsShouldReturnIds() {
        Set<String> ids = Collections.singleton("plugin-a");
        when(registryWebService.getRegisteredPlugins()).thenReturn(ids);

        ApiResult<Set<String>> result = controller.plugins();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("plugin-a");
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void statisticsShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.statistics())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(registryWebService);
    }
}
