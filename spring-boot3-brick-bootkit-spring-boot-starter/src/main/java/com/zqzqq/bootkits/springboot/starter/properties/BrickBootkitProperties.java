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


package com.zqzqq.bootkits.springboot.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brick BootKit 配置属性
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Data
@ConfigurationProperties(prefix = "brick-bootkit")
public class BrickBootkitProperties {

    /**
     * 是否启用插件框架
     */
    private boolean enabled = true;

    /**
     * 插件路径
     */
    private String pluginPath = "./plugins";

    /**
     * 是否自动发现插件
     */
    private boolean autoDiscover = true;

    /**
     * 是否启用 EventBus
     */
    private boolean enableEventBus = true;

    /**
     * EventBus 线程池大小
     */
    private int eventBusThreadPoolSize = 10;

    /**
     * 是否启用健康检查
     */
    private boolean enableHealthCheck = true;

    /**
     * 健康检查间隔（秒）
     */
    private long healthCheckIntervalSeconds = 60;

    /**
     * 是否启用自动恢复
     */
    private boolean enableAutoRecovery = true;

    /**
     * 插件最大重启次数
     */
    private int maxRestartCount = 3;
}
