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

import com.zqzqq.bootkits.core.exception.PluginException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * File-system based cluster lock provider.
 */
public class FileClusterLockProvider implements ClusterLockProvider {

    private final Path lockDirectory;

    public FileClusterLockProvider(Path lockDirectory) {
        this.lockDirectory = lockDirectory;
        try {
            Files.createDirectories(lockDirectory);
        } catch (IOException e) {
            throw new PluginException("Create cluster lock directory failed: " + lockDirectory, e);
        }
    }

    @Override
    public ClusterLock acquire(String key, Duration timeout) {
        String fileName = sanitizeKey(key) + ".lck";
        Path lockFile = lockDirectory.resolve(fileName);
        long deadline = System.nanoTime() + timeout.toNanos();

        try {
            FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            while (true) {
                try {
                    FileLock lock = channel.tryLock();
                    if (lock != null) {
                        return new FileClusterLock(channel, lock);
                    }
                } catch (OverlappingFileLockException ignored) {
                    // Lock already held in current JVM.
                }

                if (System.nanoTime() >= deadline) {
                    channel.close();
                    throw new PluginException("Acquire cluster lock timeout: " + key);
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    channel.close();
                    throw new PluginException("Acquire cluster lock interrupted: " + key, e);
                }
            }
        } catch (IOException e) {
            throw new PluginException("Acquire cluster lock failed: " + key, e);
        }
    }

    private String sanitizeKey(String key) {
        if (key == null || key.isEmpty()) {
            return "unknown";
        }
        return key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static final class FileClusterLock implements ClusterLock {
        private final FileChannel channel;
        private final FileLock lock;

        private FileClusterLock(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public void close() {
            try {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
            } catch (IOException ignored) {
                // ignore
            } finally {
                try {
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
}
