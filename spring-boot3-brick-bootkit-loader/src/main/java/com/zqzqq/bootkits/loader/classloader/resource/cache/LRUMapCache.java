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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * LRU 缂撳瓨瀹炵幇
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public class LRUMapCache<K, V> implements Cache<K, V>{

    private final Map<K, Entity<V>> cacheMap;

    private final StampedLock lock = new StampedLock();

    private final int size;
    private final long timeout;

    public LRUMapCache(int size, long timeout){
        this.size = size;
        this.timeout = timeout;
        this.cacheMap = new CacheLinkedHashMap<K, Entity<V>>(size);
    }

    @Override
    public void put(K key, V value) {
        long stamp = lock.writeLock();
        try {
            Entity<V> entity = new Entity<>(value, timeout);
            if (isFull(key)) {
                cleanExpired(false);
            }
            cacheMap.put(key, entity);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int size = cacheMap.size();
        if(!lock.validate(stamp)){
            stamp = lock.readLock();
            try {
                size = cacheMap.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public V get(K key) {
        long stamp = lock.tryOptimisticRead();
        Entity<V> entity = cacheMap.get(key);
        if(!lock.validate(stamp)){
            stamp = lock.readLock();
            try {
                entity = cacheMap.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        if(entity != null){
            if(entity.isExpired()){
                remove(key);
                return null;
            }
            return entity.getValue();
        }
        return null;
    }

    @Override
    public V getOrDefault(K key, Supplier<V> supplier, boolean defaultAdded) {
        long stamp = lock.tryOptimisticRead();
        Entity<V> entity = cacheMap.get(key);
        if(!lock.validate(stamp)){
            stamp = lock.readLock();
            try {
                entity = cacheMap.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        if(entity != null){
            if(entity.isExpired()){
                remove(key);
            } else {
                return entity.getValue();
            }
        }

        V v = supplier.get();
        if(v != null){
            if(defaultAdded){
                put(key, v);
            }
        }
        return v;
    }

    @Override
    public V remove(K key) {
        long stamp = lock.writeLock();
        try {
            Entity<V> cacheValue = cacheMap.remove(key);
            if (cacheValue != null) {
                return cacheValue.getValue();
            } else {
                return null;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void clear() {
        clear(null);
    }

    @Override
    public void clear(Consumer<V> consumer) {
        long stamp = lock.writeLock();
        try {
            if(consumer == null){
                cacheMap.clear();
                return;
            }
            Iterator<Map.Entry<K, Entity<V>>> iterator = cacheMap.entrySet().iterator();
            while (iterator.hasNext()){
                try {
                    Map.Entry<K, Entity<V>> entityEntry = iterator.next();
                    Entity<V> value = entityEntry.getValue();
                    if(value == null){
                        iterator.remove();
                        continue;
                    }
                    V v = value.getValue();
                    if(v == null){
                        iterator.remove();
                        continue;
                    }
                    consumer.accept(v);
                } catch (Exception e){
                    // 忽略
                }
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public int cleanExpired() {
        return cleanExpired(true);
    }

    private boolean isFull(K key) {
        if (size == 0) {
            return false;
        }
        if(cacheMap.size() < size){
            return false;
        }
        return !cacheMap.containsKey(key);
    }

    public int cleanExpired(boolean isLock) {
        if(!isLock){
            return actualCleanExpired();
        }
        long stamp = lock.writeLock();
        try {
            return actualCleanExpired();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private int actualCleanExpired(){
        Iterator<Map.Entry<K, Entity<V>>> cacheMapIterator = cacheMap.entrySet().iterator();
        int removeCount = 0;
        while (cacheMapIterator.hasNext()){
            Map.Entry<K, Entity<V>> entityMap = cacheMapIterator.next();
            Entity<V> value = entityMap.getValue();
            if(value == null || value.isExpired()){
                cacheMapIterator.remove();
                removeCount++;
            }
        }
        return removeCount;
    }

}

