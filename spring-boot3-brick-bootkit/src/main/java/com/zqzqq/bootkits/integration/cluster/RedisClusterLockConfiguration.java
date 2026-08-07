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
