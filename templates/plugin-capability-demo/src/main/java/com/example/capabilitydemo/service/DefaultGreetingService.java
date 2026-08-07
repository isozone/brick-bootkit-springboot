package com.example.capabilitydemo.service;

import com.zqzqq.bootkits.sdk.annotation.BrickService;

/**
 * 问候服务实现。
 * <p>
 * {@code @BrickService} 将该实现注册到插件服务注册中心（PluginServiceRegistry），
 * 宿主或其他插件可通过 GreetingService 接口跨插件调用。
 */
@BrickService(value = GreetingService.class, version = "1.0.0", description = "演示问候服务")
public class DefaultGreetingService implements GreetingService {

    @Override
    public String greet(String name) {
        return "Hello, " + (name == null || name.isEmpty() ? "world" : name)
                + " from capability-demo-plugin";
    }
}
