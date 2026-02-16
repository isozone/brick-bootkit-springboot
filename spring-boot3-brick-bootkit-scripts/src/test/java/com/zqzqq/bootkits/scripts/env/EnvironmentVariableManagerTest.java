package com.zqzqq.bootkits.scripts.env;

import com.zqzqq.bootkits.scripts.env.SimpleEnvironmentVariableManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 环境变量管理器测试类
 * 全面测试环境变量管理功能
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("环境变量管理器测试")
public class EnvironmentVariableManagerTest {
    
    private EnvironmentVariableManager manager;
    private EnvironmentVariableManager.EnvironmentVariable testVar;
    
    @BeforeEach
    void setUp() {
        manager = new SimpleEnvironmentVariableManager();
        testVar = new EnvironmentVariableManager.EnvironmentVariable(
            "TEST_VAR", "test_value", EnvironmentVariableManager.VariableType.STRING,
            EnvironmentVariableManager.Scope.PROJECT, false, "测试变量", "test"
        );
    }
    
    @Test
    @DisplayName("测试基础变量操作")
    void testBasicOperations() {
        // 测试设置变量
        assertTrue(manager.setVariable("KEY1", "value1", EnvironmentVariableManager.VariableType.STRING, 
                                     EnvironmentVariableManager.Scope.PROJECT, "描述", "source"));
        assertTrue(manager.hasVariable("KEY1"));
        
        // 测试获取变量
        Optional<EnvironmentVariableManager.EnvironmentVariable> var = manager.getVariable("KEY1");
        assertTrue(var.isPresent());
        assertEquals("value1", var.get().getValue());
        assertEquals("描述", var.get().getDescription());
        assertEquals("source", var.get().getSource());
        
        // 测试删除变量
        assertTrue(manager.removeVariable("KEY1"));
        assertFalse(manager.hasVariable("KEY1"));
        
        // 测试删除不存在的变量
        assertFalse(manager.removeVariable("NON_EXISTENT"));
    }
    
    @Test
    @DisplayName("测试类型转换功能")
    void testTypeConversion() {
        // 设置不同类型的变量
        manager.setVariable("STR_VAR", "hello", EnvironmentVariableManager.VariableType.STRING);
        manager.setVariable("INT_VAR", 42, EnvironmentVariableManager.VariableType.INTEGER);
        manager.setVariable("LONG_VAR", 123456789L, EnvironmentVariableManager.VariableType.LONG);
        manager.setVariable("DOUBLE_VAR", 3.14, EnvironmentVariableManager.VariableType.DOUBLE);
        manager.setVariable("BOOL_VAR", true, EnvironmentVariableManager.VariableType.BOOLEAN);
        
        // 测试类型转换
        assertEquals("hello", manager.getStringValue("STR_VAR").orElse(null));
        assertEquals(Integer.valueOf(42), manager.getIntegerValue("INT_VAR").orElse(null));
        assertEquals(Long.valueOf(123456789L), manager.getLongValue("LONG_VAR").orElse(null));
        assertEquals(Double.valueOf(3.14), manager.getDoubleValue("DOUBLE_VAR").orElse(null));
        assertEquals(Boolean.TRUE, manager.getBooleanValue("BOOL_VAR").orElse(null));
        
        // 测试跨类型转换
        assertEquals("42", manager.getStringValue("INT_VAR").orElse(null));
        assertNull(manager.getIntegerValue("STR_VAR").orElse(null));
    }
    
    @Test
    @DisplayName("测试作用域管理")
    void testScopeManagement() {
        // 在不同作用域设置变量
        manager.setVariable("SCOPE_VAR", "system", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.SYSTEM, "系统变量", "test");
        manager.setVariable("SCOPE_VAR", "user", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.USER, "用户变量", "test");
        manager.setVariable("SCOPE_VAR", "project", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "项目变量", "test");
        manager.setVariable("SCOPE_VAR", "process", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROCESS, "进程变量", "test");
        
        // 测试按作用域查询
        assertTrue(manager.getVariablesByScope(EnvironmentVariableManager.Scope.SYSTEM).size() >= 1);
        assertTrue(manager.getVariablesByScope(EnvironmentVariableManager.Scope.USER).size() >= 1);
        assertEquals(1, manager.getVariablesByScope(EnvironmentVariableManager.Scope.PROJECT).size());
        assertTrue(manager.getVariablesByScope(EnvironmentVariableManager.Scope.PROCESS).size() >= 1);
        assertEquals("process", manager.getStringValue("SCOPE_VAR").orElse(null));
        
        // 测试作用域清理
        int cleared = manager.clearScope(EnvironmentVariableManager.Scope.PROJECT);
        assertEquals(1, cleared);
        assertEquals(0, manager.getVariablesByScope(EnvironmentVariableManager.Scope.PROJECT).size());
        
        // 测试清理所有作用域
        int totalCleared = manager.clearAll();
        assertTrue(totalCleared > 0);
        assertEquals(0, manager.getAllVariables().size());
    }
    
