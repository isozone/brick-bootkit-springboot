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
 * Import/export documentation.
 * Import/export documentation.
 *
 * @author starBlues
 * @since 4.0.1
 */
public class SimpleScriptImportExportManager {

    private static final String CURRENT_SCHEMA_VERSION = "4.0.1";
    private static final String LEGACY_SCHEMA_VERSION = "legacy";
    private static final int MAX_SCHEMA_MIGRATION_STEPS = 128;
    private static final int DEFAULT_MAX_PREVIEW_ITEMS = 200;
    
    /**
     * Import/export documentation.
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
            return JSON; // Legacy garbled comment removed.
        }
    }
    
    /**
     * Import/export documentation.
     */
    public static class ImportExportOptions {
        private final boolean includeScripts;
        private final boolean includeEnvironment;
        private final boolean includeCache;
        private final boolean overwriteExisting;
        private final boolean validateData;
        private final boolean dryRun;
        private final boolean strictSchemaValidation;
        private final int maxMigrationSteps;
        private final int maxPreviewItems;
        private final boolean createBackup;
        private final String backupLocation;
        private final Map<String, Object> metadata;
        
        private ImportExportOptions(Builder builder) {
            this.includeScripts = builder.includeScripts;
            this.includeEnvironment = builder.includeEnvironment;
            this.includeCache = builder.includeCache;
            this.overwriteExisting = builder.overwriteExisting;
            this.validateData = builder.validateData;
            this.dryRun = builder.dryRun;
            this.strictSchemaValidation = builder.strictSchemaValidation;
            this.maxMigrationSteps = builder.maxMigrationSteps;
            this.maxPreviewItems = builder.maxPreviewItems;
            this.createBackup = builder.createBackup;
            this.backupLocation = builder.backupLocation;
            this.metadata = new HashMap<>(builder.metadata);
        }
        
        public static class Builder {
            private boolean includeScripts = true;
            private boolean includeEnvironment = true;
            private boolean includeCache = false;
            private boolean overwriteExisting = false;
            private boolean validateData = true;
            private boolean dryRun = false;
            private boolean strictSchemaValidation = false;
            private int maxMigrationSteps = MAX_SCHEMA_MIGRATION_STEPS;
            private int maxPreviewItems = DEFAULT_MAX_PREVIEW_ITEMS;
            private boolean createBackup = true;
            private String backupLocation = "backups";
            private Map<String, Object> metadata = new HashMap<>();
            
            public Builder includeScripts(boolean include) {
                this.includeScripts = include;
                return this;
            }

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

            public Builder maxMigrationSteps(int maxMigrationSteps) {
                this.maxMigrationSteps = maxMigrationSteps;
                return this;
            }

            public Builder maxPreviewItems(int maxPreviewItems) {
                this.maxPreviewItems = maxPreviewItems;
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
                if (maxMigrationSteps <= 0) {
                    throw new IllegalArgumentException("maxMigrationSteps must be greater than 0");
                }
                if (maxPreviewItems <= 0) {
                    throw new IllegalArgumentException("maxPreviewItems must be greater than 0");
                }
                return new ImportExportOptions(this);
            }
        }
        
        public static Builder newBuilder() {
            return new Builder();
        }
        
