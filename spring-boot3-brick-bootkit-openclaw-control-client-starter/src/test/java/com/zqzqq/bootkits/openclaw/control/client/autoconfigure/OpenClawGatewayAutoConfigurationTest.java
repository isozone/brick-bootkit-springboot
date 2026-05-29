package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.OpenClawGatewayClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawGatewayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenClawGatewayAutoConfiguration.class));

    @Test
    void shouldStayDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OpenClawGatewayClient.class));
    }

    @Test
    void shouldCreateOfficialGatewayClientWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "openclaw.gateway.enabled=true",
                        "openclaw.gateway.base-url=http://127.0.0.1:18789",
                        "openclaw.gateway.auth-token=token-1"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenClawGatewayClient.class);
                    OpenClawGatewayProperties properties = context.getBean(OpenClawGatewayProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo("http://127.0.0.1:18789");
                    assertThat(properties.getAuthToken()).isEqualTo("token-1");
                });
    }
}