    @Test
    @DisplayName("测试查询和过滤功能")
    void testQueryAndFiltering() {
        // 设置测试数据
        manager.setVariable("APP_NAME", "MyApp", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "应用名称", "config");
        manager.setVariable("APP_VERSION", "1.0", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "应用版本", "config");
        manager.setVariable("DB_HOST", "localhost", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "数据库主机", "config");
        manager.setVariable("DEBUG_MODE", true, EnvironmentVariableManager.VariableType.BOOLEAN, 
                          EnvironmentVariableManager.Scope.PROJECT, "调试模式", "runtime");
        
        // 测试键模式查询
        List<EnvironmentVariableManager.EnvironmentVariable> appVars = manager.findVariablesByKeyPattern("APP_*");
        assertEquals(2, appVars.size());
        
        // 测试类型查询
        List<EnvironmentVariableManager.EnvironmentVariable> boolVars = manager.findVariablesByType(EnvironmentVariableManager.VariableType.BOOLEAN);
        assertEquals(1, boolVars.size());
        assertEquals("DEBUG_MODE", boolVars.get(0).getKey());
        
        // 测试复杂查询
        EnvironmentVariableManager.EnvironmentVariableQuery query = EnvironmentVariableManager.EnvironmentVariableQuery.newBuilder()
                .keyPattern("APP_*")
                .type(EnvironmentVariableManager.VariableType.STRING)
                .sourcePattern("config")
                .build();
        
        List<EnvironmentVariableManager.EnvironmentVariable> queryResult = manager.queryVariables(query);
        assertEquals(2, queryResult.size());
        
        // 测试搜索功能
        List<EnvironmentVariableManager.EnvironmentVariable> searchResult = manager.searchVariables("应用");
        assertEquals(2, searchResult.size());
    }
    
    @Test
    @DisplayName("测试导入导出功能")
    void testImportExport() {
        // 设置测试数据
        Map<String, Object> testData = new HashMap<>();
        testData.put("EXPORT_TEST_1", "value1");
        testData.put("EXPORT_TEST_2", 42);
        testData.put("EXPORT_TEST_3", true);
        
        // 导入数据
        int imported = manager.importFromMap(testData, EnvironmentVariableManager.Scope.PROJECT, true, "import-test");
        assertEquals(3, imported);
        
        // 验证导入
        assertTrue(manager.hasVariable("EXPORT_TEST_1"));
        assertTrue(manager.hasVariable("EXPORT_TEST_2"));
        assertTrue(manager.hasVariable("EXPORT_TEST_3"));
        
        // 测试导出为Map
        Map<String, String> exportedMap = manager.exportToMap(EnvironmentVariableManager.Scope.PROJECT);
        assertTrue(exportedMap.containsKey("EXPORT_TEST_1"));
        assertEquals("value1", exportedMap.get("EXPORT_TEST_1"));
        assertEquals("42", exportedMap.get("EXPORT_TEST_2"));
        assertEquals("true", exportedMap.get("EXPORT_TEST_3"));
        
        // 测试导出为JSON
        String json = manager.exportToJson(EnvironmentVariableManager.Scope.PROJECT);
        assertNotNull(json);
        assertTrue(json.contains("EXPORT_TEST_1"));
        
        // 清空数据
        manager.clearScope(EnvironmentVariableManager.Scope.PROJECT);
        
        // 从JSON导入
        boolean importedFromJson = manager.importFromJson(json, true);
        assertTrue(importedFromJson);
        
        // 验证JSON导入
        assertTrue(manager.hasVariable("EXPORT_TEST_1"));
        assertEquals("value1", manager.getStringValue("EXPORT_TEST_1").orElse(null));
    }
    
