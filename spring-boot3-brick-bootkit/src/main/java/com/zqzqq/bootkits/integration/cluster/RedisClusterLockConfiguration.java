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


package com.zqzqq.bootkits.integration.cluster;

import com.zqzqq.bootkits.core.lock.ClusterLockProvider;
import com.zqzqq.bootkits.core.lock.RedisClusterLockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 分布式锁自动装配。
 * <p>
 * 独立配置类：仅当 classpath 中存在 StringRedisTemplate（宿主引入了
 * spring-boot-starter-data-redis）时才加载，避免无 Redis 依赖时
 * 因方法签名引用 Redis 类导致类加载失败。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
public class RedisClusterLockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ClusterLockProvider redisClusterLockProvider(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        return new RedisClusterLockProvider(redisTemplate);
    }
}
