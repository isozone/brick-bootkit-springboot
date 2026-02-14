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


import java.util.Collection;

/**
 * 缓存接口
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public interface MultiCache<K, V> extends Cache<K, Collection<V>>{

    /**
     * put 单个值
     * @param key 缓存的key
     * @param value 缓存的值
     */
    void putSingle(K key, V value);

    /**
     * 得到第一个 value
     * @param key 缓存的key
     * @return 第一个 value
     */
    V getFirst(K key);

}