        public boolean isIncludeScripts() { return includeScripts; }
        public boolean isIncludeEnvironment() { return includeEnvironment; }
        public boolean isIncludeCache() { return includeCache; }
        public boolean isOverwriteExisting() { return overwriteExisting; }
        public boolean isValidateData() { return validateData; }
        public boolean isDryRun() { return dryRun; }
        public boolean isStrictSchemaValidation() { return strictSchemaValidation; }
        public int getMaxMigrationSteps() { return maxMigrationSteps; }
        public int getMaxPreviewItems() { return maxPreviewItems; }
        public boolean isCreateBackup() { return createBackup; }
        public String getBackupLocation() { return backupLocation; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Import/export documentation.
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

        public void addWarning(String warningType, String warning) {
            warnings.add(formatWarning(warningType, warning));
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
     * Import/export documentation.
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
     * Import/export documentation.
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

        public void addWarning(String warningType, String warning) {
            warnings.add(formatWarning(warningType, warning));
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

    private static class AliasStatistics {
        private final Map<String, Integer> hitCounters = new LinkedHashMap<>();

        void hit(String aliasName) {
            hitCounters.merge(aliasName, 1, Integer::sum);
        }

        int totalHits() {
            return hitCounters.values().stream().mapToInt(Integer::intValue).sum();
        }

        boolean isEmpty() {
            return hitCounters.isEmpty();
        }

        Map<String, Integer> snapshot() {
            return new LinkedHashMap<>(hitCounters);
        }
    }

    private static class ImportBreakdown {
        private long scriptsTotal;
        private long scriptsImported;
        private long scriptsSkipped;
        private long scriptsFailed;
        private final List<String> scriptsWouldImportIds = new ArrayList<>();
        private final List<String> scriptsWouldSkipIds = new ArrayList<>();
        private final List<String> scriptsWouldFailIds = new ArrayList<>();
        private long environmentTotal;
        private long environmentImported;
        private long environmentSkipped;
        private long environmentFailed;
        private final List<String> environmentWouldImportKeys = new ArrayList<>();
        private final List<String> environmentWouldSkipKeys = new ArrayList<>();
        private final List<String> environmentWouldFailKeys = new ArrayList<>();
    }
    
    /**
     * Import/export documentation.
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
     * Import/export documentation.
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
     * Import/export documentation.
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
     * Import/export documentation.
     */
    private final Map<String, SimpleScriptConfig> scriptConfigs;
    private final Map<String, String> environmentVariables;
    
    /**
     * Import/export documentation.
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
     * Import/export documentation.
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
            context.addWarning("migration.legacy", "Legacy schema warning");
            return "4.0.0";
        });

        registerSchemaMigration("4.0.0", (data, context) -> {
            context.addWarning("migration.default", "Legacy schema warning");
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
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult exportAll(String outputPath, ExportFormat format, ImportExportOptions options) {
        try {
            ImportExportOptions safeOptions = options != null
                ? options
                : ImportExportOptions.newBuilder().build();
            // Legacy garbled comment removed.
            Map<String, Object> exportData = collectExportData(safeOptions);
            
            // Legacy garbled comment removed.
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("Unsupported export format: " + format, Arrays.asList("Unsupported export format"));
            }
            
            String serializedData = handler.serialize(exportData);
            
            // Legacy garbled comment removed.
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "Export completed: " + outputPath,
                getTotalItems(exportData), 
                getTotalItems(exportData)
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("Import/export failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult importAll(String inputPath, ImportExportOptions options) {
        try {
            ImportExportOptions safeOptions = options != null
                ? options
                : ImportExportOptions.newBuilder().build();
            Path inputFile = Paths.get(inputPath);
            if (!Files.exists(inputFile)) {
                return ImportExportResult.failure("File does not exist: " + inputPath, Arrays.asList("File not found"));
            }
            
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);
            
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null || !handler.canImport()) {
                return ImportExportResult.failure("Unsupported format: " + format, Arrays.asList("Format handler cannot import"));
            }
            
            // Legacy garbled comment removed.
            @SuppressWarnings("unchecked")
            Map<String, Object> rawData = (Map<String, Object>) handler.deserialize(content, Map.class);

            String originalSchemaVersion = resolveSchemaVersion(rawData);
            List<String> migrationWarnings = new ArrayList<>();
            Map<String, Object> migrationStats = new HashMap<>();
            AliasStatistics aliasStatistics = new AliasStatistics();
            Map<String, Object> data = normalizeImportData(rawData, migrationWarnings, aliasStatistics);
            ImportBreakdown importBreakdown = calculateImportBreakdown(data, safeOptions);
            String finalSchemaVersion;
            try {
                finalSchemaVersion = applySchemaMigrations(
                    data,
                    migrationWarnings,
                    migrationStats,
                    safeOptions.isStrictSchemaValidation(),
                    safeOptions.getMaxMigrationSteps()
                );
            } catch (Exception migrationException) {
                ImportExportResult failureResult = ImportExportResult.failure(
                    "Import failed: " + migrationException.getMessage(),
                    Arrays.asList(migrationException.getMessage())
                );
                failureResult.addStatistic("dryRun", safeOptions.isDryRun());
                failureResult.addStatistic("schemaVersion.original", originalSchemaVersion);
                migrationStats.forEach(failureResult::addStatistic);
                addAliasStatistics(failureResult, aliasStatistics);
                addWarningStatistics(failureResult, migrationWarnings);
                addImportBreakdownStatistics(failureResult, importBreakdown);
                return failureResult;
            }
            
            // Legacy garbled comment removed.
            if (safeOptions.isValidateData() && !validateData(data, safeOptions)) {
                ImportExportResult validateFailure = ImportExportResult.failure(
                    "Data validation failed",
                    Arrays.asList("Invalid import payload format")
                );
                validateFailure.addStatistic("dryRun", safeOptions.isDryRun());
                validateFailure.addStatistic("schemaVersion.original", originalSchemaVersion);
                validateFailure.addStatistic("schemaVersion.final", finalSchemaVersion);
                migrationStats.forEach(validateFailure::addStatistic);
                addAliasStatistics(validateFailure, aliasStatistics);
                addWarningStatistics(validateFailure, migrationWarnings);
                addImportBreakdownStatistics(validateFailure, importBreakdown);
                return validateFailure;
            }

            if (safeOptions.isDryRun()) {
                ImportExportResult previewResult = previewImportData(data, safeOptions, migrationWarnings, aliasStatistics);
                previewResult.addStatistic("dryRun", true);
                previewResult.addStatistic("schemaVersion.original", originalSchemaVersion);
                previewResult.addStatistic("schemaVersion.final", finalSchemaVersion);
                migrationStats.forEach(previewResult::addStatistic);
                addAliasStatistics(previewResult, aliasStatistics);
                addWarningStatistics(previewResult, migrationWarnings);
                addImportBreakdownStatistics(previewResult, importBreakdown);
                addDryRunProjectionStatistics(previewResult, importBreakdown, safeOptions.getMaxPreviewItems());
                return previewResult;
            }
            
            // Legacy garbled comment removed.
            ImportExportResult result = importData(data, safeOptions, migrationWarnings, aliasStatistics);
            result.addStatistic("dryRun", false);
            result.addStatistic("schemaVersion.original", originalSchemaVersion);
            result.addStatistic("schemaVersion.final", finalSchemaVersion);
            migrationStats.forEach(result::addStatistic);
            addAliasStatistics(result, aliasStatistics);
            addWarningStatistics(result, migrationWarnings);
            addImportBreakdownStatistics(result, importBreakdown);
            return result;
            
        } catch (Exception e) {
            return ImportExportResult.failure("Import/export failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult exportEnvironment(String outputPath, ExportFormat format) {
        try {
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("Unsupported export format: " + format, Arrays.asList("Unsupported export format"));
            }
            
            String serializedData = handler.serialize(environmentVariables);
            
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "Environment export completed: " + outputPath,
                environmentVariables.size(), 
                environmentVariables.size()
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("Import/export failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult importEnvironment(String inputPath, boolean overwrite) {
        return importEnvironment(inputPath, overwrite, false);
    }

    public ImportExportResult importEnvironment(String inputPath, boolean overwrite, boolean dryRun) {
        return importEnvironment(inputPath, overwrite, dryRun, DEFAULT_MAX_PREVIEW_ITEMS);
    }

    public ImportExportResult importEnvironment(String inputPath, boolean overwrite, boolean dryRun, int maxPreviewItems) {
        try {
            if (maxPreviewItems <= 0) {
                throw new IllegalArgumentException("maxPreviewItems must be greater than 0");
            }
            Path inputFile = Paths.get(inputPath);
            if (!Files.exists(inputFile)) {
                return ImportExportResult.failure("File does not exist: " + inputPath, Arrays.asList("File not found"));
            }
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);

            FormatHandler handler = formatHandlers.get(format);
            if (handler == null || !handler.canImport()) {
                return ImportExportResult.failure("Unsupported format: " + format, Arrays.asList("Format handler cannot import"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> envData = (Map<String, Object>) handler.deserialize(content, Map.class);
            List<String> wouldImportKeys = new ArrayList<>();
            List<String> wouldSkipKeys = new ArrayList<>();
            List<String> wouldFailKeys = new ArrayList<>();

            if (envData == null) {
                ImportExportResult result = ImportExportResult.success("Environment import succeeded", 0, 0);
                result.addStatistic("dryRun", dryRun);
                result.addStatistic("environment.total", 0L);
                result.addStatistic("environment.imported", 0L);
                result.addStatistic("environment.skipped", 0L);
                result.addStatistic("environment.failed", 0L);
                if (dryRun) {
                    addEnvironmentDryRunProjectionStatistics(
                        result,
                        0L,
                        0L,
                        0L,
                        0L,
                        wouldImportKeys,
                        wouldSkipKeys,
                        wouldFailKeys,
                        maxPreviewItems
                    );
                }
                return result;
            }

            long imported = 0;
            long skipped = 0;
            long failed = 0;
            for (Map.Entry<String, Object> entry : envData.entrySet()) {
                String envKey = entry.getKey();
                if (envKey == null || envKey.trim().isEmpty()) {
                    failed++;
                    wouldFailKeys.add("<blank-env-key>");
                    continue;
                }
                if (overwrite || !environmentVariables.containsKey(envKey)) {
                    if (!dryRun) {
                        environmentVariables.put(
                            envKey,
                            entry.getValue() == null ? null : String.valueOf(entry.getValue())
                        );
                    }
                    imported++;
                    wouldImportKeys.add(envKey);
                } else {
                    skipped++;
                    wouldSkipKeys.add(envKey);
                }
            }

            ImportExportResult result;
            if (failed > 0) {
                result = ImportExportResult.partialSuccess(
                    dryRun ? "Environment import preview completed with partial failures (no write)" : "Environment import completed with partial failures",
                    envData.size(),
                    imported,
                    failed,
                    new ArrayList<>(),
                    Arrays.asList("Some environment entries were invalid and skipped")
                );
            } else {
                result = ImportExportResult.success(
                    dryRun ? "Environment import preview succeeded (no write)" : "Environment import succeeded",
                    envData.size(),
                    imported
                );
            }
            result.addStatistic("dryRun", dryRun);
            result.addStatistic("environment.total", (long) envData.size());
            result.addStatistic("environment.imported", imported);
            result.addStatistic("environment.skipped", skipped);
            result.addStatistic("environment.failed", failed);
            if (dryRun) {
                addEnvironmentDryRunProjectionStatistics(
                    result,
                    (long) envData.size(),
                    imported,
                    skipped,
                    failed,
                    wouldImportKeys,
                    wouldSkipKeys,
                    wouldFailKeys,
                    maxPreviewItems
                );
            }
            return result;

        } catch (Exception e) {
            return ImportExportResult.failure("Environment import failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }

    /**
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
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
                "Script export completed: " + outputPath,
                processed, 
                processed
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("Import/export failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult importScripts(String inputPath, boolean overwrite) {
        return importScripts(inputPath, overwrite, false);
    }

    /**
     * Import/export documentation.
     *
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     * Import/export documentation.
     */
    public ImportExportResult importScripts(String inputPath, boolean overwrite, boolean dryRun) {
        return importScripts(inputPath, overwrite, dryRun, DEFAULT_MAX_PREVIEW_ITEMS);
    }

    public ImportExportResult importScripts(String inputPath, boolean overwrite, boolean dryRun, int maxPreviewItems) {
        try {
            if (maxPreviewItems <= 0) {
                throw new IllegalArgumentException("maxPreviewItems must be greater than 0");
            }
            Path inputFile = Paths.get(inputPath);
            String content = new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8);
            ExportFormat format = ExportFormat.fromFileName(inputPath);

            FormatHandler handler = formatHandlers.get(format);
            if (handler == null || !handler.canImport()) {
                return ImportExportResult.failure("Unsupported format: " + format, Arrays.asList("Format handler cannot import"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsData = (Map<String, Object>) handler.deserialize(content, Map.class);
            if (scriptsData == null) {
                scriptsData = new LinkedHashMap<>();
            }

            long imported = 0;
            long skipped = 0;
            long failed = 0;
            List<String> errors = new ArrayList<>();
            AliasStatistics aliasStatistics = new AliasStatistics();
            List<String> wouldImportIds = new ArrayList<>();
            List<String> wouldSkipIds = new ArrayList<>();
            List<String> wouldFailIds = new ArrayList<>();

            for (Map.Entry<String, Object> entry : scriptsData.entrySet()) {
                try {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = toScriptConfig(entry.getValue(), aliasStatistics);
                    if (config == null) {
                        failed++;
                        wouldFailIds.add(scriptId == null || scriptId.trim().isEmpty() ? "<blank-script-id>" : scriptId);
                        errors.add("Script config is null, scriptId=" + scriptId);
                        continue;
                    }

                    if (overwrite || !scriptConfigs.containsKey(scriptId)) {
                        if (!dryRun) {
                            scriptConfigs.put(scriptId, config);
                        }
                        imported++;
                        wouldImportIds.add(scriptId);
                    } else {
                        skipped++;
                        wouldSkipIds.add(scriptId);
                    }
                } catch (Exception e) {
                    failed++;
                    String scriptId = entry.getKey();
                    wouldFailIds.add(scriptId == null || scriptId.trim().isEmpty() ? "<blank-script-id>" : scriptId);
                    errors.add("Script import failed: " + e.getMessage());
                }
            }

            ImportExportResult result;
            if (failed > 0) {
                result = ImportExportResult.partialSuccess(
                    "Script import completed with partial failures",
                    imported + failed,
                    imported,
                    failed,
                    new ArrayList<>(),
                    errors
                );
            } else {
                result = ImportExportResult.success(
                    dryRun ? "Script import preview succeeded (no write)" : "Script import succeeded",
                    imported,
                    imported
                );
            }
            result.addStatistic("dryRun", dryRun);
            result.addStatistic("scripts.total", (long) scriptsData.size());
            result.addStatistic("scripts.imported", imported);
            result.addStatistic("scripts.skipped", skipped);
            result.addStatistic("scripts.failed", failed);
            if (dryRun) {
                addScriptsDryRunProjectionStatistics(
                    result,
                    (long) scriptsData.size(),
                    imported,
                    skipped,
                    failed,
                    wouldImportIds,
                    wouldSkipIds,
                    wouldFailIds,
                    maxPreviewItems
                );
            }
            addAliasStatistics(result, aliasStatistics);
            return result;

        } catch (Exception e) {
            return ImportExportResult.failure("Script import failed: " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }

    public void addScriptConfig(String scriptId, SimpleScriptConfig config) {
        scriptConfigs.put(scriptId, config);
    }
    
    /**
     * Import/export documentation.
     */
    public SimpleScriptConfig getScriptConfig(String scriptId) {
        return scriptConfigs.get(scriptId);
    }
    
    /**
     * Import/export documentation.
     */
    public SimpleScriptConfig removeScriptConfig(String scriptId) {
        return scriptConfigs.remove(scriptId);
    }
    
    /**
     * Import/export documentation.
     */
    public List<String> getAllScriptIds() {
        return new ArrayList<>(scriptConfigs.keySet());
    }
    
    /**
     * Import/export documentation.
     */
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }
    
    /**
     * Import/export documentation.
     */
    public String getEnvironmentVariable(String key) {
        return environmentVariables.get(key);
    }
    
    /**
     * Import/export documentation.
     */
    public String removeEnvironmentVariable(String key) {
        return environmentVariables.remove(key);
    }
    
    /**
     * Import/export documentation.
     */
    public Map<String, String> getAllEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }
    
    // Legacy garbled comment removed.
    
    /**
     * Import/export documentation.
     */
    private Map<String, Object> collectExportData(ImportExportOptions options) {
        Map<String, Object> data = new HashMap<>();
        
        // Legacy garbled comment removed.
        data.put("version", CURRENT_SCHEMA_VERSION);
        data.put("schemaVersion", CURRENT_SCHEMA_VERSION);
        data.put("exportTime", LocalDateTime.now().toString());
        data.put("exporter", "SimpleScriptImportExportManager");
        data.put("options", options);
        
        // Legacy garbled comment removed.
        if (options.isIncludeScripts()) {
            data.put("scripts", new HashMap<>(scriptConfigs));
        }
        
        // Legacy garbled comment removed.
        if (options.isIncludeEnvironment()) {
            data.put("environment", new HashMap<>(environmentVariables));
        }
        
        // Legacy garbled comment removed.
        if (options.isIncludeCache()) {
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("cacheEnabled", true);
            cacheData.put("cacheSize", 0);
            data.put("cache", cacheData);
        }
        
        return data;
    }
    
    /**
     * Import/export documentation.
     */
    private boolean validateData(Map<String, Object> data, ImportExportOptions options) {
        if (data == null) {
            return false;
        }
        Object version = data.get("version");
        if (version == null || String.valueOf(version).trim().isEmpty()) {
            return false;
        }

        ImportExportOptions safeOptions = options != null
            ? options
            : ImportExportOptions.newBuilder().build();
        boolean hasScripts = data.get("scripts") instanceof Map;
        boolean hasEnvironment = data.get("environment") instanceof Map;

        boolean scriptsEnabled = safeOptions.isIncludeScripts();
        boolean environmentEnabled = safeOptions.isIncludeEnvironment();
        if (!scriptsEnabled && !environmentEnabled) {
            return false;
        }
        return (scriptsEnabled && hasScripts) || (environmentEnabled && hasEnvironment);
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
            boolean strictSchemaValidation,
            int maxMigrationSteps) {

        String original = resolveSchemaVersion(data);
        String current = original;
        MigrationContext context = new MigrationContext(warnings, stats);
        Set<String> visitedVersions = new LinkedHashSet<>();
        int migrationSteps = 0;

        while (!CURRENT_SCHEMA_VERSION.equals(current)) {
            if (!visitedVersions.add(current)) {
                String message = "Schema migration cycle detected at version: " + current;
                recordMigrationFailureStats(stats, context, original, current, "cycle", message, maxMigrationSteps);
                throw new IllegalStateException(message);
            }
            if (migrationSteps >= maxMigrationSteps) {
                String message = "Schema migration step limit exceeded: " + maxMigrationSteps;
                recordMigrationFailureStats(stats, context, original, current, "step_limit", message, maxMigrationSteps);
                throw new IllegalStateException(message);
            }

            SchemaMigration migration = schemaMigrations.get(current);
            if (migration == null) {
                if (strictSchemaValidation) {
                    String message = "Unsupported schemaVersion: " + current;
                    recordMigrationFailureStats(stats, context, original, current, "unknown_schema", message, maxMigrationSteps);
                    throw new IllegalArgumentException(message);
                }
                addTypedWarning(warnings, "migration.unknown-version",
                    "Unsupported schemaVersion: " + current + ", continue in compatibility mode");
                break;
            }

            String next = migration.migrate(data, context);
            if (next == null || next.trim().isEmpty()) {
                String message = "Schema migration returned an empty target version, from=" + current;
                recordMigrationFailureStats(stats, context, original, current, "empty_target", message, maxMigrationSteps);
                throw new IllegalStateException(message);
            }
            String normalizedNext = normalizeSchemaVersion(next);
            if (normalizedNext.equals(current)) {
                String message = "Schema migration did not advance version, from=" + current;
                recordMigrationFailureStats(stats, context, original, current, "no_progress", message, maxMigrationSteps);
                throw new IllegalStateException(message);
            }

            context.addPathStep(current, normalizedNext);
            current = normalizedNext;
            migrationSteps++;
        }

        data.put("schemaVersion", current);
        data.put("version", current);
        stats.put("migration.steps", context.getStepCount());
        stats.put("migration.path", context.getPath());
        stats.put("migration.visitedVersions", new ArrayList<>(visitedVersions));
        stats.put("migration.maxSteps", maxMigrationSteps);
        stats.put("migration.originalSchemaVersion", original);
        stats.put("migration.finalSchemaVersion", current);
        return current;
    }

    private ImportExportResult previewImportData(
            Map<String, Object> data,
            ImportExportOptions options,
            List<String> warnings,
            AliasStatistics aliasStatistics) {
        long wouldImport = 0;
        long wouldFail = 0;
        List<String> errors = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.get("scripts");
            if (scriptsDataObj != null && options.isIncludeScripts()) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    SimpleScriptConfig config = toScriptConfig(entry.getValue(), aliasStatistics);
                    if (config == null) {
                        wouldFail++;
                        errors.add("Invalid script config, scriptId=" + entry.getKey());
                        continue;
                    }
                    if (options.isOverwriteExisting() || !scriptConfigs.containsKey(entry.getKey())) {
                        wouldImport++;
                    }
                }
            }
        } catch (Exception e) {
            wouldFail++;
            errors.add("Preview failed while parsing scripts: " + e.getMessage());
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
            errors.add("Preview failed while parsing environment: " + e.getMessage());
        }

        if (wouldFail > 0 || !warnings.isEmpty()) {
            return ImportExportResult.partialSuccess(
                "Import preview completed (no write), with warnings/errors",
                wouldImport + wouldFail,
                wouldImport,
                wouldFail,
                warnings,
                errors
            );
        }

        return ImportExportResult.success("Import preview succeeded (no write)", wouldImport, wouldImport);
    }
    
    /**
     * Import/export documentation.
     */
    private ImportExportResult importData(
            Map<String, Object> data,
            ImportExportOptions options,
            List<String> warnings,
            AliasStatistics aliasStatistics) {
        long totalImported = 0;
        long totalFailed = 0;
        List<String> errors = new ArrayList<>();
        
        // Legacy garbled comment removed.
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.get("scripts");
            if (scriptsDataObj != null && options.isIncludeScripts()) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = toScriptConfig(entry.getValue(), aliasStatistics);
                    if (config == null) {
                        totalFailed++;
                        errors.add("Invalid script config, scriptId=" + scriptId);
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
            errors.add("Import failed while parsing scripts: " + e.getMessage());
        }
        
        // Legacy garbled comment removed.
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
            errors.add("Import failed while parsing environment: " + e.getMessage());
        }
        
        if (totalFailed > 0) {
            return ImportExportResult.partialSuccess(
                "Data import completed with partial failures", 
                totalImported + totalFailed, 
                totalImported, 
                totalFailed, 
                warnings, 
                errors
            );
        } else if (!warnings.isEmpty()) {
            return ImportExportResult.partialSuccess(
                "Data import succeeded with compatibility warnings",
                totalImported,
                totalImported,
                0,
                warnings,
                errors
            );
        } else {
            return ImportExportResult.success("Data import succeeded", totalImported, totalImported);
        }
    }
    
    /**
     * Import/export documentation.
     */
    private SimpleScriptConfig toScriptConfig(Object rawConfig) {
        return toScriptConfig(rawConfig, null);
    }

    private SimpleScriptConfig toScriptConfig(Object rawConfig, AliasStatistics aliasStatistics) {
        if (rawConfig == null) {
            return null;
        }
        if (rawConfig instanceof SimpleScriptConfig) {
            return (SimpleScriptConfig) rawConfig;
        }
        if (rawConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> normalized = normalizeScriptConfigMap((Map<String, Object>) rawConfig, aliasStatistics);
            return gson.fromJson(gson.toJson(normalized), SimpleScriptConfig.class);
        }
        return gson.fromJson(gson.toJson(rawConfig), SimpleScriptConfig.class);
    }

    private Map<String, Object> normalizeImportData(
            Map<String, Object> rawData,
            List<String> warnings,
            AliasStatistics aliasStatistics) {
        if (rawData == null) {
            return null;
        }

        Map<String, Object> normalized = new HashMap<>(rawData);

        Object scripts = normalized.get("scripts");
        if (!(scripts instanceof Map)) {
            String legacyScriptsKey = "scriptConfigs";
            Object legacyScripts = normalized.get("scriptConfigs");
            if (legacyScripts == null) {
                legacyScriptsKey = "script_configs";
                legacyScripts = normalized.get("script_configs");
            }
            if (legacyScripts == null) {
                legacyScriptsKey = "script-configs";
                legacyScripts = normalized.get("script-configs");
            }
            if (legacyScripts instanceof Map) {
                normalized.put("scripts", legacyScripts);
                addTypedWarning(warnings, "compat.legacy-scripts-key",
                    "Detected legacy key scriptConfigs and remapped to scripts");
                if (aliasStatistics != null) {
                    aliasStatistics.hit("root.scripts<-" + legacyScriptsKey);
                }
            }
        }

        Object environment = normalized.get("environment");
        if (!(environment instanceof Map)) {
            String legacyEnvKey = "environmentVariables";
            Object legacyEnvironment = normalized.get("environmentVariables");
            if (legacyEnvironment == null) {
                legacyEnvKey = "environment_variables";
                legacyEnvironment = normalized.get("environment_variables");
            }
            if (legacyEnvironment == null) {
                legacyEnvKey = "env";
                legacyEnvironment = normalized.get("env");
            }
            if (legacyEnvironment == null) {
                legacyEnvKey = "envVars";
                legacyEnvironment = normalized.get("envVars");
            }
            if (legacyEnvironment instanceof Map) {
                normalized.put("environment", legacyEnvironment);
                addTypedWarning(warnings, "compat.legacy-environment-key",
                    "Detected legacy key environmentVariables/env and remapped to environment");
                if (aliasStatistics != null) {
                    aliasStatistics.hit("root.environment<-" + legacyEnvKey);
                }
            }
        }

        if (!(normalized.get("scripts") instanceof Map) && isLikelyScriptsOnlyPayload(rawData)) {
            normalized.put("scripts", rawData);
            addTypedWarning(warnings, "compat.scripts-only-payload",
                "Detected scripts-only payload and wrapped as scripts node");
        }

        Object version = normalized.get("version");
        if (version == null || String.valueOf(version).trim().isEmpty()) {
            normalized.put("version", LEGACY_SCHEMA_VERSION);
            addTypedWarning(warnings, "compat.missing-version",
                "Import data misses version, fallback to legacy compatibility");
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

    private Map<String, Object> normalizeScriptConfigMap(
            Map<String, Object> rawConfig,
            AliasStatistics aliasStatistics) {
        Map<String, Object> normalized = new HashMap<>(rawConfig);

        applyAlias(normalized, aliasStatistics, "scriptType", "script_type", "type");
        applyAlias(normalized, aliasStatistics, "scriptPath", "script_path", "path");
        applyAlias(normalized, aliasStatistics, "workingDirectory", "working_directory", "workdir");
        applyAlias(normalized, aliasStatistics, "retryCount", "retry_count");
        applyAlias(normalized, aliasStatistics, "timeout", "timeout_ms");
        applyAlias(normalized, aliasStatistics, "enabled", "is_enabled");
        applyAlias(normalized, aliasStatistics, "environment", "environmentVariables", "environment_variables", "env");

        return normalized;
    }

    private void applyAlias(
            Map<String, Object> target,
            AliasStatistics aliasStatistics,
            String canonicalKey,
            String... aliases) {
        if (target.containsKey(canonicalKey) && target.get(canonicalKey) != null) {
            return;
        }
        for (String alias : aliases) {
            if (target.containsKey(alias)) {
                target.put(canonicalKey, target.get(alias));
                if (aliasStatistics != null) {
                    aliasStatistics.hit("script." + canonicalKey + "<-" + alias);
                }
                return;
            }
        }
    }

    private static String formatWarning(String warningType, String warning) {
        String safeType = (warningType == null || warningType.trim().isEmpty()) ? "general" : warningType.trim();
        String safeWarning = warning == null ? "" : warning;
        return "[" + safeType + "] " + safeWarning;
    }

    private void addTypedWarning(List<String> warnings, String warningType, String warning) {
        warnings.add(formatWarning(warningType, warning));
    }

    private void addAliasStatistics(ImportExportResult result, AliasStatistics aliasStatistics) {
        if (result == null || aliasStatistics == null || aliasStatistics.isEmpty()) {
            return;
        }
        result.addStatistic("alias.hits.total", aliasStatistics.totalHits());
        result.addStatistic("alias.hits.byField", aliasStatistics.snapshot());
    }

    private ImportBreakdown calculateImportBreakdown(Map<String, Object> data, ImportExportOptions options) {
        ImportBreakdown breakdown = new ImportBreakdown();
        if (data == null) {
            return breakdown;
        }
        ImportExportOptions safeOptions = options != null
            ? options
            : ImportExportOptions.newBuilder().build();

        Object scriptsObject = data.get("scripts");
        if (scriptsObject instanceof Map && safeOptions.isIncludeScripts()) {
            Map<?, ?> scriptsData = (Map<?, ?>) scriptsObject;
            breakdown.scriptsTotal = scriptsData.size();

            for (Map.Entry<?, ?> entry : scriptsData.entrySet()) {
                String scriptId = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                try {
                    SimpleScriptConfig config = toScriptConfig(entry.getValue(), null);
                    if (config == null || scriptId == null || scriptId.trim().isEmpty()) {
                        breakdown.scriptsFailed++;
                        breakdown.scriptsWouldFailIds.add(
                            (scriptId == null || scriptId.trim().isEmpty()) ? "<blank-script-id>" : scriptId
                        );
                        continue;
                    }
                    if (safeOptions.isOverwriteExisting() || !scriptConfigs.containsKey(scriptId)) {
                        breakdown.scriptsImported++;
                        breakdown.scriptsWouldImportIds.add(scriptId);
                    } else {
                        breakdown.scriptsSkipped++;
                        breakdown.scriptsWouldSkipIds.add(scriptId);
                    }
                } catch (Exception ex) {
                    breakdown.scriptsFailed++;
                    breakdown.scriptsWouldFailIds.add(
                        (scriptId == null || scriptId.trim().isEmpty()) ? "<blank-script-id>" : scriptId
                    );
                }
            }
        }

        if (!safeOptions.isIncludeEnvironment()) {
            return breakdown;
        }

        Object envObject = data.get("environment");
        if (!(envObject instanceof Map)) {
            return breakdown;
        }

        Map<?, ?> envData = (Map<?, ?>) envObject;
        breakdown.environmentTotal = envData.size();
        for (Map.Entry<?, ?> entry : envData.entrySet()) {
            String envKey = entry.getKey() == null ? null : String.valueOf(entry.getKey());
            if (envKey == null || envKey.trim().isEmpty()) {
                breakdown.environmentFailed++;
                breakdown.environmentWouldFailKeys.add("<blank-env-key>");
                continue;
            }
            if (safeOptions.isOverwriteExisting() || !environmentVariables.containsKey(envKey)) {
                breakdown.environmentImported++;
                breakdown.environmentWouldImportKeys.add(envKey);
            } else {
                breakdown.environmentSkipped++;
                breakdown.environmentWouldSkipKeys.add(envKey);
            }
        }
        return breakdown;
    }

    private void addImportBreakdownStatistics(ImportExportResult result, ImportBreakdown breakdown) {
        if (result == null || breakdown == null) {
            return;
        }
        result.addStatistic("scripts.total", breakdown.scriptsTotal);
        result.addStatistic("scripts.imported", breakdown.scriptsImported);
        result.addStatistic("scripts.skipped", breakdown.scriptsSkipped);
        result.addStatistic("scripts.failed", breakdown.scriptsFailed);
        result.addStatistic("environment.total", breakdown.environmentTotal);
        result.addStatistic("environment.imported", breakdown.environmentImported);
        result.addStatistic("environment.skipped", breakdown.environmentSkipped);
        result.addStatistic("environment.failed", breakdown.environmentFailed);
    }

    private void addDryRunProjectionStatistics(
            ImportExportResult result,
            ImportBreakdown breakdown,
            int maxPreviewItems) {
        if (result == null || breakdown == null) {
            return;
        }
        List<String> scriptsWouldImportIds = limitPreviewItems(breakdown.scriptsWouldImportIds, maxPreviewItems);
        List<String> scriptsWouldSkipIds = limitPreviewItems(breakdown.scriptsWouldSkipIds, maxPreviewItems);
        List<String> scriptsWouldFailIds = limitPreviewItems(breakdown.scriptsWouldFailIds, maxPreviewItems);
        List<String> envWouldImportKeys = limitPreviewItems(breakdown.environmentWouldImportKeys, maxPreviewItems);
        List<String> envWouldSkipKeys = limitPreviewItems(breakdown.environmentWouldSkipKeys, maxPreviewItems);
        List<String> envWouldFailKeys = limitPreviewItems(breakdown.environmentWouldFailKeys, maxPreviewItems);

        long scriptsImportTruncated = breakdown.scriptsWouldImportIds.size() - scriptsWouldImportIds.size();
        long scriptsSkipTruncated = breakdown.scriptsWouldSkipIds.size() - scriptsWouldSkipIds.size();
        long scriptsFailTruncated = breakdown.scriptsWouldFailIds.size() - scriptsWouldFailIds.size();
        long envImportTruncated = breakdown.environmentWouldImportKeys.size() - envWouldImportKeys.size();
        long envSkipTruncated = breakdown.environmentWouldSkipKeys.size() - envWouldSkipKeys.size();
        long envFailTruncated = breakdown.environmentWouldFailKeys.size() - envWouldFailKeys.size();
        long totalAvailable = breakdown.scriptsWouldImportIds.size()
            + breakdown.scriptsWouldSkipIds.size()
            + breakdown.scriptsWouldFailIds.size()
            + breakdown.environmentWouldImportKeys.size()
            + breakdown.environmentWouldSkipKeys.size()
            + breakdown.environmentWouldFailKeys.size();
        long totalReturned = scriptsWouldImportIds.size()
            + scriptsWouldSkipIds.size()
            + scriptsWouldFailIds.size()
            + envWouldImportKeys.size()
            + envWouldSkipKeys.size()
            + envWouldFailKeys.size();
        long totalTruncated = totalAvailable - totalReturned;

        result.addStatistic("scripts.wouldTotal", breakdown.scriptsTotal);
        result.addStatistic("scripts.wouldImport", breakdown.scriptsImported);
        result.addStatistic("scripts.wouldSkip", breakdown.scriptsSkipped);
        result.addStatistic("scripts.wouldFail", breakdown.scriptsFailed);
        result.addStatistic("scripts.wouldImport.ids", scriptsWouldImportIds);
        result.addStatistic("scripts.wouldSkip.ids", scriptsWouldSkipIds);
        result.addStatistic("scripts.wouldFail.ids", scriptsWouldFailIds);
        result.addStatistic("scripts.wouldImport.ids.truncatedCount", scriptsImportTruncated);
        result.addStatistic("scripts.wouldSkip.ids.truncatedCount", scriptsSkipTruncated);
        result.addStatistic("scripts.wouldFail.ids.truncatedCount", scriptsFailTruncated);

        result.addStatistic("environment.wouldTotal", breakdown.environmentTotal);
        result.addStatistic("environment.wouldImport", breakdown.environmentImported);
        result.addStatistic("environment.wouldSkip", breakdown.environmentSkipped);
        result.addStatistic("environment.wouldFail", breakdown.environmentFailed);
        result.addStatistic("environment.wouldImport.keys", envWouldImportKeys);
        result.addStatistic("environment.wouldSkip.keys", envWouldSkipKeys);
        result.addStatistic("environment.wouldFail.keys", envWouldFailKeys);
        result.addStatistic("environment.wouldImport.keys.truncatedCount", envImportTruncated);
        result.addStatistic("environment.wouldSkip.keys.truncatedCount", envSkipTruncated);
        result.addStatistic("environment.wouldFail.keys.truncatedCount", envFailTruncated);

        boolean previewItemsLimited = scriptsImportTruncated > 0
            || scriptsSkipTruncated > 0
            || scriptsFailTruncated > 0
            || envImportTruncated > 0
            || envSkipTruncated > 0
            || envFailTruncated > 0;
        addDryRunPreviewSummaryStatistics(result, maxPreviewItems, previewItemsLimited, totalAvailable, totalReturned, totalTruncated);
    }

    private void addScriptsDryRunProjectionStatistics(
            ImportExportResult result,
            long total,
            long imported,
            long skipped,
            long failed,
            List<String> wouldImportIds,
            List<String> wouldSkipIds,
            List<String> wouldFailIds,
            int maxPreviewItems) {
        if (result == null) {
            return;
        }
        List<String> importIds = limitPreviewItems(wouldImportIds, maxPreviewItems);
        List<String> skipIds = limitPreviewItems(wouldSkipIds, maxPreviewItems);
        List<String> failIds = limitPreviewItems(wouldFailIds, maxPreviewItems);

        long importTruncated = (wouldImportIds == null ? 0 : wouldImportIds.size()) - importIds.size();
        long skipTruncated = (wouldSkipIds == null ? 0 : wouldSkipIds.size()) - skipIds.size();
        long failTruncated = (wouldFailIds == null ? 0 : wouldFailIds.size()) - failIds.size();
        long totalAvailable = (wouldImportIds == null ? 0 : wouldImportIds.size())
            + (wouldSkipIds == null ? 0 : wouldSkipIds.size())
            + (wouldFailIds == null ? 0 : wouldFailIds.size());
        long totalReturned = importIds.size() + skipIds.size() + failIds.size();
        long totalTruncated = totalAvailable - totalReturned;

        result.addStatistic("scripts.wouldTotal", total);
        result.addStatistic("scripts.wouldImport", imported);
        result.addStatistic("scripts.wouldSkip", skipped);
        result.addStatistic("scripts.wouldFail", failed);
        result.addStatistic("scripts.wouldImport.ids", importIds);
        result.addStatistic("scripts.wouldSkip.ids", skipIds);
        result.addStatistic("scripts.wouldFail.ids", failIds);
        result.addStatistic("scripts.wouldImport.ids.truncatedCount", importTruncated);
        result.addStatistic("scripts.wouldSkip.ids.truncatedCount", skipTruncated);
        result.addStatistic("scripts.wouldFail.ids.truncatedCount", failTruncated);
        addDryRunPreviewSummaryStatistics(
            result,
            maxPreviewItems,
            importTruncated > 0 || skipTruncated > 0 || failTruncated > 0,
            totalAvailable,
            totalReturned,
            totalTruncated
        );
    }

    private void addEnvironmentDryRunProjectionStatistics(
            ImportExportResult result,
            long total,
            long imported,
            long skipped,
            long failed,
            List<String> wouldImportKeys,
            List<String> wouldSkipKeys,
            List<String> wouldFailKeys,
            int maxPreviewItems) {
        if (result == null) {
            return;
        }
        List<String> importKeys = limitPreviewItems(wouldImportKeys, maxPreviewItems);
        List<String> skipKeys = limitPreviewItems(wouldSkipKeys, maxPreviewItems);
        List<String> failKeys = limitPreviewItems(wouldFailKeys, maxPreviewItems);

        long importTruncated = (wouldImportKeys == null ? 0 : wouldImportKeys.size()) - importKeys.size();
        long skipTruncated = (wouldSkipKeys == null ? 0 : wouldSkipKeys.size()) - skipKeys.size();
        long failTruncated = (wouldFailKeys == null ? 0 : wouldFailKeys.size()) - failKeys.size();
        long totalAvailable = (wouldImportKeys == null ? 0 : wouldImportKeys.size())
            + (wouldSkipKeys == null ? 0 : wouldSkipKeys.size())
            + (wouldFailKeys == null ? 0 : wouldFailKeys.size());
        long totalReturned = importKeys.size() + skipKeys.size() + failKeys.size();
        long totalTruncated = totalAvailable - totalReturned;

        result.addStatistic("environment.wouldTotal", total);
        result.addStatistic("environment.wouldImport", imported);
        result.addStatistic("environment.wouldSkip", skipped);
        result.addStatistic("environment.wouldFail", failed);
        result.addStatistic("environment.wouldImport.keys", importKeys);
        result.addStatistic("environment.wouldSkip.keys", skipKeys);
        result.addStatistic("environment.wouldFail.keys", failKeys);
        result.addStatistic("environment.wouldImport.keys.truncatedCount", importTruncated);
        result.addStatistic("environment.wouldSkip.keys.truncatedCount", skipTruncated);
        result.addStatistic("environment.wouldFail.keys.truncatedCount", failTruncated);
        addDryRunPreviewSummaryStatistics(
            result,
            maxPreviewItems,
            importTruncated > 0 || skipTruncated > 0 || failTruncated > 0,
            totalAvailable,
            totalReturned,
            totalTruncated
        );
    }

    private void addDryRunPreviewSummaryStatistics(
            ImportExportResult result,
            int maxPreviewItems,
            boolean previewItemsLimited,
            long previewItemsAvailable,
            long previewItemsReturned,
            long previewItemsTruncated) {
        if (result == null) {
            return;
        }
        result.addStatistic("dryRun.maxPreviewItems", maxPreviewItems);
        result.addStatistic("dryRun.previewItemsLimited", previewItemsLimited);
        result.addStatistic("dryRun.previewItemsAvailable", previewItemsAvailable);
        result.addStatistic("dryRun.previewItemsReturned", previewItemsReturned);
        result.addStatistic("dryRun.previewItemsTruncated", previewItemsTruncated);
    }

    private List<String> limitPreviewItems(List<String> items, int maxPreviewItems) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        if (items.size() <= maxPreviewItems) {
            return new ArrayList<>(items);
        }
        return new ArrayList<>(items.subList(0, maxPreviewItems));
    }

    private void addWarningStatistics(ImportExportResult result, List<String> warnings) {
        if (result == null || warnings == null) {
            return;
        }
        Map<String, Integer> grouped = new LinkedHashMap<>();
        for (String warning : warnings) {
            String type = extractWarningType(warning);
            grouped.merge(type, 1, Integer::sum);
        }
        result.addStatistic("warning.total", warnings.size());
        result.addStatistic("warning.byType", grouped);
        result.addStatistic("migration.warning.total", warnings.size());
        result.addStatistic("migration.warning.byType", grouped);
    }

    private String extractWarningType(String warning) {
        if (warning == null || warning.isEmpty()) {
            return "general";
        }
        if (warning.startsWith("[")) {
            int end = warning.indexOf(']');
            if (end > 1) {
                return warning.substring(1, end);
            }
        }
        return "general";
    }

    private void recordMigrationFailureStats(
            Map<String, Object> stats,
            MigrationContext context,
            String original,
            String current,
            String failureType,
            String message,
            int maxMigrationSteps) {
        if (stats == null) {
            return;
        }
        stats.put("migration.error.type", failureType);
        stats.put("migration.error.message", message);
        stats.put("migration.error.at", current);
        stats.put("migration.steps", context == null ? 0 : context.getStepCount());
        stats.put("migration.path", context == null ? Collections.emptyList() : context.getPath());
        stats.put("migration.originalSchemaVersion", original);
        stats.put("migration.finalSchemaVersion", current);
        stats.put("migration.maxSteps", maxMigrationSteps);
    }

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

