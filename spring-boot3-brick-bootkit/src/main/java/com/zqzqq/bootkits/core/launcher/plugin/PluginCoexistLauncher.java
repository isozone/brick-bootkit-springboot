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


package com.zqzqq.bootkits.core.launcher.plugin;

import com.zqzqq.bootkits.core.classloader.PluginGeneralUrlClassLoader;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolved;
import com.zqzqq.bootkits.loader.classloader.GeneralUrlClassLoader;
import com.zqzqq.bootkits.loader.launcher.LauncherContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 插件共存模式启动器
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
@Slf4j
public class PluginCoexistLauncher extends AbstractPluginLauncher {


    public PluginCoexistLauncher(PluginInteractive pluginInteractive,
                                 PluginLaunchInvolved pluginLaunchInvolved) {
        super(pluginInteractive, pluginLaunchInvolved);
    }

    @Override
    protected ClassLoader createPluginClassLoader(String... args) throws Exception {
        PluginGeneralUrlClassLoader classLoader = new PluginGeneralUrlClassLoader(
                pluginInteractive.getPluginDescriptor().getPluginId(),
                getParentClassLoader());
        classLoader.addResource(pluginInteractive.getPluginDescriptor());
        return classLoader;
    }

    protected GeneralUrlClassLoader getParentClassLoader() throws Exception {
        ClassLoader contextClassLoader = LauncherContext.getMainClassLoader();
        if(contextClassLoader instanceof GeneralUrlClassLoader){
            return (GeneralUrlClassLoader) contextClassLoader;
        } else {
            throw new Exception("无法父类加载器: " + contextClassLoader.getClass().getName());
        }
    }

}

