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

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.core.DefaultPluginInsideInfo;
import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.DevPluginDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.decrypt.EmptyPluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.integration.AutoIntegrationConfiguration;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.extract.DefaultOpExtractFactory;
import com.zqzqq.bootkits.spring.extract.OpExtractFactory;
import com.zqzqq.bootkits.spring.invoke.DefaultInvokeSupperCache;
import com.zqzqq.bootkits.spring.invoke.InvokeSupperCache;
import com.zqzqq.bootkits.utils.FilesUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 插件鑷繁鐨処nteractive
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class PluginOneselfInteractive implements PluginInteractive {

    private final PluginInsideInfo pluginInsideInfo;
    private final MainApplicationContext mainApplicationContext;
    private final IntegrationConfiguration configuration;
    private final InvokeSupperCache invokeSupperCache;
    private final OpExtractFactory opExtractFactory;

    public PluginOneselfInteractive(){
        this.pluginInsideInfo = createPluginInsideInfo();
        this.mainApplicationContext = new EmptyMainApplicationContext();
        this.configuration = new AutoIntegrationConfiguration();
        this.invokeSupperCache = new DefaultInvokeSupperCache();
        this.opExtractFactory = new DefaultOpExtractFactory();
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

    private PluginInsideInfo createPluginInsideInfo(){
        EmptyPluginDescriptorDecrypt descriptorDecrypt = new EmptyPluginDescriptorDecrypt();
        try (PluginDescriptorLoader pluginDescriptorLoader = new DevPluginDescriptorLoader(descriptorDecrypt)){
            Path classesPath = Paths.get(this.getClass().getResource("/").toURI()).getParent();
            String metaInf = FilesUtils.joiningFilePath(classesPath.toString(), PackageStructure.META_INF_NAME);
            InsidePluginDescriptor pluginDescriptor = pluginDescriptorLoader.load(Paths.get(metaInf));
            if(pluginDescriptor == null){
                throw new RuntimeException("没有发现插件信息, 请使用构建工具提供的Maven插件对插件进行编译!");
            }
            return new DefaultPluginInsideInfo(pluginDescriptor);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}

