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

import static org.assertj.core.api.Assertions.assertThat;
import com.zqzqq.bootkits.core.communication.CanaryRoutingResolver;

class WeightedServiceRoutingTest {

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
    void weightedPickSplitsTrafficByWeight() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        ServiceMetadata aMeta = ServiceMetadata.builder()
                .version("1.0").weight(90)
                .loadBalancing(ServiceMetadata.LoadBalancingStrategy.WEIGHTED)
                .build();
        ServiceMetadata bMeta = ServiceMetadata.builder()
                .version("2.0").weight(10)
                .loadBalancing(ServiceMetadata.LoadBalancingStrategy.WEIGHTED)
                .build();
        registry.registerService("plugin-a", SampleService.class, new ImplA(), aMeta);
        registry.registerService("plugin-b", SampleService.class, new ImplB(), bMeta);

        int a = 0;
        int b = 0;
        for (int i = 0; i < 20000; i++) {
            SampleService s = registry.getService(null, SampleService.class);
            if ("A".equals(s.name())) {
                a++;
            } else {
                b++;
            }
        }

        // 90/10 分流，留出误差容忍
        assertThat(a).isGreaterThan(b * 5);
        assertThat(b).isGreaterThan(0);
    }

    @Test
    void nonWeightedKeepsHighestPriority() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        ServiceMetadata aMeta = ServiceMetadata.builder().version("1.0").priority(10).build();
        ServiceMetadata bMeta = ServiceMetadata.builder().version("2.0").priority(1).build();
        registry.registerService("plugin-a", SampleService.class, new ImplA(), aMeta);
        registry.registerService("plugin-b", SampleService.class, new ImplB(), bMeta);

        SampleService s = registry.getService(null, SampleService.class);
        assertThat(s.name()).isEqualTo("A");
    }

    @Test
    void specificPluginBypassesWeightedSelection() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        ServiceMetadata aMeta = ServiceMetadata.builder()
                .version("1.0").weight(90)
                .loadBalancing(ServiceMetadata.LoadBalancingStrategy.WEIGHTED)
                .build();
        ServiceMetadata bMeta = ServiceMetadata.builder()
                .version("2.0").weight(10)
                .loadBalancing(ServiceMetadata.LoadBalancingStrategy.WEIGHTED)
                .build();
        registry.registerService("plugin-a", SampleService.class, new ImplA(), aMeta);
        registry.registerService("plugin-b", SampleService.class, new ImplB(), bMeta);

        SampleService s = registry.getService("plugin-b", SampleService.class);
        assertThat(s.name()).isEqualTo("B");
    }

    @Test
    void canaryModeEnablesAutomaticWeightedSplit() {
        DefaultPluginServiceRegistry registry = new DefaultPluginServiceRegistry();
        registry.setCanaryRoutingResolver(new CanaryRoutingResolver() {
            @Override
            public boolean isCanaryEnabled() {
                return true;
            }
        });
        // 两个版本均未显式声明 WEIGHTED，仅优先级相同
        ServiceMetadata aMeta = ServiceMetadata.builder().version("1.0").priority(1).build();
        ServiceMetadata bMeta = ServiceMetadata.builder().version("2.0").priority(1).build();
        registry.registerService("plugin-a", SampleService.class, new ImplA(), aMeta);
        registry.registerService("plugin-b", SampleService.class, new ImplB(), bMeta);

        int a = 0;
        int b = 0;
        for (int i = 0; i < 20000; i++) {
            SampleService s = registry.getService(null, SampleService.class);
            if ("A".equals(s.name())) {
                a++;
            } else {
                b++;
            }
        }
        // 灰度模式默认基线/金丝雀 90/10 分流
        assertThat(a).isGreaterThan(b * 5);
        assertThat(b).isGreaterThan(0);
    }
}
