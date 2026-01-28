package com.zqzqq.bootkits.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j API 文档配置
 * 
 * @author brick-bootkit
 */
@Configuration
@EnableConfigurationProperties(BrickWebProperties.class)
public class Knife4jConfiguration {

    private final BrickWebProperties properties;

    public Knife4jConfiguration(BrickWebProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI brickWebOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        
        // API 信息
        Info info = new Info()
                .title(properties.getApiTitle())
                .version("1.0.0")
                .description("Brick BootKit Web Management API")
                .contact(new Contact()
                        .name("Brick BootKit")
                        .email("huzhenjie@rjnetwork.net.cn"))
                .license(new License()
                        .name("Apache License 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
        
        openAPI.info(info);
        
        // 服务器
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("/").description("Current Server"));
        openAPI.servers(servers);
        
        return openAPI;
    }
}
