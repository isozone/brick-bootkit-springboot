package com.zqzqq.bootkits.core.lock;

import java.time.Duration;

/**
 * Cross-instance lock provider.
 */
public interface ClusterLockProvider {

    /**
     * Acquire a cluster lock by key.
     *
     * @param key lock key
     * @param timeout timeout for acquiring lock
     * @return lock handle
     */
    ClusterLock acquire(String key, Duration timeout);
}
