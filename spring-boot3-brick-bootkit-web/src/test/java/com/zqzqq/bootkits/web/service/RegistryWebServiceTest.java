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

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.RegistryStatistics;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 服务注册中心 Web 服务单元测试。
 */
@DisplayName("RegistryWebService Test")
class RegistryWebServiceTest {

    private PluginServiceRegistry registry;
    private RegistryWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        registry = mock(PluginServiceRegistry.class);
        ObjectProvider<PluginServiceRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        service = new RegistryWebService(provider);
    }

    @Test
    @DisplayName("获取注册中心统计")
    void getStatisticsShouldReturnData() {
        RegistryStatistics stats = new RegistryStatistics(3, 2, 3, 1);
        when(registry.getStatistics()).thenReturn(stats);

        RegistryStatistics actual = service.getStatistics();

        assertThat(actual.getTotalServices()).isEqualTo(3);
        assertThat(actual.getTotalPlugins()).isEqualTo(2);
    }

    @Test
    @DisplayName("按插件分组获取服务")
    void getServicesGroupedByPluginShouldReturnGroups() {
        when(registry.getRegisteredPlugins()).thenReturn(Collections.singleton("plugin-a"));
        Set<Class<?>> interfaces = Collections.<Class<?>>singleton(String.class);
        when(registry.getServicesByPlugin("plugin-a")).thenReturn(interfaces);
        ServiceDescriptor descriptor = mock(ServiceDescriptor.class);
        doReturn(String.class).when(descriptor).getServiceInterface();
        when(registry.getServiceDescriptor("plugin-a", String.class)).thenReturn(descriptor);

        List<RegistryWebService.PluginServiceGroup> groups = service.getServicesGroupedByPlugin();

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getPluginId()).isEqualTo("plugin-a");
        assertThat(groups.get(0).getServices()).hasSize(1);
    }

    @Test
    @DisplayName("获取指定插件服务")
    void getServicesByPluginShouldReturnList() {
        Set<Class<?>> interfaces = Collections.<Class<?>>singleton(String.class);
        when(registry.getServicesByPlugin("plugin-a")).thenReturn(interfaces);
        ServiceDescriptor descriptor = mock(ServiceDescriptor.class);
        doReturn(String.class).when(descriptor).getServiceInterface();
        when(registry.getServiceDescriptor("plugin-a", String.class)).thenReturn(descriptor);

        List<ServiceDescriptor> actual = service.getServicesByPlugin("plugin-a");

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isSameAs(descriptor);
    }

    @Test
    @DisplayName("插件 ID 为空时抛出异常")
    void getServicesByPluginShouldFailOnEmptyId() {
        assertThatThrownBy(() -> service.getServicesByPlugin("  "))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("获取已注册插件 ID")
    void getRegisteredPluginsShouldReturnSet() {
        when(registry.getRegisteredPlugins()).thenReturn(Collections.singleton("plugin-a"));

        Set<String> actual = service.getRegisteredPlugins();

        assertThat(actual).containsExactly("plugin-a");
    }

    @Test
    @DisplayName("注册中心缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenRegistryMissing() {
        ObjectProvider<PluginServiceRegistry> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        RegistryWebService emptyService = new RegistryWebService(emptyProvider);

        assertThatThrownBy(emptyService::getStatistics)
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
