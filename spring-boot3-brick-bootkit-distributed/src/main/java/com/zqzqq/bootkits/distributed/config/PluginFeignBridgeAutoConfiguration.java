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

package com.zqzqq.bootkits.distributed.config;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.feign.PluginFeignBridgePostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign → 插件服务桥接自动配置。
 * <p>
 * 仅当分布式模块启用、Feign 在 classpath 上、且存在全局 {@link PluginServiceRegistry} 时生效。
 * 把既是 {@code @FeignClient} 又被注册为插件能力的接口，透明改为走插件注册中心
 * （本地优先 / 远端 gRPC），调用方代码与接口零改动。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "plugin.distributed", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "feign.Feign")
@ConditionalOnBean(PluginServiceRegistry.class)
@ConditionalOnProperty(prefix = "plugin.distributed.feign-bridge", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PluginFeignBridgeAutoConfiguration {

    @Bean
    public PluginFeignBridgePostProcessor pluginFeignBridgePostProcessor(
            PluginServiceRegistry registry,
            DistributedPluginProperties properties) {
        return new PluginFeignBridgePostProcessor(registry, properties.isFeignBridgeEnabled());
    }
}
