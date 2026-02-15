package com.zqzqq.bootkits.core.lock;

import java.time.Duration;

/**
 * No-op cluster lock provider for single instance mode.
 */
public class NoOpClusterLockProvider implements ClusterLockProvider {

    private static final ClusterLock NO_OP_LOCK = () -> {
    };

    @Override
    public ClusterLock acquire(String key, Duration timeout) {
        return NO_OP_LOCK;
    }
}
