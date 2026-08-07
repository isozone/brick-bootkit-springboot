package com.example.capabilitydemo;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 新能力演示插件引导类。
 * <p>
 * 演示串联以下能力：
 * <ul>
 *   <li>服务注册中心：{@code @BrickService} 注册 / {@code @BrickServiceReference} 引用</li>
 *   <li>事件总线：{@code @BrickEventListener} 监听插件生命周期事件</li>
 *   <li>安全中心：安装时自动执行代码扫描（危险模式如 Runtime.exec 会被标记）</li>
 *   <li>配置热更新：{@link com.zqzqq.bootkits.core.config.PluginConfigurationManager} 管理插件配置</li>
 * </ul>
 */
@SpringBootApplication
public class CapabilityDemoPluginBootstrap extends SpringPluginBootstrap {

    public static void main(String[] args) {
        new CapabilityDemoPluginBootstrap().run(args);
    }
}
