package com.zqzqq.bootkits.web.config;

import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Brick Web 自动配置类
 * 自动从 IntegrationConfiguration 读取插件相关配置
 * 
 * @author brick-bootkit
 */
@Slf4j
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)  // 确保在 SpringDoc 之后加载，避免覆盖其配置
@ConditionalOnProperty(prefix = "plugins.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BrickWebProperties.class)
public class BrickWebAutoConfiguration implements WebMvcConfigurer {

    /**
     * 当存在 IntegrationConfiguration 时使用的配置
     */
    @Configuration
    @ConditionalOnBean(IntegrationConfiguration.class)
    protected static class WithIntegrationConfiguration {
        
        private final IntegrationConfiguration integrationConfiguration;

        public WithIntegrationConfiguration(IntegrationConfiguration integrationConfiguration) {
            this.integrationConfiguration = integrationConfiguration;
        }

        @Bean
        @Primary
        @ConditionalOnMissingBean
        public BrickWebProperties brickWebProperties() {
            BrickWebProperties properties = new BrickWebProperties();
            
            try {
                // 从 IntegrationConfiguration 自动读取插件相关配置
                properties.setPluginPaths(integrationConfiguration.pluginPath());
                properties.setUploadTempPath(integrationConfiguration.uploadTempPath());
                properties.setBackupPath(integrationConfiguration.backupPath());
                properties.setPluginRestPathPrefix(integrationConfiguration.pluginRestPathPrefix());
                log.info("Brick Web 配置已从 IntegrationConfiguration 加载");
            } catch (Exception e) {
                log.warn("无法从 IntegrationConfiguration 加载配置，使用默认配置: {}", e.getMessage());
                setDefaultProperties(properties);
            }
            
            return properties;
        }
    }

    /**
     * 当不存在 IntegrationConfiguration 时使用的默认配置
     */
    @Configuration
    @ConditionalOnMissingBean(IntegrationConfiguration.class)
    protected static class WithoutIntegrationConfiguration {

        @Bean
        @Primary
        @ConditionalOnMissingBean
        public BrickWebProperties brickWebProperties() {
            BrickWebProperties properties = new BrickWebProperties();
            setDefaultProperties(properties);
            log.info("Brick Web 使用默认配置（未检测到 IntegrationConfiguration）");
            return properties;
        }
    }

    private static void setDefaultProperties(BrickWebProperties properties) {
        properties.setPluginPaths(java.util.Collections.emptyList());
        properties.setUploadTempPath(System.getProperty("java.io.tmpdir"));
        properties.setBackupPath(System.getProperty("java.io.tmpdir") + "/brick-backup");
        properties.setPluginRestPathPrefix("/api/brick");
    }

    /**
     * 注册静态资源处理器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j API 文档资源
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/")
                .resourceChain(true);
        
        // webjars 资源（Swagger UI 使用）
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true);
        
        // Vue 打包的所有静态资源（包括 index.html、assets 等）
        registry.addResourceHandler("/brick-web/**")
                .addResourceLocations("classpath:/static/brick-web/")
                .resourceChain(true);
    }
    
    /**
     * 配置视图控制器，直接返回静态HTML文件
     * 避免 Thymeleaf 视图解析器干扰
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径重定向到 Vue 首页
        registry.addRedirectViewController("/", "/brick-web/");
    }
}