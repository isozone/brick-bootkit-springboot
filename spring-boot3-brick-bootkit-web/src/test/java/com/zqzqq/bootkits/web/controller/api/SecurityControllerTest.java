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


package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.security.PluginPermission;
import com.zqzqq.bootkits.core.security.PluginSecurityPolicy;
import com.zqzqq.bootkits.core.security.PluginSecurityValidationResult;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.SecurityWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 安全中心 Controller 单元测试。
 */
@DisplayName("SecurityController Test")
class SecurityControllerTest {

    private SecurityWebService securityWebService;
    private PluginWebAuthorizationService authorizationService;
    private SecurityController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        securityWebService = mock(SecurityWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<SecurityWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(securityWebService);
        controller = new SecurityController(provider, authorizationService);
    }

    @Test
    @DisplayName("按插件 ID 扫描返回验证结果")
    void scanByPluginIdShouldReturnResult() {
        PluginSecurityValidationResult result = new PluginSecurityValidationResult("demo-plugin");
        when(securityWebService.scanPluginById("demo-plugin")).thenReturn(result);

        ApiResult<PluginSecurityValidationResult> apiResult = controller.scanByPluginId("demo-plugin");

        assertThat(apiResult.isSuccess()).isTrue();
        assertThat(apiResult.getData()).isSameAs(result);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VERIFY, "demo-plugin");
        verify(securityWebService).scanPluginById("demo-plugin");
    }

    @Test
    @DisplayName("按路径扫描返回验证结果")
    void scanByPathShouldReturnResult() {
        PluginSecurityValidationResult result = new PluginSecurityValidationResult("path-plugin");
        when(securityWebService.scanPluginByPath("/tmp/plugin.jar")).thenReturn(result);

        ApiResult<PluginSecurityValidationResult> apiResult = controller.scanByPath("/tmp/plugin.jar");

        assertThat(apiResult.isSuccess()).isTrue();
        verify(securityWebService).scanPluginByPath("/tmp/plugin.jar");
    }

    @Test
    @DisplayName("获取插件安全策略")
    void getPolicyShouldReturnPolicy() {
        PluginSecurityPolicy policy = PluginSecurityPolicy.builder().allowFileSystemAccess(true).build();
        when(securityWebService.getPolicy("demo-plugin")).thenReturn(policy);

        ApiResult<PluginSecurityPolicy> apiResult = controller.getPolicy("demo-plugin");

        assertThat(apiResult.isSuccess()).isTrue();
        assertThat(apiResult.getData()).isSameAs(policy);
        verify(securityWebService).getPolicy("demo-plugin");
    }

    @Test
    @DisplayName("设置插件安全策略")
    void setPolicyShouldInvokeService() {
        SecurityController.PolicyRequest request = new SecurityController.PolicyRequest();
        request.setPluginId("demo-plugin");
        request.setAllowFileSystemAccess(true);
        request.setAllowNetworkAccess(false);
        request.setAllowReflectionAccess(true);

        ApiResult<Void> apiResult = controller.setPolicy(request);

        assertThat(apiResult.isSuccess()).isTrue();
        verify(securityWebService).setPolicy(eq("demo-plugin"), any(PluginSecurityPolicy.class));
    }

    @Test
    @DisplayName("授予权限返回成功")
    void grantPermissionShouldInvokeService() {
        SecurityController.PermissionRequest request = new SecurityController.PermissionRequest();
        request.setPluginId("demo-plugin");
        request.setType("file_system");
        request.setTarget("/tmp");
        request.setAction("read");

        ApiResult<Void> apiResult = controller.grantPermission(request);

        assertThat(apiResult.isSuccess()).isTrue();
        verify(securityWebService).grantPermission(eq("demo-plugin"), any(PluginPermission.class));
    }

    @Test
    @DisplayName("获取插件权限列表")
    void getPermissionsShouldReturnList() {
        PluginPermission permission = PluginPermission.fileSystem("/tmp", "read");
        Set<PluginPermission> permissions = Collections.singleton(permission);
        when(securityWebService.getPermissions("demo-plugin")).thenReturn(permissions);

        ApiResult<Set<PluginPermission>> apiResult = controller.getPermissions("demo-plugin");

        assertThat(apiResult.isSuccess()).isTrue();
        assertThat(apiResult.getData()).containsExactly(permission);
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void scanShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("permission denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VERIFY, "demo-plugin");

        assertThatThrownBy(() -> controller.scanByPluginId("demo-plugin"))
                .isInstanceOf(PluginWebAuthorizationException.class)
                .hasMessageContaining("permission denied");
        verifyNoInteractions(securityWebService);
    }

    @Test
    @DisplayName("服务未启用时抛出 IllegalStateException")
    @SuppressWarnings("unchecked")
    void scanShouldFailWhenServiceMissing() {
        ObjectProvider<SecurityWebService> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        SecurityController emptyController = new SecurityController(emptyProvider, authorizationService);

        assertThatThrownBy(() -> emptyController.scanByPluginId("demo-plugin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未启用");
    }
}
