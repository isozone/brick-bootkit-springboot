/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.annotation.OneselfConfig;
import com.zqzqq.bootkits.bootstrap.coexist.CoexistAllowAutoConfiguration;
import com.zqzqq.bootkits.bootstrap.launcher.BootstrapLauncher;
import com.zqzqq.bootkits.bootstrap.launcher.BootstrapLauncherFactory;
import com.zqzqq.bootkits.bootstrap.launcher.DefaultBootstrapLauncherFactory;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.bootstrap.realize.AutowiredTypeDefiner;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.loader.launcher.DevelopmentModeSetting;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件引导抽象类。插件入口需集成本抽象类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public abstract class SpringPluginBootstrap {

    @Getter
    private ProcessorContext.RunMode runMode = ProcessorContext.RunMode.ONESELF;
    @Getter
    private volatile PluginInteractive pluginInteractive;
    @Getter
    private final List<SpringPluginProcessor> customPluginProcessors = new ArrayList<>();
    @Getter
    private final CoexistAllowAutoConfiguration coexistAllowAutoConfiguration = new CoexistAllowAutoConfiguration();

    private final BootstrapLauncherFactory launcherFactory = new DefaultBootstrapLauncherFactory();

    public SpringPluginBootstrap() {
        SpringPluginBootstrapBinder.set(this);
    }

    public final SpringPluginHook run(String[] args){
        return run(this.getClass(), args);
    }

    public final SpringPluginHook run(Class<?> primarySources, String[] args){
        return run(new Class[]{ primarySources }, args);
    }

    public final SpringPluginHook run(Class<?>[] primarySources, String[] args){
        return start(primarySources, args);
    }

    private SpringPluginHook start(Class<?>[] primarySources, String[] args){
        configCoexistAllowAutoConfiguration(this.coexistAllowAutoConfiguration);
        applyStandaloneDevelopmentModeConfig();
        createPluginInteractive();
        addCustomSpringPluginProcessor();
        BootstrapLauncher bootstrapLauncher = launcherFactory.create(this);
        return bootstrapLauncher.launch(primarySources, args);
    }

    public final SpringPluginBootstrap setPluginInteractive(PluginInteractive pluginInteractive) {
        this.pluginInteractive = pluginInteractive;
        this.runMode = ProcessorContext.RunMode.PLUGIN;
        return this;
    }

    public final SpringPluginBootstrap addSpringPluginProcessor(SpringPluginProcessor springPluginProcessor){
        if(springPluginProcessor != null){
            customPluginProcessors.add(springPluginProcessor);
        }
        return this;
    }

    protected final void createPluginInteractive(){
        if(pluginInteractive != null){
            return;
        }
        createPluginInteractiveOfOneself();
    }

    protected final void createPluginInteractiveOfOneself(){
        this.pluginInteractive = new PluginOneselfInteractive(this.getClass());
    }

    private void applyStandaloneDevelopmentModeConfig() {
        if (runMode != ProcessorContext.RunMode.ONESELF) {
            return;
        }
        OneselfConfig oneselfConfig = this.getClass().getAnnotation(OneselfConfig.class);
        if (oneselfConfig == null) {
            return;
        }
        DevelopmentModeSetting.setStandaloneDevelopmentMode(oneselfConfig.developmentMode());
    }


    /**
     * 子类自定义插件SpringPluginProcessor
     */
    protected void addCustomSpringPluginProcessor(){}

    /**
     * 设置 AutowiredTypeDefiner
     * @return AutowiredTypeDefiner
     * @since 3.0.3
     */
    protected AutowiredTypeDefiner autowiredTypeDefiner(){
        return null;
    }

    /**
     * 在Coexist模式下手动配置spring-boot-auto-configuration类
     * @param configuration 配置的类
     */
    protected void configCoexistAllowAutoConfiguration(CoexistAllowAutoConfiguration configuration){

    }

}
