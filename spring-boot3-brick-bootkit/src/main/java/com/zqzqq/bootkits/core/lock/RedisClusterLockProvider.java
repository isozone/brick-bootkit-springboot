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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁实现。
 * <p>
 * 基于 Redis SET NX EX 原子命令实现跨实例锁，锁持有者通过唯一 token 标识，
 * 释放时使用 Lua 脚本保证"只有持有者才能释放"，避免误删他人锁。
 * <p>
 * 依赖 {@code spring-boot-starter-data-redis}（optional），宿主引入 Redis 依赖
 * 并通过 {@code plugin.clusterLockProviderBeanName} 指定本 Bean 后生效。
 */
public class RedisClusterLockProvider implements ClusterLockProvider {

    private static final String LOCK_PREFIX = "brick-bootkit:lock:";

    /**
     * 释放锁的 Lua 脚本：仅当 key 的 value 等于持有者 token 时才删除。
     */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisClusterLockProvider(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ClusterLock acquire(String key, Duration timeout) {
        String lockKey = LOCK_PREFIX + sanitizeKey(key);
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + timeout.toNanos();

        try {
            while (true) {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, token, timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (Boolean.TRUE.equals(acquired)) {
                    return new RedisClusterLock(redisTemplate, lockKey, token);
                }

                if (System.nanoTime() >= deadline) {
                    throw new PluginException("Acquire redis cluster lock timeout: " + key);
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PluginException("Acquire redis cluster lock interrupted: " + key, e);
                }
            }
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException("Acquire redis cluster lock failed: " + key, e);
        }
    }

    private String sanitizeKey(String key) {
        if (key == null || key.isEmpty()) {
            return "unknown";
        }
        return key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Redis 锁句柄：关闭时通过 Lua 脚本安全释放。
     */
    private static final class RedisClusterLock implements ClusterLock {

        private final StringRedisTemplate redisTemplate;
        private final String lockKey;
        private final String token;
        private volatile boolean released = false;

        private RedisClusterLock(StringRedisTemplate redisTemplate, String lockKey, String token) {
            this.redisTemplate = redisTemplate;
            this.lockKey = lockKey;
            this.token = token;
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            released = true;
            try {
                redisTemplate.execute(RELEASE_SCRIPT,
                        Collections.singletonList(lockKey),
                        Collections.singletonList(token));
            } catch (Exception ignored) {
                // 释放失败不抛出，锁会随 TTL 自动过期
            }
        }
    }
}
