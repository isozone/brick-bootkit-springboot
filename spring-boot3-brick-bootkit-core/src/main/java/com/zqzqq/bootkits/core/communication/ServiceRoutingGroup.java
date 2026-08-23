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
 * 多版本服务路由分组（用于金丝雀权重配置可视化）。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public class ServiceRoutingGroup {

    /** 接口简单名 */
    private String interfaceName;

    /** 接口全限定名（用于回写权重时定位 Class） */
    private String interfaceClass;

    /** 该接口下的多版本提供方 */
    private List<ServiceRoutingProvider> providers;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public List<ServiceRoutingProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<ServiceRoutingProvider> providers) {
        this.providers = providers;
    }
}
