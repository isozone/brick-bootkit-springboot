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

/**
 * 路由分组中的单个服务提供方信息。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public class ServiceRoutingProvider {

    /** 提供方插件 ID */
    private String pluginId;

    /** 当前分流权重 */
    private int weight;

    /** 负载均衡策略（ROUND_ROBIN / WEIGHTED ...） */
    private String loadBalancing;

    /** 优先级 */
    private int priority;

    /** 是否处于金丝雀可分流状态（WEIGHTED 或全局灰度模式） */
    private boolean canaryEligible;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getLoadBalancing() {
        return loadBalancing;
    }

    public void setLoadBalancing(String loadBalancing) {
        this.loadBalancing = loadBalancing;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isCanaryEligible() {
        return canaryEligible;
    }

    public void setCanaryEligible(boolean canaryEligible) {
        this.canaryEligible = canaryEligible;
    }
}
