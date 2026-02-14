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

package com.zqzqq.bootkits.loader.classloader.resource.cache;

import java.util.function.Supplier;

/**
 * 缓存过期调度接口
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public interface CacheExpirationTrigger {

    /**
     * 添加缓存过期调度
     * @param key 缓存的key
     * @param cache 缓存对象
     */
    void addCache(String key, Cache<?, ?> cache);

    /***
     * 获取缓存
     * @param key 缓存的key
     * @param cacheSupplier 不存在时提供者，并添加到缓存中
     * @return 缓存
     * @param <K> K
     * @param <V> V
     */
    <K, V> Cache<K, V> getCache(String key, Supplier<Cache<K, V>> cacheSupplier);

    /**
     * 移除缓存过期调度
     * @param key 缓存的key
     */
    void removeCache(String key);

    /**
     * 启动调度
     */
    void start();

    /**
     * 停止调度
     */
    void stop();

}

