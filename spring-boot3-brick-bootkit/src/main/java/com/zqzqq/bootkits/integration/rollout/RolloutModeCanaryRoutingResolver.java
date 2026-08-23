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

package com.zqzqq.bootkits.integration.rollout;

import com.zqzqq.bootkits.core.communication.CanaryRoutingResolver;
import com.zqzqq.bootkits.core.communication.ServiceDescriptor;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;

/**
 * 基于集成配置中发布模式（plugin.rolloutMode）的金丝雀路由解析器。
 * 当模式为 GRAY 时启用按权重分流，使同一接口的多版本实现自动进行灰度流量分配。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public class RolloutModeCanaryRoutingResolver implements CanaryRoutingResolver {

    private final IntegrationConfiguration configuration;

    public RolloutModeCanaryRoutingResolver(IntegrationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isCanaryEnabled() {
        if (configuration == null) {
            return false;
        }
        PluginRolloutMode mode = configuration.pluginRolloutMode();
        return mode == PluginRolloutMode.GRAY;
    }
}
