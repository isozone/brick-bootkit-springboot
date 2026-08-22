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



package com.zqzqq.bootkits.core.launcher.plugin.involved;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.spring.ApplicationContext;
import com.zqzqq.bootkits.spring.SpringPluginHook;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件 applicationContext 获取器
 *
 * @author starBlues
 * @since  3.0.0
 * @version 3.1.0
 */
public class PluginApplicationContextGetter implements PluginLaunchInvolved{

    private static final Map<String, ApplicationContext> PLUGIN_CONTEXTS = new ConcurrentHashMap<>();

    @Override
    public void after(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, SpringPluginHook pluginHook) throws Exception {
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        PLUGIN_CONTEXTS.put(descriptor.getPluginId(), pluginHook.getApplicationContext());
    }

    @Override
    public void close(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        PLUGIN_CONTEXTS.remove(descriptor.getPluginId());
    }

    public static ApplicationContext get(String pluginId){
        return PLUGIN_CONTEXTS.get(pluginId);
    }

    public static Map<String, ApplicationContext> get(){
        return Collections.unmodifiableMap(PLUGIN_CONTEXTS);
    }

}

