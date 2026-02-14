package com.zqzqq.bootkits.scripts.importexport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 简化版脚本导入导出管理器
 * 提供基础的脚本配置和数据导入导出功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class SimpleScriptImportExportManager {
    
    /**
     * 导入导出格式枚举
     */
    public enum ExportFormat {
        JSON(".json"),
        XML(".xml"),
        YAML(".yaml"),
        PROPERTIES(".properties"),
        ZIP(".zip");
        
        private final String extension;
        
        ExportFormat(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public static ExportFormat fromFileName(String fileName) {
            String lowerName = fileName.toLowerCase();
            for (ExportFormat format : values()) {
                if (lowerName.endsWith(format.extension)) {
                    return format;
                }
            }
            return JSON; // 默认格式
        }
    }
    
    /**
     * 导入导出选项
     */
    public static class ImportExportOptions {
        private final boolean includeEnvironment;
        private final boolean includeCache;
        private final boolean overwriteExisting;
        private final boolean validateData;
        private final boolean createBackup;
        private final String backupLocation;
        private final Map<String, Object> metadata;
        
        private ImportExportOptions(Builder builder) {
            this.includeEnvironment = builder.includeEnvironment;
            this.includeCache = builder.includeCache;
            this.overwriteExisting = builder.overwriteExisting;
            this.validateData = builder.validateData;
            this.createBackup = builder.createBackup;
            this.backupLocation = builder.backupLocation;
            this.metadata = new HashMap<>(builder.metadata);
        }
        
        public static class Builder {
            private boolean includeEnvironment = true;
            private boolean includeCache = false;
            private boolean overwriteExisting = false;
            private boolean validateData = true;
            private boolean createBackup = true;
            private String backupLocation = "backups";
            private Map<String, Object> metadata = new HashMap<>();
            
            public Builder includeEnvironment(boolean include) {
                this.includeEnvironment = include;
                return this;
            }
            
            public Builder includeCache(boolean include) {
                this.includeCache = include;
                return this;
            }
            
            public Builder overwriteExisting(boolean overwrite) {
                this.overwriteExisting = overwrite;
                return this;
            }
            
            public Builder validateData(boolean validate) {
                this.validateData = validate;
                return this;
            }
            
            public Builder createBackup(boolean create) {
                this.createBackup = create;
                return this;
            }
            
            public Builder backupLocation(String location) {
                this.backupLocation = location;
                return this;
            }
            
            public Builder addMetadata(String key, Object value) {
                this.metadata.put(key, value);
                return this;
            }
            
            public ImportExportOptions build() {
                return new ImportExportOptions(this);
            }
        }
        
        public static Builder newBuilder() {
            return new Builder();
        }
        
        public boolean isIncludeEnvironment() { return includeEnvironment; }
        public boolean isIncludeCache() { return includeCache; }
        public boolean isOverwriteExisting() { return overwriteExisting; }
        public boolean isValidateData() { return validateData; }
        public boolean isCreateBackup() { return createBackup; }
        public String getBackupLocation() { return backupLocation; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * 导入导出结果
     */
    public static class ImportExportResult {
        private final boolean success;
        private final String message;
        private final long itemsProcessed;
        private final long itemsSucceeded;
        private final long itemsFailed;
        private final List<String> warnings;
        private final List<String> errors;
        private final Map<String, Object> statistics;
        private final LocalDateTime timestamp;
        
        public ImportExportResult(boolean success, String message, long itemsProcessed, 
                                long itemsSucceeded, long itemsFailed, List<String> warnings, 
                                List<String> errors, Map<String, Object> statistics) {
            this.success = success;
            this.message = message;
            this.itemsProcessed = itemsProcessed;
            this.itemsSucceeded = itemsSucceeded;
            this.itemsFailed = itemsFailed;
            this.warnings = new ArrayList<>(warnings);
            this.errors = new ArrayList<>(errors);
            this.statistics = new HashMap<>(statistics);
            this.timestamp = LocalDateTime.now();
        }
        
        public static ImportExportResult success(String message, long itemsProcessed, long itemsSucceeded) {
            return new ImportExportResult(true, message, itemsProcessed, itemsSucceeded, 0L, 
                                        new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        }
        
        public static ImportExportResult partialSuccess(String message, long itemsProcessed, 
                                                      long itemsSucceeded, long itemsFailed, 
                                                      List<String> warnings, List<String> errors) {
            return new ImportExportResult(true, message, itemsProcessed, itemsSucceeded, itemsFailed, 
                                        warnings, errors, new HashMap<>());
        }
        
        public static ImportExportResult failure(String message, List<String> errors) {
            return new ImportExportResult(false, message, 0, 0, 0, new ArrayList<>(), errors, new HashMap<>());
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getItemsProcessed() { return itemsProcessed; }
        public long getItemsSucceeded() { return itemsSucceeded; }
        public long getItemsFailed() { return itemsFailed; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        public Map<String, Object> getStatistics() { return statistics; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addStatistic(String key, Object value) {
            statistics.put(key, value);
        }
        
        @Override
        public String toString() {
            return String.format("ImportExportResult{success=%s, processed=%d, succeeded=%d, failed=%d, message='%s'}", 
                    success, itemsProcessed, itemsSucceeded, itemsFailed, message);
        }
    }
    
    /**
     * 导入导出数据容器
     */
    public static class ImportExportData {
        private final String version;
        private final LocalDateTime exportTime;
        private final String exporter;
        private final Map<String, Object> metadata;
        private final Map<String, String> environment;
        private final Map<String, Object> scripts;
        private final Map<String, Object> cache;
        
        public ImportExportData(String version, String exporter, Map<String, Object> metadata,
                              Map<String, String> environment, Map<String, Object> scripts,
                              Map<String, Object> cache) {
            this.version = version;
            this.exportTime = LocalDateTime.now();
            this.exporter = exporter;
            this.metadata = new HashMap<>(metadata);
            this.environment = new HashMap<>(environment);
            this.scripts = new HashMap<>(scripts);
            this.cache = new HashMap<>(cache);
        }
        
        public String getVersion() { return version; }
        public LocalDateTime getExportTime() { return exportTime; }
        public String getExporter() { return exporter; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Map<String, String> getEnvironment() { return environment; }
        public Map<String, Object> getScripts() { return scripts; }
        public Map<String, Object> getCache() { return cache; }
    }
    
    private final Gson gson;
    private final Map<ExportFormat, FormatHandler> formatHandlers;
    
    /**
     * 格式处理器接口
     */
    public interface FormatHandler {
        String getName();
        String getContentType();
        boolean canImport();
        boolean canExport();
        String serialize(Object data) throws Exception;
        Object deserialize(String content, Class<?> typeClass) throws Exception;
        String getFileExtension();
    }
    
    /**
     * JSON格式处理器
     */
    public static class JsonFormatHandler implements FormatHandler {
        private final Gson gson;
        
        public JsonFormatHandler() {
            this.gson = new GsonBuilder().setPrettyPrinting().create();
        }
        
        @Override
        public String getName() { return "JSON"; }
        
        @Override
        public String getContentType() { return "application/json"; }
        
        @Override
        public boolean canImport() { return true; }
        
        @Override
        public boolean canExport() { return true; }
        
        @Override
        public String serialize(Object data) throws Exception {
            return gson.toJson(data);
        }
        
        @Override
        public Object deserialize(String content, Class<?> typeClass) throws Exception {
            return gson.fromJson(content, typeClass);
        }
        
        @Override
        public String getFileExtension() { return ".json"; }
    }
    
    /**
     * Properties格式处理器
     */
    public static class PropertiesFormatHandler implements FormatHandler {
        @Override
        public String getName() { return "Properties"; }
        
        @Override
        public String getContentType() { return "text/plain"; }
        
        @Override
        public boolean canImport() { return true; }
        
        @Override
        public boolean canExport() { return true; }
        
        @Override
        public String serialize(Object data) throws Exception {
            if (!(data instanceof Map)) {
                throw new IllegalArgumentException("Properties format only supports Map data");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) data;
            StringBuilder sb = new StringBuilder();
            
            for (Map.Entry<String, Object> entry : mapData.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            return sb.toString();
        }
        
        @Override
        public Object deserialize(String content, Class<?> typeClass) throws Exception {
            Properties props = new Properties();
            props.load(new StringReader(content));
            
            Map<String, String> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
            
            return result;
        }
        
        @Override
        public String getFileExtension() { return ".properties"; }
    }
    
    /**
     * 简单的脚本配置
     */
    public static class SimpleScriptConfig {
        private String id;
        private String name;
        private String description;
        private String scriptType;
        private String scriptPath;
        private String workingDirectory;
        private Map<String, String> environment;
        private long timeout;
        private int retryCount;
        private boolean enabled;
        private List<String> tags;
        
        public SimpleScriptConfig() {
            this.environment = new HashMap<>();
            this.tags = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getScriptType() { return scriptType; }
        public void setScriptType(String scriptType) { this.scriptType = scriptType; }
        
        public String getScriptPath() { return scriptPath; }
        public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
        
        public String getWorkingDirectory() { return workingDirectory; }
        public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
        
        public Map<String, String> getEnvironment() { return environment; }
        public void setEnvironment(Map<String, String> environment) { this.environment = environment; }
        
        public long getTimeout() { return timeout; }
        public void setTimeout(long timeout) { this.timeout = timeout; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
    
    /**
     * 简单的脚本存储
     */
    private final Map<String, SimpleScriptConfig> scriptConfigs;
    private final Map<String, String> environmentVariables;
    
    /**
     * 构造函数
     */
    public SimpleScriptImportExportManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.formatHandlers = new EnumMap<>(ExportFormat.class);
        this.scriptConfigs = new ConcurrentHashMap<>();
        this.environmentVariables = new ConcurrentHashMap<>();
        
        initializeFormatHandlers();
    }
    
    /**
     * 初始化格式处理器
     */
    private void initializeFormatHandlers() {
        formatHandlers.put(ExportFormat.JSON, new JsonFormatHandler());
        formatHandlers.put(ExportFormat.PROPERTIES, new PropertiesFormatHandler());
    }
    
    /**
     * 导出完整配置
     *
     * @param outputPath 输出路径
     * @param format 导出格式
     * @param options 导出选项
     * @return 导出结果
     */
    public ImportExportResult exportAll(String outputPath, ExportFormat format, ImportExportOptions options) {
        try {
            // 收集数据
            Map<String, Object> exportData = collectExportData(options);
            
            // 序列化数据
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("不支持的格式: " + format, Arrays.asList("格式处理器未找到"));
            }
            
            String serializedData = handler.serialize(exportData);
            
            // 写入文件
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "配置导出成功: " + outputPath, 
                getTotalItems(exportData), 
                getTotalItems(exportData)
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("导出失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 导入完整配置
     *
     * @param inputPath 输入路径
     * @param options 导入选项
     * @return 导入结果
     */
    public ImportExportResult importAll(String inputPath, ImportExportOptions options) {
        try {
            Path inputFile = Paths.get(inputPath);
            if (!Files.exists(inputFile)) {
                return ImportExportResult.failure("文件不存在: " + inputPath, Arrays.asList("文件未找到"));
            }
            
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);
            
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null || !handler.canImport()) {
                return ImportExportResult.failure("不支持的格式: " + format, Arrays.asList("格式处理器不支持导入"));
            }
            
            // 反序列化数据
            ImportExportData data = (ImportExportData) handler.deserialize(content, ImportExportData.class);
            
            // 验证数据
            if (options.isValidateData() && !validateData(data)) {
                return ImportExportResult.failure("数据验证失败", Arrays.asList("导入的数据格式不正确"));
            }
            
            // 导入数据
            return importData(data, options);
            
        } catch (Exception e) {
            return ImportExportResult.failure("导入失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 导出环境变量
     *
     * @param outputPath 输出路径
     * @param format 导出格式
     * @return 导出结果
     */
    public ImportExportResult exportEnvironment(String outputPath, ExportFormat format) {
        try {
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("不支持的格式: " + format, Arrays.asList("格式处理器未找到"));
            }
            
            String serializedData = handler.serialize(environmentVariables);
            
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "环境变量导出成功: " + outputPath, 
                environmentVariables.size(), 
                environmentVariables.size()
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("环境变量导出失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 导入环境变量
     *
     * @param inputPath 输入路径
     * @param overwrite 是否覆盖
     * @return 导入结果
     */
    public ImportExportResult importEnvironment(String inputPath, boolean overwrite) {
        try {
            Path inputFile = Paths.get(inputPath);
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);
            
            FormatHandler handler = formatHandlers.get(format);
            @SuppressWarnings("unchecked")
            Map<String, String> envData = (Map<String, String>) handler.deserialize(content, Map.class);
            
            long imported = 0;
            for (Map.Entry<String, String> entry : envData.entrySet()) {
                if (overwrite || !environmentVariables.containsKey(entry.getKey())) {
                    environmentVariables.put(entry.getKey(), entry.getValue());
                    imported++;
                }
            }
            
            return ImportExportResult.success("环境变量导入成功", imported, imported);
            
        } catch (Exception e) {
            return ImportExportResult.failure("环境变量导入失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 导出脚本配置
     *
     * @param outputPath 输出路径
     * @param scriptIds 脚本ID列表（null表示导出所有）
     * @param format 导出格式
     * @return 导出结果
     */
    public ImportExportResult exportScripts(String outputPath, List<String> scriptIds, ExportFormat format) {
        try {
            Map<String, SimpleScriptConfig> scriptsData = new HashMap<>();
            
            List<String> idsToExport = scriptIds != null ? scriptIds : new ArrayList<>(scriptConfigs.keySet());
            long processed = 0;
            
            for (String scriptId : idsToExport) {
                SimpleScriptConfig config = scriptConfigs.get(scriptId);
                if (config != null) {
                    scriptsData.put(scriptId, config);
                    processed++;
                }
            }
            
            FormatHandler handler = formatHandlers.get(format);
            String serializedData = handler.serialize(scriptsData);
            
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "脚本配置导出成功: " + outputPath, 
                processed, 
                processed
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("脚本导出失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 导入脚本配置
     *
     * @param inputPath 输入路径
     * @param overwrite 是否覆盖已存在的配置
     * @return 导入结果
     */
    public ImportExportResult importScripts(String inputPath, boolean overwrite) {
        try {
            Path inputFile = Paths.get(inputPath);
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);
            
            FormatHandler handler = formatHandlers.get(format);
            @SuppressWarnings("unchecked")
            Map<String, SimpleScriptConfig> scriptsData = (Map<String, SimpleScriptConfig>) handler.deserialize(content, Map.class);
            
            long imported = 0;
            long failed = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map.Entry<String, SimpleScriptConfig> entry : scriptsData.entrySet()) {
                try {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = entry.getValue();
                    
                    if (overwrite || !scriptConfigs.containsKey(scriptId)) {
                        scriptConfigs.put(scriptId, config);
                        imported++;
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("导入脚本失败: " + e.getMessage());
                }
            }
            
            if (failed > 0) {
                return ImportExportResult.partialSuccess(
                    "脚本导入完成， 部分失败", 
                    imported + failed, 
                    imported, 
                    failed, 
                    new ArrayList<>(), 
                    errors
                );
            } else {
                return ImportExportResult.success("脚本导入成功", imported, imported);
            }
            
        } catch (Exception e) {
            return ImportExportResult.failure("脚本导入失败: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 添加脚本配置
     */
    public void addScriptConfig(String scriptId, SimpleScriptConfig config) {
        scriptConfigs.put(scriptId, config);
    }
    
    /**
     * 获取脚本配置
     */
    public SimpleScriptConfig getScriptConfig(String scriptId) {
        return scriptConfigs.get(scriptId);
    }
    
    /**
     * 移除脚本配置
     */
    public SimpleScriptConfig removeScriptConfig(String scriptId) {
        return scriptConfigs.remove(scriptId);
    }
    
    /**
     * 获取所有脚本ID
     */
    public List<String> getAllScriptIds() {
        return new ArrayList<>(scriptConfigs.keySet());
    }
    
    /**
     * 设置环境变量
     */
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }
    
    /**
     * 获取环境变量
     */
    public String getEnvironmentVariable(String key) {
        return environmentVariables.get(key);
    }
    
    /**
     * 移除环境变量
     */
    public String removeEnvironmentVariable(String key) {
        return environmentVariables.remove(key);
    }
    
    /**
     * 获取所有环境变量
     */
    public Map<String, String> getAllEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 收集导出数据
     */
    private Map<String, Object> collectExportData(ImportExportOptions options) {
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息
        data.put("version", "4.0.1");
        data.put("exportTime", LocalDateTime.now().toString());
        data.put("exporter", "SimpleScriptImportExportManager");
        data.put("options", options);
        
        // 脚本配置
        data.put("scripts", new HashMap<>(scriptConfigs));
        
        // 环境变量
        if (options.isIncludeEnvironment()) {
            data.put("environment", new HashMap<>(environmentVariables));
        }
        
        // 缓存
        if (options.isIncludeCache()) {
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("cacheEnabled", true);
            cacheData.put("cacheSize", 0);
            data.put("cache", cacheData);
        }
        
        return data;
    }
    
    /**
     * 验证导入数据
     */
    private boolean validateData(ImportExportData data) {
        return data != null && 
               data.getVersion() != null && 
               !data.getVersion().trim().isEmpty();
    }
    
    /**
     * 导入数据
     */
    private ImportExportResult importData(ImportExportData data, ImportExportOptions options) {
        long totalImported = 0;
        long totalFailed = 0;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // 导入脚本配置
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.getScripts();
            if (scriptsDataObj != null) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    String scriptId = entry.getKey();
                    @SuppressWarnings("unchecked")
                    SimpleScriptConfig config = (SimpleScriptConfig) entry.getValue();
                    
                    if (options.isOverwriteExisting() || !scriptConfigs.containsKey(scriptId)) {
                        scriptConfigs.put(scriptId, config);
                        totalImported++;
                    }
                }
            }
        } catch (Exception e) {
            errors.add("导入脚本配置失败: " + e.getMessage());
        }
        
        // 导入环境变量
        try {
            Map<String, String> envData = data.getEnvironment();
            if (envData != null && options.isIncludeEnvironment()) {
                for (Map.Entry<String, String> entry : envData.entrySet()) {
                    if (options.isOverwriteExisting() || !environmentVariables.containsKey(entry.getKey())) {
                        environmentVariables.put(entry.getKey(), entry.getValue());
                        totalImported++;
                    }
                }
            }
        } catch (Exception e) {
            errors.add("导入环境变量失败: " + e.getMessage());
        }
        
        if (totalFailed > 0) {
            return ImportExportResult.partialSuccess(
                "数据导入完成， 部分失败", 
                totalImported + totalFailed, 
                totalImported, 
                totalFailed, 
                warnings, 
                errors
            );
        } else {
            return ImportExportResult.success("数据导入成功", totalImported, totalImported);
        }
    }
    
    /**
     * 获取总项目数
     */
    private long getTotalItems(Map<String, Object> data) {
        long total = 0;
        for (Object value : data.values()) {
            if (value instanceof Map) {
                total += ((Map<?, ?>) value).size();
            } else if (value instanceof Collection) {
                total += ((Collection<?>) value).size();
            }
        }
        return total;
    }
    
    @Override
    public String toString() {
        return String.format("SimpleScriptImportExportManager{scripts=%d, envVars=%d, formats=%d}", 
                scriptConfigs.size(), environmentVariables.size(), formatHandlers.size());
    }
}
