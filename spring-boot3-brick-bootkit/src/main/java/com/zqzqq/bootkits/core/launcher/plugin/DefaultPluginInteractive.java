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
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.extract.DefaultExtractFactory;
import com.zqzqq.bootkits.spring.extract.ExtractFactory;
import com.zqzqq.bootkits.spring.extract.OpExtractFactory;
import com.zqzqq.bootkits.spring.invoke.InvokeSupperCache;

/**
 * 默认的插件交互实现
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class DefaultPluginInteractive implements PluginInteractive{

    private final PluginInsideInfo pluginInsideInfo;
    private final MainApplicationContext mainApplicationContext;
    private final IntegrationConfiguration configuration;
    private final InvokeSupperCache invokeSupperCache;
    private final OpExtractFactory opExtractFactory;

    public DefaultPluginInteractive(PluginInsideInfo pluginInsideInfo,
                                    MainApplicationContext mainApplicationContext,
                                    IntegrationConfiguration configuration,
                                    InvokeSupperCache invokeSupperCache) {
        this.pluginInsideInfo = pluginInsideInfo;
        this.mainApplicationContext = mainApplicationContext;
        this.configuration = configuration;
        this.invokeSupperCache = invokeSupperCache;
        this.opExtractFactory = createOpExtractFactory();
    }

    protected OpExtractFactory createOpExtractFactory(){
        DefaultExtractFactory defaultExtractFactory = (DefaultExtractFactory)ExtractFactory.getInstant();
        return (OpExtractFactory) defaultExtractFactory.getTarget();
    }

    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        return pluginInsideInfo.getPluginDescriptor();
    }

    @Override
    public PluginInsideInfo getPluginInsideInfo() {
        return pluginInsideInfo;
    }

    @Override
    public MainApplicationContext getMainApplicationContext() {
        return mainApplicationContext;
    }

    @Override
    public IntegrationConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public InvokeSupperCache getInvokeSupperCache() {
        return invokeSupperCache;
    }

    @Override
    public OpExtractFactory getOpExtractFactory() {
        return opExtractFactory;
    }
}

