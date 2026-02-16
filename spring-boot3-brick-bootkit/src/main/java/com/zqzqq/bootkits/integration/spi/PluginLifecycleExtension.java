package com.zqzqq.bootkits.integration.spi;

import com.zqzqq.bootkits.core.PluginInsideInfo;

/**
 * SPI lifecycle extension for plugin runtime.
 */
public interface PluginLifecycleExtension {

    default String getExtensionId() {
        return getClass().getName();
    }

    default int getOrder() {
        return 0;
    }

    default void initialize(PluginLifecycleExtensionContext context) {
    }

    default void beforeInstall(PluginInsideInfo pluginInsideInfo) {
    }

    default void afterInstall(PluginInsideInfo pluginInsideInfo) {
    }

    default void beforeStart(PluginInsideInfo pluginInsideInfo) {
    }

    default void afterStart(PluginInsideInfo pluginInsideInfo) {
    }

    default void beforeStop(PluginInsideInfo pluginInsideInfo) {
    }

    default void afterStop(PluginInsideInfo pluginInsideInfo) {
    }

    default void beforeUninstall(PluginInsideInfo pluginInsideInfo) {
    }

    default void afterUninstall(PluginInsideInfo pluginInsideInfo) {
    }

    default void destroy() {
    }
}
