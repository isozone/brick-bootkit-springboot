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

package com.zqzqq.bootkits.loader.classloader.resource.storage;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.cache.CacheExpirationTrigger;
import com.zqzqq.bootkits.loader.classloader.resource.cache.DefaultCacheExpirationTrigger;
import com.zqzqq.bootkits.loader.classloader.resource.cache.LRUMultiMapUnifiedListCache;
import com.zqzqq.bootkits.loader.classloader.resource.cache.MultiCache;
import com.zqzqq.bootkits.loader.utils.ObjectUtils;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * 快速且可缓存的资源存储
 * 优点: 释放前速度比较快, 释放后可根据LRU缓存机制进行缓存
 * 缺点: 释放前占用内存比较高
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.2
 */
public class CacheFastResourceStorage extends AbstractResourceStorage {

    protected final MultiCache<String, Resource> resourceStorage;
    private final ResourceStorage cacheResourceStorage;

    private volatile boolean release = false;

    @SuppressWarnings("all")
    public CacheFastResourceStorage(String key) {
        this.cacheResourceStorage = new CachePerpetualResourceStorage();
        // 链容量1000, 过期时间3分钟
        CacheExpirationTrigger trigger = DefaultCacheExpirationTrigger
                .getCacheExpirationTrigger(3, TimeUnit.MINUTES);
        resourceStorage = (MultiCache<String, Resource>) trigger.getCache(key,
                () -> new LRUMultiMapUnifiedListCache<String, Resource>(1000, 180000));
    }

    @Override
    protected void addResource(Resource resource) throws Exception {
        if(!release){
            cacheResourceStorage.add(resource);
            return;
        }
        resource.resolveByte();
        String name = formatResourceName(resource.getName());
        resourceStorage.putSingle(name, resource);
    }

    @Override
    public boolean exist(String name) {
        if(!release){
            return cacheResourceStorage.exist(name);
        }
        return getFirst(name) != null;
    }

    @Override
    public Resource getFirst(String name) {
        if(!release){
            return cacheResourceStorage.getFirst(name);
        }
        if(ObjectUtils.isEmpty(name)){
            return null;
        }
        name = formatResourceName(name);
        Resource firstResource = resourceStorage.getFirst(name);
        if(firstResource != null){
            return firstResource;
        }
        return searchResource(name);
    }

    @Override
    public Enumeration<Resource> get(String name) {
        if(!release){
            return cacheResourceStorage.get(name);
        }
        if(ObjectUtils.isEmpty(name)){
            return Collections.emptyEnumeration();
        }
        name = formatResourceName(name);
        Collection<Resource> resources = resourceStorage.get(name);
        if(!ObjectUtils.isEmpty(resources)){
            return Collections.enumeration(resources);
        }
        return searchResources(name);
    }

    @Override
    public InputStream getFirstInputStream(String name) {
        Resource resource = getFirst(name);
        return openStream(resource);
    }

    @Override
    public Enumeration<InputStream> getInputStream(String name) {
        if(!release){
            return cacheResourceStorage.getInputStream(name);
        }
        Enumeration<Resource> resources = get(name);
        if(!ObjectUtils.isEmpty(resources)){
            return openStream(resources);
        }
        Enumeration<Resource> searchResources = searchResources(name);
        return openStream(searchResources);
    }

    @Override
    public synchronized void release() throws Exception {
        if(!release){
            try {
                // 确保缓存资源存储被正确关闭
                if (cacheResourceStorage != null) {
                    cacheResourceStorage.close();
                }
            } catch (Exception e) {
                // 忽略异常，确保继续执行
            }
        }
        
        // 清理过期资源
        try {
            if (resourceStorage != null) {
                resourceStorage.cleanExpired();
            }
        } catch (Exception e) {
            // 忽略异常，确保继续执行
        }
        
        release = true;
    }

    @Override
    public void close() throws Exception {
        try {
            // 清理并释放所有资源
            if (resourceStorage != null) {
                resourceStorage.clear(resource -> {
                    if (resource != null) {
                        ResourceUtils.release(resource);
                    }
                });
            }
            
            // 关闭缓存资源存储
            if (cacheResourceStorage != null) {
                try {
                    cacheResourceStorage.close();
                } catch (Exception e) {
                    // 忽略异常，确保继续执行
                }
            }
            
            // 调用父类关闭方法
            super.close();
        } catch (Exception e) {
            throw new Exception("关闭资源存储异常", e);
        }
    }


}

