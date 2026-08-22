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


package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginInfoTest {

    @Test
    void shouldSnapshotRuntimeFieldsFromSourcePluginInfo() {
        InsidePluginDescriptor descriptor = mock(InsidePluginDescriptor.class);
        when(descriptor.getPluginId()).thenReturn("demo-plugin");
        when(descriptor.getPluginPath()).thenReturn("plugins/demo-plugin.jar");

        PluginInfo source = mock(PluginInfo.class);
        when(source.getPluginDescriptor()).thenReturn(descriptor);
        when(source.getPluginState()).thenReturn(EnhancedPluginState.STARTED);
        when(source.getStartTime()).thenReturn(123L);
        when(source.getStopTime()).thenReturn(456L);
        when(source.isFollowSystem()).thenReturn(true);

        ClassLoader classLoader = getClass().getClassLoader();
        when(source.getClassLoader()).thenReturn(classLoader);

        Map<String, Object> extensionInfo = new HashMap<>();
        extensionInfo.put("mode", "isolated");
        when(source.getExtensionInfo()).thenReturn(extensionInfo);

        DefaultPluginInfo snapshot = new DefaultPluginInfo(source);

        extensionInfo.put("mode", "mutated");

        assertThat(snapshot.getPluginId()).isEqualTo("demo-plugin");
        assertThat(snapshot.getPluginPath()).isEqualTo("plugins/demo-plugin.jar");
        assertThat(snapshot.getPluginState()).isEqualTo(EnhancedPluginState.STARTED);
        assertThat(snapshot.getStartTime()).isEqualTo(123L);
        assertThat(snapshot.getStopTime()).isEqualTo(456L);
        assertThat(snapshot.isFollowSystem()).isTrue();
        assertThat(snapshot.getClassLoader()).isSameAs(classLoader);
        assertThat(snapshot.getExtensionInfo()).containsEntry("mode", "isolated");
        assertThatThrownBy(() -> snapshot.getExtensionInfo().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
