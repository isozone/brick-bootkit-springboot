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


package com.zqzqq.bootkits.distributed.registry;

import java.util.List;
import java.util.Set;

/**
 * 分布式服务目录抽象。
 * <p>
 * 负责维护「服务接口 ↔ 执行节点」的映射，供执行节点注册、宿主发现。
 */
public interface ServiceDirectory {

    /**
     * 注册一个远端服务。
     */
    void register(RemoteServiceRegistration registration);

    /**
     * 批量注册远端服务。
     */
    void registerAll(List<RemoteServiceRegistration> registrations);

    /**
     * 心跳续期，刷新对应服务记录的 TTL。
     */
    void heartbeat(String serviceInterface, String pluginId, String nodeId);

    /**
     * 查询某服务接口的全部可用远端节点。
     */
    List<RemoteServiceRegistration> lookup(String serviceInterface);

    /**
     * 查询某服务接口 + 指定插件提供的远端节点。
     */
    RemoteServiceRegistration lookup(String serviceInterface, String pluginId);

    /**
     * 注销单个远端服务。
     */
    void unregister(String serviceInterface, String pluginId, String nodeId);

    /**
     * 按节点注销该节点下的全部远端服务（节点下线时调用）。
     */
    void unregisterAllByNode(String nodeId);

    /**
     * 获取目录中全部服务接口名。
     */
    Set<String> allServiceInterfaces();
}