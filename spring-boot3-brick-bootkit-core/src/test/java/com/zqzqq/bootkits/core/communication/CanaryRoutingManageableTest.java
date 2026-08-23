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

package com.zqzqq.bootkits.core.communication;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link DefaultPluginServiceRegistry} 作为 {@link CanaryRoutingManageable}
 * 的路由权重查看与运行时调整能力。
 */
class CanaryRoutingManageableTest {

    interface SampleService {
        String name();
    }

    static class ImplA implements SampleService {
        @Override
        public String name() {
            return "A";
        }
    }

    static class ImplB implements SampleService {
        @Override
        public String name() {
            return "B";
        }
    }

    @Test
    void describeRoutingGroupsOnlyMultiProviderInterfaces() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        registry.registerService("plugin-a", SampleService.class, new ImplA(),
                ServiceMetadata.builder().version("1.0").priority(1).build());
        registry.registerService("plugin-b", SampleService.class, new ImplB(),
                ServiceMetadata.builder().version("2.0").priority(1).build());
        // 单实现接口不应出现在分组中
        registry.registerService("plugin-c", Runnable.class, (Runnable) () -> {},
                ServiceMetadata.builder().version("1.0").build());

        List<ServiceRoutingGroup> groups = registry.describeRouting();

        assertThat(groups).hasSize(1);
        ServiceRoutingGroup group = groups.get(0);
        assertThat(group.getInterfaceClass()).isEqualTo(SampleService.class.getName());
        assertThat(group.getProviders()).hasSize(2);
        assertThat(group.getProviders()).allMatch(p -> !p.isCanaryEligible());
    }

    @Test
    void setServiceWeightUpdatesWeightAndEnablesWeighted() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        registry.registerService("plugin-a", SampleService.class, new ImplA(),
                ServiceMetadata.builder().version("1.0").priority(1).build());
        registry.registerService("plugin-b", SampleService.class, new ImplB(),
                ServiceMetadata.builder().version("2.0").priority(1).build());

        registry.setServiceWeight("plugin-b", SampleService.class, 10);

        List<ServiceRoutingGroup> groups = registry.describeRouting();
        ServiceRoutingProvider b = groups.get(0).getProviders().stream()
                .filter(p -> "plugin-b".equals(p.getPluginId()))
                .findFirst().orElseThrow();
        assertThat(b.getWeight()).isEqualTo(10);
        assertThat(b.getLoadBalancing()).isEqualTo(ServiceMetadata.LoadBalancingStrategy.WEIGHTED.name());
        assertThat(b.isCanaryEligible()).isTrue();
    }

    @Test
    void canaryEnabledMarksProvidersEligible() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        registry.setCanaryRoutingResolver(() -> true);
        registry.registerService("plugin-a", SampleService.class, new ImplA(),
                ServiceMetadata.builder().version("1.0").priority(1).build());
        registry.registerService("plugin-b", SampleService.class, new ImplB(),
                ServiceMetadata.builder().version("2.0").priority(1).build());

        List<ServiceRoutingGroup> groups = registry.describeRouting();
        assertThat(groups.get(0).getProviders()).allMatch(ServiceRoutingProvider::isCanaryEligible);
    }
}
