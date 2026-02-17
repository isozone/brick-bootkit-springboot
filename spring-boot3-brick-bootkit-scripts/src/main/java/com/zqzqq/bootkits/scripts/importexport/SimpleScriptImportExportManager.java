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
 * 缂傚倸鍊搁崐鐑芥嚄閼稿灚鍙忔い鎾卞灩绾惧鏌熼崜褏甯涢柣鎾存礋閺屸€愁吋閸愩劌顬嬫繝鈷€鍐ㄨ埞妞ゎ叀鍎婚ˇ铏亜閵娿儻韬鐐诧工铻ｅ〒姘煎灡閸嶉潧顪冮妶鍡樺暗闁哥姵鍔楅埀顒佺婵炲﹪寮婚敐鍡樺劅闁挎繂妫欏В鍕⒑閸涘鐒奸柛銉戝懐鍘梻浣虹《閸撴繈鎮烽姀銈呯；闁瑰墽绮崑銊╂⒒閸喓鈽夊ù鐘筹耿濮婃椽骞栭悙娴嬪亾閵堝鍌ㄩ柛鎾楀啫鐏婃繝鐢靛У绾板秹寮查鍕厱闁哄洢鍔屾晶顕€鏌嶈閸撴盯宕戦妶澶婅摕?
 * 闂傚倸鍊风粈浣革耿鏉堚晛鍨濇い鏍仜缁€澶愭煙閻戞ɑ鈷愭い鎰矙閺岋綁濮€閵忊晝鍔搁梺鍝勫閸庣敻寮昏缁犳稑顫濋悡搴㈢暟闂備浇銆€閸嬫捇鏌曟繛鐐珕闁绘挾鍠栭弻鏇熺箾閸喖濮庡銈呮禋閸撶喖寮诲☉銏犵闁绘劘妫勬禒顕€鎮楀▓鍨灓闁轰礁顭烽妴浣肝旈崨顓狀槹濡炪倖鍔戦崹鑲╃矈鐎靛摜纾介柛灞捐壘閳ь剚鎮傚畷鏉款潩鐠鸿櫣鐛ラ梺鍝勭▉閸樿偐绮婚姣綊宕楅崗鑲╃▏闂佸搫顑勭欢姘跺蓟閿濆憘鏃堝焵椤掑嫭鍋嬮柛鈩冪懅閻牓鏌ㄩ弴鐐测偓褰掑磹閻㈠憡鐓熼柕蹇嬪€栧☉褔鎮介姘卞煟闁哄本绋戦埢搴ょ疀閺冩垶锟ラ梻渚€鈧偛鑻晶浼存煛娴ｈ鍊愮€规洏鍨奸ˇ鍦偓瑙勬礈閺佸銆佸鈧慨鈧柣妯活問濡茬兘姊绘担铏瑰笡婵☆偂绶氶、姘愁樄鐎?
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
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃瀚板ù婊堢畺閺岋綁濮€閵堝棙閿柣銏╁灠閻栧ジ寮诲☉銏犖ч幖绮光偓宕囩潉闁诲孩顔栭崰鏇犲垝濞嗘挸绠栭柍鈺佸暞閸庣喖鏌ㄥ┑鍡樼ォ闁衡偓閵娾晜鈷掑ù锝呮啞閹牊銇勯幋婵囶棦闁诡垰鐭傞、娆撳箣閹烘梻鐣?
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
            return JSON; // 濠电姵顔栭崰妤冩暜濡ゅ啰鐭欓柟鐑樸仜閳ь剨绠撳畷濂稿Ψ椤旇姤娅嶅┑鐘垫暩婵敻鎳濇ィ鍐ㄧ闁逞屽墴濮婅櫣绱掑Ο璇茬濠电偞鎸抽ˉ鎾斥槈閻㈢鐒?
        }
    }
    
    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃瀚板ù婊堢畺閺岋綁濮€閵堝棙閿柣銏╁灠閻栧ジ寮诲☉銏犖ч幖绮光偓宕囩潉闂備礁鎼懟顖炲箠韫囨搩鍤楅柛鏇ㄥ灠缁犳盯鏌嶇憴鍕姢濞?
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
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃瀚板ù婊堢畺閺岋綁濮€閵堝棙閿柣銏╁灠閻栧ジ寮诲☉銏犖ч幖绮光偓宕囶啋缂傚倷娴囨ご鍝ユ暜閿熺姷宓佹俊顖氱毞閸嬫捇鎮介崨濠冪彸闂?
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
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃瀚板ù婊堢畺閺岋綁濮€閵堝棙閿柣銏╁灠閻栧ジ寮诲☉銏犖ч幖绮光偓宕囩潉闁诲孩顔栭崳顕€宕戞繝鍥х畺鐟滄柨鐣烽悡搴樻斀闁割偅绋戞禍浼存⒒娴ｇ瓔鍤欏Δ鐘殿焾閿曘垽鏌嗗鍛枃闂佸湱鍋撻弸濂稿几?
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
     * 闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閺夎法鍔撮梺鍛婂姂閸斿海绮ｉ悙鐑樷拺闁告稑锕ユ径鍕煕濡湱鐭欑€规洘鍔欓幃娆撴倻濡厧骞堥梻浣瑰缁诲倿宕锝勭箚濞寸姴顑嗛崑銊︺亜閺嶃劎鈯曢悘蹇ュ閳?
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
     * JSON闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閺夎法鍔撮梺鍛婂姂閸斿海绮ｉ悙鐑樷拺闁告稑锕ユ径鍕煕濡湱鐭欑€规洘鍔欓幃娆撴倻濡厧骞?
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
     * Properties闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閺夎法鍔撮梺鍛婂姂閸斿海绮ｉ悙鐑樷拺闁告稑锕ユ径鍕煕濡湱鐭欑€规洘鍔欓幃娆撴倻濡厧骞?
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
     * 缂傚倸鍊搁崐鐑芥嚄閼稿灚鍙忔い鎾卞灩绾惧鏌熼崜褏甯涢柣鎾存礃娣囧﹪濡堕崟顓炲缂備讲鍋撻柛顐ゅ枔缁犻箖鏌涢銈呮瀻闁诲繑鎸抽弻鐔兼惞椤愨偓椤忓牞缍栨繝闈涱儛閺佸洭鏌ｉ弮鍌ょ劸妞ゅ繋鍗冲濠氬磼濞嗘垵濡藉┑鐘灪椤洨鍒掗弮鍫濋唶闁哄洨鍠庢禒?
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
     * 缂傚倸鍊搁崐鐑芥嚄閼稿灚鍙忔い鎾卞灩绾惧鏌熼崜褏甯涢柣鎾存礃娣囧﹪濡堕崟顓炲缂備讲鍋撻柛顐ゅ枔缁犻箖鏌涢銈呮瀻闁诲繑鎸抽弻鐔兼惞椤愨偓椤忓牞缍栨繝闈涱儛閺佸洭鏌ｉ弮鍌ょ劸妞ゅ繋鍗冲铏规嫚閹绘帩鍔夌紓浣割儐鐢繝寮€ｎ喗鈷?
     */
    private final Map<String, SimpleScriptConfig> scriptConfigs;
    private final Map<String, String> environmentVariables;
    
    /**
     * 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩椤愩垹绁梺绯曟杹閸嬫挸顪冮妶鍡楃瑨闁稿﹦顭堥悺顓㈡⒒娴ｈ鍋犻柛濠冪箖缁傚秶鎹勬笟顖氭?
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
     * 闂傚倸鍊风粈渚€骞夐敍鍕殰婵°倕鍟畷鏌ユ煕瀹€鈧崕鎴犵礊閺嶎厽鐓欓柣妤€鐗婄欢鑼磼閳ь剙鐣濋崟顒傚幐閻庡箍鍎遍崯顐ｄ繆閸噮鐔嗛悷娆忓婵鏌嶈閸撴岸顢欓弽顓炵獥闁哄稁鍋呭畷鏌ユ煙閺夊灝褰勯柛銉墮閻撴盯鏌涘☉鍗炴灈濞寸姭鏅犲铏瑰寲閺囩偛鈷夊銈冨妼濡繈骞?
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
            context.addWarning("migration.legacy", "legacy 闂傚倸鍊峰ù鍥ь浖閵娾晜鍤勯柤绋跨仛濞呯姵淇婇妶鍌氫壕闂佷紮绲介悘姘跺箯閸涙潙绀堥柟缁樺笚濞呭﹪姊绘担钘変汗闁告鍕珷閹艰揪绲炬刊濂告煥濠靛棙宸濈痪鍓у帶椤法鎹勯崫鍕О闂佹悶鍊栭悷鈺呭蓟?schemaVersion 4.0.0");
            return "4.0.0";
        });

        registerSchemaMigration("4.0.0", (data, context) -> {
            context.addWarning("migration.default", "schemaVersion 4.0.0 闂備浇顕у锕傦綖婢舵劖鍋ら柡鍥╁С閻掑﹥銇勮箛鎾搭棤缂佹唻绠撻弻銈吤圭€ｎ偅鐝栭梺娲诲幗閹瑰洭骞冭ぐ鎺戠倞闁靛鍎崇粊鐑芥⒑?4.0.1");
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
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煟濮楀棗浜滃ù婊堢畺閺岋綁鎮㈤崫鍕垫毉濠电姭鍋撳ù鐓庣摠閻撴盯鏌涢埥鍡楀箻缂佸鍣ｉ弻锛勪沪閺傘倕浼愬銈嗘尭閵堢鐣烽崼鏇炵厸闁稿本澹曢崑?
     *
     * @param outputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柛姘愁潐閵囧嫰骞樼捄鐩掞絿鈧娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param format 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煏韫囨洖孝闁哥偑鍔戝铏圭磼濡纰嶅┑鐐存尦椤ユ挸鈽夐悽绋跨劦?
     * @param options 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煏韫囥儳纾块柛鏂跨仛缁绘繈濮€閿濆懐鍘梺鍛婃⒐閸ㄥ潡宕洪埀?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煟濡吋鏆╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
     */
    public ImportExportResult exportAll(String outputPath, ExportFormat format, ImportExportOptions options) {
        try {
            ImportExportOptions safeOptions = options != null
                ? options
                : ImportExportOptions.newBuilder().build();
            // 闂傚倸鍊峰ù鍥Υ閳ь剟鏌涚€ｎ偅宕岄柡灞剧洴椤㈡洟鏁愰崱娆樻О闂備浇澹堢亸娆愮箾閳ь剟鏌＄仦绋垮⒉闁瑰嘲鎳樺畷顐﹀礋閸偅鐦旈梺?
            Map<String, Object> exportData = collectExportData(safeOptions);
            
            // 闂傚倷鑳堕幊鎾诲触鐎ｎ亶鐒芥繛鍡樺灦瀹曟煡鏌熼悧鍫熺凡闁搞劌鍊归妵鍕疀閹捐泛顣虹紓浣插亾鐎光偓閸曨剛鍘搁悗骞垮劚閸燁偅淇婇懞銉х闁割偅绻勯崺锝夋煛?
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌ｉ姀銏╃劸闁哄鐒﹂妵鍕即濡も偓娴滄儳螖閻橀潧浠﹂柛鏃€鐗犻獮蹇涘川鐎涙ê鈧粯淇婇姘倯婵炲牊鎮傚缁樻媴閸涘﹥鍎撶紓浣割槸閻栫厧鐣烽姀锝庢Ь濠? " + format, Arrays.asList("闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閺夎法鍔撮梺鍛婂姂閸斿海绮ｉ悙鐑樷拺闁告稑锕ユ径鍕煕濡湱鐭欑€规洘鍔欓幃娆撴倻濡厧骞堥梻浣瑰缁诲倿宕锝勭箚闁兼亽鍎崇弧鈧梺閫炲苯澧存い銏★耿婵偓闁绘ɑ顔栭崥瀣⒒娴ｇ瓔鍤冮柛銊ラ叄瀹曟﹢顢旈崟顐ょ懇"));
            }
            
            String serializedData = handler.serialize(exportData);
            
            // 闂傚倸鍊风粈渚€骞夐敓鐘茬闁哄稁鍘介崑锟犳煏婢跺棙娅呴柣顓燁殜閺屾盯鍩勯崘顏佹濠碘槅鍋呴敃銏ゅ蓟濞戙垹唯妞ゆ牜鍋為宥咁渻?
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "闂傚倸鍊搁崐鐑芥倿閿曗偓椤灝螣閼测晝鐓嬮梺鍓插亝濞叉﹢宕戦鍫熺厱闁斥晛鍟伴幉鍧楁煕鐎ｎ偅宕岄柟顔荤矙濡啫鈽夊Ο鍦笡闂傚倷鐒﹂幃鍫曞磹閵堝鐭楅柛鎰靛枤瀹撲線鎮楅敐搴℃灈缂佺姵绋掗妵鍕箳閸℃ぞ澹曟俊? " + outputPath, 
                getTotalItems(exportData), 
                getTotalItems(exportData)
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煟閺冨牜妫戠紒鎰仱閺屸剝寰勬繝鍕拡闂佺顑呴ˇ鎶铰? " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃瀚板ù婊堢畺閺岋綁鎮㈤崫鍕垫毉濠电姭鍋撳ù鐓庣摠閻撴盯鏌涢埥鍡楀箻缂佸鍣ｉ弻锛勪沪閺傘倕浼愬銈嗘尭閵堢鐣烽崼鏇炵厸闁稿本澹曢崑?
     *
     * @param inputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柣顓燁殜閺屾盯鍩勯崘顏呭櫗閻庤娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param options 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳呴柛鏂跨仛缁绘繈濮€閿濆懐鍘梺鍛婃⒐閸ㄥ潡宕洪埀?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃ê鐏╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
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
            
            // 闂傚倸鍊风粈渚€骞夐敓鐘冲仭闁靛／鍛厠闁诲骸鐏氶悺鏇熸叏閾忣偁浜滈柟鎯у船閻忊晝绱掗埀顒勫幢濡湱绠氶梺闈涚墕閸婂憡绂嶆ィ鍐╃厽閹艰揪绱曟禒娑㈡煟閹绢垪鍋撻幇浣告濡炪倖鍔ч梽鍕磻鐎ｎ喗鐓曟い鎰剁悼缁犳﹢鏌?
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
            
            // 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ帊鑳堕埀顒夊墴濮婅櫣绱掑鍡樼暥闂佺粯顨呭Λ娑氬垝?
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
            
            // 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸倖鎴︽倵椤掑嫭鈷戠紓浣癸供濞堟棃鏌ｅΔ鈧Λ娑氬垝?
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
            return ImportExportResult.failure("闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏′繆椤栨繃顏犵紒鎰仱閺屸剝寰勬繝鍕拡闂佺顑呴ˇ鎶铰? " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煏韫囧ň鍋撻崗鍛版濠碉紕鍋戦崐鏍偋濡ゅ懎绀勭憸鐗堝笒濮规煡鏌￠崘銊у闁绘挻娲樼换娑㈠幢濡搫衼闂佸吋婢樺锟犲蓟?
     *
     * @param outputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柛姘愁潐閵囧嫰骞樼捄鐩掞絿鈧娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param format 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煏韫囨洖孝闁哥偑鍔戝铏圭磼濡纰嶅┑鐐存尦椤ユ挸鈽夐悽绋跨劦?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁汇垹澹婇弫鍥煟濡吋鏆╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
     */
    public ImportExportResult exportEnvironment(String outputPath, ExportFormat format) {
        try {
            FormatHandler handler = formatHandlers.get(format);
            if (handler == null) {
                return ImportExportResult.failure("濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌ｉ姀銏╃劸闁哄鐒﹂妵鍕即濡も偓娴滄儳螖閻橀潧浠﹂柛鏃€鐗犻獮蹇涘川鐎涙ê鈧粯淇婇姘倯婵炲牊鎮傚缁樻媴閸涘﹥鍎撶紓浣割槸閻栫厧鐣烽姀锝庢Ь濠? " + format, Arrays.asList("闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閺夎法鍔撮梺鍛婂姂閸斿海绮ｉ悙鐑樷拺闁告稑锕ユ径鍕煕濡湱鐭欑€规洘鍔欓幃娆撴倻濡厧骞堥梻浣瑰缁诲倿宕锝勭箚闁兼亽鍎崇弧鈧梺閫炲苯澧存い銏★耿婵偓闁绘ɑ顔栭崥瀣⒒娴ｇ瓔鍤冮柛銊ラ叄瀹曟﹢顢旈崟顐ょ懇"));
            }
            
            String serializedData = handler.serialize(environmentVariables);
            
            Path outputFile = Paths.get(outputPath);
            Files.write(outputFile, serializedData.getBytes(StandardCharsets.UTF_8));
            
            return ImportExportResult.success(
                "闂傚倸鍊烽懗鍓佸垝椤栨粌鍨濋柣妯款嚙閸ㄥ倸霉閸忚偐鏆橀柍褜鍓欓崐鎸庝繆閹间礁鐓涘ù锝堟閸橆剚绻濆▓鍨灍闁靛洦鐩畷鎴﹀箻缂佹鍘遍梺鎸庣箓閻楀﹪顢旈悩缁樼厓鐟滄粓宕滃☉銏犳瀬闁归棿绀侀崒銊ф喐閺傝法鏆﹂柣銏犲閺佸洭鏌曡箛鏇炐ユい鎾存そ濮婇缚銇愰幒鎴滃枈闂佸憡鎸婚悷鈺呮晲? " + outputPath, 
                environmentVariables.size(), 
                environmentVariables.size()
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("闂傚倸鍊烽懗鍓佸垝椤栨粌鍨濋柣妯款嚙閸ㄥ倸霉閸忚偐鏆橀柍褜鍓欓崐鎸庝繆閹间礁鐓涘ù锝堟閸橆剚绻濆▓鍨灍闁靛洦鐩畷鎴﹀箻缂佹鍘遍梺鎸庣箓閻楀﹪顢旈悩缁樼厓鐟滄粓宕滃☉銏犳瀬闁归棿绀侀崒銊ф喐閺傝法鏆﹂柣銏犲閺佸洭鏌ｉ弮鍫缂佹劗鍋ら弻鈩冨緞婵犲嫬鈷堥梺绋款儏椤︽娊路? " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冨ù鐘灩椤啴濡堕崱娆忣潷闂佸憡鍨电紞濠傤嚕閹惰棄绫嶉柛顐ゅ枔閸樺崬鈹戦悙鍙夘棡闁告枮鍥ㄥ殌闁秆勵殕閻?
     *
     * @param inputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柣顓燁殜閺屾盯鍩勯崘顏呭櫗閻庤娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param overwrite 闂傚倸鍊风粈渚€骞栭銈傚亾濮樺崬鍘寸€规洝顫夌€靛ジ寮堕幋鐘垫毎濠电偠鎻徊鍧楀磿閵堝鐤繛宸簼閻撴洟鏌嶉埡浣告殶闁瑰啿瀚惀?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃ê鐏╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
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
     * 闂傚倸鍊峰ù鍥敋瑜嶉湁闁绘垼妫勯弸渚€鏌熼梻瀵割槮闁稿被鍔庨幉鎼佸棘鐠恒劍娈鹃梺姹囧灩婢瑰﹪寮崶顒佺厪闊洤艌閸嬫捇鎼归銈嗘瘓婵犵數濮甸鏍窗濡ゅ懏鏅濋柍鍝勬噹閸屻劌霉閻樺樊鍎忕紒鐙€鍨跺娲敆閳ь剛绮旈悽绋跨厱闁硅揪闄勯悡娑橆熆鐠哄搫顒㈢痪顓炵埣閹虫牠寮介鐔叉嫼?     *
     * @param outputPath 闂傚倸鍊风粈渚€骞栭位鍥敍閻愭潙浜辨繝鐢靛Т濞层倗绮绘导瀛樼厵闂傚倸顕ˇ锕傛煕濮樻剚娼愰柕鍥у楠炴鎹勯惄鎺炵悼閳ь剝顫夊ú鏍囨导鏉懳﹂柛鏇ㄥ灠閸愨偓闂侀潧顭俊鍥р枔閵堝洨纾?
     * @param scriptIds 闂傚倸鍊搁崐鐑芥嚄閸洖鍌ㄧ憸鏃堝箖濞差亜惟鐟滃秹寮搁崼鈶╁亾楠炲灝鍔氶柟閿嬪灴閹虫捇宕稿Δ浣哄幗閻熸粌閰ｉ崺鈧い鎺嶇婵秵鎱ㄦ繝鍐┿仢鐎规洏鍔嶇换婵嬪磼濠婂懏鍤冨┑锛勫亼閸婃垿宕濆畝鍕婂洭宕归锛勭畾闂佸湱铏庨崰鏍矆閸愨斂浜滈柡鍐ㄦ搐琚氬┑鐐叉噺閻楃姴顫忛搹鍦煋闁糕剝顨嗗﹢浼存煟閹捐泛濡眑闂傚倸鍊峰ù鍥х暦閻㈢纾婚柣鎰暩閻瑩鐓崶銊р槈缂佲偓婢舵劕绠规繛锝庡墮婵＄厧顩奸崨顓涙斀妞ゆ梻鍗抽幋鐐存珷閹兼番鍔岀壕濠氭煙閸撗呭笡闁稿鍔戝濠氬醇閻旇　妲堟繛瀵稿У閿曘垹顫忓ú顏勭闁绘劖绁撮崑鎾诲Χ婢跺﹦鐓戦梺鍛婂姦閸犳艾鐣垫担琛″亾楠炲灝鍔氭い锔诲灦閹偤宕归鐘辩盎闂佸湱鍎ら崹鐢割敂閳哄懏鍊垫慨姗嗗墻濡插憡銇勯鐐村仴闁硅櫕绮撻幃浠嬫倷閸忓浜鹃柟鐑樻⒒绾惧ジ鏌?
     * @param format 闂傚倸鍊峰ù鍥敋瑜嶉湁闁绘垼妫勯弸渚€鏌熼梻瀵割槮闁稿被鍔庨幉鎼佸棘鐠恒劍娈鹃梺姹囧灩婢瑰﹪寮崶顒佺厪闊洦娲栧瓭闂佸摜鍋戦崝鎴濐潖閾忓湱纾兼俊顖濐嚙绾板秴鈹戦悙瀛樺唉妞ゃ儲鎸搁埥澶愭偨缁嬭法鍔?
     * @return 闂傚倸鍊峰ù鍥敋瑜嶉湁闁绘垼妫勯弸渚€鏌熼梻瀵割槮闁稿被鍔庨幉鎼佸棘鐠恒劍娈鹃梺姹囧灩婢瑰﹪寮崶顒佺厽婵☆垰鍚嬮弳鈺呮煥濞戞瑧鐭掓慨濠呮閹叉挳宕熼銏犘戞俊鐐€栧ú锕傚储閻ｅ瞼鐭?
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
                "闂傚倸鍊烽懗鍫曞储瑜旈幃娲Ω瑜嶉弸鍫⑩偓骞垮劚閹锋垿鎳撻崹顔氬綊鎮℃惔锝嗘喖闂佺粯鎸撮崑鎾绘煟鎼达紕鐣柛搴″船鐓ら柕濞炬櫅閸戠娀鏌涘畝鈧崑鐐哄磹閻㈠憡鐓熼柕蹇嬪€栧☉褔鎮介姘卞煟闁哄本鐩俊鑸垫償閳ュ磭鐫勬俊銈囧Х閸嬫盯顢栨径鎰祦闁搞儺鍓﹂弫鍡涙煃瑜滈崜鐔兼晲? " + outputPath, 
                processed, 
                processed
            );
            
        } catch (Exception e) {
            return ImportExportResult.failure("闂傚倸鍊烽懗鍫曞储瑜旈幃娲Ω瑜嶉弸鍫⑩偓骞垮劚閹锋垿鎳撻崸妤佺厵婵炲牆鐏濋弸鎾绘煕鐎ｎ偅宕岄柟顔荤矙濡啫鈽夊Ο鍦笡闂傚倷鐒﹂幃鍫曞磹閵堝鍌ㄩ柦妯侯樈閸ゆ鏌涢弴銊ュ缂傚秴娲﹂妵鍕箛閳轰礁澹? " + e.getMessage(), Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冩繛鍫燁殔閳规垿顢欓弬銈勭返闂佸憡眉缁瑩鐛箛娑欑劶鐎广儱妫岄幏娲⒑閸濆嫷妲兼繛澶嬫礉閸婃挳姊?
     *
     * @param inputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柣顓燁殜閺屾盯鍩勯崘顏呭櫗閻庤娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param overwrite 闂傚倸鍊风粈渚€骞栭銈傚亾濮樺崬鍘寸€规洝顫夌€靛ジ寮堕幋鐘垫毎濠电偠鎻徊鍧楀磿閵堝鐤繛宸簼閻撴洟鏌嶉埡浣告殶闁瑰啿瀚惀顏堟倷椤掆偓椤ュ鏌嶈閸撴盯骞婇幘璇茬疅闂勫洭濡甸幇鏉跨妞ゆ棁妫勬禒閬嶆⒑闂堟单鍫ュ疾濞戙垹绠查柛顐ｆ礃閻撳繐鈹戦悙鎴濆暙椤忣偊鏌￠崱娆忊枅婵﹦绮幏鍛喆閸曨厼鍤掔紓鍌欑椤︻垶鎮ユ總绋跨畺?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃ê鐏╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
     */
    public ImportExportResult importScripts(String inputPath, boolean overwrite) {
        return importScripts(inputPath, overwrite, false);
    }

    /**
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冩繛鍫燁殔閳规垿顢欓弬銈勭返闂佸憡眉缁瑩鐛箛娑欑劶鐎广儱妫岄幏娲⒑閸濆嫷妲兼繛澶嬫礉閸婃挳姊绘担鍦菇闁稿﹥娲栬灋婵犻潧顑呴弸浣衡偓骞垮劚椤︻垶鏌嬮崶顒佺厽闁哄倹瀵ч崯鐐寸箾閸粌宓嗘慨濠囩細閵囨劙骞掗幘瀵稿礁缂傚倷娴囬崺鏍х暆閹间礁违闁稿瞼鍋涢悡娑樏归敐鍛仾闁烩晩鍨跺畷娲焵椤掍降浜滈柟鍝勬娴滈箖鏌ф导娆戠М闁哄苯绉规俊鐑藉Ψ瑜濈槐鐐电磼缂併垹骞愰柛瀣崌濮?
     *
     * @param inputPath 闂傚倷绀侀幖顐λ囬鐐村亱濠电姴娲ょ粻浼存煙闂傚顦﹂柣顓燁殜閺屾盯鍩勯崘顏呭櫗閻庤娲栭ˇ浼村Φ閸曨垰鍐€闁靛濡囧▓銈囩磽?
     * @param overwrite 闂傚倸鍊风粈渚€骞栭銈傚亾濮樺崬鍘寸€规洝顫夌€靛ジ寮堕幋鐘垫毎濠电偠鎻徊鍧楀磿閵堝鐤繛宸簼閻撴洟鏌嶉埡浣告殶闁瑰啿瀚惀顏堟倷椤掆偓椤ュ鏌嶈閸撴盯骞婇幘璇茬疅闂勫洭濡甸幇鏉跨妞ゆ棁妫勬禒閬嶆⒑闂堟单鍫ュ疾濞戙垹绠查柛顐ｆ礃閻撳繐鈹戦悙鎴濆暙椤忣偊鏌￠崱娆忊枅婵﹦绮幏鍛喆閸曨厼鍤掔紓鍌欑椤︻垶鎮ユ總绋跨畺?
     * @param dryRun 闂傚倸鍊风粈渚€骞栭銈傚亾濮樺崬鍘寸€规洝顫夌€靛ジ寮堕幋鐘垫毎濠电偠鎻徊钘夛耿闁秲鈧懘鎮滈懞銉у幈濠电偞鍨堕悷锕傚磿濡ゅ懏鐓涢柛鈩兩戠粈鍐ㄇ庨崶褝韬い銏℃礋閹垽鎽庨崒姘兼濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌熺紒銏犳灈闁汇値鍣ｉ弻鐔煎箲閹伴潧娈紓浣插亾?
     * @return 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閸℃ê鐏╅柨娑欑矒濮婅櫣鎲撮崟顐㈠Б濡炪倖娲﹂崢鐣岀矉?
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
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫梺鎼炲€曞ú锕傚箞閵娿儺娼ㄩ柛鈩冾殔缁犳椽姊洪崷顓炲幋濞存粏娉涢～蹇撁洪鍕獓闁荤姵浜介崜閬嶅Χ婢跺鍘?
     */
    public SimpleScriptConfig getScriptConfig(String scriptId) {
        return scriptConfigs.get(scriptId);
    }
    
    /**
     * 缂傚倸鍊搁崐椋庣矆娓氣偓钘濇い鏍亹閳ь剨绠戦悾锟犲箥閾忣偆鈧椽鏌熼崗鑲╂殬闁告柨绉瑰畷鐢稿炊閵婏箑寮垮┑顔筋殔濡鐛Δ鍛厵闁绘垶鍨濋幉鐐叏婵犲啯銇濈€殿喖澧庨幑鍕Ω閹扳晛鈧繈寮?
     */
    public SimpleScriptConfig removeScriptConfig(String scriptId) {
        return scriptConfigs.remove(scriptId);
    }
    
    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫╃紓浣哄О閸庣敻寮诲鍫闂佸憡鎸鹃崰搴敋閿濆鍨傛い鎰╁灮缁愮偤鏌熼懖鈺勊夐柍褜鍓氶崜姘涘┑瀣拻濞达絽鎲￠幆鍫ユ煟濡も偓缁绘﹢鐛崘鈺冪懝?
     */
    public List<String> getAllScriptIds() {
        return new ArrayList<>(scriptConfigs.keySet());
    }
    
    /**
     * 闂傚倷娴囧畷鍨叏瀹曞洨鐭嗗ù锝堫潐濞呯姴霉閻樺樊鍎愰柛瀣典邯閺屾盯鍩勯崘顏佸闂佺顑嗙换鍐Φ閸曨垰鍐€闁靛鍎卞В鍫ユ⒑閸濆嫭顥炴俊顐㈠暣瀵鈽夊Ο閿嬬€婚梺鍦閸庡ジ鍩€椤掆偓濞硷繝寮?
     */
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }
    
    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫梺绋款儐缁诲啴濡甸崟顖氬唨闁靛鍎卞В鍫ユ⒑閸濆嫭顥炴俊顐㈠暣瀵鈽夊Ο閿嬬€婚梺鍦閸庡ジ鍩€椤掆偓濞硷繝寮?
     */
    public String getEnvironmentVariable(String key) {
        return environmentVariables.get(key);
    }
    
    /**
     * 缂傚倸鍊搁崐椋庣矆娓氣偓钘濇い鏍亹閳ь剨绠戦悾锟犲箥閾忣偆鈧椽鏌熼崗鑲╂殬闁告柨绉瑰畷鎴濃攽閸ャ儰绨婚梺鍝勭Р閸斿矂宕甸鍕厸闁糕剝蓱椤ュ牓鏌＄仦鍓р槈闁宠閰ｉ獮鍥箚閹靛啿浜鹃柛褎顨嗛悡?
     */
    public String removeEnvironmentVariable(String key) {
        return environmentVariables.remove(key);
    }
    
    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫╃紓浣哄О閸庣敻寮诲鍫闂佸憡鎸鹃崰搴敋閿濆鍨傛い鎰╁灮缁愮偤鏌ｈ箛鏇炰户闁哄拋鍋婇幃楣冩倷椤掑倻鐦堥梺鍐茬殱閸嬫捇鏌涢幇鈺佸婵炲牜鍙冮幃宄邦煥閸曨剛鍑″銈忓閺佽顕?
     */
    public Map<String, String> getAllEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }
    
    // ==================== 缂傚倸鍊搁崐椋庣矆娓氣偓钘濇い鏇楀亾闁诡喚鍋ら弫鍐焵椤掑嫭鏅濋柕蹇婂墲婵挳鎮峰▎蹇擃伀缂佸鐖煎娲川婵犱胶绻侀梺鍛婎焾婢瑰牓路閸涘瓨鈷掑ù锝呮啞閹牓鏌涙繝浣虹煓闁挎繄鍋炲鍕沪閹冨箲?====================
    
    /**
     * 闂傚倸鍊峰ù鍥Υ閳ь剟鏌涚€ｎ偅宕岄柡灞剧洴椤㈡洟鏁愰崱娆樻О闂備浇澹堢亸娆愮箾閳ь剟鏌熼绛嬫疁闁诡喕绮欏Λ鍐ㄢ槈濡湱甯涢梻鍌欑劍閹爼宕愰妶澶婄煑闁告劦鐓堥崵鏇熴亜閹板爼妾柛瀣儔閺屾盯顢曢敐鍥╃暭闂?
     */
    private Map<String, Object> collectExportData(ImportExportOptions options) {
        Map<String, Object> data = new HashMap<>();
        
        // 闂傚倸鍊烽懗鍓佹兜閸洖鐤炬繝闈涱儏绾惧潡鏌曢崼婵囧珔闁兼澘娼￠弻锝呂旈埀顒勬偋韫囨稑鐓樼€广儱顦伴悡鍐煃鏉炴壆顦﹂柡鍡樻礈缁?
        data.put("version", CURRENT_SCHEMA_VERSION);
        data.put("schemaVersion", CURRENT_SCHEMA_VERSION);
        data.put("exportTime", LocalDateTime.now().toString());
        data.put("exporter", "SimpleScriptImportExportManager");
        data.put("options", options);
        
        // 闂傚倸鍊烽懗鍫曞储瑜旈幃娲Ω瑜嶉弸鍫⑩偓骞垮劚閹锋垿鎳撻崹顔氬綊鎮℃惔锝嗘喖闂佺粯鎸撮崑鎾绘煟鎼达紕鐣柛搴″船鐓ら柕濞炬櫅閸?
        if (options.isIncludeScripts()) {
            data.put("scripts", new HashMap<>(scriptConfigs));
        }
        
        // 闂傚倸鍊烽懗鍓佸垝椤栨粌鍨濋柣妯款嚙閸ㄥ倸霉閸忚偐鏆橀柍褜鍓欓崐鎸庝繆閹间礁鐓涘ù锝堟閸橆剚绻濆▓鍨灍闁靛洦鐩畷鎴﹀箻缂佹鍘?
        if (options.isIncludeEnvironment()) {
            data.put("environment", new HashMap<>(environmentVariables));
        }
        
        // 缂傚倸鍊搁崐鎼佸磹閹间礁纾圭憸鐗堝笒缁犱即鏌熼梻瀵稿妽闁?
        if (options.isIncludeCache()) {
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("cacheEnabled", true);
            cacheData.put("cacheSize", 0);
            data.put("cache", cacheData);
        }
        
        return data;
    }
    
    /**
     * 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃缂佸銇樻竟鏇㈡⒑濮瑰洤鐏叉繛浣冲懏鏆滈柛顐ｆ礃閻撴洘绻涢崱妤冪妞ゃ儯鍨婚埀顒侇問閸ｎ噣宕戞繝鍥х畺鐟滄柨鐣烽悡搴樻斀闁割偅绋戞禍?
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
                    String message = "閺堫亞鐓?schemaVersion: " + current;
                    recordMigrationFailureStats(stats, context, original, current, "unknown_schema", message, maxMigrationSteps);
                    throw new IllegalArgumentException(message);
                }
                addTypedWarning(warnings, "migration.unknown-version",
                    "閺堫亞鐓?schemaVersion: " + current + "锛屾寜鍏煎妯″紡缁х画澶勭悊");
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
                        errors.add("濠电姷顣藉Σ鍛村磻閸涱収鐔嗘俊顖氱毞閸嬫挸顫濋悡搴＄睄闁芥鍠栭弻娑樷槈濡吋鎲奸梺鎼炲€曞ú锕傚箞閵娿儺娼ㄩ柛鈩冾殔缁犳椽姊洪崷顓炲幋濞存粏娉涢～蹇撁洪鍕獓闁荤姵浜介崜閬嶅Χ婢跺鍘介梺闈涚墕閹冲繘藟閻愮鍋撻崹顐ｇ凡閻庢凹鍘奸…鍥疀濞戣鲸鏅濋梺? 闂傚倸鍊搁崐鐑芥倿閿曗偓椤灝螣閼测晝鐓嬮梺鍓插亝濞叉﹢宕戦鍫熺厱闁斥晛鍠氶悞浠嬫煃缂佹ɑ顥堥柡灞熷棛鐤€闁规儳鍟挎潏鍛存⒑? scriptId=" + entry.getKey());
                        continue;
                    }
                    if (options.isOverwriteExisting() || !scriptConfigs.containsKey(entry.getKey())) {
                        wouldImport++;
                    }
                }
            }
        } catch (Exception e) {
            wouldFail++;
            errors.add("濠电姷顣藉Σ鍛村磻閸涱収鐔嗘俊顖氱毞閸嬫挸顫濋悡搴＄睄闁芥鍠栭弻娑樷槈濡吋鎲奸梺鎼炲€曞ú锕傚箞閵娿儺娼ㄩ柛鈩冾殔缁犳椽姊洪崷顓炲幋濞存粏娉涢～蹇撁洪鍕獓闁荤姵浜介崜閬嶅Χ婢跺鍘介梺闈涚墕閹冲繘藟閻愮鍋撻崹顐ｇ凡閻庢凹鍘奸…鍥疀濞戣鲸鏅濋梺? " + e.getMessage());
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
            errors.add("濠电姷顣藉Σ鍛村磻閸涱収鐔嗘俊顖氱毞閸嬫挸顫濋悡搴＄睄闁芥鍠栭弻娑樷槈濡吋鎲奸梺绋款儐缁诲啴濡甸崟顖氬唨闁靛鍎卞В鍫ユ⒑閸濆嫭顥炴俊顐㈠暣瀵鈽夊Ο閿嬬€婚梺鍦閸庡ジ鍩€椤掆偓濞硷繝寮诲☉銏℃櫜闁搞儜鍐瀱闁诲骸鐏氬妯尖偓姘煎幖椤洩绠涘☉杈ㄦ櫇闂? " + e.getMessage());
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
     * 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸倖鎴︽倵椤掑嫭鈷戠紓浣癸供濞堟棃鏌ｅΔ鈧Λ娑氬垝?
     */
    private ImportExportResult importData(
            Map<String, Object> data,
            ImportExportOptions options,
            List<String> warnings,
            AliasStatistics aliasStatistics) {
        long totalImported = 0;
        long totalFailed = 0;
        List<String> errors = new ArrayList<>();
        
        // 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冩繛鍫燁殔閳规垿顢欓弬銈勭返闂佸憡眉缁瑩鐛箛娑欑劶鐎广儱妫岄幏娲⒑閸濆嫷妲兼繛澶嬫礉閸婃挳姊?
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> scriptsDataObj = (Map<String, Object>) data.get("scripts");
            if (scriptsDataObj != null && options.isIncludeScripts()) {
                for (Map.Entry<String, Object> entry : scriptsDataObj.entrySet()) {
                    String scriptId = entry.getKey();
                    SimpleScriptConfig config = toScriptConfig(entry.getValue(), aliasStatistics);
                    if (config == null) {
                        totalFailed++;
                        errors.add("闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冩繛鍫燁殔閳规垿顢欓弬銈勭返闂佸憡眉缁瑩鐛箛娑欑劶鐎广儱妫岄幏娲⒑閸濆嫷妲兼繛澶嬫礉閸婃挳姊绘担鍦菇闁稿﹥娲栬灋婵炲棗娴氶崵妤呮煕閺囥劌澧紓宥呮处閵囧嫰骞囬埡浣稿? 闂傚倸鍊搁崐鐑芥倿閿曗偓椤灝螣閼测晝鐓嬮梺鍓插亝濞叉﹢宕戦鍫熺厱闁斥晛鍠氶悞浠嬫煃缂佹ɑ顥堥柡灞熷棛鐤€闁规儳鍟挎潏鍛存⒑? scriptId=" + scriptId);
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
            errors.add("闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冩繛鍫燁殔閳规垿顢欓弬銈勭返闂佸憡眉缁瑩鐛箛娑欑劶鐎广儱妫岄幏娲⒑閸濆嫷妲兼繛澶嬫礉閸婃挳姊绘担鍦菇闁稿﹥娲栬灋婵炲棗娴氶崵妤呮煕閺囥劌澧紓宥呮处閵囧嫰骞囬埡浣稿? " + e.getMessage());
        }
        
        // 闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冨ù鐘灩椤啴濡堕崱娆忣潷闂佸憡鍨电紞濠傤嚕閹惰棄绫嶉柛顐ゅ枔閸樺崬鈹戦悙鍙夘棡闁告枮鍥ㄥ殌闁秆勵殕閻?
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
            errors.add("闂傚倷娴囬褍霉閻戣棄鏋侀柟闂寸閸屻劎鎲搁弬璺ㄦ殾闁挎繂顦獮銏＄箾閹寸偟鎳冨ù鐘灩椤啴濡堕崱娆忣潷闂佸憡鍨电紞濠傤嚕閹惰棄绫嶉柛顐ゅ枔閸樺崬鈹戦悙鍙夘棡闁告枮鍥ㄥ殌闁秆勵殕閻撴洟鏌ㄥ┑鍡樻悙闁告柨顑囬埀顒€鐏氬妯尖偓姘煎幖椤洩绠涘☉杈ㄦ櫇闂? " + e.getMessage());
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
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫╃紓浣哄У閸ㄧ敻鍩為幋锔藉亹闁告劕寮堕幆娑㈡⒑闁偛鑻晶顔剧磼婢跺﹦绉烘鐐叉瀹曠喖顢涢敐鍡樻珦闂備礁鍚嬫禍浠嬪磿閺屻儺鏁?
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
        result.addStatistic("dryRun.maxPreviewItems", maxPreviewItems);
        result.addStatistic("dryRun.previewItemsLimited", previewItemsLimited);
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
        result.addStatistic("dryRun.maxPreviewItems", maxPreviewItems);
        result.addStatistic("dryRun.previewItemsLimited", importTruncated > 0 || skipTruncated > 0 || failTruncated > 0);
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
        result.addStatistic("dryRun.maxPreviewItems", maxPreviewItems);
        result.addStatistic("dryRun.previewItemsLimited", importTruncated > 0 || skipTruncated > 0 || failTruncated > 0);
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

