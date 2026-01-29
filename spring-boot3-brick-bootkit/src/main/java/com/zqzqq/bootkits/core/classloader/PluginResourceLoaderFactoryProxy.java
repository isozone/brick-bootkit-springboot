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
import com.zqzqq.bootkits.core.descriptor.PluginType;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.MsgUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * 插件资源加载工厂代理
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.1
 */
@Slf4j
public class PluginResourceLoaderFactoryProxy implements PluginResourceLoaderFactory {
    private static final Logger log = LoggerFactory.getLogger(PluginResourceLoaderFactoryProxy.class);


    private final ResourceLoaderFactory target;
    private final ResourceLoaderFactory parent;

    public PluginResourceLoaderFactoryProxy(ResourceLoaderFactory target) {
        this(target, null);
    }

    public PluginResourceLoaderFactoryProxy(ResourceLoaderFactory target, ResourceLoaderFactory parent) {
        this.target = target;
        this.parent = parent;
    }

    @Override
    public void addResource(InsidePluginDescriptor descriptor) throws Exception {
        PluginType pluginType = descriptor.getType();
        log.debug("插件[{}]类型: {}, isNested: {}, isOuter: {}, isDir: {}",
                MsgUtils.getPluginUnique(descriptor),
                pluginType,
                PluginType.isNestedPackage(pluginType),
                PluginType.isOuterPackage(pluginType),
                PluginType.isDirPackage(pluginType));
        if(PluginType.isNestedPackage(pluginType)){
            log.debug("使用 NestedPluginJarResourceLoader 加载插件[{}]", MsgUtils.getPluginUnique(descriptor));
            NestedPluginJarResourceLoader resourceLoader =
                    new NestedPluginJarResourceLoader(descriptor, target, parent);
            target.addResource(resourceLoader);
        } else if(PluginType.isOuterPackage(pluginType)){
            log.debug("使用 addOuterPluginClasspath 加载插件[{}]", MsgUtils.getPluginUnique(descriptor));
            addOuterPluginClasspath(descriptor);
            addLibFile(descriptor);
        } else {
            log.debug("使用 addDirPluginClasspath 加载插件[{}]", MsgUtils.getPluginUnique(descriptor));
            addDirPluginClasspath(descriptor);
            addLibFile(descriptor);
        }
    }

    private void addOuterPluginClasspath(InsidePluginDescriptor descriptor) throws Exception{
        String pluginPath = descriptor.getPluginPath();
        File existFile = FilesUtils.getExistFile(pluginPath);
        if(existFile != null){
            addResource(existFile);
            log.debug("插件[{}]Classpath已被加载: {}", MsgUtils.getPluginUnique(descriptor), existFile.getPath());
        } else {
            throw new PluginException("没有发现插件路径: " + pluginPath);
        }
    }

    private void addDirPluginClasspath(InsidePluginDescriptor descriptor) throws Exception {
        String pluginClassPath = descriptor.getPluginClassPath();
        // dev模式下，pluginClassPath是相对于insidePluginPath的相对路径（如"classes"）
        // 需要拼接成完整路径
        Path insidePluginPath = descriptor.getInsidePluginPath();
        String pluginUnique = MsgUtils.getPluginUnique(descriptor);

        log.info("插件[{}]的insidePluginPath: {}", pluginUnique, insidePluginPath);
        log.info("插件[{}]的pluginClassPath: {}", pluginUnique, pluginClassPath);

        File classesDir = insidePluginPath.resolve(pluginClassPath.replace("/", "")).toFile();

        log.info("插件[{}]的classesDir路径: {}, exists: {}, isDirectory: {}",
                pluginUnique, classesDir.getPath(), classesDir.exists(), classesDir.isDirectory());

        if(classesDir.exists() && classesDir.isDirectory()){
            log.info("插件[{}]准备加载Classpath: {}", pluginUnique, classesDir.getPath());
            addResource(classesDir);
            log.info("插件[{}]Classpath已被加载: {}", pluginUnique, classesDir.getPath());
        } else {
            log.error("插件[{}]未发现Classpath: {}", pluginUnique, classesDir.getPath());
        }
    }


    private void addLibFile(InsidePluginDescriptor pluginDescriptor) throws Exception {
        Set<PluginLibInfo> pluginLibInfos = pluginDescriptor.getPluginLibInfo();
        if(ObjectUtils.isEmpty(pluginLibInfos)){
            return;
        }
        String pluginUnique = MsgUtils.getPluginUnique(pluginDescriptor);
        String pluginLibDir = pluginDescriptor.getPluginLibDir();
        if(!ObjectUtils.isEmpty(pluginLibDir)){
            log.info("插件[{}]依赖加载目录: {}", pluginUnique, pluginLibDir);
        }
        if(pluginLibInfos.isEmpty()){
            log.warn("插件[{}]依赖为空", pluginUnique);
            return;
        }
        for (PluginLibInfo pluginLibInfo : pluginLibInfos) {
            File existFile = FilesUtils.getExistFile(pluginLibInfo.getPath());
            if(existFile != null){
                if(pluginLibInfo.isLoadToMain()){
                    // 加载到主程序中
                    parent.addResource(existFile);
                    log.debug("插件[{}]依赖被加载到主程序中: {}", pluginUnique, existFile.getPath());
                } else {
                    target.addResource(existFile);
                    log.debug("插件[{}]依赖被加载: {}", pluginUnique, existFile.getPath());
                }
            }
        }
    }


    @Override
    public void addResource(String path) throws Exception {
        target.addResource(path);
    }

    @Override
    public void addResource(File file) throws Exception {
        target.addResource(file);
    }

    @Override
    public void addResource(Path path) throws Exception {
        target.addResource(path);
    }

    @Override
    public void addResource(URL url) throws Exception {
        target.addResource(url);
    }

    @Override
    public void addResource(Resource resource) throws Exception {
        target.addResource(resource);
    }

    @Override
    public void addResource(ResourceLoader resourceLoader) throws Exception {
        target.addResource(resourceLoader);
    }

    @Override
    public Resource findFirstResource(String name) {
        return target.findFirstResource(name);
    }

    @Override
    public Enumeration<Resource> findAllResource(String name) {
        return target.findAllResource(name);
    }

    @Override
    public InputStream getInputStream(String name) {
        return target.getInputStream(name);
    }

    @Override
    public List<URL> getUrls() {
        return target.getUrls();
    }

    @Override
    public void close() throws Exception {
        target.close();
    }

    @Override
    public void release() throws Exception {
        target.release();
    }
}