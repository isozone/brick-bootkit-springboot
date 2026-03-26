package com.example.hostbroken;

import com.zqzqq.bootkits.integration.DefaultIntegrationConfiguration;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateBrokenMainPackageConfiguration {

    @Bean
    public IntegrationConfiguration integrationConfiguration() {
        return new DefaultIntegrationConfiguration() {
            @Override
            public String mainPackage() {
                return "";
            }
        };
    }
}