    @Test
    @DisplayName("测试系统环境变量导入")
    void testSystemEnvironmentImport() {
        // 设置系统环境变量
        Map<String, String> systemEnv = System.getenv();
        if (systemEnv.containsKey("PATH")) {
            // 导入PATH环境变量
            int imported = manager.importFromSystemEnvironment("PATH", EnvironmentVariableManager.Scope.SYSTEM, true);
            assertTrue(imported > 0);
            assertTrue(manager.hasVariable("PATH"));
        }
    }
    
    @Test
    @DisplayName("测试统计和监控功能")
    void testStatisticsAndMonitoring() {
        // 设置测试数据
        manager.setVariable("STAT_VAR_1", "value1", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.SYSTEM, "统计变量1", "stats");
        manager.setVariable("STAT_VAR_2", 123, EnvironmentVariableManager.VariableType.INTEGER, 
                          EnvironmentVariableManager.Scope.PROJECT, "统计变量2", "stats");
        manager.setVariable("STAT_VAR_3", true, EnvironmentVariableManager.VariableType.BOOLEAN, 
                          EnvironmentVariableManager.Scope.PROJECT, "统计变量3", "stats");
        
        // 测试统计信息
        EnvironmentVariableManager.EnvironmentVariableStatistics stats = manager.getStatistics();
        assertTrue(stats.getTotalVariables() >= 3);
        assertTrue(stats.getSystemScopeCount() >= 1);
        assertTrue(stats.getProjectScopeCount() >= 2);
        
        // 测试作用域分布
        Map<EnvironmentVariableManager.Scope, Long> scopeDist = manager.getScopeDistribution();
        assertTrue(scopeDist.get(EnvironmentVariableManager.Scope.SYSTEM) >= 1);
        assertTrue(scopeDist.get(EnvironmentVariableManager.Scope.PROJECT) >= 2);
        
        // 测试类型分布
        Map<EnvironmentVariableManager.VariableType, Long> typeDist = manager.getTypeDistribution();
        assertTrue(typeDist.get(EnvironmentVariableManager.VariableType.STRING) >= 1);
        assertTrue(typeDist.get(EnvironmentVariableManager.VariableType.INTEGER) >= 1);
        assertTrue(typeDist.get(EnvironmentVariableManager.VariableType.BOOLEAN) >= 1);
    }
    
    @Test
    @DisplayName("测试验证功能")
    void testValidation() {
        // 测试键验证
        assertTrue(manager.validateKey("VALID_KEY"));
        assertTrue(manager.validateKey("KEY_123"));
        assertFalse(manager.validateKey(""));// 空键
        assertFalse(manager.validateKey("invalid-key"));// 包含短横线
        assertFalse(manager.validateKey("123key"));// 以数字开头
        assertFalse(manager.validateKey(null));// null键
        
        // 测试值验证
        assertTrue(manager.validateValue("string", EnvironmentVariableManager.VariableType.STRING));
        assertTrue(manager.validateValue(42, EnvironmentVariableManager.VariableType.INTEGER));
        assertTrue(manager.validateValue("42", EnvironmentVariableManager.VariableType.INTEGER));
        assertTrue(manager.validateValue(true, EnvironmentVariableManager.VariableType.BOOLEAN));
        assertTrue(manager.validateValue("true", EnvironmentVariableManager.VariableType.BOOLEAN));
        assertFalse(manager.validateValue("not_a_number", EnvironmentVariableManager.VariableType.INTEGER));
        
        // 测试类型转换
        assertEquals("42", manager.convertValue(42, EnvironmentVariableManager.VariableType.STRING));
        assertEquals(Integer.valueOf(42), manager.convertValue("42", EnvironmentVariableManager.VariableType.INTEGER));
        assertEquals(Boolean.TRUE, manager.convertValue("true", EnvironmentVariableManager.VariableType.BOOLEAN));
    }
    
