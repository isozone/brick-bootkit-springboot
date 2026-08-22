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

package com.zqzqq.bootkits.springboot.starter.actuator;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluginHealthIndicatorTest {

    @Mock
    private PluginManager pluginManager;

    @Test
    void healthUpWhenStarted() {
        PluginInfo info = mock(PluginInfo.class);
        when(info.getPluginId()).thenReturn("a");
        when(pluginManager.getPlugins()).thenReturn(List.of(info));
        when(pluginManager.getStartedPlugins()).thenReturn(List.of(info));

        PluginHealthIndicator indicator = new PluginHealthIndicator(pluginManager);
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void healthDownWhenNotStarted() {
        PluginInfo info = mock(PluginInfo.class);
        when(info.getPluginId()).thenReturn("a");
        when(pluginManager.getPlugins()).thenReturn(List.of(info));
        when(pluginManager.getStartedPlugins()).thenReturn(Collections.emptyList());

        PluginHealthIndicator indicator = new PluginHealthIndicator(pluginManager);
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void healthUpWithNullManager() {
        PluginHealthIndicator indicator = new PluginHealthIndicator(null);
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
    }
}
