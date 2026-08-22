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
import com.zqzqq.bootkits.core.dependency.PluginCompatibilityResult;
import com.zqzqq.bootkits.core.dependency.PluginDependencyManager;
import com.zqzqq.bootkits.core.dependency.PluginDependencyResolution;
import com.zqzqq.bootkits.core.descriptor.DefaultDependencyPlugin;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
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
 * 依赖分析 Web 服务单元测试。
 */
@DisplayName("DependencyWebService Test")
class DependencyWebServiceTest {

    private PluginDependencyManager dependencyManager;
    private PluginManager pluginManager;
    private DependencyWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dependencyManager = mock(PluginDependencyManager.class);
        pluginManager = mock(PluginManager.class);

        ObjectProvider<PluginDependencyManager> dmProvider = mock(ObjectProvider.class);
        when(dmProvider.getIfAvailable()).thenReturn(dependencyManager);
        ObjectProvider<PluginManager> pmProvider = mock(ObjectProvider.class);
        when(pmProvider.getIfAvailable()).thenReturn(pluginManager);

        service = new DependencyWebService(dmProvider, pmProvider);
    }

    @Test
    @DisplayName("获取依赖图")
    void getDependencyGraphShouldReturnNodes() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        InsidePluginDescriptor descriptor = mock(InsidePluginDescriptor.class);
        when(pluginInfo.getPluginId()).thenReturn("plugin-a");
        when(pluginInfo.getPluginDescriptor()).thenReturn(descriptor);
        when(descriptor.getName()).thenReturn("Plugin A");
        when(descriptor.getDependencyPlugin()).thenReturn(Collections.emptyList());
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginInfo));

        DependencyWebService.DependencyGraph graph = service.getDependencyGraph();

        assertThat(graph.getNodes()).hasSize(1);
        assertThat(graph.getNodes().get(0).getId()).isEqualTo("plugin-a");
        assertThat(graph.getEdges()).isEmpty();
    }

    @Test
    @DisplayName("依赖图包含依赖边")
    void getDependencyGraphShouldIncludeEdges() {
        PluginInfo pluginA = mock(PluginInfo.class);
        InsidePluginDescriptor descriptorA = mock(InsidePluginDescriptor.class);
        when(pluginA.getPluginId()).thenReturn("plugin-a");
        when(pluginA.getPluginDescriptor()).thenReturn(descriptorA);
        when(descriptorA.getName()).thenReturn("Plugin A");

        DefaultDependencyPlugin dep = new DefaultDependencyPlugin();
        dep.setId("plugin-b");
        dep.setVersion("1.0.0");
        dep.setOptional(false);
        when(descriptorA.getDependencyPlugin()).thenReturn(Collections.singletonList(dep));

        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginA));

        DependencyWebService.DependencyGraph graph = service.getDependencyGraph();

        assertThat(graph.getEdges()).hasSize(1);
        assertThat(graph.getEdges().get(0).getFrom()).isEqualTo("plugin-a");
        assertThat(graph.getEdges().get(0).getTo()).isEqualTo("plugin-b");
    }

    @Test
    @DisplayName("获取插件依赖详情")
    void getPluginDependencyDetailShouldReturnData() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        InsidePluginDescriptor descriptor = mock(InsidePluginDescriptor.class);
        when(pluginInfo.getPluginId()).thenReturn("plugin-a");
        when(pluginInfo.getPluginDescriptor()).thenReturn(descriptor);
        when(descriptor.getName()).thenReturn("Plugin A");
        when(descriptor.getPluginVersion()).thenReturn("1.0.0");
        when(descriptor.getDependencyPlugin()).thenReturn(Collections.emptyList());
        when(pluginManager.getPlugin("plugin-a")).thenReturn(pluginInfo);

        Map<String, Object> detail = service.getPluginDependencyDetail("plugin-a");

        assertThat(detail.get("pluginId")).isEqualTo("plugin-a");
        assertThat(detail.get("version")).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("插件不存在时抛出异常")
    void getPluginDependencyDetailShouldFailWhenMissing() {
        when(pluginManager.getPlugin("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.getPluginDependencyDetail("missing"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("获取依赖解析结果")
    void resolveDependenciesShouldReturnData() {
        PluginDependencyResolution resolution = PluginDependencyResolution.success(Collections.emptyList());
        when(dependencyManager.resolveDependencies("plugin-a")).thenReturn(resolution);

        PluginDependencyResolution actual = service.resolveDependencies("plugin-a");

        assertThat(actual.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("检查插件兼容性")
    void checkCompatibilityShouldReturnResult() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginInfo.getPluginId()).thenReturn("plugin-a");
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginInfo));

        PluginCompatibilityResult result = PluginCompatibilityResult.compatible();
        when(dependencyManager.checkCompatibility(eq("plugin-a"), any())).thenReturn(result);

        PluginCompatibilityResult actual = service.checkCompatibility("plugin-a");

        assertThat(actual.isCompatible()).isTrue();
    }

    @Test
    @DisplayName("获取反向依赖")
    void getReverseDependenciesShouldReturnDependents() {
        PluginInfo pluginA = mock(PluginInfo.class);
        InsidePluginDescriptor descriptorA = mock(InsidePluginDescriptor.class);
        when(pluginA.getPluginId()).thenReturn("plugin-a");
        when(pluginA.getPluginDescriptor()).thenReturn(descriptorA);

        DefaultDependencyPlugin dep = new DefaultDependencyPlugin();
        dep.setId("plugin-b");
        when(descriptorA.getDependencyPlugin()).thenReturn(Collections.singletonList(dep));

        PluginInfo pluginB = mock(PluginInfo.class);
        InsidePluginDescriptor descriptorB = mock(InsidePluginDescriptor.class);
        when(pluginB.getPluginId()).thenReturn("plugin-b");
        when(pluginB.getPluginDescriptor()).thenReturn(descriptorB);
        when(descriptorB.getDependencyPlugin()).thenReturn(Collections.emptyList());

        when(pluginManager.getPlugins()).thenReturn(java.util.Arrays.asList(pluginA, pluginB));

        List<String> dependents = service.getReverseDependencies("plugin-b");

        assertThat(dependents).containsExactly("plugin-a");
    }

    @Test
    @DisplayName("获取版本矩阵")
    void getVersionMatrixShouldReturnRows() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        InsidePluginDescriptor descriptor = mock(InsidePluginDescriptor.class);
        when(pluginInfo.getPluginId()).thenReturn("plugin-a");
        when(pluginInfo.getPluginDescriptor()).thenReturn(descriptor);
        when(descriptor.getName()).thenReturn("Plugin A");
        when(descriptor.getPluginVersion()).thenReturn("1.0.0");
        when(descriptor.getRequires()).thenReturn("1.0.0");
        when(descriptor.getDependencyPlugin()).thenReturn(Collections.emptyList());
        when(pluginInfo.getPluginState()).thenReturn(null);
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginInfo));

        List<DependencyWebService.PluginVersionRow> rows = service.getVersionMatrix();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPluginId()).isEqualTo("plugin-a");
        assertThat(rows.get(0).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("依赖管理器缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenManagerMissing() {
        ObjectProvider<PluginDependencyManager> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        DependencyWebService emptyService = new DependencyWebService(emptyProvider,
                mock(ObjectProvider.class));

        assertThatThrownBy(() -> emptyService.resolveDependencies("plugin-a"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
