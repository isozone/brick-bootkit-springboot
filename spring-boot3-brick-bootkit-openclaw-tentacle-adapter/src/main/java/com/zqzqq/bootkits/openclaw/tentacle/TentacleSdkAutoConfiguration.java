package com.zqzqq.bootkits.openclaw.tentacle;

import com.zqzqq.bootkits.openclaw.control.spi.OpenClawRuntimeIntegration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "cloud.aiai.APP")
public class TentacleSdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "tentacleReflectiveRuntimeIntegration")
    public OpenClawRuntimeIntegration tentacleReflectiveRuntimeIntegration() {
        return new ReflectiveTentacleRuntimeIntegration();
    }
}
