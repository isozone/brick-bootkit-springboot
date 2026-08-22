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

import com.zqzqq.bootkits.core.dependency.PluginCompatibilityResult;
import com.zqzqq.bootkits.core.dependency.PluginDependencyResolution;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.DependencyWebService;
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
 * 依赖分析 Controller 单元测试。
 */
@DisplayName("DependencyController Test")
class DependencyControllerTest {

    private DependencyWebService dependencyWebService;
    private PluginWebAuthorizationService authorizationService;
    private DependencyController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dependencyWebService = mock(DependencyWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<DependencyWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(dependencyWebService);
        controller = new DependencyController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取插件依赖图")
    void graphShouldReturnData() {
        DependencyWebService.DependencyGraph graph = new DependencyWebService.DependencyGraph();
        graph.addNode("plugin-a", "Plugin A");
        graph.addNode("plugin-b", "Plugin B");
        graph.addEdge("plugin-a", "plugin-b", false);
        when(dependencyWebService.getDependencyGraph()).thenReturn(graph);

        ApiResult<DependencyWebService.DependencyGraph> result = controller.graph();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getNodes()).hasSize(2);
        assertThat(result.getData().getEdges()).hasSize(1);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("获取插件依赖详情")
    void detailShouldReturnData() {
        Map<String, Object> detail = Collections.singletonMap("pluginId", "plugin-a");
        when(dependencyWebService.getPluginDependencyDetail("plugin-a")).thenReturn(detail);

        ApiResult<Map<String, Object>> result = controller.detail("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("pluginId")).isEqualTo("plugin-a");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, "plugin-a");
    }

    @Test
    @DisplayName("获取插件依赖解析结果")
    void resolveShouldReturnData() {
        PluginDependencyResolution resolution = PluginDependencyResolution.success(Collections.emptyList());
        when(dependencyWebService.resolveDependencies("plugin-a")).thenReturn(resolution);

        ApiResult<PluginDependencyResolution> result = controller.resolve("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("检查插件兼容性")
    void compatibilityShouldReturnData() {
        PluginCompatibilityResult compatibility = PluginCompatibilityResult.compatible();
        when(dependencyWebService.checkCompatibility("plugin-a")).thenReturn(compatibility);

        ApiResult<PluginCompatibilityResult> result = controller.compatibility("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isCompatible()).isTrue();
    }

    @Test
    @DisplayName("升级影响面分析")
    void impactShouldReturnList() {
        when(dependencyWebService.getReverseDependencies("plugin-a"))
                .thenReturn(Collections.singletonList("plugin-b"));

        ApiResult<List<String>> result = controller.impact("plugin-a");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("plugin-b");
    }

    @Test
    @DisplayName("获取版本兼容性矩阵")
    void matrixShouldReturnList() {
        when(dependencyWebService.getVersionMatrix()).thenReturn(Collections.emptyList());

        ApiResult<List<DependencyWebService.PluginVersionRow>> result = controller.matrix();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void graphShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.graph())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(dependencyWebService);
    }
}
