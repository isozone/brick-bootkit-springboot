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


package com.zqzqq.bootkits.core.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 插件锁管理器
 */
public class PluginLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> pluginLocks = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    public void executeWithGlobalWriteLock(Runnable action) {
        globalLock.writeLock().lock();
        try {
            action.run();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    public <T> T executeWithGlobalWriteLock(java.util.function.Supplier<T> action) {
        globalLock.writeLock().lock();
        try {
            return action.get();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    public void executeWithGlobalReadLock(Runnable action) {
        globalLock.readLock().lock();
        try {
            action.run();
        } finally {
            globalLock.readLock().unlock();
        }
    }

    public <T> T executeWithGlobalReadLock(java.util.function.Supplier<T> action) {
        globalLock.readLock().lock();
        try {
            return action.get();
        } finally {
            globalLock.readLock().unlock();
        }
    }

    public void executeWithPluginLock(String pluginId, Runnable action) {
        ReentrantLock lock = pluginLocks.computeIfAbsent(pluginId, k -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                pluginLocks.remove(pluginId, lock);
            }
        }
    }

    public <T> T executeWithPluginLock(String pluginId, java.util.function.Supplier<T> action) {
        ReentrantLock lock = pluginLocks.computeIfAbsent(pluginId, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                pluginLocks.remove(pluginId, lock);
            }
        }
    }

    public void executeWithMultiPluginLock(List<String> pluginIds, Runnable action) {
        List<String> sortedIds = new ArrayList<>(pluginIds);
        Collections.sort(sortedIds);
        
        List<ReentrantLock> acquiredLocks = new ArrayList<>();
        try {
            for (String id : sortedIds) {
                ReentrantLock lock = pluginLocks.computeIfAbsent(id, k -> new ReentrantLock());
                lock.lock();
                acquiredLocks.add(lock);
            }
            action.run();
        } finally {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                ReentrantLock lock = acquiredLocks.get(i);
                lock.unlock();
                if (!lock.hasQueuedThreads()) {
                    pluginLocks.remove(sortedIds.get(i), lock);
                }
            }
        }
    }
}
