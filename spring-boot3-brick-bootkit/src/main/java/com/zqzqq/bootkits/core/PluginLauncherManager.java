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



package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.admission.PluginAdmissionContext;
import com.zqzqq.bootkits.core.admission.PluginAdmissionOperation;
import com.zqzqq.bootkits.core.admission.PluginAdmissionPipeline;
import com.zqzqq.bootkits.core.checker.PluginLauncherChecker;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.exception.PluginProhibitStopException;
import com.zqzqq.bootkits.core.launcher.plugin.DefaultPluginInteractive;
import com.zqzqq.bootkits.core.launcher.plugin.PluginCoexistLauncher;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.core.launcher.plugin.PluginIsolationLauncher;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolved;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolvedFactory;
import com.zqzqq.bootkits.core.lock.ClusterLockProvider;
import com.zqzqq.bootkits.core.migration.PluginMigrationOptions;
import com.zqzqq.bootkits.core.migration.PluginMigrationService;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.listener.DefaultPluginListenerFactory;
import com.zqzqq.bootkits.integration.listener.PluginListenerFactory;
import com.zqzqq.bootkits.integration.spi.PluginLifecycleExtensionManager;
import com.zqzqq.bootkits.loader.launcher.AbstractLauncher;
import com.zqzqq.bootkits.loader.launcher.DevelopmentModeSetting;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.MainApplicationContextProxy;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.spring.invoke.DefaultInvokeSupperCache;
import com.zqzqq.bootkits.spring.invoke.InvokeSupperCache;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.springframework.context.support.GenericApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin launcher manager.
 */
public class PluginLauncherManager extends DefaultPluginManager {

    private final Map<String, RegistryPluginInfo> registryInfo = new ConcurrentHashMap<>();

    private final MainApplicationContext mainApplicationContext;
    private final GenericApplicationContext mainGenericApplicationContext;
    private final IntegrationConfiguration configuration;
    private final InvokeSupperCache invokeSupperCache;
    private final PluginLaunchInvolved pluginLaunchInvolved;
    private final PluginMigrationService migrationService;
    private final PluginAdmissionPipeline admissionPipeline;
    private final PluginLifecycleExtensionManager extensionManager;

    public PluginLauncherManager(RealizeProvider realizeProvider,
                                 GenericApplicationContext applicationContext,
                                 IntegrationConfiguration configuration) {
        this(realizeProvider, applicationContext, configuration, null, null, null);
    }

    public PluginLauncherManager(RealizeProvider realizeProvider,
                                 GenericApplicationContext applicationContext,
                                 IntegrationConfiguration configuration,
                                 ClusterLockProvider clusterLockProvider,
                                 PluginAdmissionPipeline admissionPipeline,
                                 PluginLifecycleExtensionManager extensionManager) {
        super(realizeProvider, configuration, clusterLockProvider);
        this.mainApplicationContext = new MainApplicationContextProxy(applicationContext, applicationContext);
        this.mainGenericApplicationContext = applicationContext;
        this.configuration = configuration;
        this.invokeSupperCache = new DefaultInvokeSupperCache();
        this.pluginLaunchInvolved = new PluginLaunchInvolvedFactory();
        this.admissionPipeline = admissionPipeline;
        this.extensionManager = extensionManager;

        Path migrationStateDir = getClusterSharedRoot().resolve(".plugin-state").resolve("migrations");
        PluginMigrationOptions migrationOptions = new PluginMigrationOptions(
                configuration.migrationValidateChecksum(),
                configuration.migrationContinueOnError()
        );
        this.migrationService = new PluginMigrationService(applicationContext, migrationStateDir, migrationOptions);

        addCustomPluginChecker();
        if (this.extensionManager != null) {
            this.extensionManager.getContext().bindPluginManager(this);
            this.extensionManager.initialize();
        }
    }

    private void addCustomPluginChecker() {
        List<PluginLauncherChecker> pluginCheckers = SpringBeanUtils.getBeans(mainGenericApplicationContext,
                PluginLauncherChecker.class);
        for (PluginLauncherChecker pluginChecker : pluginCheckers) {
            super.launcherChecker.add(pluginChecker);
        }
    }

    @Override
    protected PluginListenerFactory createPluginListenerFactory() {
        return new DefaultPluginListenerFactory(mainGenericApplicationContext);
    }

    @Override
    public synchronized List<PluginInfo> loadPlugins() {
        this.pluginLaunchInvolved.initialize(mainGenericApplicationContext, configuration);
        return super.loadPlugins();
    }

