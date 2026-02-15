/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core;

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
import com.zqzqq.bootkits.core.migration.PluginMigrationService;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.listener.DefaultPluginListenerFactory;
import com.zqzqq.bootkits.integration.listener.PluginListenerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件启动管理器
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public class PluginLauncherManager extends DefaultPluginManager{

    private final Map<String, RegistryPluginInfo> registryInfo = new ConcurrentHashMap<>();


    private final MainApplicationContext mainApplicationContext;
    private final GenericApplicationContext mainGenericApplicationContext;
    private final IntegrationConfiguration configuration;
    private final InvokeSupperCache invokeSupperCache;
    private final PluginLaunchInvolved pluginLaunchInvolved;
    private final PluginMigrationService migrationService;

    public PluginLauncherManager(RealizeProvider realizeProvider,
                                 GenericApplicationContext applicationContext,
                                 IntegrationConfiguration configuration) {
        super(realizeProvider, configuration);
        this.mainApplicationContext = new MainApplicationContextProxy(applicationContext, applicationContext);
        this.mainGenericApplicationContext = applicationContext;
        this.configuration = configuration;
        this.invokeSupperCache = new DefaultInvokeSupperCache();
        this.pluginLaunchInvolved = new PluginLaunchInvolvedFactory();
        Path migrationStateDir = getClusterSharedRoot().resolve(".plugin-state").resolve("migrations");
        this.migrationService = new PluginMigrationService(applicationContext, migrationStateDir);
        addCustomPluginChecker();
    }

    private void addCustomPluginChecker(){
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
        launcherChecker.checkCanStart(pluginInsideInfo);
        try {
            InsidePluginDescriptor pluginDescriptor = pluginInsideInfo.getPluginDescriptor();
            PluginInteractive pluginInteractive = new DefaultPluginInteractive(pluginInsideInfo,
                    mainApplicationContext, configuration, invokeSupperCache);
            AbstractLauncher<SpringPluginHook> pluginLauncher;
            if(DevelopmentModeSetting.isolation()){
                pluginLauncher = new PluginIsolationLauncher(pluginInteractive, pluginLaunchInvolved);
            } else if(DevelopmentModeSetting.coexist()){
                pluginLauncher = new PluginCoexistLauncher(pluginInteractive, pluginLaunchInvolved);
            } else {
                throw DevelopmentModeSetting.getUnknownModeException();
            }
            SpringPluginHook springPluginHook = pluginLauncher.run();
            RegistryPluginInfo registryPluginInfo = new RegistryPluginInfo(pluginDescriptor, springPluginHook);
            registryInfo.put(pluginDescriptor.getPluginId(), registryPluginInfo);
            pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED);
            super.startFinish(pluginInsideInfo);
        } catch (Exception e){
            // 启动失败, 进行停止
            pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED_FAILURE);
            throw e;
        }
    }

    @Override
    protected void stop(PluginInsideInfo pluginInsideInfo, PluginCloseType closeType) throws Exception {
        launcherChecker.checkCanStop(pluginInsideInfo);
        String pluginId = pluginInsideInfo.getPluginId();
        RegistryPluginInfo registryPluginInfo = registryInfo.get(pluginId);
        if(registryPluginInfo == null){
            throw new PluginException("没有发现插件 '" + pluginId +  "' 信息");
        }
        try {
            SpringPluginHook springPluginHook = registryPluginInfo.getSpringPluginHook();
            // 校验是否可停止
            springPluginHook.stopVerify();
            // 关闭插件
            springPluginHook.close(closeType);
            // 移除插件相互调用缓存的信息
            invokeSupperCache.remove(pluginId);
            // 移除插件注册信息
            registryInfo.remove(pluginId);
            super.stop(pluginInsideInfo, closeType);
        } catch (Exception e){
            if(e instanceof PluginProhibitStopException){
                // 禁止停止, 不设置插件状态
                throw e;
            }
            pluginInsideInfo.setPluginState(EnhancedPluginState.STOPPED_FAILURE);
            throw e;
        }
    }

    @Override
    protected void beforeInstall(PluginInsideInfo pluginInsideInfo) throws Exception {
        migrationService.applyInstallMigrations(pluginInsideInfo.getPluginDescriptor());
    }

    @Override
    protected void beforeUninstall(PluginInsideInfo pluginInsideInfo) throws Exception {
        migrationService.applyUninstallMigrations(pluginInsideInfo.getPluginDescriptor());
    }


    static class RegistryPluginInfo{
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
