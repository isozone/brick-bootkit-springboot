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

package com.zqzqq.bootkits.loader.classloader.resource.loader;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;
import com.zqzqq.bootkits.loader.launcher.isolation.ResourceLoaderFactoryGetter;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;

/**
 * 默认的资源加载加工厂?
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class DefaultResourceLoaderFactory implements ResourceLoaderFactory{

    private final ResourceStorage resourceStorage;

    public DefaultResourceLoaderFactory(String classLoaderName) {
        resourceStorage = ResourceLoaderFactoryGetter.getResourceStorage(classLoaderName);
    }

    @Override
    public void addResource(String path) throws Exception{
        if(path == null || "".equals(path)){
            return;
        }
        addResource(Paths.get(path));
    }

    @Override
    public void addResource(File file) throws Exception{
        if(file == null){
            return;
        }
        addResource(file.toPath());
    }

    @Override
    public void addResource(Path path) throws Exception{
        if(path == null){
            return;
        }
        if(!Files.exists(path)){
            return;
        }
        addResource(path.toUri().toURL());
    }

    @Override
    public void addResource(URL url) throws Exception{
        AbstractResourceLoader resourceLoader = null;
        if(ResourceUtils.isJarFileUrl(url)) {
            if(ResourceUtils.isJarProtocolUrl(url)){
                resourceLoader = new JarResourceLoader(url);
            } else {
                resourceLoader = new JarResourceLoader(Paths.get(url.toURI()).toFile());
            }
        } else if(ResourceUtils.isZipFileUrl(url)){
            resourceLoader = new JarResourceLoader(Paths.get(url.toURI()).toFile());
        } else if(ResourceUtils.isFileUrl(url)){
            resourceLoader = new ClassPathLoader(url);
        }
        if(resourceLoader != null){
            addResource(resourceLoader);
        }
    }

    @Override
    public void addResource(Resource resource) throws Exception {
        resourceStorage.addBaseUrl(resource.getBaseUrl());
        resourceStorage.add(resource);
    }

    @Override
    public synchronized void addResource(ResourceLoader resourceLoader) throws Exception {
        if(resourceLoader == null){
            return;
        }
        resourceStorage.addBaseUrl(resourceLoader.getBaseUrl());
        resourceLoader.load(resourceStorage);
    }

    @Override
    public Resource findFirstResource(String name) {
        return resourceStorage.getFirst(name);
    }

    @Override
    public Enumeration<Resource> findAllResource(String name) {
        return resourceStorage.get(name);
    }

    @Override
    public InputStream getInputStream(String name) {
        return resourceStorage.getFirstInputStream(name);
    }

    @Override
    public List<URL> getUrls() {
        return resourceStorage.getBaseUrl();
    }

    @Override
    public void close() throws Exception {
        try {
            if (resourceStorage != null) {
                resourceStorage.close();
            }
        } catch (Exception e) {
            throw new Exception("关闭资源存储异常", e);
        }
    }

    @Override
    public void release() throws Exception {
        try {
            if (resourceStorage != null) {
                resourceStorage.release();
            }
        } catch (Exception e) {
            throw new Exception("释放资源存储异常", e);
        }
    }
}

