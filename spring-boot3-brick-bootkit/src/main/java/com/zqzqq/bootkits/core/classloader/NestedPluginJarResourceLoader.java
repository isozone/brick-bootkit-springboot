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

package com.zqzqq.bootkits.core.classloader;


import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginLibInfo;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.loader.classloader.resource.loader.*;
import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;
import com.zqzqq.bootkits.utils.MsgUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * 嵌套插件jar加载器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
@Slf4j
public class NestedPluginJarResourceLoader extends AbstractResourceLoader {
    private static final Logger log = LoggerFactory.getLogger(NestedPluginJarResourceLoader.class);


    private final InsidePluginDescriptor pluginDescriptor;
    private final ResourceLoaderFactory resourceLoaderFactory;
    private final ResourceLoaderFactory parentResourceLoaderFactory;

    public NestedPluginJarResourceLoader(InsidePluginDescriptor pluginDescriptor,
                                         ResourceLoaderFactory resourceLoaderFactory) throws Exception {
        this(pluginDescriptor, resourceLoaderFactory, null);
    }

    public NestedPluginJarResourceLoader(InsidePluginDescriptor pluginDescriptor,
                                         ResourceLoaderFactory resourceLoaderFactory,
                                         ResourceLoaderFactory parentResourceLoaderFactory) throws Exception {
        super(new URL("jar:" + pluginDescriptor.getInsidePluginPath().toUri().toURL() + "!/"));
        this.pluginDescriptor = pluginDescriptor;
        this.resourceLoaderFactory = resourceLoaderFactory;
        this.parentResourceLoaderFactory = parentResourceLoaderFactory;
    }

    @Override
    protected void loadOfChild(ResourceStorage resourceStorage) throws Exception {
        try (JarFile jarFile = new JarFile(pluginDescriptor.getInsidePluginPath().toFile())) {
            addClassPath(resourceStorage, jarFile);
            addLib(jarFile);
        }
    }

    private void addClassPath(ResourceStorage resourceStorage, JarFile jarFile) throws Exception{
        String classesPath = pluginDescriptor.getPluginClassPath();
        Enumeration<JarEntry> entries = jarFile.entries();
        resourceLoaderFactory.addResource(new BaseURLResourceLoader(baseUrl));
        resourceLoaderFactory.addResource(new BaseURLResourceLoader(new URL(baseUrl, "classes/")));
        while (entries.hasMoreElements()){
            JarEntry jarEntry = entries.nextElement();
            if(!jarEntry.getName().startsWith(classesPath)){
                continue;
            }
            String realName = jarEntry.getName().replace(classesPath, "");
            URL url = new URL(baseUrl.toString() + jarEntry.getName());
            resourceLoaderFactory.addResource(new DefaultResource(realName, baseUrl, url));
            resourceStorage.add(new CacheResource(realName, baseUrl, url, ()->{
                return getClassBytes(realName, jarFile.getInputStream(jarEntry), true);
            }));
        }
    }

    private void addLib(JarFile jarFile) throws Exception {
        JarEntry jarEntry = null;
        Set<PluginLibInfo> pluginLibInfos = pluginDescriptor.getPluginLibInfo();
        String pluginUnique = MsgUtils.getPluginUnique(pluginDescriptor);
        for (PluginLibInfo pluginLibInfo : pluginLibInfos) {
            String entryName = pluginLibInfo.getPath();
            jarEntry = jarFile.getJarEntry(entryName);
            if(jarEntry == null){
                log.debug("Not found: " + pluginLibInfo.getPath());
                continue;
            }
            if (jarEntry.getMethod() != ZipEntry.STORED) {
                throw new PluginException("插件依赖压缩方式错误, 必须使用存储(stored)压缩方式");
            }
            InputStream jarFileInputStream = jarFile.getInputStream(jarEntry);
            URL url = new URL(baseUrl.toString() + pluginLibInfo.getPath() + "!/");
            if(parentResourceLoaderFactory != null && pluginLibInfo.isLoadToMain()){
                parentResourceLoaderFactory.addResource(new JarResourceLoader(url, new JarInputStream(jarFileInputStream)));
                log.debug("插件[{}]依赖被加载到主程序包中: {}", pluginUnique, pluginLibInfo.getPath());
            } else {
                JarResourceLoader jarResourceLoader = new JarResourceLoader(url, new JarInputStream(jarFileInputStream));
                resourceLoaderFactory.addResource(jarResourceLoader);
                log.debug("插件[{}]依赖被加载: {}", pluginUnique, pluginLibInfo.getPath());
            }
        }
    }

}

