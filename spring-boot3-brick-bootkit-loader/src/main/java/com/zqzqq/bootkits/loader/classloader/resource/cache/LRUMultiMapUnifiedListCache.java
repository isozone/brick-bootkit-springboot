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

import com.zqzqq.bootkits.utils.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * LRU MultiMap List 缓存实现，list 集合中的元素统一过期
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public class LRUMultiMapUnifiedListCache<K, V> implements MultiCache<K, V>{

    private final Map<K, EntityList> cacheMap;

    private final StampedLock lock = new StampedLock();

    private final int maxSize;
    private final long timeout;

    private final AtomicInteger size = new AtomicInteger(0);

    public LRUMultiMapUnifiedListCache(int maxSize, long timeout){
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.cacheMap = new CacheLinkedHashMap<K, EntityList>(maxSize, entry->{
            EntityList value = entry.getValue();
            if(value != null){
                size.addAndGet(-value.size());
            } else {
                size.addAndGet(-1);
            }
        });
    }

    @Override
    public void put(K key, Collection<V> value) {
        if(ObjectUtils.isEmpty(value)){
            return;
        }
        long stamp = lock.writeLock();
        try {
            if (isFull(key)) {
                cleanExpired(false);
            }
            EntityList entityList = cacheMap.computeIfAbsent(key, k -> new EntityList(timeout));
            entityList.add(value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putSingle(K key, V value) {
        put(key, ObjectUtils.<V>toList(value));
    }

    @Override
    public V getFirst(K key) {
        Collection<V> collection = get(key);
        return ObjectUtils.getFirst(collection);
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int s = size.get();
        if(!lock.validate(stamp)){
            stamp = lock.readLock();
            try {
                s = size.get();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return s;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public Collection<V> get(K key) {
        long stamp = lock.tryOptimisticRead();
        EntityList entityList = cacheMap.get(key);
        if(!lock.validate(stamp)){
            stamp = lock.readLock();
            try {
                entityList = cacheMap.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        if(entityList == null){
            return null;
        }
        if(entityList.isExpired()){
            remove(key);
            return null;
        }
        return Collections.unmodifiableList(entityList.getValue());
    }

    @Override
    public Collection<V> getOrDefault(K key, Supplier<Collection<V>> supplier, boolean defaultAdded) {
        Collection<V> collection = get(key);
        if(!ObjectUtils.isEmpty(collection)){
            return collection;
        }
        Collection<V> addCollection = supplier.get();
        if(ObjectUtils.isEmpty(addCollection)){
            return null;
        }
        if(defaultAdded){
            put(key, addCollection);
            return addCollection;
        }
        return null;
    }

    @Override
    public Collection<V> remove(K key) {
        long stamp = lock.writeLock();
        try {
            EntityList cacheValue = cacheMap.remove(key);
            if (cacheValue == null) {
                return null;
            }
            size.addAndGet(-cacheValue.size());
            return cacheValue.getValue();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void clear() {
        clear(null);
    }

    @Override
    public void clear(Consumer<Collection<V>> consumer) {
        long stamp = lock.writeLock();
        try {
            if(consumer == null){
                cacheMap.clear();
                return;
            }
            Iterator<Map.Entry<K, EntityList>> iterator = cacheMap.entrySet().iterator();
            while (iterator.hasNext()){
                try {
                    Map.Entry<K, EntityList> entityMap = iterator.next();
                    EntityList entityList = entityMap.getValue();
                    if(entityList == null){
                        iterator.remove();
                        continue;
                    }
                    List<V> value = entityList.getValue();
                    if(value != null){
                        consumer.accept(value);
                    }
                    iterator.remove();
                } catch (Exception e){
                    // 忽略
                }
            }
        } finally {
            size.set(0);
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public int cleanExpired() {
        return cleanExpired(true);
    }

    private boolean isFull(K key) {
        if (maxSize == 0) {
            return false;
        }
        if(cacheMap.size() < maxSize){
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
        Iterator<Map.Entry<K, EntityList>> cacheMapIterator = cacheMap.entrySet().iterator();
        int removeCount = 0;
        while (cacheMapIterator.hasNext()){
            Map.Entry<K, EntityList> entityMap = cacheMapIterator.next();
            EntityList value = entityMap.getValue();
            if(value == null){
                cacheMapIterator.remove();
            } else {
                if(value.isExpired()){
                    int valueSize = value.size();
                    cacheMapIterator.remove();
                    size.addAndGet(-valueSize);
                    removeCount = removeCount + valueSize;
                }
            }
        }
        return removeCount;
    }

    private class EntityList extends Entity<List<V>> implements AutoCloseable{

        public EntityList(long ttl) {
            super(new ArrayList<>(), ttl);
        }

        public EntityList(List<V> list, long ttl) {
            super(list, ttl);
        }

        public synchronized void add(V v){
            getValue().add(v);
            size.addAndGet(1);
        }

        public synchronized void add(Collection<V> list){
            if(ObjectUtils.isEmpty(list)){
                return;
            }
            getValue().addAll(list);
            size.addAndGet(list.size());
        }

        public synchronized V getFirst(){
            List<V> list = getValue();
            if(ObjectUtils.isEmpty(list)){
                return null;
            }
            return list.get(0);
        }

        public synchronized int size(){
            return getValue().size();
        }

        @Override
        public synchronized void close() throws Exception {
            List<V> value = getValue();
            size.addAndGet(-value.size());
            value.clear();
        }
    }

}

