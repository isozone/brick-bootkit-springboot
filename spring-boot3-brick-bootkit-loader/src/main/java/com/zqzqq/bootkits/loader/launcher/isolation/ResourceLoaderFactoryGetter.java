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



package com.zqzqq.bootkits.loader.launcher.isolation;

import com.zqzqq.bootkits.loader.classloader.resource.loader.DefaultResourceLoaderFactory;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.loader.classloader.resource.storage.AbstractResourceStorage;
import com.zqzqq.bootkits.loader.classloader.resource.storage.CacheFastResourceStorage;
import com.zqzqq.bootkits.loader.classloader.resource.storage.CachePerpetualResourceStorage;
import lombok.Getter;

import java.util.Objects;
import java.util.Properties;

/**
 * 获取ResourceLoaderFactory
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class ResourceLoaderFactoryGetter {

    private static final String PARAMS_KEY = "--resource.store.mode";


    /**
     * 资源存储模式-永久缓存。速度最快，但启动前后占用内存比较内存存储？
     */
    private static final String RESOURCE_MODE_PERPETUAL = "perpetual";

    /**
     * 资源存储模式-快速模式且内存存储相对较低。启动占用内存稍？ 速度比较？ 启动完成后占用内存会降低
     */
    private static final String RESOURCE_MODE_FAST_LOW = "fast-low";


    /**
     * 资源存储模式--缓存可释放模式？
     */
    private static final String RESOURCE_MODE_CACHE_RELEASED = "cache-released";


    private static volatile String resourceMode;


    static ResourceLoaderFactory get(String classLoaderName, String... args){
        if(resourceMode == null){
            synchronized (ResourceLoaderFactory.class){
                if(resourceMode == null){
                    resourceMode = parseArg(args);
                }
            }
        }
        return new DefaultResourceLoaderFactory(classLoaderName);
    }

    private static String parseArg(String... args){
        for (String arg : args) {
            if(arg != null && arg.startsWith(PARAMS_KEY)){
                String[] split = arg.split("=");
                if(split.length == 2){
                    return split[1].trim().toLowerCase();
                }
            }
        }
        return null;
    }

    public static AbstractResourceStorage getResourceStorage(String key){
        if(Objects.equals(resourceMode, RESOURCE_MODE_PERPETUAL)){
            System.out.println("[ResourceLoaderFactoryGetter] 使用 CachePerpetualResourceStorage (永久缓存模式)");
            return new CachePerpetualResourceStorage();
        } else {
            System.out.println("[ResourceLoaderFactoryGetter] 使用 CacheFastResourceStorage (快速缓存模式), key=" + key);
            return new CacheFastResourceStorage(key);
        }
    }

    @Getter
    public static class PluginMeta{
        private final String packageType;
        private final Properties properties;

        public PluginMeta(String packageType, Properties properties) {
            this.packageType = packageType;
            this.properties = properties;
        }
    }

}

