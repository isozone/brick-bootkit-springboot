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



package com.zqzqq.bootkits.loader.classloader.resource.storage;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.DefaultResource;
import com.zqzqq.bootkits.loader.utils.IOUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 插件的资源存储类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 4.0.0
 */
public abstract class AbstractResourceStorage implements ResourceStorage {

    private static final Logger log = LoggerFactory.getLogger(AbstractResourceStorage.class);
    private final Set<URL> baseUrls = new HashSet<>();
    private final ArrayDeque<URL> hotUrls = new ArrayDeque<>();
    private final List<InputStream> inputStreams = new CopyOnWriteArrayList<>();

    @Override
    public void addBaseUrl(URL url){
        if(url == null){
            return;
        }
        synchronized (baseUrls){
            baseUrls.add(url);
        }
    }

    @Override
    public List<URL> getBaseUrl(){
        synchronized (baseUrls){
            return Collections.unmodifiableList(new ArrayList<>(baseUrls));
        }
    }


    @Override
    public final void add(Resource resource) throws Exception {
        addResource(resource);
    }

    @Override
    public void close() throws Exception {
        for (InputStream inputStream : inputStreams) {
            IOUtils.closeQuietly(inputStream);
        }
        inputStreams.clear();
        hotUrls.clear();
        baseUrls.clear();
    }

    /**
     * 子类添加资源
     * @param resource 资源
     * @throws Exception 添加移除
     */
    protected abstract void addResource(Resource resource) throws Exception;

    /**
     * 格式化资源名称
     * @param name 资源名称
     * @return String
     */
    protected final String formatResourceName(String name) {
        return ResourceUtils.formatStandardName(name);
    }

    protected final InputStream openStream(Resource resource){
        if(resource == null){
            return null;
        }
        try {
            InputStream inputStream = resource.getUrl().openStream();
            inputStreams.add(inputStream);
            return inputStream;
        } catch (IOException e) {
            log.error("Failed to open stream for resource: {}", resource.getName(), e);
            return null;
        }
    }

    protected final Enumeration<InputStream> openStream(Enumeration<Resource> resources){
        if(resources == null){
            return Collections.emptyEnumeration();
        }
        return new Enumeration<InputStream>() {
            @Override
            public boolean hasMoreElements() {
                return resources.hasMoreElements();
            }

            @Override
            public InputStream nextElement() {
                Resource resource = resources.nextElement();
                return openStream(resource);
            }
        };
    }

    protected final void closeResources(List<Resource> resources){
        if(ObjectUtils.isEmpty(resources)){
            return;
        }
        for (Resource resource : resources) {
            IOUtils.closeQuietly(resource);
        }
        resources.clear();
    }

    /**
     * 搜索单个资源。优先从热缓存中查找，其次查找所有基础URL。
     * 
     * @param name 资源名称
     * @return 找到的资源，未找到返回 null
     */
    protected final synchronized Resource searchResource(String name) {
        // 1. 优先从热缓存中查找
        URL matchBaseUrl = searchInHotUrls(name);
        
        // 2. 如果热缓存未找到，从所有基础URL中查找
        if (matchBaseUrl == null) {
            matchBaseUrl = searchInBaseUrls(name);
        }
        
        // 3. 创建并返回资源
        if (matchBaseUrl != null) {
            return createAndAddResource(name, matchBaseUrl);
        }
        
        return null;
    }

    /**
     * 在热缓存中搜索资源
     * @param name 资源名称
     * @return 匹配的基础URL，未找到返回null
     */
    private URL searchInHotUrls(String name) {
        List<URL> searched = new ArrayList<>();
        
        for (int i = 0; i < hotUrls.size(); i++) {
            URL baseUrl = hotUrls.pollFirst();
            if (baseUrl == null) {
                break;
            }
            
            searched.add(baseUrl);
            
            if (ResourceUtils.getExistUrl(baseUrl, name) != null) {
                // 找到，将搜索过的URL按顺序放回热缓存
                for (URL url : searched) {
                    hotUrls.addLast(url);
                }
                hotUrls.addFirst(baseUrl);
                return baseUrl;
            }
        }
        
        // 未找到，将搜索过的URL按顺序放回热缓存
        for (URL url : searched) {
            hotUrls.addLast(url);
        }
        return null;
    }

    /**
     * 在所有基础URL中搜索资源
     * @param name 资源名称
     * @return 匹配的基础URL，未找到返回null
     */
    private URL searchInBaseUrls(String name) {
        List<URL> baseUrls = getBaseUrl();
        
        for (URL baseUrl : baseUrls) {
            if (ResourceUtils.getExistUrl(baseUrl, name) != null) {
                // 找到，添加到热缓存
                hotUrls.addFirst(baseUrl);
                return baseUrl;
            }
        }
        
        return null;
    }

    /**
     * 创建并添加资源
     * @param name 资源名称
     * @param baseUrl 基础URL
     * @return 创建的资源
     */
    private Resource createAndAddResource(String name, URL baseUrl) {
        URL existUrl = ResourceUtils.getExistUrl(baseUrl, name);
        try {
            Resource resource = new DefaultResource(name, baseUrl, existUrl);
            addResource(resource);
            return resource;
        } catch (Exception e) {
            log.warn("Failed to add resource to storage: {}", name, e);
            return null;
        }
    }
    protected final Enumeration<Resource> searchResources(String name){
        List<URL> baseUrls = getBaseUrl();
        return new InternalEnumeration(baseUrls, name);
    }


    private class InternalEnumeration implements Enumeration<Resource>{

        private final List<URL> baseUrls;
        private final String name;

        private int index = 0;
        private Resource resource = null;

        private InternalEnumeration(List<URL> baseUrls, String name) {
            this.baseUrls = baseUrls;
            this.name = name;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }
        @Override
        public Resource nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            Resource r = resource;
            resource = null;
            return r;
        }

        private boolean next() {
            if (resource != null) {
                return true;
            } else {
                URL baseUrl;
                while (index < baseUrls.size()){
                    baseUrl = baseUrls.get(index++);
                    resource = getResource(baseUrl);
                    if(resource != null){
                        return true;
                    }
                }
                return false;
            }
        }

        private Resource getResource(URL baseUrl){
            URL existUrl = ResourceUtils.getExistUrl(baseUrl, name);
            if(existUrl == null){
                return null;
            }
            Resource resource = new DefaultResource(name, baseUrl, existUrl);
            addResourceWrapper(resource);
            return resource;
        }
    }

    private void addResourceWrapper(Resource resource){
        if(resource == null){
            return;
        }
        try {
            addResource(resource);
        } catch (Exception e){
            log.warn("Failed to add resource wrapper: {}", resource.getName(), e);
        }
    }
}
