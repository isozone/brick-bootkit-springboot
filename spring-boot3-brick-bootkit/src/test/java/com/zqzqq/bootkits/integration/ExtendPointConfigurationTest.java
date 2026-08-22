/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

        assertThat(configuration.getMainPackage()).isEqualTo("com.example.demo");
        assertThat(configuration.mainPackage()).isEqualTo("com/example/demo");
    }
}
