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
import com.zqzqq.bootkits.loader.utils.ObjectUtils;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 永久缓存
 * 优点: 速度快
 * 缺点: 占用内存非常高
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class CachePerpetualResourceStorage extends AbstractResourceStorage {

    protected final Map<String, List<Resource>> resourceStorage = new ConcurrentHashMap<>();
    
    /**
     * 最大缓存条目数，默认为1000，超过时淘汰最早的条目
     */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * 记录资源添加顺序，用于LRU淘汰
     */
    private final LinkedHashMap<String, Long> accessOrder = new LinkedHashMap<>();

    @Override
    public void addResource(Resource resource) throws Exception {
        resource.resolveByte();
        String name = formatResourceName(resource.getName());
        
        // 检查是否超过最大容量
        if (resourceStorage.size() >= MAX_CACHE_SIZE && !resourceStorage.containsKey(name)) {
            // 淘汰最早的条目
            String oldestKey = accessOrder.keySet().iterator().next();
            resourceStorage.remove(oldestKey);
            accessOrder.remove(oldestKey);
        }
        
        List<Resource> resources = resourceStorage.computeIfAbsent(name, k -> new ArrayList<>());
        resources.add(resource);
        accessOrder.put(name, System.currentTimeMillis());
    }

    @Override
    public boolean exist(String name) {
        if(ObjectUtils.isEmpty(name)){
            return false;
        }
        name = formatResourceName(name);
        return resourceStorage.containsKey(name);
    }

    @Override
    public Resource getFirst(String name) {
        if(ObjectUtils.isEmpty(name)){
            return null;
        }
        name = formatResourceName(name);
        List<Resource> resources = resourceStorage.get(name);
        if(ObjectUtils.isEmpty(resources)){
            return null;
        }
        return resources.get(0);
    }

    @Override
    public Enumeration<Resource> get(String name) {
        if(ObjectUtils.isEmpty(name)){
            return Collections.emptyEnumeration();
        }
        name = formatResourceName(name);
        List<Resource> resources = resourceStorage.get(name);
        if(ObjectUtils.isEmpty(resources)){
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(resources);
    }

    @Override
    public InputStream getFirstInputStream(String name) {
        Resource resource = getFirst(name);
        return openStream(resource);
    }

    @Override
    public Enumeration<InputStream> getInputStream(String name) {
        Enumeration<Resource> resources = get(name);
        if(ObjectUtils.isEmpty(resources)){
            return Collections.emptyEnumeration();
        }
        return openStream(resources);
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (List<Resource> resourceList : resourceStorage.values()) {
            closeResources(resourceList);
        }
        resourceStorage.clear();
        accessOrder.clear();
    }

}

