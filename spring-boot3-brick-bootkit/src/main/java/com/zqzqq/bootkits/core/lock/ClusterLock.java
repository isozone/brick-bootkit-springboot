package com.zqzqq.bootkits.core.lock;

/**
 * Cluster lock handle for cross-instance synchronization.
 */
public interface ClusterLock extends AutoCloseable {

    @Override
    void close();
}