    @Test
    @DisplayName("测试批量操作")
    void testBatchOperations() {
        // 测试批量设置
        Map<String, Object> batchVars = new HashMap<>();
        batchVars.put("BATCH_1", "value1");
        batchVars.put("BATCH_2", 42);
        batchVars.put("BATCH_3", true);
        
        boolean batchSetResult = manager.setVariables(batchVars, EnvironmentVariableManager.Scope.PROJECT, "batch-test");
        assertTrue(batchSetResult);
        
        // 验证批量设置
        assertTrue(manager.hasVariable("BATCH_1"));
        assertTrue(manager.hasVariable("BATCH_2"));
        assertTrue(manager.hasVariable("BATCH_3"));
        
        // 测试批量删除
        List<String> keysToRemove = Arrays.asList("BATCH_1", "BATCH_2", "BATCH_3");
        int removedCount = manager.removeVariables(keysToRemove);
        assertEquals(3, removedCount);
        
        // 验证批量删除
        assertFalse(manager.hasVariable("BATCH_1"));
        assertFalse(manager.hasVariable("BATCH_2"));
        assertFalse(manager.hasVariable("BATCH_3"));
    }
    
    @Test
    @DisplayName("测试备份和恢复")
    void testBackupAndRestore() {
        // 设置测试数据
        manager.setVariable("BACKUP_VAR", "backup_value", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "备份变量", "backup-test");
        
        // 创建备份
        String backupId = manager.backupVariables(EnvironmentVariableManager.Scope.PROJECT);
        assertNotNull(backupId);
        
        // 验证备份存在
        List<String> backups = manager.listBackups();
        assertTrue(backups.contains(backupId));
        
        // 修改数据
        manager.setVariable("BACKUP_VAR", "modified_value", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "修改后的变量", "backup-test");
        
        // 恢复备份
        boolean restoreResult = manager.restoreVariables(backupId, true);
        assertTrue(restoreResult);
        
        // 验证恢复
        Optional<String> restoredValue = manager.getStringValue("BACKUP_VAR");
        assertTrue(restoredValue.isPresent());
        assertEquals("backup_value", restoredValue.get());
        
        // 清理备份
        boolean deleteResult = manager.deleteBackup(backupId);
        assertTrue(deleteResult);
        
        // 验证备份已删除
        backups = manager.listBackups();
        assertFalse(backups.contains(backupId));
    }
    
    @Test
    @DisplayName("测试并发访问")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 启动多个线程同时操作
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待开始信号
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "CONCURRENT_VAR_" + threadId + "_" + j;
                        String value = "value_" + threadId + "_" + j;
                        
                        manager.setVariable(key, value, EnvironmentVariableManager.VariableType.STRING, 
                                          EnvironmentVariableManager.Scope.PROJECT, "并发测试变量", "concurrent-test");
                        
                        // 随机读取一些变量
                        if (j % 10 == 0) {
                            manager.getVariable(key);
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // 开始并发操作
        boolean finished = finishLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        assertTrue(finished, "并发操作应该在30秒内完成");
        assertTrue(endTime - startTime < 30000, "操作应该快速完成");
        
        // 验证最终状态
        EnvironmentVariableManager.EnvironmentVariableStatistics stats = manager.getStatistics();
        assertTrue(stats.getTotalVariables() >= threadCount * operationsPerThread);
    }
    
    @Test
    @DisplayName("测试格式化功能")
    void testFormatting() {
        // 设置复杂类型变量
        List<String> listValue = Arrays.asList("item1", "item2", "item3");
        Map<String, Integer> mapValue = new HashMap<>();
        mapValue.put("key1", 10);
        mapValue.put("key2", 20);
        
        manager.setVariable("LIST_VAR", listValue, EnvironmentVariableManager.VariableType.LIST, 
                          EnvironmentVariableManager.Scope.PROJECT, "列表变量", "format-test");
        manager.setVariable("MAP_VAR", mapValue, EnvironmentVariableManager.VariableType.MAP, 
                          EnvironmentVariableManager.Scope.PROJECT, "映射变量", "format-test");
        manager.setVariable("STRING_VAR", "simple_string", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "字符串变量", "format-test");
        
        // 测试格式化
        Optional<EnvironmentVariableManager.EnvironmentVariable> listVar = manager.getVariable("LIST_VAR");
        Optional<EnvironmentVariableManager.EnvironmentVariable> mapVar = manager.getVariable("MAP_VAR");
        Optional<EnvironmentVariableManager.EnvironmentVariable> stringVar = manager.getVariable("STRING_VAR");
        
        assertTrue(listVar.isPresent());
        assertTrue(mapVar.isPresent());
        assertTrue(stringVar.isPresent());
        
        String listFormatted = manager.formatValue(listVar.get());
        String mapFormatted = manager.formatValue(mapVar.get());
        String stringFormatted = manager.formatValue(stringVar.get());
        
        assertNotNull(listFormatted);
        assertNotNull(mapFormatted);
        assertNotNull(stringFormatted);
        assertEquals("simple_string", stringFormatted);
        
        // 测试显示名称
        String listDisplayName = manager.getDisplayName(listVar.get());
        String mapDisplayName = manager.getDisplayName(mapVar.get());
        
        assertTrue(listDisplayName.contains("LIST_VAR"));
        assertTrue(mapDisplayName.contains("MAP_VAR"));
        assertTrue(listDisplayName.contains("PROJECT"));
        assertTrue(mapDisplayName.contains("PROJECT"));
    }
    
