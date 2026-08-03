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
