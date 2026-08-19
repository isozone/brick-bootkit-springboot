package com.zqzqq.bootkits.distributed.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.distributed.metrics.DistributedMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式服务目录。
 * <p>
 * 实现职责：
 * <ul>
 *   <li>执行节点启动时注册「插件 → 服务接口 → 节点 gRPC 地址」；</li>
 *   <li>定时心跳续期（Redis TTL），实现节点/服务下线自动过期；</li>
 *   <li>宿主查询某服务接口的可用远端节点，据此创建远程代理。</li>
 * </ul>
 * <p>
 * Redis 数据结构：
 * <pre>
 *   key            : {prefix}:svc:{serviceInterface}
 *   value（Hash）  : field = {pluginId}@{nodeId}，value = JSON(RemoteServiceRegistration)
 *   TTL            : 每次操作刷新 heartbeatTtlSeconds
 * </pre>
 */
public class RedisServiceDirectory implements ServiceDirectory {

    private static final Logger log = LoggerFactory.getLogger(RedisServiceDirectory.class);

    private static final String SVC_KEY_SUFFIX = ":svc:";
    private static final String FIELD_SEP = "@";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String prefix;
    private final long heartbeatTtlSeconds;
    /** 本地兜底缓存有效期（毫秒）：Redis 故障时用不超过该生效期的 last-known-good 快照而非直接不可用。 */
    private final long localCacheTtlMillis;
    /** 分布式指标（可为空，为空则不采集）。 */
    private final DistributedMetrics metrics;

    /**
     * 本地兜底缓存：serviceInterface -> (快照时间戳, 节点快照)。
     * Redis 不可用时降级返回，牺牲一致性换取可用性，Redis 恢复后自动切回实时目录。
     */
    private final java.util.Map<String, CacheEntry> localFallback =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final class CacheEntry {
        final long capturedAt;
        final List<RemoteServiceRegistration> snapshot;

        CacheEntry(long capturedAt, List<RemoteServiceRegistration> snapshot) {
            this.capturedAt = capturedAt;
            this.snapshot = snapshot;
        }
    }

    public RedisServiceDirectory(StringRedisTemplate redis,
                                 ObjectMapper mapper,
                                 String prefix,
                                 long heartbeatTtlSeconds) {
        this(redis, mapper, prefix, heartbeatTtlSeconds, 30_000L, null);
    }

    public RedisServiceDirectory(StringRedisTemplate redis,
                                 ObjectMapper mapper,
                                 String prefix,
                                 long heartbeatTtlSeconds,
                                 long localCacheTtlMillis) {
        this(redis, mapper, prefix, heartbeatTtlSeconds, localCacheTtlMillis, null);
    }

    public RedisServiceDirectory(StringRedisTemplate redis,
                                 ObjectMapper mapper,
                                 String prefix,
                                 long heartbeatTtlSeconds,
                                 long localCacheTtlMillis,
                                 DistributedMetrics metrics) {
        this.redis = redis;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.prefix = prefix;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
        this.localCacheTtlMillis = localCacheTtlMillis;
        this.metrics = metrics;
    }

    @Override
    public void register(RemoteServiceRegistration registration) {
        String key = serviceKey(registration.getServiceInterface());
        String field = fieldOf(registration.getPluginId(), registration.getNodeId());
        try {
            // 字段值在 Redis 服务目录中必须带有新鲜度时间戳，供 lookup 做 PVC 过滤
            redis.opsForHash().put(key, field, serialize(registration));
            redis.expire(key, Duration.ofSeconds(heartbeatTtlSeconds));
        } catch (Exception e) {
            log.warn("注册远端服务到 Redis 失败: {} {}", registration, e.getMessage());
        }
    }

    @Override
    public void registerAll(List<RemoteServiceRegistration> registrations) {
        for (RemoteServiceRegistration registration : registrations) {
            register(registration);
        }
    }

    @Override
    public void heartbeat(String serviceInterface, String pluginId, String nodeId) {
        String key = serviceKey(serviceInterface);
        try {
            redis.expire(key, Duration.ofSeconds(heartbeatTtlSeconds));
        } catch (Exception e) {
            log.warn("远端服务续期失败: {} {} {}", serviceInterface, pluginId, nodeId, e.getMessage());
        }
    }

    @Override
    public List<RemoteServiceRegistration> lookup(String serviceInterface) {
        String key = serviceKey(serviceInterface);
        List<RemoteServiceRegistration> result = new ArrayList<>();
        long staleBefore = System.currentTimeMillis() - heartbeatTtlSeconds * 1000L;
        try {
            List<Object> values = redis.opsForHash().values(key);
            if (values == null) {
                return result;
            }
            for (Object value : values) {
                if (value == null) {
                    continue;
                }
                RemoteServiceRegistration parsed = parse(value.toString());
                if (parsed == null) {
                    continue;
                }
                // 关键：单个 Hash 共享一条 TTL，任一节点续期都会保活整条 key，
                // 因此必须依据字段内的 registeredAt 独立过滤「已掉线节点」的陈旧注册，
                // 避免宿主持续把调用路由到已崩溃的执行节点。
                if (parsed.getRegisteredAt() < staleBefore) {
                    deleteField(key, parsed);
                    continue;
                }
                result.add(parsed);
            }
            // 查询成功：刷新本地 last-known-good 快照，供 Redis 故障时降级
            refreshFallback(serviceInterface, result);
            if (metrics != null) {
                metrics.recordRegistryLookupSuccess();
            }
        } catch (Exception e) {
            log.warn("查询远端服务失败，降级使用本地缓存: serviceInterface={}, err={}",
                    serviceInterface, e.getMessage());
            if (metrics != null) {
                metrics.recordRegistryFallbackUsed();
            }
            return fallbackOf(serviceInterface);
        }
        return result;
    }

