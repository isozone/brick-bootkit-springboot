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

    private static final String CURRENT_SCHEMA_VERSION = "4.0.1";
    private static final String LEGACY_SCHEMA_VERSION = "legacy";
    
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
        private final boolean dryRun;
        private final boolean strictSchemaValidation;
        private final boolean createBackup;
        private final String backupLocation;
        private final Map<String, Object> metadata;
        
        private ImportExportOptions(Builder builder) {
            this.includeEnvironment = builder.includeEnvironment;
            this.includeCache = builder.includeCache;
            this.overwriteExisting = builder.overwriteExisting;
            this.validateData = builder.validateData;
            this.dryRun = builder.dryRun;
            this.strictSchemaValidation = builder.strictSchemaValidation;
            this.createBackup = builder.createBackup;
            this.backupLocation = builder.backupLocation;
            this.metadata = new HashMap<>(builder.metadata);
        }
        
        public static class Builder {
            private boolean includeEnvironment = true;
            private boolean includeCache = false;
            private boolean overwriteExisting = false;
            private boolean validateData = true;
            private boolean dryRun = false;
            private boolean strictSchemaValidation = false;
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

            public Builder dryRun(boolean dryRun) {
                this.dryRun = dryRun;
                return this;
            }

            public Builder strictSchemaValidation(boolean strictSchemaValidation) {
                this.strictSchemaValidation = strictSchemaValidation;
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
        public boolean isDryRun() { return dryRun; }
        public boolean isStrictSchemaValidation() { return strictSchemaValidation; }
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
    private final Map<String, SchemaMigration> schemaMigrations;
    
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

    public interface SchemaMigration {
        String migrate(Map<String, Object> data, MigrationContext context);
    }

    public static class MigrationContext {
        private final List<String> warnings;
        private final Map<String, Object> statistics;
        private final List<String> path = new ArrayList<>();

        private MigrationContext(List<String> warnings, Map<String, Object> statistics) {
            this.warnings = warnings;
            this.statistics = statistics;
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addPathStep(String fromVersion, String toVersion) {
            path.add(fromVersion + " -> " + toVersion);
        }

        public int getStepCount() {
            return path.size();
        }

        public List<String> getPath() {
            return new ArrayList<>(path);
        }

        public void putStatistic(String key, Object value) {
            statistics.put(key, value);
        }
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
        this.schemaMigrations = new LinkedHashMap<>();
        this.scriptConfigs = new ConcurrentHashMap<>();
        this.environmentVariables = new ConcurrentHashMap<>();
        
        initializeFormatHandlers();
        initializeSchemaMigrations();
    }
    
    /**
     * 初始化格式处理器
     */
    private void initializeFormatHandlers() {
        formatHandlers.put(ExportFormat.JSON, new JsonFormatHandler());
        formatHandlers.put(ExportFormat.PROPERTIES, new PropertiesFormatHandler());
    }

    private void initializeSchemaMigrations() {
        registerSchemaMigration(LEGACY_SCHEMA_VERSION, (data, context) -> {
            if (!data.containsKey("exporter")) {
                data.put("exporter", "legacy-import");
            }
            context.addWarning("legacy 数据已迁移到 schemaVersion 4.0.0");
            return "4.0.0";
        });

        registerSchemaMigration("4.0.0", (data, context) -> {
            context.addWarning("schemaVersion 4.0.0 已迁移到 4.0.1");
            return CURRENT_SCHEMA_VERSION;
        });
    }

    public void registerSchemaMigration(String fromVersion, SchemaMigration migration) {
        if (fromVersion == null || fromVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("fromVersion must not be empty");
        }
        if (migration == null) {
            throw new IllegalArgumentException("migration must not be null");
        }
        schemaMigrations.put(normalizeSchemaVersion(fromVersion), migration);
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
            @SuppressWarnings("unchecked")
            Map<String, Object> rawData = (Map<String, Object>) handler.deserialize(content, Map.class);

            String originalSchemaVersion = resolveSchemaVersion(rawData);
            List<String> migrationWarnings = new ArrayList<>();
            Map<String, Object> migrationStats = new HashMap<>();
            Map<String, Object> data = normalizeImportData(rawData, migrationWarnings);
            String finalSchemaVersion = applySchemaMigrations(
                data,
                migrationWarnings,
                migrationStats,
                options.isStrictSchemaValidation()
            );
            
            // 验证数据
            if (options.isValidateData() && !validateData(data)) {
                return ImportExportResult.failure("数据验证失败", Arrays.asList("导入的数据格式不正确"));
            }

            if (options.isDryRun()) {
                ImportExportResult previewResult = previewImportData(data, options, migrationWarnings);
                previewResult.addStatistic("dryRun", true);
                previewResult.addStatistic("schemaVersion.original", originalSchemaVersion);
                previewResult.addStatistic("schemaVersion.final", finalSchemaVersion);
                migrationStats.forEach(previewResult::addStatistic);
                return previewResult;
            }
            
            // 导入数据
            ImportExportResult result = importData(data, options, migrationWarnings);
            result.addStatistic("dryRun", false);
            result.addStatistic("schemaVersion.original", originalSchemaVersion);
            result.addStatistic("schemaVersion.final", finalSchemaVersion);
            migrationStats.forEach(result::addStatistic);
            return result;
            
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
        return importScripts(inputPath, overwrite, false);
    }

    /**
     * 导入脚本配置（支持预检模式）
     *
     * @param inputPath 输入路径
     * @param overwrite 是否覆盖已存在的配置
     * @param dryRun 是否仅预检不写入
     * @return 导入结果
     */
    public ImportExportResult importScripts(String inputPath, boolean overwrite, boolean dryRun) {
        try {
            Path inputFile = Paths.get(inputPath);
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);
            
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null || !handler.canImport()) {
                return ImportExportResult.failure("不支持的格式: " + format, Arrays.asList("格式处理器不支持导入"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsData = (Map<String, Object>) handler.deserialize(content, Map.class);
            
            long imported = 0;
            long failed = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : scriptsData.entrySet()) {
                try {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = toScriptConfig(entry.getValue());
                    if (config == null) {
                        failed++;
                        errors.add("导入脚本失败: 配置为空, scriptId=" + scriptId);
                        continue;
                    }
                    
                    if (overwrite || !scriptConfigs.containsKey(scriptId)) {
                        if (!dryRun) {
                            scriptConfigs.put(scriptId, config);
                        }
                        imported++;
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("导入脚本失败: " + e.getMessage());
                }
            }
            
            if (failed > 0) {
                ImportExportResult result = ImportExportResult.partialSuccess(
                    "脚本导入完成， 部分失败", 
                    imported + failed, 
                    imported, 
                    failed, 
                    new ArrayList<>(), 
                    errors
                );
                result.addStatistic("dryRun", dryRun);
                return result;
            } else {
                ImportExportResult result = ImportExportResult.success(
                    dryRun ? "脚本导入预检成功（未写入）" : "脚本导入成功",
                    imported,
                    imported
                );
                result.addStatistic("dryRun", dryRun);
                return result;
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
        data.put("version", CURRENT_SCHEMA_VERSION);
        data.put("schemaVersion", CURRENT_SCHEMA_VERSION);
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
    private boolean validateData(Map<String, Object> data) {
        if (data == null) {
            return false;
        }
        Object version = data.get("version");
        if (version == null || String.valueOf(version).trim().isEmpty()) {
            return false;
        }

        boolean hasScripts = data.get("scripts") instanceof Map;
        boolean hasEnvironment = data.get("environment") instanceof Map;
        return hasScripts || hasEnvironment;
    }

    private String resolveSchemaVersion(Map<String, Object> data) {
        if (data == null) {
            return LEGACY_SCHEMA_VERSION;
        }

        Object schemaVersion = data.get("schemaVersion");
        if (schemaVersion != null && !String.valueOf(schemaVersion).trim().isEmpty()) {
            return normalizeSchemaVersion(String.valueOf(schemaVersion));
        }

        Object version = data.get("version");
        if (version != null && !String.valueOf(version).trim().isEmpty()) {
            return normalizeSchemaVersion(String.valueOf(version));
        }

        return LEGACY_SCHEMA_VERSION;
    }

    private String normalizeSchemaVersion(String rawVersion) {
        if (rawVersion == null) {
            return LEGACY_SCHEMA_VERSION;
        }
        String normalized = rawVersion.trim();
        if (normalized.isEmpty()) {
            return LEGACY_SCHEMA_VERSION;
        }
        if (LEGACY_SCHEMA_VERSION.equalsIgnoreCase(normalized)) {
            return LEGACY_SCHEMA_VERSION;
        }
        return normalized;
    }

    private String applySchemaMigrations(
            Map<String, Object> data,
            List<String> warnings,
            Map<String, Object> stats,
            boolean strictSchemaValidation) {

        String original = resolveSchemaVersion(data);
        String current = original;
        MigrationContext context = new MigrationContext(warnings, stats);

        while (!CURRENT_SCHEMA_VERSION.equals(current)) {
            SchemaMigration migration = schemaMigrations.get(current);
            if (migration == null) {
                if (strictSchemaValidation) {
                    throw new IllegalArgumentException("未知 schemaVersion: " + current);
                }
                warnings.add("未知 schemaVersion: " + current + "，按兼容模式继续处理");
                break;
            }

            String next = migration.migrate(data, context);
            if (next == null || next.trim().isEmpty()) {
                throw new IllegalStateException("schema 迁移返回了空版本, from=" + current);
            }
            String normalizedNext = normalizeSchemaVersion(next);
            if (normalizedNext.equals(current)) {
                throw new IllegalStateException("schema 迁移未推进版本, from=" + current);
            }

            context.addPathStep(current, normalizedNext);
            current = normalizedNext;
        }

        data.put("schemaVersion", current);
        data.put("version", current);
        stats.put("migration.steps", context.getStepCount());
        stats.put("migration.path", context.getPath());
        stats.put("migration.originalSchemaVersion", original);
        stats.put("migration.finalSchemaVersion", current);
        return current;
    }

    private ImportExportResult previewImportData(Map<String, Object> data, ImportExportOptions options, List<String> warnings) {
        long wouldImport = 0;
        long wouldFail = 0;
        List<String> errors = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.get("scripts");
            if (scriptsDataObj != null) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    SimpleScriptConfig config = toScriptConfig(entry.getValue());
                    if (config == null) {
                        wouldFail++;
                        errors.add("预检脚本配置失败: 配置为空, scriptId=" + entry.getKey());
                        continue;
                    }
                    if (options.isOverwriteExisting() || !scriptConfigs.containsKey(entry.getKey())) {
                        wouldImport++;
                    }
                }
            }
        } catch (Exception e) {
            wouldFail++;
            errors.add("预检脚本配置失败: " + e.getMessage());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envData = (Map<String, Object>) data.get("environment");
            if (envData != null && options.isIncludeEnvironment()) {
                for (String key : envData.keySet()) {
                    if (options.isOverwriteExisting() || !environmentVariables.containsKey(key)) {
                        wouldImport++;
                    }
                }
            }
        } catch (Exception e) {
            wouldFail++;
            errors.add("预检环境变量失败: " + e.getMessage());
        }

        if (wouldFail > 0 || !warnings.isEmpty()) {
            return ImportExportResult.partialSuccess(
                "数据预检完成（未写入）",
                wouldImport + wouldFail,
                wouldImport,
                wouldFail,
                warnings,
                errors
            );
        }

        return ImportExportResult.success("数据预检成功（未写入）", wouldImport, wouldImport);
    }
    
    /**
     * 导入数据
     */
    private ImportExportResult importData(Map<String, Object> data, ImportExportOptions options, List<String> warnings) {
        long totalImported = 0;
        long totalFailed = 0;
        List<String> errors = new ArrayList<>();
        
        // 导入脚本配置
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.get("scripts");
            if (scriptsDataObj != null) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = toScriptConfig(entry.getValue());
                    if (config == null) {
                        totalFailed++;
                        errors.add("导入脚本配置失败: 配置为空, scriptId=" + scriptId);
                        continue;
                    }
                    
                    if (options.isOverwriteExisting() || !scriptConfigs.containsKey(scriptId)) {
                        scriptConfigs.put(scriptId, config);
                        totalImported++;
                    }
                }
            }
        } catch (Exception e) {
            totalFailed++;
            errors.add("导入脚本配置失败: " + e.getMessage());
        }
        
        // 导入环境变量
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envData = (Map<String, Object>) data.get("environment");
            if (envData != null && options.isIncludeEnvironment()) {
                for (Map.Entry<String, Object> entry : envData.entrySet()) {
                    if (options.isOverwriteExisting() || !environmentVariables.containsKey(entry.getKey())) {
                        environmentVariables.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
                        totalImported++;
                    }
                }
            }
        } catch (Exception e) {
            totalFailed++;
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
        } else if (!warnings.isEmpty()) {
            return ImportExportResult.partialSuccess(
                "数据导入成功（包含兼容迁移提示）",
                totalImported,
                totalImported,
                0,
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
    private SimpleScriptConfig toScriptConfig(Object rawConfig) {
        if (rawConfig == null) {
            return null;
        }
        if (rawConfig instanceof SimpleScriptConfig) {
            return (SimpleScriptConfig) rawConfig;
        }
        if (rawConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> normalized = normalizeScriptConfigMap((Map<String, Object>) rawConfig);
            return gson.fromJson(gson.toJson(normalized), SimpleScriptConfig.class);
        }
        return gson.fromJson(gson.toJson(rawConfig), SimpleScriptConfig.class);
    }

    private Map<String, Object> normalizeImportData(Map<String, Object> rawData, List<String> warnings) {
        if (rawData == null) {
            return null;
        }

        Map<String, Object> normalized = new HashMap<>(rawData);

        Object scripts = normalized.get("scripts");
        if (!(scripts instanceof Map)) {
            Object legacyScripts = normalized.get("scriptConfigs");
            if (legacyScripts == null) {
                legacyScripts = normalized.get("script_configs");
            }
            if (legacyScripts == null) {
                legacyScripts = normalized.get("script-configs");
            }
            if (legacyScripts instanceof Map) {
                normalized.put("scripts", legacyScripts);
                warnings.add("检测到旧字段 scriptConfigs，已自动迁移为 scripts");
            }
        }

        Object environment = normalized.get("environment");
        if (!(environment instanceof Map)) {
            Object legacyEnvironment = normalized.get("environmentVariables");
            if (legacyEnvironment == null) {
                legacyEnvironment = normalized.get("environment_variables");
            }
            if (legacyEnvironment == null) {
                legacyEnvironment = normalized.get("env");
            }
            if (legacyEnvironment == null) {
                legacyEnvironment = normalized.get("envVars");
            }
            if (legacyEnvironment instanceof Map) {
                normalized.put("environment", legacyEnvironment);
                warnings.add("检测到旧字段 environmentVariables/env，已自动迁移为 environment");
            }
        }

        if (!(normalized.get("scripts") instanceof Map) && isLikelyScriptsOnlyPayload(rawData)) {
            normalized.put("scripts", rawData);
            warnings.add("检测到纯脚本导入结构，已自动按 scripts 节点兼容");
        }

        Object version = normalized.get("version");
        if (version == null || String.valueOf(version).trim().isEmpty()) {
            normalized.put("version", LEGACY_SCHEMA_VERSION);
            warnings.add("导入数据缺少 version，已按 legacy 兼容处理");
        }

        Object schemaVersion = normalized.get("schemaVersion");
        if (schemaVersion == null || String.valueOf(schemaVersion).trim().isEmpty()) {
            normalized.put("schemaVersion", normalized.get("version"));
        }

        return normalized;
    }

    private boolean isLikelyScriptsOnlyPayload(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        Set<String> knownMetaKeys = new HashSet<>(Arrays.asList(
            "version", "exportTime", "exporter", "options", "scripts", "environment", "cache"
        ));
        if (data.keySet().stream().anyMatch(knownMetaKeys::contains)) {
            return false;
        }

        int scriptLikeCount = 0;
        for (Object value : data.values()) {
            if (value instanceof Map && looksLikeScriptConfig((Map<?, ?>) value)) {
                scriptLikeCount++;
            }
        }
        return scriptLikeCount > 0;
    }

    private boolean looksLikeScriptConfig(Map<?, ?> configData) {
        return configData.containsKey("scriptType")
            || configData.containsKey("script_type")
            || configData.containsKey("scriptPath")
            || configData.containsKey("script_path");
    }

    private Map<String, Object> normalizeScriptConfigMap(Map<String, Object> rawConfig) {
        Map<String, Object> normalized = new HashMap<>(rawConfig);

        applyAlias(normalized, "scriptType", "script_type", "type");
        applyAlias(normalized, "scriptPath", "script_path", "path");
        applyAlias(normalized, "workingDirectory", "working_directory", "workdir");
        applyAlias(normalized, "retryCount", "retry_count");
        applyAlias(normalized, "timeout", "timeout_ms");
        applyAlias(normalized, "enabled", "is_enabled");
        applyAlias(normalized, "environment", "environmentVariables", "environment_variables", "env");

        return normalized;
    }

    private void applyAlias(Map<String, Object> target, String canonicalKey, String... aliases) {
        if (target.containsKey(canonicalKey) && target.get(canonicalKey) != null) {
            return;
        }
        for (String alias : aliases) {
            if (target.containsKey(alias)) {
                target.put(canonicalKey, target.get(alias));
                return;
            }
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
