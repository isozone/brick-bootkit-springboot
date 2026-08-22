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

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.classloader.ComposeMainResourceMatcher;
import com.zqzqq.bootkits.core.classloader.DefaultMainResourceMatcher;
import com.zqzqq.bootkits.core.classloader.MainResourceMatcher;
import com.zqzqq.bootkits.core.classloader.PluginClassLoader;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolved;
import com.zqzqq.bootkits.loader.classloader.GenericClassLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.DefaultResourceLoaderFactory;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.loader.launcher.LauncherContext;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.utils.MsgUtils;
import com.zqzqq.bootkits.utils.SpringBeanCustomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 插件隔离模式启动器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class PluginIsolationLauncher extends AbstractPluginLauncher {

    private static final Map<String, PluginClassLoader> CLASS_LOADER_CACHE = new WeakHashMap<>();

    protected final PluginInsideInfo pluginInsideInfo;
    protected final MainResourceMatcher mainResourceMatcher;

    public PluginIsolationLauncher(PluginInteractive pluginInteractive,
                                   PluginLaunchInvolved pluginLaunchInvolved) {
        super(pluginInteractive, pluginLaunchInvolved);
        this.pluginInsideInfo = pluginInteractive.getPluginInsideInfo();
        this.mainResourceMatcher = getMainResourceMatcher(pluginInteractive);
    }

    protected MainResourceMatcher getMainResourceMatcher(PluginInteractive pluginInteractive){
        MainApplicationContext mainApplicationContext = pluginInteractive.getMainApplicationContext();
        // 获取主程序包定义的资源匹配
        List<MainResourceMatcher> mainResourceMatchers =
                SpringBeanCustomUtils.getBeans(mainApplicationContext, MainResourceMatcher.class);

        List<MainResourceMatcher> resourceMatchers = new ArrayList<>(mainResourceMatchers);
        // 新增插件定义的资源匹配器
        resourceMatchers.add(new DefaultMainResourceMatcher(
                new PluginMainResourcePatternDefiner(pluginInteractive)
        ));
        return new ComposeMainResourceMatcher(resourceMatchers);
    }

    @Override
    protected ClassLoader createPluginClassLoader(String... args) throws Exception {
        PluginClassLoader pluginClassLoader = getPluginClassLoader();
        pluginClassLoader.addResource(pluginInsideInfo.getPluginDescriptor());
        return pluginClassLoader;
    }

    @Override
    protected SpringPluginHook launch(ClassLoader classLoader, String... args) throws Exception {
        SpringPluginHook springPluginHook = super.launch(classLoader, args);
        ResourceUtils.release(classLoader);
        return springPluginHook;
    }

    protected synchronized PluginClassLoader getPluginClassLoader() throws Exception {
        String pluginId = pluginInsideInfo.getPluginId();
        String key = MsgUtils.getPluginUnique(pluginInsideInfo.getPluginDescriptor());
        PluginClassLoader classLoader = CLASS_LOADER_CACHE.get(key);
        if(classLoader != null){
            return classLoader;
        }
        PluginClassLoader pluginClassLoader = new PluginClassLoader(
                pluginId, getParentClassLoader(),  getParentClassLoader(), getResourceLoaderFactory(),
                mainResourceMatcher
        );
        CLASS_LOADER_CACHE.put(key, pluginClassLoader);
        return pluginClassLoader;
    }

    protected ResourceLoaderFactory getResourceLoaderFactory(){
        return new DefaultResourceLoaderFactory(pluginInsideInfo.getPluginId());
    }

    protected GenericClassLoader getParentClassLoader() throws Exception {
        ClassLoader contextClassLoader = LauncherContext.getMainClassLoader();
        if(contextClassLoader instanceof GenericClassLoader){
            return (GenericClassLoader) contextClassLoader;
        } else {
            throw new Exception("无法父类加载: " + contextClassLoader.getClass().getName());
        }
    }

}

