package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.DefaultOpenClawGatewayClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawGatewayClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "openclaw.gateway", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenClawGatewayProperties.class)
public class OpenClawGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenClawGatewayClient openClawGatewayClient(OpenClawGatewayProperties properties) {
        return new DefaultOpenClawGatewayClient(properties);
    }
}
