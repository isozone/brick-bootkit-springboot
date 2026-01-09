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

package com.zqzqq.bootkits.loader.classloader;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.loader.classloader.resource.storage.EmptyResourceStorage;
import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;

/**
 * 通用的Url ClassLoader
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class GeneralUrlClassLoader extends URLClassLoader implements ResourceLoaderFactory {

    private static final Logger log = LoggerFactory.getLogger(GeneralUrlClassLoader.class);
    private final String name;
    private final ResourceLoaderFactory classLoaderTranslator;

    private final ResourceStorage resourceStorage = new EmptyResourceStorage();

    public void mergeResources(URLClassLoader sourceLoader) {
        try {
            if (sourceLoader != null) {
                for (URL url : sourceLoader.getURLs()) {
                    if (url != null) {
                        this.addResource(url);
                        log.debug("Merged resource: {}", url);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Resource merge warning: {}", e.getMessage(), e);
        }
    }

    public GeneralUrlClassLoader(String name, ClassLoader parent) {
        super(new URL[]{}, parent);
        this.name = name;
        this.classLoaderTranslator = new ClassLoaderTranslator(this);
    }

    public String getName() {
        return name;
    }

    @Override
    public void addResource(String path) throws Exception {
        addResource(Paths.get(path));
    }

    @Override
    public void addResource(File file) throws Exception {
        if(!file.exists()){
            throw new FileNotFoundException("Not found file:" + file.getPath());
        }
        addURL(file.toPath().toUri().toURL());
    }

    @Override
    public void addResource(Path path) throws Exception {
        addResource(path.toFile());
    }

    @Override
    public void addResource(URL url) throws Exception {
        super.addURL(url);
    }

    @Override
    public void addResource(Resource resource) throws Exception {
        addResource(resource.getUrl());
    }

    @Override
    public void addResource(ResourceLoader resourceLoader) throws Exception {
        resourceLoader.load(resourceStorage);
    }

    @Override
    public Resource findFirstResource(String name) {
        return classLoaderTranslator.findFirstResource(name);
    }

    @Override
    public Enumeration<Resource> findAllResource(String name) {
        return classLoaderTranslator.findAllResource(name);
    }

    @Override
    public InputStream getInputStream(String name) {
        return classLoaderTranslator.getInputStream(name);
    }

    @Override
    public List<URL> getUrls() {
        return classLoaderTranslator.getUrls();
    }
}