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


package com.zqzqq.bootkits.integration.spi;

import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * Runtime context for SPI lifecycle extensions.
 */
public class PluginLifecycleExtensionContext {

    private final ApplicationContext applicationContext;
    private final IntegrationConfiguration configuration;
    private PluginManager pluginManager;

    public PluginLifecycleExtensionContext(ApplicationContext applicationContext,
                                           IntegrationConfiguration configuration) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public IntegrationConfiguration getConfiguration() {
        return configuration;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public void bindPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
}
