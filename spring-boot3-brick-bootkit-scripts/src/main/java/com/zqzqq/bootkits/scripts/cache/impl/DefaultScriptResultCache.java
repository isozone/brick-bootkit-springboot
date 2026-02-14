package com.zqzqq.bootkits.scripts.cache.impl;

import com.zqzqq.bootkits.scripts.cache.*;
import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 脚本结果缓存默认实现
 * 提供高性能的脚本执行结果缓存功能，支持多种过期策略和LRU/LFU淘汰
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptResultCache implements ScriptResultCache {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptResultCache.class);
    
    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> expirationTimes = new ConcurrentHashMap<>();
    
    // 访问统计
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalCachePuts = new AtomicLong(0);
    private final AtomicLong totalCacheEvictions = new AtomicLong(0);
    private final AtomicLong totalCacheOperations = new AtomicLong(0);
    private final AtomicLong totalAccessTime = new AtomicLong(0);
    
    // 最近访问时间（用于LRU）
    private final Map<String, LocalDateTime> lastAccessTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> accessCounts = new ConcurrentHashMap<>();
    
    // 配置和组件
    private CacheConfiguration configuration;
    private CacheKeyGenerator keyGenerator;
    private ScheduledExecutorService cleanupExecutor;
    
    // 锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 构造函数
     */
    public DefaultScriptResultCache() {
        this(new CacheConfiguration());
    }
    
    /**
     * 构造函数
     *
     * @param configuration 缓存配置
     */
    public DefaultScriptResultCache(CacheConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new CacheConfiguration();
        this.keyGenerator = new DefaultCacheKeyGenerator();
        
        // 启动清理任务
        startCleanupTask();
        
        logger.info("脚本结果缓存已初始化: maxSize={}, maxSizeBytes={}MB", 
                   configuration.getMaxCacheSize(), 
                   configuration.getMaxCacheSizeBytes() / (1024 * 1024));
    }
    
    @Override
    public CacheOperationResult put(String scriptPath, String scriptContent, Map<String, Object> parameters,
                                   ScriptExecutionResult executionResult, Long expirationTimeMs) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (scriptPath == null || scriptContent == null || executionResult == null) {
                return CacheOperationResult.failure("参数不能为空");
            }
            
            String cacheKey = keyGenerator.generateCacheKey(scriptPath, scriptContent, parameters);
            if (cacheKey.length() > configuration.getMaxCacheKeyLength()) {
                return CacheOperationResult.failure("缓存键过长: " + cacheKey.length());
            }
            
            // 计算缓存条目大小
            long sizeBytes = calculateCacheEntrySize(scriptPath, scriptContent, parameters, executionResult);
            
            // 创建缓存条目
            CacheEntry entry = new CacheEntry(cacheKey, executionResult, LocalDateTime.now(), 
                                            scriptPath, parameters, generateScriptHash(scriptContent), sizeBytes);
            
            // 执行缓存操作
            lock.writeLock().lock();
            try {
                // 检查缓存大小限制
                if (cache.size() >= configuration.getMaxCacheSize() && !cache.containsKey(cacheKey)) {
                    evictEntries(1); // 淘汰一个条目
                }
                
                // 检查内存大小限制
                if (getCacheSizeBytes() + sizeBytes > configuration.getMaxCacheSizeBytes() && !cache.containsKey(cacheKey)) {
                    evictEntriesBySize(sizeBytes); // 淘汰足够大小的条目
                }
                
                // 存储缓存条目
                cache.put(cacheKey, entry);
                
                // 设置过期时间
                long expirationMillis = expirationTimeMs != null ? expirationTimeMs : configuration.getDefaultExpirationTimeMs();
                LocalDateTime expirationTime = LocalDateTime.now()
                    .plusSeconds(expirationMillis / 1000)
                    .plusNanos((expirationMillis % 1000) * 1_000_000);
                expirationTimes.put(cacheKey, expirationTime);
                
                // 更新访问统计
                lastAccessTimes.put(cacheKey, LocalDateTime.now());
                accessCounts.put(cacheKey, 1L);
                
                totalCachePuts.incrementAndGet();
                totalCacheOperations.incrementAndGet();
                
                long operationTime = System.currentTimeMillis() - startTime;
                totalAccessTime.addAndGet(operationTime);
                
                logger.debug("缓存存储成功: {} (大小: {} bytes)", cacheKey, sizeBytes);
                return CacheOperationResult.success("缓存存储成功", entry, operationTime);
                
            } finally {
                lock.writeLock().unlock();
            }
            
        } catch (Exception e) {
            logger.error("缓存存储失败", e);
            totalCacheOperations.incrementAndGet();
            return CacheOperationResult.failure("缓存存储失败: " + e.getMessage());
        }
    }
    
    @Override
    public Optional<ScriptExecutionResult> get(String scriptPath, String scriptContent, Map<String, Object> parameters) {
        if (scriptPath == null || scriptContent == null) {
            return Optional.empty();
        }
        
        String cacheKey = keyGenerator.generateCacheKey(scriptPath, scriptContent, parameters);
        return get(cacheKey);
    }
    
    @Override
    public Optional<ScriptExecutionResult> get(String cacheKey) {
        long startTime = System.currentTimeMillis();
        totalCacheOperations.incrementAndGet();
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(cacheKey);
            
            if (entry == null) {
                totalCacheMisses.incrementAndGet();
                long operationTime = System.currentTimeMillis() - startTime;
                totalAccessTime.addAndGet(operationTime);
                return Optional.empty();
            }
            
            // 检查是否过期
            LocalDateTime expirationTime = expirationTimes.get(cacheKey);
            if (expirationTime != null && LocalDateTime.now().isAfter(expirationTime)) {
                // 过期，移除条目
                removeInternal(cacheKey);
                totalCacheMisses.incrementAndGet();
                long operationTime = System.currentTimeMillis() - startTime;
                totalAccessTime.addAndGet(operationTime);
                return Optional.empty();
            }
            
            // 缓存命中，更新访问统计
            updateAccessStats(cacheKey);
            totalCacheHits.incrementAndGet();
            
            long operationTime = System.currentTimeMillis() - startTime;
            totalAccessTime.addAndGet(operationTime);
            
            logger.debug("缓存命中: {}", cacheKey);
            return Optional.of(entry.getExecutionResult());
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean contains(String cacheKey) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(cacheKey);
            if (entry == null) {
                return false;
            }
            
            // 检查是否过期
            LocalDateTime expirationTime = expirationTimes.get(cacheKey);
            if (expirationTime != null && LocalDateTime.now().isAfter(expirationTime)) {
                removeInternal(cacheKey);
                return false;
            }
            
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public CacheOperationResult remove(String cacheKey) {
        long startTime = System.currentTimeMillis();
        totalCacheOperations.incrementAndGet();
        
        lock.writeLock().lock();
        try {
            boolean removed = removeInternal(cacheKey);
            
            long operationTime = System.currentTimeMillis() - startTime;
            totalAccessTime.addAndGet(operationTime);
            
            if (removed) {
                logger.debug("缓存移除成功: {}", cacheKey);
                return CacheOperationResult.success("缓存移除成功", null, operationTime);
            } else {
                return CacheOperationResult.failure("缓存键不存在: " + cacheKey);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public CacheOperationResult clear() {
        long startTime = System.currentTimeMillis();
        totalCacheOperations.incrementAndGet();
        
        lock.writeLock().lock();
        try {
            int removedCount = cache.size();
            
            cache.clear();
            expirationTimes.clear();
            lastAccessTimes.clear();
            accessCounts.clear();
            
            long operationTime = System.currentTimeMillis() - startTime;
            totalAccessTime.addAndGet(operationTime);
            
            logger.info("缓存清空成功，移除 {} 个条目", removedCount);
            return CacheOperationResult.success("缓存清空成功", null, operationTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public CacheOperationResult cleanup() {
        long startTime = System.currentTimeMillis();
        totalCacheOperations.incrementAndGet();
        
        lock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            int removedCount = 0;
            
            Iterator<Map.Entry<String, LocalDateTime>> iterator = expirationTimes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, LocalDateTime> entry = iterator.next();
                if (now.isAfter(entry.getValue())) {
                    removeInternal(entry.getKey());
                    iterator.remove();
                    removedCount++;
                }
            }
            
            // 根据过期策略进行额外清理
            switch (configuration.getExpirationStrategy()) {
                case LRU:
                    cleanupLRU();
                    break;
                case LFU:
                    cleanupLFU();
                    break;
                case ACCESS_BASED:
                    cleanupByAccessCount();
                    break;
            }
            
            long operationTime = System.currentTimeMillis() - startTime;
            totalAccessTime.addAndGet(operationTime);
            
            logger.debug("缓存清理完成，移除 {} 个过期条目", removedCount);
            return CacheOperationResult.success("清理完成，移除 " + removedCount + " 个过期条目", null, operationTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public CacheStatistics getStatistics() {
        lock.readLock().lock();
        try {
            long totalRequests = totalCacheHits.get() + totalCacheMisses.get();
            double hitRate = totalRequests > 0 ? (double) totalCacheHits.get() / totalRequests : 0.0;
            double evictionRate = totalCachePuts.get() > 0 ? 
                (double) totalCacheEvictions.get() / totalCachePuts.get() : 0.0;
            
            long averageAccessTime = totalCacheOperations.get() > 0 ? 
                totalAccessTime.get() / totalCacheOperations.get() : 0;
            
            // 按脚本类型统计
            Map<String, Integer> entriesByScriptType = cache.values().stream()
                .collect(Collectors.groupingBy(
                    entry -> getScriptTypeFromPath(entry.getScriptPath()),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ));
            
            // 最常访问的条目
            Map<String, Integer> mostAccessedEntries = accessCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ));
            
            return new CacheStatistics(
                totalCacheHits.get(),
                totalCacheMisses.get(),
                totalCachePuts.get(),
                totalCacheEvictions.get(),
                cache.size(),
                configuration.getMaxCacheSize(),
                getCacheSizeBytes(),
                configuration.getMaxCacheSizeBytes(),
                hitRate,
                evictionRate,
                getLastHitTime(),
                getLastMissTime(),
                getLastEvictionTime(),
                entriesByScriptType,
                mostAccessedEntries,
                averageAccessTime,
                totalCacheOperations.get()
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void resetStatistics() {
        totalCacheHits.set(0);
        totalCacheMisses.set(0);
        totalCachePuts.set(0);
        totalCacheEvictions.set(0);
        totalCacheOperations.set(0);
        totalAccessTime.set(0);
    }
    
    @Override
    public void setCacheKeyGenerator(CacheKeyGenerator keyGenerator) {
        if (keyGenerator != null) {
            this.keyGenerator = keyGenerator;
        }
    }
    
    @Override
    public CacheKeyGenerator getCacheKeyGenerator() {
        return keyGenerator;
    }
    
    @Override
    public long getCacheSize() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public long getCacheSizeBytes() {
        lock.readLock().lock();
        try {
            return cache.values().stream()
                .mapToLong(CacheEntry::getSizeBytes)
                .sum();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void setConfiguration(CacheConfiguration config) {
        if (config != null) {
            this.configuration = config;
            // 重启清理任务
            stopCleanupTask();
            startCleanupTask();
        }
    }
    
    @Override
    public CacheConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public CacheOperationResult warmup(List<CacheEntry> scriptEntries) {
        if (scriptEntries == null || scriptEntries.isEmpty()) {
            return CacheOperationResult.failure("预热条目列表不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (CacheEntry entry : scriptEntries) {
            try {
                CacheOperationResult result = put(
                    entry.getScriptPath(),
                    "", // 这里应该有实际脚本内容
                    entry.getParameters(),
                    entry.getExecutionResult(),
                    null
                );
                
                if (result.isSuccess()) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.warn("预热条目失败: {}", entry.getScriptPath(), e);
            }
        }
        
        long operationTime = System.currentTimeMillis() - startTime;
        
        String message = String.format("预热完成: %d/%d 个条目成功", successCount, scriptEntries.size());
        return CacheOperationResult.success(message, null, operationTime);
    }
    
    @Override
    public String exportCache(String format) {
        lock.readLock().lock();
        try {
            if ("json".equalsIgnoreCase(format)) {
                return exportAsJson();
            } else if ("xml".equalsIgnoreCase(format)) {
                return exportAsXml();
            } else {
                return exportAsJson();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public CacheOperationResult importCache(String content, String format, boolean overwrite) {
        if (content == null || content.trim().isEmpty()) {
            return CacheOperationResult.failure("导入内容不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        int importedCount = 0;
        int updatedCount = 0;
        
        try {
            List<CacheEntry> entriesToImport;
            if ("json".equalsIgnoreCase(format)) {
                entriesToImport = importFromJson(content);
            } else if ("xml".equalsIgnoreCase(format)) {
                entriesToImport = importFromXml(content);
            } else {
                entriesToImport = importFromJson(content);
            }
            
            for (CacheEntry entry : entriesToImport) {
                try {
                    CacheOperationResult result = put(
                        entry.getScriptPath(),
                        "", // 这里应该有实际脚本内容
                        entry.getParameters(),
                        entry.getExecutionResult(),
                        null
                    );
                    
                    if (result.isSuccess()) {
                        if (overwrite && cache.containsKey(entry.getCacheKey())) {
                            updatedCount++;
                        } else {
                            importedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("导入缓存条目失败: {}", entry.getScriptPath(), e);
                }
            }
            
            long operationTime = System.currentTimeMillis() - startTime;
            String message = String.format("导入完成: 导入=%d, 更新=%d", importedCount, updatedCount);
            
            return CacheOperationResult.success(message, null, operationTime);
        } catch (Exception e) {
            return CacheOperationResult.failure("导入失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getUsageReport() {
        CacheStatistics stats = getStatistics();
        
        StringBuilder report = new StringBuilder();
        report.append("=== 脚本结果缓存使用报告 ===\n\n");
        
        report.append("基本统计:\n");
        report.append(String.format("  总缓存命中: %d\n", stats.getTotalCacheHits()));
        report.append(String.format("  总缓存未命中: %d\n", stats.getTotalCacheMisses()));
        report.append(String.format("  缓存命中率: %.2f%%\n", stats.getHitRate() * 100));
        report.append(String.format("  总缓存操作: %d\n", stats.getTotalCacheOperations()));
        report.append(String.format("  平均访问时间: %d ms\n\n", stats.getAverageAccessTimeMs()));
        
        report.append("缓存大小:\n");
        report.append(String.format("  当前条目数: %d/%d\n", stats.getCurrentCacheSize(), stats.getMaxCacheSize()));
        report.append(String.format("  当前大小: %.2f MB/%.2f MB\n", 
            stats.getTotalCacheSizeBytes() / (1024.0 * 1024.0),
            stats.getMaxCacheSizeBytes() / (1024.0 * 1024.0)));
        report.append(String.format("  总缓存存储: %d\n", stats.getTotalCachePuts()));
        report.append(String.format("  总缓存淘汰: %d\n", stats.getTotalCacheEvictions()));
        report.append(String.format("  淘汰率: %.2f%%\n\n", stats.getEvictionRate() * 100));
        
        if (!stats.getCacheEntriesByScriptType().isEmpty()) {
            report.append("按脚本类型分布:\n");
            stats.getCacheEntriesByScriptType().forEach((type, count) ->
                report.append(String.format("  %s: %d 个条目\n", type, count)));
            report.append("\n");
        }
        
        if (!stats.getMostAccessedEntries().isEmpty()) {
            report.append("最常访问的条目 (前5个):\n");
            stats.getMostAccessedEntries().entrySet().stream()
                .limit(5)
                .forEach(entry ->
                    report.append(String.format("  %s: 访问 %d 次\n", entry.getKey(), entry.getValue())));
        }
        
        return report.toString();
    }
    
    // 私有方法
    
    private void startCleanupTask() {
        if (cleanupExecutor == null || cleanupExecutor.isShutdown()) {
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-cleanup");
                t.setDaemon(true);
                return t;
            });
            
            cleanupExecutor.scheduleWithFixedDelay(
                this::cleanup,
                configuration.getCleanupIntervalMs(),
                configuration.getCleanupIntervalMs(),
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    private void stopCleanupTask() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private boolean removeInternal(String cacheKey) {
        CacheEntry removed = cache.remove(cacheKey);
        if (removed != null) {
            expirationTimes.remove(cacheKey);
            lastAccessTimes.remove(cacheKey);
            accessCounts.remove(cacheKey);
            return true;
        }
        return false;
    }
    
    private void updateAccessStats(String cacheKey) {
        lastAccessTimes.put(cacheKey, LocalDateTime.now());
        accessCounts.merge(cacheKey, 1L, Long::sum);
    }
    
    private void evictEntries(int count) {
        switch (configuration.getExpirationStrategy()) {
            case LRU:
                evictLRU(count);
                break;
            case LFU:
                evictLFU(count);
                break;
            case ACCESS_BASED:
                evictByAccessCount(count);
                break;
            default:
                evictOldest(count);
                break;
        }
    }
    
    private void evictEntriesBySize(long requiredSize) {
        // 按大小降序排序，优先淘汰大条目
        List<String> sortedKeys = cache.keySet().stream()
            .sorted((k1, k2) -> Long.compare(cache.get(k2).getSizeBytes(), cache.get(k1).getSizeBytes()))
            .collect(Collectors.toList());
        
        long freedSize = 0;
        int evictedCount = 0;
        
        for (String key : sortedKeys) {
            if (freedSize >= requiredSize) {
                break;
            }
            
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                removeInternal(key);
                freedSize += entry.getSizeBytes();
                evictedCount++;
                totalCacheEvictions.incrementAndGet();
            }
        }
        
        logger.debug("按大小淘汰完成: 释放 {} bytes, 淘汰 {} 个条目", freedSize, evictedCount);
    }
    
    private void evictLRU(int count) {
        List<String> sortedKeys = lastAccessTimes.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        evictKeys(sortedKeys.subList(0, Math.min(count, sortedKeys.size())));
    }
    
    private void evictLFU(int count) {
        List<String> sortedKeys = accessCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        evictKeys(sortedKeys.subList(0, Math.min(count, sortedKeys.size())));
    }
    
    private void evictByAccessCount(int count) {
        List<String> sortedKeys = accessCounts.entrySet().stream()
            .filter(entry -> entry.getValue() < 5) // 访问次数少于5次的
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        evictKeys(sortedKeys.subList(0, Math.min(count, sortedKeys.size())));
    }
    
    private void evictOldest(int count) {
        List<String> sortedKeys = cache.values().stream()
            .sorted(Comparator.comparing(CacheEntry::getCreatedTime))
            .map(CacheEntry::getCacheKey)
            .collect(Collectors.toList());
        
        evictKeys(sortedKeys.subList(0, Math.min(count, sortedKeys.size())));
    }
    
    private void evictKeys(List<String> keysToEvict) {
        for (String key : keysToEvict) {
            removeInternal(key);
            totalCacheEvictions.incrementAndGet();
        }
        
        logger.debug("淘汰了 {} 个缓存条目", keysToEvict.size());
    }
    
    private void cleanupLRU() {
        // LRU清理已在cleanup()方法中处理
    }
    
    private void cleanupLFU() {
        // LFU清理已在cleanup()方法中处理
    }
    
    private void cleanupByAccessCount() {
        // 清理访问次数少的条目
        accessCounts.entrySet().removeIf(entry -> entry.getValue() == 0);
    }
    
    private long calculateCacheEntrySize(String scriptPath, String scriptContent, 
                                       Map<String, Object> parameters, ScriptExecutionResult result) {
        long size = 0;
        
        // 计算脚本路径大小
        if (scriptPath != null) {
            size += scriptPath.length() * 2; // Unicode字符约2字节
        }
        
        // 计算脚本内容大小
        if (scriptContent != null) {
            size += scriptContent.length() * 2;
        }
        
        // 计算参数大小
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                size += entry.getKey().length() * 2;
                if (entry.getValue() != null) {
                    size += entry.getValue().toString().length() * 2;
                }
            }
        }
        
        // 计算执行结果大小（简化估算）
        if (result != null) {
            size += 1024; // 估算执行结果大小
        }
        
        return size;
    }
    
    private String generateScriptHash(String scriptContent) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(scriptContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toString(scriptContent.hashCode());
        }
    }
    
    private String getScriptTypeFromPath(String scriptPath) {
        if (scriptPath == null) return "unknown";
        String lowerPath = scriptPath.toLowerCase();
        if (lowerPath.endsWith(".sh") || lowerPath.endsWith(".bash") || lowerPath.endsWith(".zsh")) {
            return "shell";
        } else if (lowerPath.endsWith(".py")) {
            return "python";
        } else if (lowerPath.endsWith(".lua")) {
            return "lua";
        } else if (lowerPath.endsWith(".ps1")) {
            return "powershell";
        } else if (lowerPath.endsWith(".rb")) {
            return "ruby";
        } else if (lowerPath.endsWith(".pl")) {
            return "perl";
        } else if (lowerPath.endsWith(".js")) {
            return "nodejs";
        } else if (lowerPath.endsWith(".groovy")) {
            return "groovy";
        } else {
            return "other";
        }
    }
    
    private LocalDateTime getLastHitTime() {
        // 简化实现，返回当前时间
        return LocalDateTime.now();
    }
    
    private LocalDateTime getLastMissTime() {
        // 简化实现，返回当前时间
        return LocalDateTime.now();
    }
    
    private LocalDateTime getLastEvictionTime() {
        // 简化实现，返回当前时间
        return LocalDateTime.now();
    }
    
    private String exportAsJson() {
        StringBuilder json = new StringBuilder("{\n  \"cacheEntries\": [\n");
        
        List<CacheEntry> entries = new ArrayList<>(cache.values());
        for (int i = 0; i < entries.size(); i++) {
            CacheEntry entry = entries.get(i);
            json.append("    {\n");
            json.append("      \"cacheKey\": \"").append(entry.getCacheKey()).append("\",\n");
            json.append("      \"scriptPath\": \"").append(entry.getScriptPath()).append("\",\n");
            json.append("      \"createdTime\": \"").append(entry.getCreatedTime()).append("\",\n");
            json.append("      \"accessCount\": ").append(entry.getAccessCount()).append(",\n");
            json.append("      \"sizeBytes\": ").append(entry.getSizeBytes()).append("\n");
            json.append("    }");
            if (i < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString();
    }
    
    private String exportAsXml() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<cacheEntries>\n");
        
        for (CacheEntry entry : cache.values()) {
            xml.append("  <entry>\n");
            xml.append("    <cacheKey>").append(entry.getCacheKey()).append("</cacheKey>\n");
            xml.append("    <scriptPath>").append(entry.getScriptPath()).append("</scriptPath>\n");
            xml.append("    <createdTime>").append(entry.getCreatedTime()).append("</createdTime>\n");
            xml.append("    <accessCount>").append(entry.getAccessCount()).append("</accessCount>\n");
            xml.append("    <sizeBytes>").append(entry.getSizeBytes()).append("</sizeBytes>\n");
            xml.append("  </entry>\n");
        }
        
        xml.append("</cacheEntries>");
        return xml.toString();
    }
    
    private List<CacheEntry> importFromJson(String content) {
        // 简化实现，返回空列表
        // 实际实现应该使用JSON解析库
        return Collections.emptyList();
    }
    
    private List<CacheEntry> importFromXml(String content) {
        // 简化实现，返回空列表
        // 实际实现应该使用XML解析库
        return Collections.emptyList();
    }
    
    /**
     * 默认缓存键生成器
     */
    private static class DefaultCacheKeyGenerator implements CacheKeyGenerator {
        @Override
        public String generateCacheKey(String scriptPath, String scriptContent, Map<String, Object> parameters) {
            StringBuilder key = new StringBuilder();
            
            // 添加脚本路径哈希
            if (scriptPath != null) {
                key.append(Integer.toHexString(scriptPath.hashCode()));
            }
            
            // 添加脚本内容哈希
            if (scriptContent != null) {
                key.append("_").append(Integer.toHexString(scriptContent.hashCode()));
            }
            
            // 添加参数哈希
            if (parameters != null && !parameters.isEmpty()) {
                String paramHash = parameters.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
                key.append("_").append(Integer.toHexString(paramHash.hashCode()));
            }
            
            return key.toString();
        }
    }
}
