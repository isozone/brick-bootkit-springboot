package com.example.capabilitydemo.service;

/**
 * 演示插件对外提供的服务接口。
 * <p>
 * 宿主或其他插件可通过服务注册中心发现并调用该服务。
 */
public interface GreetingService {

    /**
     * 返回问候语
     */
    String greet(String name);
}
