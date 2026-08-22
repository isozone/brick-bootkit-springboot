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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 简化版脚本导入导出管理器测试运行器
 * 简单的手动测试运行器
 *
 * @author starBlues
 * @since 4.0.1
 */
public class SimpleScriptImportExportManagerTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== 脚本导入导出管理器测试开始 ===");
        
        SimpleScriptImportExportManager manager = new SimpleScriptImportExportManager();
        
        try {
            // 测试基础功能
            testBasicFunctionality(manager);
            
            // 测试JSON格式导入导出
            testJsonFormatImportExport(manager);
            
            // 测试Properties格式导入导出
            testPropertiesFormatImportExport(manager);
            
            // 测试脚本配置管理
            testScriptConfigManagement(manager);
            
            // 测试环境变量管理
            testEnvironmentVariableManagement(manager);
            
            // 测试格式处理器
            testFormatHandlers(manager);
            
            System.out.println("=== 所有测试通过 ===");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testBasicFunctionality(SimpleScriptImportExportManager manager) {
        System.out.println("\n--- 测试基础功能 ---");
        
        // 添加测试数据
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
        
        manager.addScriptConfig("test_script", config);
        manager.setEnvironmentVariable("TEST_VAR", "test_value");
        manager.setEnvironmentVariable("ANOTHER_VAR", "another_value");
        
        // 验证添加
        assert manager.getScriptConfig("test_script") != null : "脚本配置应该被添加";
        assert "test_value".equals(manager.getEnvironmentVariable("TEST_VAR")) : "环境变量应该正确";
        
        System.out.println("✓ 基础功能测试通过");
    }
    
    private static void testJsonFormatImportExport(SimpleScriptImportExportManager manager) throws Exception {
        System.out.println("\n--- 测试JSON格式导入导出 ---");
        
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
                manager.exportAll(tempFile.toString(), 
                                 SimpleScriptImportExportManager.ExportFormat.JSON, 
                                 options);
            
            assert exportResult.isSuccess() : "导出应该成功";
            assert Files.exists(tempFile) : "导出文件应该存在";
            
            // 验证文件内容
            String content = new String(Files.readAllBytes(tempFile));
            assert content.contains("\"version\"") : "文件应该包含版本信息";
            assert content.contains("\"scripts\"") : "文件应该包含脚本信息";
            assert content.contains("\"environment\"") : "文件应该包含环境变量信息";
            
            // 清理测试数据
            manager.removeScriptConfig("test_script");
            manager.removeEnvironmentVariable("TEST_VAR");
            
            // 测试导入
            SimpleScriptImportExportManager.ImportExportResult importResult = 
                manager.importAll(tempFile.toString(), options);
            
            assert importResult.isSuccess() || importResult.getItemsSucceeded() > 0 : "导入应该成功或部分成功";
            
            // 验证导入的数据
            SimpleScriptImportExportManager.SimpleScriptConfig importedConfig = manager.getScriptConfig("test_script");
            assert importedConfig != null : "脚本配置应该被导入";
            assert "Test Script".equals(importedConfig.getName()) : "脚本名称应该正确";
            
            String envVar = manager.getEnvironmentVariable("TEST_VAR");
            assert "test_value".equals(envVar) : "环境变量应该被导入";
            
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
        
        System.out.println("✓ JSON格式导入导出测试通过");
    }
    
    private static void testPropertiesFormatImportExport(SimpleScriptImportExportManager manager) throws Exception {
        System.out.println("\n--- 测试Properties格式导入导出 ---");
        
        // 创建临时文件
        Path tempFile = Files.createTempFile("test", ".properties");
        
        try {
            // 测试导出环境变量
            SimpleScriptImportExportManager.ImportExportResult exportResult = 
                manager.exportEnvironment(tempFile.toString(), 
                                        SimpleScriptImportExportManager.ExportFormat.PROPERTIES);
            
            assert exportResult.isSuccess() : "环境变量导出应该成功";
            assert Files.exists(tempFile) : "导出文件应该存在";
            
            // 验证文件内容
            String content = new String(Files.readAllBytes(tempFile));
            assert content.contains("TEST_VAR=test_value") : "文件应该包含正确的属性";
            assert content.contains("ANOTHER_VAR=another_value") : "文件应该包含正确的属性";
            
            // 清理测试数据
            manager.removeEnvironmentVariable("TEST_VAR");
            manager.removeEnvironmentVariable("ANOTHER_VAR");
            
            // 测试导入环境变量
            SimpleScriptImportExportManager.ImportExportResult importResult = 
                manager.importEnvironment(tempFile.toString(), true);
            
            assert importResult.isSuccess() : "环境变量导入应该成功";
            
            // 验证导入的数据
            assert manager.getEnvironmentVariable("TEST_VAR") != null : "变量应该被导入";
            assert manager.getEnvironmentVariable("ANOTHER_VAR") != null : "变量应该被导入";
            assert "test_value".equals(manager.getEnvironmentVariable("TEST_VAR")) : "环境变量值应该正确";
            assert "another_value".equals(manager.getEnvironmentVariable("ANOTHER_VAR")) : "环境变量值应该正确";
            
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
        
        System.out.println("✓ Properties格式导入导出测试通过");
    }
    
    private static void testScriptConfigManagement(SimpleScriptImportExportManager manager) {
        System.out.println("\n--- 测试脚本配置管理 ---");
        
        // 测试添加脚本配置
        SimpleScriptImportExportManager.SimpleScriptConfig config = new SimpleScriptImportExportManager.SimpleScriptConfig();
        config.setId("new_script");
        config.setName("New Script");
        config.setScriptType("PYTHON");
        config.setScriptPath("/path/to/script.py");
        
        manager.addScriptConfig("new_script", config);
        
        // 验证添加
        SimpleScriptImportExportManager.SimpleScriptConfig retrievedConfig = manager.getScriptConfig("new_script");
        assert retrievedConfig != null : "脚本配置应该被添加";
        assert "New Script".equals(retrievedConfig.getName()) : "脚本名称应该正确";
        assert "PYTHON".equals(retrievedConfig.getScriptType()) : "脚本类型应该正确";
        
        // 测试获取所有脚本ID
        List<String> allIds = manager.getAllScriptIds();
        assert allIds.contains("new_script") : "所有脚本ID应该包含新脚本";
        
        // 测试移除脚本配置
        SimpleScriptImportExportManager.SimpleScriptConfig removedConfig = manager.removeScriptConfig("new_script");
        assert removedConfig != null : "被移除的脚本配置应该返回";
        assert manager.getScriptConfig("new_script") == null : "移除后脚本配置应该为空";
        
        System.out.println("✓ 脚本配置管理测试通过");
    }
    
    private static void testEnvironmentVariableManagement(SimpleScriptImportExportManager manager) {
        System.out.println("\n--- 测试环境变量管理 ---");
        
        // 测试设置环境变量
        manager.setEnvironmentVariable("NEW_VAR", "new_value");
        
        // 验证设置
        String retrievedValue = manager.getEnvironmentVariable("NEW_VAR");
        assert "new_value".equals(retrievedValue) : "环境变量值应该正确";
        
        // 测试获取所有环境变量
        Map<String, String> allVars = manager.getAllEnvironmentVariables();
        assert allVars.containsKey("NEW_VAR") : "所有环境变量应该包含新变量";
        assert allVars.containsKey("TEST_VAR") : "所有环境变量应该包含测试变量";
        
        // 测试移除环境变量
        String removedValue = manager.removeEnvironmentVariable("NEW_VAR");
        assert "new_value".equals(removedValue) : "被移除的环境变量值应该返回";
        assert manager.getEnvironmentVariable("NEW_VAR") == null : "移除后环境变量应该为空";
        
        System.out.println("✓ 环境变量管理测试通过");
    }
    
    private static void testFormatHandlers(SimpleScriptImportExportManager manager) throws Exception {
        System.out.println("\n--- 测试格式处理器 ---");
        
        // 测试JSON格式处理器
        SimpleScriptImportExportManager.JsonFormatHandler jsonHandler = new SimpleScriptImportExportManager.JsonFormatHandler();
        
        assert "JSON".equals(jsonHandler.getName()) : "处理器名称应该正确";
        assert "application/json".equals(jsonHandler.getContentType()) : "内容类型应该正确";
        assert jsonHandler.canImport() : "JSON应该支持导入";
        assert jsonHandler.canExport() : "JSON应该支持导出";
        assert ".json".equals(jsonHandler.getFileExtension()) : "文件扩展名应该正确";
        
        // 测试序列化
        Map<String, Object> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", 42);
        
        String serialized = jsonHandler.serialize(testData);
        assert serialized.contains("\"key1\"") : "序列化应该包含键";
        assert serialized.contains("\"value1\"") : "序列化应该包含值";
        
        // 测试反序列化
        Object deserialized = jsonHandler.deserialize(serialized, Map.class);
        assert deserialized != null : "反序列化结果不应该为空";
        assert deserialized instanceof Map : "反序列化结果应该是Map";
        
        // 测试Properties格式处理器
        SimpleScriptImportExportManager.PropertiesFormatHandler propsHandler = new SimpleScriptImportExportManager.PropertiesFormatHandler();
        
        assert "Properties".equals(propsHandler.getName()) : "处理器名称应该正确";
        assert ".properties".equals(propsHandler.getFileExtension()) : "文件扩展名应该正确";
        
        // 测试Properties序列化
        Map<String, String> propsData = new HashMap<>();
        propsData.put("key1", "value1");
        propsData.put("key2", "value2");
        
        String propsSerialized = propsHandler.serialize(propsData);
        assert propsSerialized.contains("key1=value1") : "Properties序列化应该包含键值对";
        
        // 测试Properties反序列化
        Object propsDeserialized = propsHandler.deserialize(propsSerialized, Map.class);
        assert propsDeserialized != null : "Properties反序列化结果不应该为空";
        
        // 测试文件格式自动检测
        assert SimpleScriptImportExportManager.ExportFormat.JSON.equals(
            SimpleScriptImportExportManager.ExportFormat.fromFileName("test.json")) : "JSON格式检测应该正确";
        assert SimpleScriptImportExportManager.ExportFormat.PROPERTIES.equals(
            SimpleScriptImportExportManager.ExportFormat.fromFileName("config.properties")) : "Properties格式检测应该正确";
        assert SimpleScriptImportExportManager.ExportFormat.JSON.equals(
            SimpleScriptImportExportManager.ExportFormat.fromFileName("unknown.txt")) : "未知格式应该默认为JSON";
        
        System.out.println("✓ 格式处理器测试通过");
    }
}