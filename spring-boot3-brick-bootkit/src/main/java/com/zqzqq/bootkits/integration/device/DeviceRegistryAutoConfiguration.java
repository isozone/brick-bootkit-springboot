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


package com.zqzqq.bootkits.integration.device;

import com.zqzqq.bootkits.core.device.DeviceRegistry;
import com.zqzqq.bootkits.core.device.DeviceRegistryManager;
import com.zqzqq.bootkits.core.eventbus.EnvelopeBusBridge;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 设备注册表 + EventBus 桥接自动配置。
 * <p>
 * 当 classpath 上存在 {@link DeviceRegistry} 和 {@link EnvelopeBus} 时自动装配。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
@Configuration
@ConditionalOnClass({DeviceRegistry.class, EnvelopeBus.class})
public class DeviceRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EnvelopeBus envelopeBus() {
        return new EnvelopeBus();
    }

    @Bean
    @ConditionalOnMissingBean
    public DeviceRegistry deviceRegistry(EnvelopeBus envelopeBus) {
        return new DeviceRegistry(envelopeBus);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeviceRegistryManager deviceRegistryManager(DeviceRegistry deviceRegistry) {
        return new DeviceRegistryManager(deviceRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public EnvelopeBusBridge envelopeBusBridge(EnvelopeBus envelopeBus, PluginEventBus pluginEventBus) {
        EnvelopeBusBridge bridge = new EnvelopeBusBridge(envelopeBus, pluginEventBus);
        bridge.start();
        return bridge;
    }
}