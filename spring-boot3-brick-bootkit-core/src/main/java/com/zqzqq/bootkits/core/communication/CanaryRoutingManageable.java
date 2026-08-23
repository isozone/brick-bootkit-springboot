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

import java.util.List;

/**
 * 金丝雀路由可管理扩展：允许在运行时查看并调整多版本服务的分流权重。
 * 与 {@link PluginServiceRegistry} 解耦，避免强制要求所有注册中心实现类（如分布式注册中心）适配。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public interface CanaryRoutingManageable {

    /**
     * 更新指定服务实现的路由权重（权重 > 0 时同时将其负载策略置为 WEIGHTED 以生效）
     */
    void setServiceWeight(String pluginId, Class<?> serviceInterface, int weight);

    /**
     * 描述当前具备金丝雀分流条件的多版本服务分组
     */
    List<ServiceRoutingGroup> describeRouting();
}
