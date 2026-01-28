package com.zqzqq.bootkits.web.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo 环境的简化集成配置
 * 提供基本的插件上传支持
 * 
 * @author brick-bootkit
 */
@Configuration
public class DemoIntegrationConfiguration {

    /**
     * Demo 插件路径配置
     */
    @Data
    public static class DemoPluginPaths {
        private List<String> pluginPath = new ArrayList<>();
        private String uploadTempPath = System.getProperty("java.io.tmpdir");
        private String backupPath = System.getProperty("java.io.tmpdir") + "/brick-backup";
        private String pluginRestPathPrefix = "/api/brick";
    }

    /**
     * 提供简化版 IntegrationConfiguration 接口实现
     */
    public interface IntegrationConfiguration {
        List<String> pluginPath();
        String uploadTempPath();
        String backupPath();
        String pluginRestPathPrefix();
    }

    /**
     * Demo 版本的 IntegrationConfiguration 实现
     */
    @Data
    public static class DemoIntegrationConfigurationImpl implements IntegrationConfiguration {
        private List<String> pluginPath = new ArrayList<>();
        private String uploadTempPath = System.getProperty("java.io.tmpdir");
        private String backupPath = System.getProperty("java.io.tmpdir") + "/brick-backup";
        private String pluginRestPathPrefix = "/api/brick";

        @Override
        public List<String> pluginPath() {
            return pluginPath;
        }

        @Override
        public String uploadTempPath() {
            return uploadTempPath;
        }

        @Override
        public String backupPath() {
            return backupPath;
        }

        @Override
        public String pluginRestPathPrefix() {
            return pluginRestPathPrefix;
        }
    }

    /**
     * 提供 DemoIntegrationConfiguration Bean
     */
    @Bean
    @Primary
    public IntegrationConfiguration demoIntegrationConfiguration() {
        return new DemoIntegrationConfigurationImpl();
    }
}
