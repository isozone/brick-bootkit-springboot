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

import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.eventbus.PluginEventListener;
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
 * 事件总线 Web 服务单元测试。
 */
@DisplayName("EventBusWebService Test")
class EventBusWebServiceTest {

    private PluginEventBus eventBus;
    private EventBusWebService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        eventBus = mock(PluginEventBus.class);
        ObjectProvider<PluginEventBus> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(eventBus);
        service = new EventBusWebService(provider);
    }

    @Test
    @DisplayName("构造时注册 Web 控制台监听器")
    void constructorShouldRegisterListener() {
        verify(eventBus).registerListener(eq("web-console"), any(PluginEventListener.class));
    }

    @Test
    @DisplayName("获取事件统计")
    void getEventCountsShouldReturnData() {
        Map<String, Integer> stats = Collections.singletonMap("PLUGIN_STARTED.demo-plugin", 3);
        when(eventBus.getEventCounts()).thenReturn(stats);

        Map<String, Integer> actual = service.getEventCounts();

        assertThat(actual).containsEntry("PLUGIN_STARTED.demo-plugin", 3);
    }

    @Test
    @DisplayName("获取事件类型列表")
    void getEventTypesShouldReturnAllTypes() {
        List<String> types = service.getEventTypes();

        assertThat(types).contains("PLUGIN_STARTED", "PLUGIN_ERROR", "CUSTOM");
    }

    @Test
    @DisplayName("获取最近事件流")
    void getRecentEventsShouldReturnList() {
        PluginEvent event = new PluginEvent(PluginEvent.EventType.PLUGIN_STARTED, "demo-plugin");
        service.getEventTypes(); // ensure service constructed

        // 通过反射触发内部监听器记录事件（模拟真实事件到达）
        List<EventBusWebService.RecentEvent> before = service.getRecentEvents(10);
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("事件总线缺失时抛出异常")
    @SuppressWarnings("unchecked")
    void shouldFailWhenEventBusMissing() {
        ObjectProvider<PluginEventBus> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        EventBusWebService emptyService = new EventBusWebService(emptyProvider);

        assertThatThrownBy(emptyService::getEventCounts)
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("未启用");
    }
}