    @Override
    public RemoteServiceRegistration lookup(String serviceInterface, String pluginId) {
        List<RemoteServiceRegistration> services = lookup(serviceInterface);
        for (RemoteServiceRegistration service : services) {
            if (service.getPluginId().equals(pluginId)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void unregister(String serviceInterface, String pluginId, String nodeId) {
        String key = serviceKey(serviceInterface);
        try {
            redis.opsForHash().delete(key, fieldOf(pluginId, nodeId));
        } catch (Exception e) {
            log.warn("注销远端服务失败: {} {} {}", serviceInterface, pluginId, nodeId, e.getMessage());
        }
    }

    @Override
    public void unregisterAllByNode(String nodeId) {
        try {
            // 仅扫描本模块前缀下的服务 key（prefix:svc:*），避免在多应用共用同一 Redis
            // 时，连带删除其他前缀应用的注册（旧实现用 "*:svc:*" 会越权）。
            Cursor<byte[]> cursor = redis.getConnectionFactory().getConnection()
                    .scan(ScanOptions.scanOptions().match(prefix + SVC_KEY_SUFFIX + "*").count(100).build());
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                redis.opsForHash().entries(key).forEach((field, value) -> {
                    if (field.toString().endsWith(FIELD_SEP + nodeId)) {
                        redis.opsForHash().delete(key, field);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("按节点注销远端服务失败: nodeId={}", nodeId, e.getMessage());
        }
    }

    @Override
    public Set<String> allServiceInterfaces() {
        Set<String> interfaces = new java.util.LinkedHashSet<>();
        try {
            Cursor<byte[]> cursor = redis.getConnectionFactory().getConnection()
                    .scan(ScanOptions.scanOptions().match(prefix + SVC_KEY_SUFFIX + "*").count(100).build());
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                interfaces.add(key.substring((prefix + SVC_KEY_SUFFIX).length()));
            }
        } catch (Exception e) {
            log.warn("扫描服务目录接口列表失败", e);
        }
        return interfaces;
    }

    private String serviceKey(String serviceInterface) {
        return prefix + SVC_KEY_SUFFIX + serviceInterface;
    }

    private static String fieldOf(String pluginId, String nodeId) {
        return pluginId + FIELD_SEP + nodeId;
    }

    private String serialize(RemoteServiceRegistration registration) {
        try {
            return mapper.writeValueAsString(registration);
        } catch (Exception e) {
            log.warn("序列化远端服务注册信息失败, 回退手写 JSON: {}", registration, e);
            return registration.toRedisValue();
        }
    }

    private void deleteField(String key, RemoteServiceRegistration registration) {
        try {
            redis.opsForHash().delete(key, fieldOf(registration.getPluginId(), registration.getNodeId()));
        } catch (Exception e) {
            log.debug("清理陈旧注册字段失败: {}", registration, e);
        }
    }

    private RemoteServiceRegistration parse(String json) {
        try {
            return mapper.readValue(json, RemoteServiceRegistration.class);
        } catch (Exception e) {
            log.warn("解析远端服务注册信息失败: {}", json, e);
            return null;
        }
    }

    void refreshTtl(String serviceInterface) {
        String key = serviceKey(serviceInterface);
        try {
            redis.expire(key, Duration.ofSeconds(heartbeatTtlSeconds));
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * 查询成功时，把结果作为 last-known-good 快照缓存。
     */
    private void refreshFallback(String serviceInterface, List<RemoteServiceRegistration> snapshot) {
        localFallback.put(serviceInterface,
                new CacheEntry(System.currentTimeMillis(), new ArrayList<>(snapshot)));
    }

    /**
     * Redis 故障时的兜底：返回缓存快照（若仍在生效期内），否则返回空列表。
     */
    private List<RemoteServiceRegistration> fallbackOf(String serviceInterface) {
        CacheEntry entry = localFallback.get(serviceInterface);
        if (entry == null) {
            if (metrics != null) {
                metrics.recordRegistryFallbackMiss();
            }
            return new ArrayList<>();
        }
        if (System.currentTimeMillis() - entry.capturedAt > localCacheTtlMillis) {
            // 缓存过期，仍视为不可用，避免返回长期陈旧(可能已全部下线)的节点
            if (metrics != null) {
                metrics.recordRegistryFallbackMiss();
            }
            return new ArrayList<>();
        }
        if (metrics != null) {
            metrics.recordRegistryFallbackHit();
        }
        return new ArrayList<>(entry.snapshot);
    }
}