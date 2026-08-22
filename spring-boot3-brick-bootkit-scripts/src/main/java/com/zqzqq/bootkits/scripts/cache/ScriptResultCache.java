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


package com.zqzqq.bootkits.scripts.cache;

import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 脚本结果缓存管理器接口
 * 提供脚本执行结果的缓存、检索和管理功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptResultCache {
    
    /**
     * 缓存条目
     */
    class CacheEntry {
        private final String cacheKey;
        private final ScriptExecutionResult executionResult;
        private final LocalDateTime createdTime;
        private final LocalDateTime lastAccessedTime;
        private final long accessCount;
        private final String scriptPath;
        private final Map<String, Object> parameters;
        private final String scriptHash;
        private final long sizeBytes;
        
        public CacheEntry(String cacheKey, ScriptExecutionResult executionResult, LocalDateTime createdTime,
                         String scriptPath, Map<String, Object> parameters, String scriptHash, long sizeBytes) {
            this.cacheKey = cacheKey;
            this.executionResult = executionResult;
            this.createdTime = createdTime;
            this.lastAccessedTime = createdTime;
            this.accessCount = 1;
            this.scriptPath = scriptPath;
            this.parameters = parameters != null ? parameters : Map.of();
            this.scriptHash = scriptHash;
            this.sizeBytes = sizeBytes;
        }
        
        // 更新访问时间
        public CacheEntry updateAccess() {
            return new CacheEntry(
                cacheKey, executionResult, createdTime, 
                scriptPath, parameters, scriptHash, sizeBytes
            ) {
                @Override
                public LocalDateTime getLastAccessedTime() {
                    return LocalDateTime.now();
                }
                
                @Override
                public long getAccessCount() {
                    return accessCount + 1;
                }
            };
        }
        
        // Getters
        public String getCacheKey() { return cacheKey; }
        public ScriptExecutionResult getExecutionResult() { return executionResult; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public LocalDateTime getLastAccessedTime() { return lastAccessedTime; }
        public long getAccessCount() { return accessCount; }
        public String getScriptPath() { return scriptPath; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getScriptHash() { return scriptHash; }
        public long getSizeBytes() { return sizeBytes; }
        
        @Override
        public String toString() {
            return "CacheEntry{" +
                    "cacheKey='" + cacheKey + '\'' +
                    ", scriptPath='" + scriptPath + '\'' +
                    ", accessCount=" + accessCount +
                    ", createdTime=" + createdTime +
                    ", sizeBytes=" + sizeBytes +
                    '}';
        }
    }
    
    /**
     * 缓存键生成器
     */
    interface CacheKeyGenerator {
        /**
         * 生成缓存键
         *
         * @param scriptPath 脚本路径
         * @param scriptContent 脚本内容
         * @param parameters 参数映射
         * @return 缓存键
         */
        String generateCacheKey(String scriptPath, String scriptContent, Map<String, Object> parameters);
    }
    
    /**
     * 缓存过期策略
     */
    enum ExpirationStrategy {
        /**
         * 永不过期
         */
        NEVER,
        /**
         * 基于时间过期
         */
        TIME_BASED,
        /**
         * 基于访问次数过期
         */
        ACCESS_BASED,
        /**
         * 基于LRU过期
         */
        LRU,
        /**
         * 基于LFU过期
         */
        LFU
    }
    
    /**
     * 缓存操作结果
     */
    class CacheOperationResult {
        private boolean success;
        private String message;
        private CacheEntry cacheEntry;
        private long operationTimeMs;
        
        public CacheOperationResult() {}
        
        public CacheOperationResult(boolean success, String message, CacheEntry cacheEntry, long operationTimeMs) {
            this.success = success;
            this.message = message;
            this.cacheEntry = cacheEntry;
            this.operationTimeMs = operationTimeMs;
        }
        
        public static CacheOperationResult success(String message, CacheEntry cacheEntry, long operationTimeMs) {
            return new CacheOperationResult(true, message, cacheEntry, operationTimeMs);
        }
        
        public static CacheOperationResult failure(String message) {
            return new CacheOperationResult(false, message, null, 0);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public CacheEntry getCacheEntry() { return cacheEntry; }
        public long getOperationTimeMs() { return operationTimeMs; }
    }
    
    /**
     * 缓存统计信息
     */
    class CacheStatistics {
        private long totalCacheHits;
        private long totalCacheMisses;
        private long totalCachePuts;
        private long totalCacheEvictions;
        private long currentCacheSize;
        private long maxCacheSize;
        private long totalCacheSizeBytes;
        private long maxCacheSizeBytes;
        private double hitRate;
        private double evictionRate;
        private LocalDateTime lastHitTime;
        private LocalDateTime lastMissTime;
        private LocalDateTime lastEvictionTime;
        private Map<String, Integer> cacheEntriesByScriptType;
        private Map<String, Integer> mostAccessedEntries;
        private long averageAccessTimeMs;
        private long totalCacheOperations;
        
        public CacheStatistics() {}
        
        public CacheStatistics(long totalCacheHits, long totalCacheMisses, long totalCachePuts,
                             long totalCacheEvictions, long currentCacheSize, long maxCacheSize,
                             long totalCacheSizeBytes, long maxCacheSizeBytes, double hitRate,
                             double evictionRate, LocalDateTime lastHitTime, LocalDateTime lastMissTime,
                             LocalDateTime lastEvictionTime, Map<String, Integer> cacheEntriesByScriptType,
                             Map<String, Integer> mostAccessedEntries, long averageAccessTimeMs,
                             long totalCacheOperations) {
            this.totalCacheHits = totalCacheHits;
            this.totalCacheMisses = totalCacheMisses;
            this.totalCachePuts = totalCachePuts;
            this.totalCacheEvictions = totalCacheEvictions;
            this.currentCacheSize = currentCacheSize;
            this.maxCacheSize = maxCacheSize;
            this.totalCacheSizeBytes = totalCacheSizeBytes;
            this.maxCacheSizeBytes = maxCacheSizeBytes;
            this.hitRate = hitRate;
            this.evictionRate = evictionRate;
            this.lastHitTime = lastHitTime;
            this.lastMissTime = lastMissTime;
            this.lastEvictionTime = lastEvictionTime;
            this.cacheEntriesByScriptType = cacheEntriesByScriptType;
            this.mostAccessedEntries = mostAccessedEntries;
            this.averageAccessTimeMs = averageAccessTimeMs;
            this.totalCacheOperations = totalCacheOperations;
        }
        
        // 计算命中率
        public static CacheStatistics withHitRate(CacheStatistics stats) {
            long totalRequests = stats.totalCacheHits + stats.totalCacheMisses;
            double hitRate = totalRequests > 0 ? (double) stats.totalCacheHits / totalRequests : 0.0;
            return new CacheStatistics(
                stats.totalCacheHits, stats.totalCacheMisses, stats.totalCachePuts,
                stats.totalCacheEvictions, stats.currentCacheSize, stats.maxCacheSize,
                stats.totalCacheSizeBytes, stats.maxCacheSizeBytes, hitRate,
                stats.evictionRate, stats.lastHitTime, stats.lastMissTime,
                stats.lastEvictionTime, stats.cacheEntriesByScriptType,
                stats.mostAccessedEntries, stats.averageAccessTimeMs,
                stats.totalCacheOperations
            );
        }
        
        // Getters
        public long getTotalCacheHits() { return totalCacheHits; }
        public long getTotalCacheMisses() { return totalCacheMisses; }
        public long getTotalCachePuts() { return totalCachePuts; }
        public long getTotalCacheEvictions() { return totalCacheEvictions; }
        public long getCurrentCacheSize() { return currentCacheSize; }
        public long getMaxCacheSize() { return maxCacheSize; }
        public long getTotalCacheSizeBytes() { return totalCacheSizeBytes; }
        public long getMaxCacheSizeBytes() { return maxCacheSizeBytes; }
        public double getHitRate() { return hitRate; }
        public double getEvictionRate() { return evictionRate; }
        public LocalDateTime getLastHitTime() { return lastHitTime; }
        public LocalDateTime getLastMissTime() { return lastMissTime; }
        public LocalDateTime getLastEvictionTime() { return lastEvictionTime; }
        public Map<String, Integer> getCacheEntriesByScriptType() { return cacheEntriesByScriptType; }
        public Map<String, Integer> getMostAccessedEntries() { return mostAccessedEntries; }
        public long getAverageAccessTimeMs() { return averageAccessTimeMs; }
        public long getTotalCacheOperations() { return totalCacheOperations; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStatistics{hits=%d, misses=%d, hitRate=%.2f%%, currentSize=%d, totalSizeBytes=%d}",
                totalCacheHits, totalCacheMisses, hitRate * 100, currentCacheSize, totalCacheSizeBytes
            );
        }
    }
    
    /**
     * 缓存配置
     */
    class CacheConfiguration {
        private long maxCacheSize = 1000;
        private long maxCacheSizeBytes = 100 * 1024 * 1024; // 100MB
        private long defaultExpirationTimeMs = 3600000; // 1小时
        private ExpirationStrategy expirationStrategy = ExpirationStrategy.TIME_BASED;
        private boolean enableStatistics = true;
        private boolean enableCompression = false;
        private int compressionThreshold = 1024; // 1KB
        private boolean enablePersistence = false;
        private String persistencePath;
        private long cleanupIntervalMs = 300000; // 5分钟
        private boolean enableCacheKeyHashing = true;
        private int maxCacheKeyLength = 256;
        
        // Getters and Setters
        public long getMaxCacheSize() { return maxCacheSize; }
        public void setMaxCacheSize(long maxCacheSize) { this.maxCacheSize = maxCacheSize; }
        
        public long getMaxCacheSizeBytes() { return maxCacheSizeBytes; }
        public void setMaxCacheSizeBytes(long maxCacheSizeBytes) { this.maxCacheSizeBytes = maxCacheSizeBytes; }
        
        public long getDefaultExpirationTimeMs() { return defaultExpirationTimeMs; }
        public void setDefaultExpirationTimeMs(long defaultExpirationTimeMs) { this.defaultExpirationTimeMs = defaultExpirationTimeMs; }
        
        public ExpirationStrategy getExpirationStrategy() { return expirationStrategy; }
        public void setExpirationStrategy(ExpirationStrategy expirationStrategy) { this.expirationStrategy = expirationStrategy; }
        
        public boolean isEnableStatistics() { return enableStatistics; }
        public void setEnableStatistics(boolean enableStatistics) { this.enableStatistics = enableStatistics; }
        
        public boolean isEnableCompression() { return enableCompression; }
        public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
        
        public int getCompressionThreshold() { return compressionThreshold; }
        public void setCompressionThreshold(int compressionThreshold) { this.compressionThreshold = compressionThreshold; }
        
        public boolean isEnablePersistence() { return enablePersistence; }
        public void setEnablePersistence(boolean enablePersistence) { this.enablePersistence = enablePersistence; }
        
        public String getPersistencePath() { return persistencePath; }
        public void setPersistencePath(String persistencePath) { this.persistencePath = persistencePath; }
        
        public long getCleanupIntervalMs() { return cleanupIntervalMs; }
        public void setCleanupIntervalMs(long cleanupIntervalMs) { this.cleanupIntervalMs = cleanupIntervalMs; }
        
        public boolean isEnableCacheKeyHashing() { return enableCacheKeyHashing; }
        public void setEnableCacheKeyHashing(boolean enableCacheKeyHashing) { this.enableCacheKeyHashing = enableCacheKeyHashing; }
        
        public int getMaxCacheKeyLength() { return maxCacheKeyLength; }
        public void setMaxCacheKeyLength(int maxCacheKeyLength) { this.maxCacheKeyLength = maxCacheKeyLength; }
    }
    
    /**
     * 缓存脚本执行结果
     *
     * @param scriptPath 脚本路径
     * @param scriptContent 脚本内容
     * @param parameters 参数映射
     * @param executionResult 执行结果
     * @param expirationTimeMs 过期时间（毫秒），null表示使用默认时间
     * @return 缓存操作结果
     */
    CacheOperationResult put(String scriptPath, String scriptContent, Map<String, Object> parameters,
                           ScriptExecutionResult executionResult, Long expirationTimeMs);
    
    /**
     * 获取缓存的脚本执行结果
     *
     * @param scriptPath 脚本路径
     * @param scriptContent 脚本内容
     * @param parameters 参数映射
     * @return 缓存的执行结果
     */
    Optional<ScriptExecutionResult> get(String scriptPath, String scriptContent, Map<String, Object> parameters);
    
    /**
     * 根据缓存键获取结果
     *
     * @param cacheKey 缓存键
     * @return 缓存的执行结果
     */
    Optional<ScriptExecutionResult> get(String cacheKey);
    
    /**
     * 检查缓存中是否存在指定的键
     *
     * @param cacheKey 缓存键
     * @return 是否存在
     */
    boolean contains(String cacheKey);
    
    /**
     * 从缓存中移除指定的键
     *
     * @param cacheKey 缓存键
     * @return 缓存操作结果
     */
    CacheOperationResult remove(String cacheKey);
    
    /**
     * 清空所有缓存
     *
     * @return 缓存操作结果
     */
    CacheOperationResult clear();
    
    /**
     * 清理过期的缓存条目
     *
     * @return 清理结果
     */
    CacheOperationResult cleanup();
    
    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    CacheStatistics getStatistics();
    
    /**
     * 重置统计信息
     */
    void resetStatistics();
    
    /**
     * 设置缓存键生成器
     *
     * @param keyGenerator 缓存键生成器
     */
    void setCacheKeyGenerator(CacheKeyGenerator keyGenerator);
    
    /**
     * 获取缓存键生成器
     *
     * @return 缓存键生成器
     */
    CacheKeyGenerator getCacheKeyGenerator();
    
    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数
     */
    long getCacheSize();
    
    /**
     * 获取当前缓存大小（字节）
     *
     * @return 缓存大小（字节）
     */
    long getCacheSizeBytes();
    
    /**
     * 设置缓存配置
     *
     * @param config 配置
     */
    void setConfiguration(CacheConfiguration config);
    
    /**
     * 获取缓存配置
     *
     * @return 配置
     */
    CacheConfiguration getConfiguration();
    
    /**
     * 预热缓存
     *
     * @param scriptEntries 预热条目列表
     * @return 预热结果
     */
    CacheOperationResult warmup(List<CacheEntry> scriptEntries);
    
    /**
     * 导出缓存
     *
     * @param format 导出格式 (JSON, XML)
     * @return 导出的内容
     */
    String exportCache(String format);
    
    /**
     * 导入缓存
     *
     * @param content 导入内容
     * @param format 导入格式 (JSON, XML)
     * @param overwrite 是否覆盖已存在的条目
     * @return 导入结果
     */
    CacheOperationResult importCache(String content, String format, boolean overwrite);
    
    /**
     * 获取缓存使用情况报告
     *
     * @return 使用情况报告
     */
    String getUsageReport();
}