    @Test
    @DisplayName("测试功能支持检查")
    void testFeatureSupport() {
        // 测试支持的特性
        assertTrue(manager.supportsFeature("scope-management"));
        assertTrue(manager.supportsFeature("type-conversion"));
        assertTrue(manager.supportsFeature("inheritance"));
        assertTrue(manager.supportsFeature("backup-restore"));
        assertTrue(manager.supportsFeature("import-export"));
        
        // 测试不支持的特性
        assertFalse(manager.supportsFeature("non-existent-feature"));
        
        // 测试能力列表
        List<String> capabilities = manager.getCapabilities();
        assertNotNull(capabilities);
        assertTrue(capabilities.size() > 0);
        assertTrue(capabilities.contains("Environment scope management"));
        assertTrue(capabilities.contains("Variable type validation and conversion"));
    }
    
    @Test
    @DisplayName("测试完整性验证")
    void testIntegrityValidation() {
        // 测试初始状态
        assertTrue(manager.validateIntegrity());
        
        // 添加一些变量
        manager.setVariable("INTEGRITY_VAR", "test", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "完整性测试", "integrity-test");
        
        // 测试修改后的状态
        assertTrue(manager.validateIntegrity());
        
        // 手动破坏完整性（这里通过反射或其他方式）
        // 在实际测试中，可能需要通过测试工具或临时修改内部状态
    }
    
    @Test
    @DisplayName("测试管理器信息")
    void testManagerInfo() {
        // 设置一些变量
        manager.setVariable("INFO_VAR", "info_value", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "信息测试", "info-test");
        
        // 获取管理器信息
        String info = manager.getManagerInfo();
        assertNotNull(info);
        assertTrue(info.contains("SimpleEnvironmentVariableManager"));
        assertTrue(info.contains("variables="));
        
        // 测试toString
        String toString = manager.toString();
        assertEquals(info, toString);
    }
    
    @Test
    @DisplayName("测试空值和边界情况")
    void testNullAndEdgeCases() {
        // 测试null值处理
        assertTrue(manager.setVariable("NULL_VAR", null, EnvironmentVariableManager.VariableType.STRING, 
                                     EnvironmentVariableManager.Scope.PROJECT, "空值测试", "null-test"));
        assertTrue(manager.hasVariable("NULL_VAR"));
        assertNull(manager.getValue("NULL_VAR").orElse(null));
        
        // 测试长字符串
        String longString = "a".repeat(1000);
        assertTrue(manager.setVariable("LONG_VAR", longString, EnvironmentVariableManager.VariableType.STRING, 
                                     EnvironmentVariableManager.Scope.PROJECT, "长字符串测试", "edge-test"));
        assertEquals(longString, manager.getStringValue("LONG_VAR").orElse(null));
        
        // 测试极值
        assertTrue(manager.setVariable("MAX_INT", Integer.MAX_VALUE, EnvironmentVariableManager.VariableType.INTEGER, 
                                     EnvironmentVariableManager.Scope.PROJECT, "最大整数", "edge-test"));
        assertEquals(Integer.MAX_VALUE, manager.getIntegerValue("MAX_INT").orElse(null));
        
        // 测试非常长的键名
        String longKey = "VERY_LONG_KEY_NAME_" + "A".repeat(300);
        assertFalse(manager.validateKey(longKey)); // 应该失败，因为超过了长度限制
    }
}
