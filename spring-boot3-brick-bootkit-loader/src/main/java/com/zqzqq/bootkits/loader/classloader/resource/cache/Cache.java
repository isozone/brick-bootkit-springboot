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



package com.zqzqq.bootkits.loader.classloader.resource.cache;


import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 缓存接口
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public interface Cache<K, V> {

    /**
     * 缓存数据
     * @param key 缓存的key
     * @param value 缓存的值
     */
    void put(K key, V value);

    /**
     * 缓存大小
     * @return int
     */
    int size();

    /**
     * 是否存在缓存
     * @param key 缓存key
     * @return true: 存在, false: 不存在
     */
    boolean containsKey(K key);

    /**
     * 获取缓存值
     * @param key 缓存的key
     * @return 缓存值不存在则返回null
     */
    V get(K key);

    /**
     * 得到缓存值。如果不存在支付默认的
     * @param key 缓存的key
     * @param supplier 默认值
     * @param defaultAdded 如果不存在则默认的添加到缓存中
     * @return V
     */
    V getOrDefault(K key, Supplier<V> supplier, boolean defaultAdded);

    /**
     * 移除缓存
     * @param key 缓存的key
     * @return 移除的值
     */
    V remove(K key);

    /**
     * 清理过期的缓存
     * @return 清理的个数
     */
    int cleanExpired();

    /**
     * 清理全部缓存
     */
    void clear();

    /**
     * 依次删除
     * @param consumer 消费
     */
    void clear(Consumer<V> consumer);

}