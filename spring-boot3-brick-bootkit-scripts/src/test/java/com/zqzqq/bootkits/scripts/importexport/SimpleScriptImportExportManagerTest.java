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


package com.zqzqq.bootkits.scripts.importexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 简化版脚本导入导出管理器测试类
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("简化版脚本导入导出管理器测试")
public class SimpleScriptImportExportManagerTest {
    
    private SimpleScriptImportExportManager importExportManager;
    
    @BeforeEach
    void setUp() {
        importExportManager = new SimpleScriptImportExportManager();
        setupTestData();
    }
    
    @Test
    @DisplayName("测试JSON格式导入导出")
    void testJsonFormatImportExport() throws Exception {
        // 创建临时文件
        Path tempFile = Files.createTempFile("test", ".json");
        
        try {
            // 测试导出
            SimpleScriptImportExportManager.ImportExportOptions options = 
                SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                    .includeEnvironment(true)
                    .validateData(true)
                    .build();
            
            SimpleScriptImportExportManager.ImportExportResult exportResult = 
                importExportManager.exportAll(tempFile.toString(), 
                                           SimpleScriptImportExportManager.ExportFormat.JSON, 
                                           options);
            
            assertTrue(exportResult.isSuccess(), "导出应该成功");
            assertTrue(Files.exists(tempFile), "导出文件应该存在");
            
            // 验证文件内容
            String content = new String(Files.readAllBytes(tempFile));
            assertTrue(content.contains("\"version\""), "文件应该包含版本信息");
            assertTrue(content.contains("\"exportTime\""), "文件应该包含导出时间");
            assertTrue(content.contains("\"scripts\""), "文件应该包含脚本信息");
            assertTrue(content.contains("\"environment\""), "文件应该包含环境变量信息");
            
            // 清理测试数据
            importExportManager.removeScriptConfig("test_script");
            importExportManager.removeEnvironmentVariable("TEST_VAR");
            
            // 测试导入
            SimpleScriptImportExportManager.ImportExportResult importResult = 
                importExportManager.importAll(tempFile.toString(), options);
            
            assertTrue(importResult.isSuccess() || importResult.getItemsSucceeded() > 0, "导入应该成功或部分成功");
            
            // 验证导入的数据
            SimpleScriptImportExportManager.SimpleScriptConfig config = importExportManager.getScriptConfig("test_script");
            assertNotNull(config, "脚本配置应该被导入");
            assertEquals("Test Script", config.getName(), "脚本名称应该正确");
            
            String envVar = importExportManager.getEnvironmentVariable("TEST_VAR");
            assertEquals("test_value", envVar, "环境变量应该被导入");
            
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    @DisplayName("测试Properties格式环境变量导入导出")
    void testPropertiesFormatEnvironment() throws Exception {
        // 创建临时文件
        Path tempFile = Files.createTempFile("test", ".properties");
        
        try {
            // 测试导出环境变量
            SimpleScriptImportExportManager.ImportExportResult exportResult = 
                importExportManager.exportEnvironment(tempFile.toString(), 
                                                   SimpleScriptImportExportManager.ExportFormat.PROPERTIES);
            
            assertTrue(exportResult.isSuccess(), "环境变量导出应该成功");
            assertTrue(Files.exists(tempFile), "导出文件应该存在");
            
            // 验证文件内容
            String content = new String(Files.readAllBytes(tempFile));
            assertTrue(content.contains("TEST_VAR=test_value"), "文件应该包含正确的属性");
            assertTrue(content.contains("ANOTHER_VAR=another_value"), "文件应该包含正确的属性");
            
            // 清理测试数据
            importExportManager.removeEnvironmentVariable("TEST_VAR");
            importExportManager.removeEnvironmentVariable("ANOTHER_VAR");
            
            // 测试导入环境变量
            SimpleScriptImportExportManager.ImportExportResult importResult = 
                importExportManager.importEnvironment(tempFile.toString(), true);
            
            assertTrue(importResult.isSuccess(), "环境变量导入应该成功");
            
            // 验证导入的数据
            assertTrue(importExportManager.getEnvironmentVariable("TEST_VAR") != null, "变量应该被导入");
            assertTrue(importExportManager.getEnvironmentVariable("ANOTHER_VAR") != null, "变量应该被导入");
            assertEquals("test_value", importExportManager.getEnvironmentVariable("TEST_VAR"));
            assertEquals("another_value", importExportManager.getEnvironmentVariable("ANOTHER_VAR"));
            
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    @DisplayName("测试脚本配置导入导出")
    void testScriptConfigImportExport() throws Exception {
        // 创建临时文件
        Path tempFile = Files.createTempFile("scripts", ".json");
        
        try {
            // 测试导出脚本配置
            List<String> scriptIds = Arrays.asList("test_script");
            SimpleScriptImportExportManager.ImportExportResult exportResult = 
                importExportManager.exportScripts(tempFile.toString(), scriptIds, 
                                               SimpleScriptImportExportManager.ExportFormat.JSON);
            
            assertTrue(exportResult.isSuccess(), "脚本配置导出应该成功");
            assertTrue(Files.exists(tempFile), "导出文件应该存在");
            
            // 验证文件内容
            String content = new String(Files.readAllBytes(tempFile));
            assertTrue(content.contains("\"test_script\""), "文件应该包含脚本ID");
            assertTrue(content.contains("\"Test Script\""), "文件应该包含脚本名称");
            assertTrue(content.contains("\"SHELL\""), "文件应该包含脚本类型");
            
            // 清理测试数据
            importExportManager.removeScriptConfig("test_script");
            
            // 测试导入脚本配置
            SimpleScriptImportExportManager.ImportExportResult importResult = 
                importExportManager.importScripts(tempFile.toString(), true);
            
            assertTrue(importResult.isSuccess(), "脚本配置导入应该成功");
            
            // 验证导入的数据
            SimpleScriptImportExportManager.SimpleScriptConfig config = importExportManager.getScriptConfig("test_script");
            assertNotNull(config, "脚本配置应该被导入");
            assertEquals("Test Script", config.getName(), "脚本名称应该正确");
            assertEquals("echo 'Hello World'", config.getScriptPath(), "脚本路径应该正确");
            assertTrue(config.isEnabled(), "脚本应该启用");
            
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试导入完整配置时的旧版本兼容")
    void testLegacyImportAllCompatibility() throws Exception {
        Path tempFile = Files.createTempFile("legacy-import-all", ".json");

        try {
            String legacyJson = "{"
                + "\"exportTime\":\"2024-01-01T00:00:00\","
                + "\"exporter\":\"legacy-exporter\","
                + "\"scriptConfigs\":{"
                + "  \"legacy_script\":{"
                + "    \"id\":\"legacy_script\","
                + "    \"name\":\"Legacy Script\","
                + "    \"script_type\":\"SHELL\","
                + "    \"script_path\":\"echo legacy\","
                + "    \"enabled\":true"
                + "  }"
                + "},"
                + "\"environmentVariables\":{"
                + "  \"LEGACY_ENV\":\"legacy_value\""
                + "}"
                + "}";
            Files.writeString(tempFile, legacyJson, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("legacy_script");
            importExportManager.removeEnvironmentVariable("LEGACY_ENV");

            SimpleScriptImportExportManager.ImportExportOptions options =
                SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                    .includeEnvironment(true)
                    .overwriteExisting(true)
                    .validateData(true)
                    .build();

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess(), "旧版本完整配置导入应该成功");
            assertFalse(result.getWarnings().isEmpty(), "兼容迁移应产生提示");
            assertEquals("legacy", result.getStatistics().get("schemaVersion.original"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.final"));
            assertEquals(Boolean.FALSE, result.getStatistics().get("dryRun"));

            SimpleScriptImportExportManager.SimpleScriptConfig config = importExportManager.getScriptConfig("legacy_script");
            assertNotNull(config, "旧版本脚本配置应该被迁移并导入");
            assertEquals("Legacy Script", config.getName());
            assertEquals("SHELL", config.getScriptType());
            assertEquals("echo legacy", config.getScriptPath());
            assertEquals("legacy_value", importExportManager.getEnvironmentVariable("LEGACY_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试导入完整配置预检模式不写入数据")
    void testImportAllDryRun() throws Exception {
        Path tempFile = Files.createTempFile("import-all-dry-run", ".json");

        try {
            String payload = "{"
                + "\"version\":\"4.0.1\","
                + "\"schemaVersion\":\"4.0.1\","
                + "\"scripts\":{"
                + "  \"dry_run_script\":{"
                + "    \"id\":\"dry_run_script\","
                + "    \"name\":\"Dry Run Script\","
                + "    \"scriptType\":\"SHELL\","
                + "    \"scriptPath\":\"echo dry-run\","
                + "    \"enabled\":true"
                + "  }"
                + "},"
                + "\"environment\":{"
                + "  \"DRY_ENV\":\"dry_value\""
                + "}"
                + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("dry_run_script");
            importExportManager.removeEnvironmentVariable("DRY_ENV");

            SimpleScriptImportExportManager.ImportExportOptions dryRunOptions =
                SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                    .includeEnvironment(true)
                    .overwriteExisting(true)
                    .dryRun(true)
                    .build();

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importAll(tempFile.toString(), dryRunOptions);

            assertTrue(result.isSuccess(), "预检模式应该成功");
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.original"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.final"));
            assertTrue(result.getItemsSucceeded() >= 2);

            assertNull(importExportManager.getScriptConfig("dry_run_script"), "预检模式不应写入脚本配置");
            assertNull(importExportManager.getEnvironmentVariable("DRY_ENV"), "预检模式不应写入环境变量");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试导入脚本配置时支持snake_case字段")
    void testImportScriptsWithSnakeCaseFields() throws Exception {
        Path tempFile = Files.createTempFile("snake-script-config", ".json");

        try {
            String scriptsJson = "{"
                + "\"snake_script\":{"
                + "  \"id\":\"snake_script\","
                + "  \"name\":\"Snake Script\","
                + "  \"script_type\":\"PYTHON\","
                + "  \"script_path\":\"python main.py\","
                + "  \"working_directory\":\"/tmp\","
                + "  \"retry_count\":2,"
                + "  \"timeout_ms\":15000,"
                + "  \"is_enabled\":true"
                + "}"
                + "}";
            Files.writeString(tempFile, scriptsJson, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("snake_script");

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importScripts(tempFile.toString(), true);
            assertTrue(result.isSuccess(), "snake_case 脚本配置导入应该成功");

            SimpleScriptImportExportManager.SimpleScriptConfig config = importExportManager.getScriptConfig("snake_script");
            assertNotNull(config, "脚本配置应该被导入");
            assertEquals("Snake Script", config.getName());
            assertEquals("PYTHON", config.getScriptType());
            assertEquals("python main.py", config.getScriptPath());
            assertEquals("/tmp", config.getWorkingDirectory());
            assertEquals(2, config.getRetryCount());
            assertEquals(15000, config.getTimeout());
            assertTrue(config.isEnabled());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试脚本导入预检模式不写入配置")
    void testImportScriptsDryRun() throws Exception {
        Path tempFile = Files.createTempFile("import-scripts-dry-run", ".json");

        try {
            String scriptsJson = "{"
                + "\"dry_script\":{"
                + "  \"id\":\"dry_script\","
                + "  \"name\":\"Dry Script\","
                + "  \"script_type\":\"SHELL\","
                + "  \"script_path\":\"echo dry\","
                + "  \"is_enabled\":true"
                + "}"
                + "}";
            Files.writeString(tempFile, scriptsJson, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("dry_script");

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importScripts(tempFile.toString(), true, true);

            assertTrue(result.isSuccess(), "脚本导入预检应该成功");
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertTrue(result.getItemsSucceeded() >= 1);
            assertNull(importExportManager.getScriptConfig("dry_script"), "脚本导入预检不应写入配置");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试严格Schema校验拒绝未知版本")
    void testStrictSchemaValidationRejectsUnknownVersion() throws Exception {
        Path tempFile = Files.createTempFile("unknown-schema-strict", ".json");

        try {
            String payload = "{"
                + "\"version\":\"9.9.9\","
                + "\"schemaVersion\":\"9.9.9\","
                + "\"scripts\":{"
                + "  \"unknown_schema_script\":{"
                + "    \"id\":\"unknown_schema_script\","
                + "    \"name\":\"Unknown Schema Script\","
                + "    \"scriptType\":\"SHELL\","
                + "    \"scriptPath\":\"echo unknown\","
                + "    \"enabled\":true"
                + "  }"
                + "}"
                + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("unknown_schema_script");

            SimpleScriptImportExportManager.ImportExportOptions options =
                SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                    .strictSchemaValidation(true)
                    .validateData(true)
                    .build();

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess(), "严格Schema校验应拒绝未知版本");
            assertTrue(result.getMessage().contains("schemaVersion"));
            assertNull(importExportManager.getScriptConfig("unknown_schema_script"), "失败时不应写入脚本配置");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("测试注册自定义Schema迁移后可导入未来版本")
    void testRegisterSchemaMigrationForFutureVersion() throws Exception {
        Path tempFile = Files.createTempFile("future-schema-import", ".json");

        try {
            importExportManager.registerSchemaMigration("9.9.9", (data, context) -> {
                context.addWarning("自定义迁移: 9.9.9 -> 4.0.1");
                return "4.0.1";
            });

            String payload = "{"
                + "\"version\":\"9.9.9\","
                + "\"schemaVersion\":\"9.9.9\","
                + "\"scripts\":{"
                + "  \"future_schema_script\":{"
                + "    \"id\":\"future_schema_script\","
                + "    \"name\":\"Future Schema Script\","
                + "    \"scriptType\":\"SHELL\","
                + "    \"scriptPath\":\"echo future\","
                + "    \"enabled\":true"
                + "  }"
                + "}"
                + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            importExportManager.removeScriptConfig("future_schema_script");

            SimpleScriptImportExportManager.ImportExportOptions options =
                SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                    .strictSchemaValidation(true)
                    .validateData(true)
                    .build();

            SimpleScriptImportExportManager.ImportExportResult result =
                importExportManager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess(), "注册迁移后应可导入未来版本");
            assertEquals("9.9.9", result.getStatistics().get("schemaVersion.original"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.final"));
            assertTrue(result.getWarnings().stream().anyMatch(msg -> msg.contains("自定义迁移")));

            SimpleScriptImportExportManager.SimpleScriptConfig config = importExportManager.getScriptConfig("future_schema_script");
            assertNotNull(config, "脚本配置应该被导入");
            assertEquals("Future Schema Script", config.getName());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    @DisplayName("测试导入导出选项")
    void testImportExportOptions() {
        // 测试默认选项
        SimpleScriptImportExportManager.ImportExportOptions defaultOptions = 
            SimpleScriptImportExportManager.ImportExportOptions.newBuilder().build();
        
        assertTrue(defaultOptions.isIncludeEnvironment(), "默认应该包含环境变量");
        assertFalse(defaultOptions.isIncludeCache(), "默认不应该包含缓存");
        assertFalse(defaultOptions.isOverwriteExisting(), "默认不应该覆盖已存在的数据");
        assertTrue(defaultOptions.isValidateData(), "默认应该验证数据");
        assertFalse(defaultOptions.isDryRun(), "默认不应该启用预检模式");
        assertFalse(defaultOptions.isStrictSchemaValidation(), "默认不应该严格校验Schema版本");
        assertTrue(defaultOptions.isCreateBackup(), "默认应该创建备份");
        
        // 测试自定义选项
        SimpleScriptImportExportManager.ImportExportOptions customOptions = 
            SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                .includeEnvironment(false)
                .includeCache(true)
                .overwriteExisting(true)
                .validateData(false)
                .dryRun(true)
                .strictSchemaValidation(true)
                .createBackup(false)
                .backupLocation("/custom/backup")
                .addMetadata("author", "test")
                .addMetadata("version", "1.0")
                .build();
        
        assertFalse(customOptions.isIncludeEnvironment(), "自定义选项应该不包含环境变量");
        assertTrue(customOptions.isIncludeCache(), "自定义选项应该包含缓存");
        assertTrue(customOptions.isOverwriteExisting(), "自定义选项应该覆盖已存在的数据");
        assertFalse(customOptions.isValidateData(), "自定义选项不应该验证数据");
        assertTrue(customOptions.isDryRun(), "自定义选项应该启用预检模式");
        assertTrue(customOptions.isStrictSchemaValidation(), "自定义选项应该严格校验Schema版本");
        assertFalse(customOptions.isCreateBackup(), "自定义选项不应该创建备份");
        assertEquals("/custom/backup", customOptions.getBackupLocation(), "自定义备份位置应该正确");
        assertEquals("test", customOptions.getMetadata().get("author"), "自定义元数据应该正确");
        assertEquals("1.0", customOptions.getMetadata().get("version"), "自定义元数据应该正确");
    }
    
    @Test
    @DisplayName("测试导入导出结果")
    void testImportExportResult() {
        // 测试成功结果
        SimpleScriptImportExportManager.ImportExportResult successResult = 
            SimpleScriptImportExportManager.ImportExportResult.success("操作成功", 10, 10);
        
        assertTrue(successResult.isSuccess(), "成功结果应该标记为成功");
        assertEquals("操作成功", successResult.getMessage(), "消息应该正确");
        assertEquals(10, successResult.getItemsProcessed(), "处理项目数应该正确");
        assertEquals(10, successResult.getItemsSucceeded(), "成功项目数应该正确");
        assertEquals(0, successResult.getItemsFailed(), "失败项目数应该为0");
        assertTrue(successResult.getWarnings().isEmpty(), "成功结果不应该有警告");
        assertTrue(successResult.getErrors().isEmpty(), "成功结果不应该有错误");
        
        // 测试部分成功结果
        List<String> warnings = Arrays.asList("警告1", "警告2");
        List<String> errors = Arrays.asList("错误1");
        
        SimpleScriptImportExportManager.ImportExportResult partialResult = 
            SimpleScriptImportExportManager.ImportExportResult.partialSuccess("部分成功", 12, 10, 2, warnings, errors);
        
        assertTrue(partialResult.isSuccess(), "部分成功结果应该标记为成功");
        assertEquals("部分成功", partialResult.getMessage(), "消息应该正确");
        assertEquals(12, partialResult.getItemsProcessed(), "处理项目数应该正确");
        assertEquals(10, partialResult.getItemsSucceeded(), "成功项目数应该正确");
        assertEquals(2, partialResult.getItemsFailed(), "失败项目数应该正确");
        assertEquals(2, partialResult.getWarnings().size(), "警告数量应该正确");
        assertEquals(1, partialResult.getErrors().size(), "错误数量应该正确");
        
        // 测试失败结果
        List<String> failureErrors = Arrays.asList("失败原因1", "失败原因2");
        
        SimpleScriptImportExportManager.ImportExportResult failureResult = 
            SimpleScriptImportExportManager.ImportExportResult.failure("操作失败", failureErrors);
        
        assertFalse(failureResult.isSuccess(), "失败结果应该标记为失败");
        assertEquals("操作失败", failureResult.getMessage(), "消息应该正确");
        assertEquals(0, failureResult.getItemsProcessed(), "处理项目数应该为0");
        assertEquals(0, failureResult.getItemsSucceeded(), "成功项目数应该为0");
        assertEquals(0, failureResult.getItemsFailed(), "失败项目数应该为0");
        assertEquals(2, failureResult.getErrors().size(), "错误数量应该正确");
        
        // 测试添加警告和错误
        successResult.addWarning("新警告");
        successResult.addError("新错误");
        successResult.addStatistic("key1", "value1");
        
        assertEquals(1, successResult.getWarnings().size(), "警告数量应该增加");
        assertEquals(1, successResult.getErrors().size(), "错误数量应该增加");
        assertTrue(successResult.getStatistics().containsKey("key1"), "统计信息应该包含新项");
    }
    
    @Test
    @DisplayName("测试格式处理器")
    void testFormatHandlers() throws Exception {
        // 测试JSON格式处理器
        SimpleScriptImportExportManager.JsonFormatHandler jsonHandler = new SimpleScriptImportExportManager.JsonFormatHandler();
        
        assertEquals("JSON", jsonHandler.getName(), "处理器名称应该正确");
        assertEquals("application/json", jsonHandler.getContentType(), "内容类型应该正确");
        assertTrue(jsonHandler.canImport(), "JSON应该支持导入");
        assertTrue(jsonHandler.canExport(), "JSON应该支持导出");
        assertEquals(".json", jsonHandler.getFileExtension(), "文件扩展名应该正确");
        
        // 测试序列化
        Map<String, Object> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", 42);
        
        String serialized = jsonHandler.serialize(testData);
        assertTrue(serialized.contains("\"key1\""), "序列化应该包含键");
        assertTrue(serialized.contains("\"value1\""), "序列化应该包含值");
        
        // 测试反序列化
        Object deserialized = jsonHandler.deserialize(serialized, Map.class);
        assertNotNull(deserialized, "反序列化结果不应该为空");
        assertTrue(deserialized instanceof Map, "反序列化结果应该是Map");
        
        // 测试Properties格式处理器
        SimpleScriptImportExportManager.PropertiesFormatHandler propsHandler = new SimpleScriptImportExportManager.PropertiesFormatHandler();
        
        assertEquals("Properties", propsHandler.getName(), "处理器名称应该正确");
        assertEquals(".properties", propsHandler.getFileExtension(), "文件扩展名应该正确");
        
        // 测试Properties序列化
        Map<String, String> propsData = new HashMap<>();
        propsData.put("key1", "value1");
        propsData.put("key2", "value2");
        
        String propsSerialized = propsHandler.serialize(propsData);
        assertTrue(propsSerialized.contains("key1=value1"), "Properties序列化应该包含键值对");
        
        // 测试Properties反序列化
        Object propsDeserialized = propsHandler.deserialize(propsSerialized, Map.class);
        assertNotNull(propsDeserialized, "Properties反序列化结果不应该为空");
    }
    
    @Test
    @DisplayName("测试文件格式自动检测")
    void testFileFormatDetection() {
        assertEquals(SimpleScriptImportExportManager.ExportFormat.JSON, 
                    SimpleScriptImportExportManager.ExportFormat.fromFileName("test.json"));
        assertEquals(SimpleScriptImportExportManager.ExportFormat.JSON, 
                    SimpleScriptImportExportManager.ExportFormat.fromFileName("test.JSON"));
        assertEquals(SimpleScriptImportExportManager.ExportFormat.PROPERTIES, 
                    SimpleScriptImportExportManager.ExportFormat.fromFileName("config.properties"));
        assertEquals(SimpleScriptImportExportManager.ExportFormat.JSON, 
                    SimpleScriptImportExportManager.ExportFormat.fromFileName("unknown.txt")); // 默认格式
    }
    
    @Test
    @DisplayName("测试管理器基本信息")
    void testManagerBasicInfo() {
        String info = importExportManager.toString();
        assertNotNull(info, "管理器信息不应该为空");
        assertTrue(info.contains("SimpleScriptImportExportManager"), "信息应该包含管理器名称");
        assertTrue(info.contains("scripts="), "信息应该包含脚本数量");
        assertTrue(info.contains("envVars="), "信息应该包含环境变量数量");
        assertTrue(info.contains("formats="), "信息应该包含格式数量");
    }
    
    @Test
    @DisplayName("测试脚本配置管理")
    void testScriptConfigManagement() {
        // 测试添加脚本配置
        SimpleScriptImportExportManager.SimpleScriptConfig config = new SimpleScriptImportExportManager.SimpleScriptConfig();
        config.setId("new_script");
        config.setName("New Script");
        config.setScriptType("PYTHON");
        config.setScriptPath("/path/to/script.py");
        
        importExportManager.addScriptConfig("new_script", config);
        
        // 验证添加
        SimpleScriptImportExportManager.SimpleScriptConfig retrievedConfig = importExportManager.getScriptConfig("new_script");
        assertNotNull(retrievedConfig, "脚本配置应该被添加");
        assertEquals("New Script", retrievedConfig.getName(), "脚本名称应该正确");
        assertEquals("PYTHON", retrievedConfig.getScriptType(), "脚本类型应该正确");
        
        // 测试获取所有脚本ID
        List<String> allIds = importExportManager.getAllScriptIds();
        assertTrue(allIds.contains("new_script"), "所有脚本ID应该包含新脚本");
        assertTrue(allIds.contains("test_script"), "所有脚本ID应该包含测试脚本");
        
        // 测试移除脚本配置
        SimpleScriptImportExportManager.SimpleScriptConfig removedConfig = importExportManager.removeScriptConfig("new_script");
        assertNotNull(removedConfig, "被移除的脚本配置应该返回");
        assertNull(importExportManager.getScriptConfig("new_script"), "移除后脚本配置应该为空");
    }
    
    @Test
    @DisplayName("测试环境变量管理")
    void testEnvironmentVariableManagement() {
        // 测试设置环境变量
        importExportManager.setEnvironmentVariable("NEW_VAR", "new_value");
        
        // 验证设置
        String retrievedValue = importExportManager.getEnvironmentVariable("NEW_VAR");
        assertEquals("new_value", retrievedValue, "环境变量值应该正确");
        
        // 测试获取所有环境变量
        Map<String, String> allVars = importExportManager.getAllEnvironmentVariables();
        assertTrue(allVars.containsKey("NEW_VAR"), "所有环境变量应该包含新变量");
        assertTrue(allVars.containsKey("TEST_VAR"), "所有环境变量应该包含测试变量");
        
        // 测试移除环境变量
        String removedValue = importExportManager.removeEnvironmentVariable("NEW_VAR");
        assertEquals("new_value", removedValue, "被移除的环境变量值应该返回");
        assertNull(importExportManager.getEnvironmentVariable("NEW_VAR"), "移除后环境变量应该为空");
    }
    
    // ==================== 辅助方法 ====================
    
    private void setupTestData() {
        // 添加测试脚本配置
        SimpleScriptImportExportManager.SimpleScriptConfig config = new SimpleScriptImportExportManager.SimpleScriptConfig();
        config.setId("test_script");
        config.setName("Test Script");
        config.setDescription("A test script for import/export");
        config.setScriptType("SHELL");
        config.setScriptPath("echo 'Hello World'");
        config.setWorkingDirectory("/tmp");
        config.setTimeout(30000);
        config.setRetryCount(3);
        config.setEnabled(true);
        config.setTags(Arrays.asList("test", "demo"));
        
        importExportManager.addScriptConfig("test_script", config);
        
        // 添加测试环境变量
        importExportManager.setEnvironmentVariable("TEST_VAR", "test_value");
        importExportManager.setEnvironmentVariable("ANOTHER_VAR", "another_value");
    }
}
