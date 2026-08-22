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

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Loads and manages plugin SPI lifecycle extensions.
 */
public class PluginLifecycleExtensionManager {

    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleExtensionManager.class);

    private final PluginLifecycleExtensionContext context;
    private final List<PluginLifecycleExtension> extensions;

    public PluginLifecycleExtensionManager(ApplicationContext applicationContext,
                                           IntegrationConfiguration configuration) {
        this.context = new PluginLifecycleExtensionContext(applicationContext, configuration);
        this.extensions = loadExtensions(applicationContext, configuration);
    }

    public PluginLifecycleExtensionContext getContext() {
        return context;
    }

    public List<PluginLifecycleExtension> getExtensions() {
        return extensions;
    }

    public void initialize() {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "initialize", () -> extension.initialize(context));
        }
    }

    public void beforeInstall(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "beforeInstall", () -> extension.beforeInstall(info));
        }
    }

    public void afterInstall(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "afterInstall", () -> extension.afterInstall(info));
        }
    }

    public void beforeStart(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "beforeStart", () -> extension.beforeStart(info));
        }
    }

    public void afterStart(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "afterStart", () -> extension.afterStart(info));
        }
    }

    public void beforeStop(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "beforeStop", () -> extension.beforeStop(info));
        }
    }

    public void afterStop(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "afterStop", () -> extension.afterStop(info));
        }
    }

    public void beforeUninstall(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "beforeUninstall", () -> extension.beforeUninstall(info));
        }
    }

    public void afterUninstall(PluginInsideInfo info) {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "afterUninstall", () -> extension.afterUninstall(info));
        }
    }

    public void destroy() {
        for (PluginLifecycleExtension extension : extensions) {
            safeInvoke(extension, "destroy", extension::destroy);
        }
    }

    private List<PluginLifecycleExtension> loadExtensions(ApplicationContext applicationContext,
                                                          IntegrationConfiguration configuration) {
        if (!Boolean.TRUE.equals(configuration.pluginLifecycleExtensionsEnabled())) {
            log.info("Plugin lifecycle extensions are disabled by configuration");
            return List.of();
        }

        Map<String, PluginLifecycleExtension> unique = new LinkedHashMap<>();

        ServiceLoader<PluginLifecycleExtension> serviceLoader = ServiceLoader.load(PluginLifecycleExtension.class,
                Thread.currentThread().getContextClassLoader());
        for (PluginLifecycleExtension extension : serviceLoader) {
            unique.putIfAbsent(extension.getExtensionId(), extension);
        }

        for (PluginLifecycleExtension extension : SpringBeanUtils.getBeans(applicationContext,
                PluginLifecycleExtension.class)) {
            unique.put(extension.getExtensionId(), extension);
        }

        List<PluginLifecycleExtension> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingInt(PluginLifecycleExtension::getOrder)
                .thenComparing(PluginLifecycleExtension::getExtensionId));
        return List.copyOf(result);
    }

    private void safeInvoke(PluginLifecycleExtension extension, String stage, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("Plugin lifecycle extension invocation failed. stage={}, extension={}",
                    stage, extension.getExtensionId(), e);
        }
    }
}
