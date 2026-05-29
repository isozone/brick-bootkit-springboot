package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.OpenClawGatewayClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openclaw.gateway")
public class OpenClawGatewayProperties extends OpenClawGatewayClientProperties {

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