    @Override
    protected void start(PluginInsideInfo pluginInsideInfo) throws Exception {
        applyAdmission(PluginAdmissionOperation.START, pluginInsideInfo.getPluginDescriptor());
        if (extensionManager != null) {
            extensionManager.beforeStart(pluginInsideInfo);
        }
        launcherChecker.checkCanStart(pluginInsideInfo);
        try {
            InsidePluginDescriptor pluginDescriptor = pluginInsideInfo.getPluginDescriptor();
            PluginInteractive pluginInteractive = new DefaultPluginInteractive(pluginInsideInfo,
                    mainApplicationContext, configuration, invokeSupperCache);
            AbstractLauncher<SpringPluginHook> pluginLauncher;
            if (DevelopmentModeSetting.isolation()) {
                pluginLauncher = new PluginIsolationLauncher(pluginInteractive, pluginLaunchInvolved);
            } else if (DevelopmentModeSetting.coexist()) {
                pluginLauncher = new PluginCoexistLauncher(pluginInteractive, pluginLaunchInvolved);
            } else {
                throw DevelopmentModeSetting.getUnknownModeException();
            }
            SpringPluginHook springPluginHook = pluginLauncher.run();
            RegistryPluginInfo registryPluginInfo = new RegistryPluginInfo(pluginDescriptor, springPluginHook);
            registryInfo.put(pluginDescriptor.getPluginId(), registryPluginInfo);
            pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED);
            if (extensionManager != null) {
                extensionManager.afterStart(pluginInsideInfo);
            }
            super.startFinish(pluginInsideInfo);
        } catch (Exception e) {
            pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED_FAILURE);
            throw e;
        }
    }

    @Override
    protected void stop(PluginInsideInfo pluginInsideInfo, PluginCloseType closeType) throws Exception {
        if (extensionManager != null) {
            extensionManager.beforeStop(pluginInsideInfo);
        }

        launcherChecker.checkCanStop(pluginInsideInfo);
        String pluginId = pluginInsideInfo.getPluginId();
        RegistryPluginInfo registryPluginInfo = registryInfo.get(pluginId);
        if (registryPluginInfo == null) {
            throw new PluginException("No plugin registry found: " + pluginId);
        }
        try {
            SpringPluginHook springPluginHook = registryPluginInfo.getSpringPluginHook();
            springPluginHook.stopVerify();
            springPluginHook.close(closeType);
            invokeSupperCache.remove(pluginId);
            registryInfo.remove(pluginId);
            super.stop(pluginInsideInfo, closeType);
            if (extensionManager != null) {
                extensionManager.afterStop(pluginInsideInfo);
            }
        } catch (Exception e) {
            if (e instanceof PluginProhibitStopException) {
                throw e;
            }
            pluginInsideInfo.setPluginState(EnhancedPluginState.STOPPED_FAILURE);
            throw e;
        }
    }

    @Override
    protected void beforeInstall(PluginInsideInfo pluginInsideInfo) throws Exception {
        applyAdmission(PluginAdmissionOperation.INSTALL, pluginInsideInfo.getPluginDescriptor());
        if (extensionManager != null) {
            extensionManager.beforeInstall(pluginInsideInfo);
        }
        migrationService.applyInstallMigrations(pluginInsideInfo.getPluginDescriptor());
        if (extensionManager != null) {
            extensionManager.afterInstall(pluginInsideInfo);
        }
    }

    @Override
    protected void beforeUninstall(PluginInsideInfo pluginInsideInfo) throws Exception {
        if (extensionManager != null) {
            extensionManager.beforeUninstall(pluginInsideInfo);
        }
        migrationService.applyUninstallMigrations(pluginInsideInfo.getPluginDescriptor());
        if (extensionManager != null) {
            extensionManager.afterUninstall(pluginInsideInfo);
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            if (extensionManager != null) {
                extensionManager.destroy();
            }
        }
    }

    private void applyAdmission(PluginAdmissionOperation operation, InsidePluginDescriptor descriptor) {
        if (admissionPipeline == null) {
            return;
        }
        admissionPipeline.evaluate(new PluginAdmissionContext(operation, descriptor, resolvePluginPath(descriptor)));
    }

    private Path resolvePluginPath(InsidePluginDescriptor descriptor) {
        if (descriptor == null || descriptor.getPluginPath() == null) {
            return null;
        }
        return Paths.get(descriptor.getPluginPath());
    }

    static class RegistryPluginInfo {
        private final PluginDescriptor descriptor;
        private final SpringPluginHook springPluginHook;

        private RegistryPluginInfo(PluginDescriptor descriptor, SpringPluginHook springPluginHook) {
            this.descriptor = descriptor;
            this.springPluginHook = springPluginHook;
        }

        public PluginDescriptor getDescriptor() {
            return descriptor;
        }

        public SpringPluginHook getSpringPluginHook() {
            return springPluginHook;
        }
    }
}
