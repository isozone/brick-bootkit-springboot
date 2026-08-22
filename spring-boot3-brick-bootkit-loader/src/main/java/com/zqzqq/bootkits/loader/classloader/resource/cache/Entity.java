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

/**
 * 缓存鍏冪礌
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public class Entity<V> {

    protected final V value;
    protected final long ttl;
    protected long lastAccessTimestamp;

    public Entity(V value, long ttl) {
        this.value = value;
        this.ttl = ttl;

        this.lastAccessTimestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
        if (ttl == 0) {
            return false;
        }
        return lastAccessTimestamp + ttl < System.currentTimeMillis();
    }

    public V getValue() {
        lastAccessTimestamp = System.currentTimeMillis();
        return value;
    }

}

