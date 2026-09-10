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


package com.zqzqq.bootkits.core.device;

import com.zqzqq.bootkits.protocol.adapter.DeviceAdapter;
import com.zqzqq.bootkits.protocol.adapter.TransportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备注册表管理器：在 Spring 环境中自动扫描 {@link DeviceAdapter} beans 并注册。
 * <p>
 * Spring 自动配置通过本类收集所有 DeviceAdapter beans + 对应的 TransportContext，
 * 一次性注册到 {@link DeviceRegistry}。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class DeviceRegistryManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryManager.class);

    private final DeviceRegistry registry;

    public DeviceRegistryManager(DeviceRegistry registry) {
        this.registry = registry;
    }

    /**
     * 批量注册设备。
     *
     * @param adapters 适配器列表
     * @param context  传输上下文（所有适配器共享同一个上下文）
     */
    public void registerAll(List<DeviceAdapter> adapters, TransportContext context) {
        if (adapters == null || adapters.isEmpty()) {
            log.info("无设备适配器需要注册");
            return;
        }
        log.info("批量注册 {} 个设备适配器", adapters.size());
        for (DeviceAdapter adapter : adapters) {
            try {
                registry.register(adapter, context);
            } catch (Exception e) {
                log.error("设备适配器注册失败: type={}", adapter.getDeviceType(), e);
            }
        }
    }

    /**
     * 按适配器 map 注册（每个适配器可有独立的传输上下文）。
     *
     * @param adapterContextMap adapter → TransportContext
     */
    public void registerAll(Map<DeviceAdapter, TransportContext> adapterContextMap) {
        if (adapterContextMap == null || adapterContextMap.isEmpty()) {
            log.info("无设备适配器需要注册");
            return;
        }
        log.info("批量注册 {} 个设备适配器（各自独立上下文）", adapterContextMap.size());
        for (Map.Entry<DeviceAdapter, TransportContext> entry : adapterContextMap.entrySet()) {
            try {
                registry.register(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("设备适配器注册失败: type={}", entry.getKey().getDeviceType(), e);
            }
        }
    }

    /**
     * 获取底层注册表。
     */
    public DeviceRegistry getRegistry() {
        return registry;
    }

    /**
     * 关闭所有设备。
     */
    public void shutdown() {
        registry.shutdown();
    }
}