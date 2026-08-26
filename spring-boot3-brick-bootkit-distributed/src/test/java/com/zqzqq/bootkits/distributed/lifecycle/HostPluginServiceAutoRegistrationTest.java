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

package com.zqzqq.bootkits.distributed.lifecycle;

import com.zqzqq.bootkits.core.communication.DefaultPluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.annotation.PluginService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HostPluginServiceAutoRegistrationTest {

    public interface HostSvc {
        String hello(String name);
    }

    @PluginService(version = "2.0.0")
    public static class HostSvcImpl implements HostSvc {
        @Override
        public String hello(String name) {
            return "host:" + name;
        }
    }

    @Test
    void shouldRegisterAnnotatedHostBeansIntoRegistry() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeanDefinitionNames()).thenReturn(new String[]{"hostSvc"});
        when(ctx.getBean("hostSvc")).thenReturn(new HostSvcImpl());

        PluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        new HostPluginServiceAutoRegistration(ctx, registry, "host").run(null);

        // 已注册到指定 pluginId，且可被检索/调用
        assertThat(registry.getServicesByPlugin("host")).contains(HostSvc.class);
        assertThat(registry.getService("host", HostSvc.class).hello("a")).isEqualTo("host:a");
    }

    @Test
    void shouldIgnoreBeansWithoutServiceAnnotation() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeanDefinitionNames()).thenReturn(new String[]{"plain"});
        when(ctx.getBean("plain")).thenReturn(new Object());

        PluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        new HostPluginServiceAutoRegistration(ctx, registry, "host").run(null);

        assertThat(registry.getRegisteredPlugins()).doesNotContain("host");
    }
}
