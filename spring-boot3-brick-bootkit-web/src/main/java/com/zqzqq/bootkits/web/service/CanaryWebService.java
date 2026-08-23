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

package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.communication.CanaryRoutingManageable;
import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.core.communication.ServiceRoutingGroup;
import com.zqzqq.bootkits.core.exception.PluginException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 金丝雀路由配置 Web 服务：读取多版本服务权重分组，并支持运行时调整分流权重。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Service
public class CanaryWebService {

    private final ObjectProvider<PluginServiceRegistry> registryProvider;

    public CanaryWebService(ObjectProvider<PluginServiceRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    public List<ServiceRoutingGroup> describeRouting() {
        PluginServiceRegistry registry = registryProvider.getIfAvailable();
        if (registry instanceof CanaryRoutingManageable) {
            return ((CanaryRoutingManageable) registry).describeRouting();
        }
        return Collections.emptyList();
    }

    public void updateWeight(String pluginId, String interfaceName, int weight) {
        PluginServiceRegistry registry = registryProvider.getIfAvailable();
        if (!(registry instanceof CanaryRoutingManageable)) {
            throw new PluginException("当前服务注册中心不支持金丝雀权重配置");
        }
        Class<?> serviceInterface;
        try {
            serviceInterface = Class.forName(interfaceName, false, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            throw new PluginException("无法解析服务接口: " + interfaceName);
        }
        ((CanaryRoutingManageable) registry).setServiceWeight(pluginId, serviceInterface, weight);
    }
}
