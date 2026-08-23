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
 * 金丝雀/灰度路由策略解析器。
 * <p>
 * 由上层（集成模块）根据当前发布模式实现，使服务注册中心在灰度模式下
 * 自动按权重分流同一接口的多版本实现，无需插件显式声明 {@code WEIGHTED} 策略。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public interface CanaryRoutingResolver {

    /**
     * 当前是否处于金丝雀/灰度路由模式（多版本服务按权重分流）
     */
    boolean isCanaryEnabled();

    /**
     * 解析某服务实现在金丝雀分流中的权重；
     * 返回 &lt;=0 表示使用默认策略（由注册中心按注册顺序分配基线/金丝雀权重）。
     */
    default int resolveWeight(ServiceDescriptor descriptor) {
        return -1;
    }

    /**
     * 基线（先注册）版本权重，默认 90
     */
    default int baselineWeight() {
        return 90;
    }

    /**
     * 金丝雀（后注册）版本权重，默认 10
     */
    default int canaryWeight() {
        return 10;
    }
}
