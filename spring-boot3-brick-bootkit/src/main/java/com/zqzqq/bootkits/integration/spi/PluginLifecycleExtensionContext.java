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
