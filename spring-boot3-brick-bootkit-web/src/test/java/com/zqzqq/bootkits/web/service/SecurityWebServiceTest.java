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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.sandbox.PluginSandbox;
import com.zqzqq.bootkits.core.security.PluginPermission;
import com.zqzqq.bootkits.core.security.PluginSecurityManager;
import com.zqzqq.bootkits.core.security.PluginSecurityPolicy;
import com.zqzqq.bootkits.core.security.PluginSecurityValidationResult;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 安全中心 Web 服务单元测试。
 */
@DisplayName("SecurityWebService Test")
class SecurityWebServiceTest {

    @TempDir
    Path tempDir;

    private PluginSecurityManager securityManager;
    private PluginSandbox sandbox;
    private PluginManager pluginManager;
    private BrickWebProperties properties;
    private SecurityWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        securityManager = mock(PluginSecurityManager.class);
        sandbox = mock(PluginSandbox.class);
        pluginManager = mock(PluginManager.class);
        properties = new BrickWebProperties();
        properties.setUploadTempPath(tempDir.toString());

        ObjectProvider<PluginSecurityManager> smProvider = mock(ObjectProvider.class);
        when(smProvider.getIfAvailable()).thenReturn(securityManager);
        ObjectProvider<PluginSandbox> sbProvider = mock(ObjectProvider.class);
        when(sbProvider.getIfAvailable()).thenReturn(sandbox);
        ObjectProvider<PluginManager> pmProvider = mock(ObjectProvider.class);
        when(pmProvider.getIfAvailable()).thenReturn(pluginManager);

        service = new SecurityWebService(smProvider, sbProvider, pmProvider, properties);
    }

    @Test
    @DisplayName("按插件 ID 扫描：从 PluginManager 解析路径并调用安全管理器")
    void scanPluginByIdShouldResolvePath() throws Exception {
        Path pluginFile = tempDir.resolve("plugin.jar");
        Files.write(pluginFile, new byte[]{1, 2, 3});

        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginInfo.getPluginPath()).thenReturn(pluginFile.toString());
        when(pluginManager.getPlugin("demo-plugin")).thenReturn(pluginInfo);

        PluginSecurityValidationResult result = new PluginSecurityValidationResult("demo-plugin");
        when(securityManager.validatePluginSecurity("demo-plugin", pluginFile))
                .thenReturn(result);

        PluginSecurityValidationResult actual = service.scanPluginById("demo-plugin");

        assertThat(actual).isSameAs(result);
        verify(securityManager).validatePluginSecurity(eq("demo-plugin"), any());
    }

    @Test
    @DisplayName("按插件 ID 扫描：插件不存在时抛出异常")
    void scanPluginByIdShouldFailWhenPluginMissing() {
        when(pluginManager.getPlugin("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.scanPluginById("missing"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("插件不存在");
    }

    @Test
    @DisplayName("获取安全策略")
    void getPolicyShouldReturnPolicy() {
        PluginSecurityPolicy policy = PluginSecurityPolicy.builder().allowFileSystemAccess(true).build();
        when(securityManager.getSecurityPolicy("demo-plugin")).thenReturn(policy);

        PluginSecurityPolicy actual = service.getPolicy("demo-plugin");

        assertThat(actual.isAllowFileSystemAccess()).isTrue();
    }

    @Test
    @DisplayName("设置安全策略并同步沙箱")
    void setPolicyShouldSyncSandbox() {
        PluginSecurityPolicy policy = PluginSecurityPolicy.builder()
                .allowFileSystemAccess(true)
                .allowNetworkAccess(false)
                .build();

        service.setPolicy("demo-plugin", policy);

        verify(securityManager).setSecurityPolicy("demo-plugin", policy);
        verify(sandbox).createPolicy("demo-plugin", true, false);
    }

    @Test
    @DisplayName("授予权限")
    void grantPermissionShouldInvokeManager() {
        PluginPermission permission = PluginPermission.fileSystem("/tmp", "read");

        service.grantPermission("demo-plugin", permission);

        verify(securityManager).grantPermission("demo-plugin", permission);
    }

    @Test
    @DisplayName("获取插件权限")
    void getPermissionsShouldReturnSet() {
        Set<PluginPermission> permissions =
                Collections.singleton(PluginPermission.fileSystem("/tmp", "read"));
        when(securityManager.getPluginPermissions("demo-plugin")).thenReturn(permissions);

        Set<PluginPermission> actual = service.getPermissions("demo-plugin");

        assertThat(actual).hasSize(1);
    }

    @Test
    @DisplayName("安全管理器缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenSecurityManagerMissing() {
        ObjectProvider<PluginSecurityManager> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        BrickWebProperties props = new BrickWebProperties();
        SecurityWebService emptyService = new SecurityWebService(emptyProvider,
                mock(ObjectProvider.class), mock(ObjectProvider.class), props);

        assertThatThrownBy(() -> emptyService.getPolicy("demo-plugin"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
