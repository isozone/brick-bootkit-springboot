package com.example.capabilitydemo.controller;

import com.example.capabilitydemo.service.GreetingService;
import com.zqzqq.bootkits.core.config.PluginConfiguration;
import com.zqzqq.bootkits.core.config.PluginConfigurationManager;
import com.zqzqq.bootkits.sdk.annotation.BrickServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 新能力演示控制器。
 * <ul>
 *   <li>服务注册中心：{@code @BrickServiceReference} 自动注入跨插件服务</li>
 *   <li>配置热更新：通过 PluginConfigurationManager 读取/更新插件配置</li>
 * </ul>
 */
@RestController
@RequestMapping("/capability-demo")
public class CapabilityDemoController {

    @BrickServiceReference(value = GreetingService.class, version = "1.0.0", optional = true)
    private GreetingService greetingService;

    @Autowired(required = false)
    private PluginConfigurationManager configurationManager;

    /**
     * 调用本插件自身注册的服务（服务注册中心）
     */
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "world") String name) {
        if (greetingService != null) {
            return greetingService.greet(name);
        }
        return "GreetingService 未注入（服务注册中心未启用或服务未注册）";
    }

    /**
     * 读取插件配置（配置热更新）
     */
    @GetMapping("/config")
    public Object getConfig(@RequestParam(defaultValue = "message") String key) {
        if (configurationManager == null) {
            return "PluginConfigurationManager 不可用（plugin.configuration.enabled=false）";
        }
        PluginConfiguration configuration = configurationManager.getConfiguration("capability-demo-plugin");
        if (configuration == null) {
            return "当前插件暂无配置";
        }
        return configuration.getProperty(key, Object.class);
    }

    /**
     * 更新插件配置（配置热更新 + 版本管理）
     */
    @GetMapping("/config/set")
    public Object setConfig(@RequestParam String key,
                            @RequestParam(defaultValue = "hello-from-config") String value) {
        if (configurationManager == null) {
            return "PluginConfigurationManager 不可用（plugin.configuration.enabled=false）";
        }
        PluginConfiguration configuration = configurationManager.getConfiguration("capability-demo-plugin");
        if (configuration == null) {
            configuration = new PluginConfiguration("capability-demo-plugin", "1.0.0");
        }
        configuration.setProperty(key, value);
        configurationManager.updateConfiguration("capability-demo-plugin", configuration, "capability-demo 控制台热更新");
        return "配置已更新: " + key + " = " + value;
    }
}
