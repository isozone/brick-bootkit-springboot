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

package com.zqzqq.bootkits.sdk;

import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.eventbus.PluginEventListener;
import com.zqzqq.bootkits.sdk.annotation.BrickEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBusAutoConfigureTest {

    @Mock
    private PluginEventBus eventBus;

    public static class SamplePlugin {
        public boolean invoked = false;

        @BrickEventListener(PluginEvent.EventType.PLUGIN_STARTED)
        public void onStarted() {
            invoked = true;
        }
    }

    @Test
    void autoConfigureRegistersAnnotatedMethod() {
        SamplePlugin plugin = new SamplePlugin();
        EventBusAutoConfigure.autoConfigure("p1", plugin, eventBus);
        verify(eventBus, times(1)).registerListener(eq("p1"), any(PluginEventListener.class));
    }

    @Test
    void autoConfigureNullSafe() {
        assertDoesNotThrow(() -> EventBusAutoConfigure.autoConfigure("p", null, eventBus));
        assertDoesNotThrow(() -> EventBusAutoConfigure.autoConfigure("p", new SamplePlugin(), null));
    }

    @Test
    void proxyInvokesAnnotatedMethodOnMatchingEvent() {
        SamplePlugin plugin = new SamplePlugin();
        EventBusAutoConfigure.autoConfigure("p1", plugin, eventBus);

        ArgumentCaptor<PluginEventListener> captor = ArgumentCaptor.forClass(PluginEventListener.class);
        verify(eventBus).registerListener(anyString(), captor.capture());

        PluginEventListener listener = captor.getValue();
        PluginEvent event = new PluginEvent(PluginEvent.EventType.PLUGIN_STARTED, "p1");
        listener.onEvent(event);

        assertTrue(plugin.invoked);
        assertTrue(event.isHandled());
    }
}
