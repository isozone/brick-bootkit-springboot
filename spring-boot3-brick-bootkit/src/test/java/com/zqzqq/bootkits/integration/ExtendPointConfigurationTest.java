package com.zqzqq.bootkits.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ExtendPointConfigurationTest {

    @Test
    void shouldAutoDetectMainPackageForAutoIntegrationConfiguration() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        AutoConfigurationPackages.register(applicationContext.getDefaultListableBeanFactory(), "com.example.demo");

        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();
        configuration.setPluginPath(Collections.singletonList("plugins"));
        configuration.setMainPackage("");

        new ExtendPointConfiguration(applicationContext, configuration);

        assertThat(configuration.mainPackage()).isEqualTo("com.example.demo");
    }
}
