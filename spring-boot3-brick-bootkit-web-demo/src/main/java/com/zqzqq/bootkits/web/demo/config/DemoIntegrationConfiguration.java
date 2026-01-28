package com.zqzqq.bootkits.web.demo.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo 环境的简化集成配置
 * 提供基本的插件上传支持
 *
 * @author brick-bootkit
 */
public class DemoIntegrationConfiguration {

    /**
     * Demo 版本的 IntegrationConfiguration 实现
     */
    @Data
    public static class DemoIntegrationConfigurationImpl {
        private List<String> pluginPath = new ArrayList<>();
        private String uploadTempPath = System.getProperty("java.io.tmpdir") + "/brick-upload-temp";
        private String backupPath = System.getProperty("java.io.tmpdir") + "/brick-backup";
        private String pluginRestPathPrefix = "/api/brick";
    }

    /**
     * 提供 DemoIntegrationConfiguration Bean
     */
    @Bean
    @Primary
    public DemoIntegrationConfiguration.DemoIntegrationConfigurationImpl demoIntegrationConfiguration() {
        return new DemoIntegrationConfiguration.DemoIntegrationConfigurationImpl();
    }
}
