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



package com.zqzqq.bootkits.loader.classloader;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.DefaultResource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * classloader 閺夌儐鍓氬畷鏌ュ闯?
 *
 * @author starBlues
 * @version 4.0.0
 * @since 3.0.4
 */
public class ClassLoaderTranslator implements ResourceLoaderFactory {

    private final URLClassLoader classLoader;

    public ClassLoaderTranslator(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void addResource(String path) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public void addResource(File file) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public void addResource(Path path) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public void addResource(URL url) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public void addResource(Resource resource) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public void addResource(ResourceLoader resourceLoader) throws Exception {
        throw new RuntimeException("Does not support!");
    }

    @Override
    public Resource findFirstResource(String name) {
        URL url = classLoader.getResource(name);
        if(url == null){
            return null;
        }
        return new DefaultResource(name, url, url);
    }

    @Override
    public Enumeration<Resource> findAllResource(String name) {
        try {
            Enumeration<URL> resources = classLoader.getResources(name);
            return new Enumeration<Resource>() {
                @Override
                public boolean hasMoreElements() {
                    return resources.hasMoreElements();
                }

                @Override
                public Resource nextElement() {
                    URL url = resources.nextElement();
                    if(url == null){
                        return null;
                    }
                    return new DefaultResource(name, url, url);
                }
            };
        } catch (IOException e) {
            return Collections.emptyEnumeration();
        }
    }

    @Override
    public InputStream getInputStream(String name) {
        return classLoader.getResourceAsStream(name);
    }

    @Override
    public List<URL> getUrls() {
        return Arrays.asList(classLoader.getURLs());
    }

    @Override
    public void close() throws Exception {

    }
}