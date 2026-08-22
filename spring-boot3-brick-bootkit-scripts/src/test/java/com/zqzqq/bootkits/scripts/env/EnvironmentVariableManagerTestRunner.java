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


package com.zqzqq.bootkits.scripts.env;

/**
 * 环境变量管理器测试运行器
 * 简单的手动测试运行器
 *
 * @author starBlues
 * @since 4.0.1
 */
public class EnvironmentVariableManagerTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== 环境变量管理器测试开始 ===");
        
        EnvironmentVariableManager manager = new SimpleEnvironmentVariableManager();
        
        try {
            // 测试基础操作
            testBasicOperations(manager);
            
            // 测试类型转换
            testTypeConversion(manager);
            
            // 测试作用域管理
            testScopeManagement(manager);
            
            // 测试查询和过滤
            testQueryAndFiltering(manager);
            
            // 测试导入导出
            testImportExport(manager);
            
            // 测试统计功能
            testStatistics(manager);
            
            // 测试验证功能
            testValidation(manager);
            
            System.out.println("=== 所有测试通过 ===");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testBasicOperations(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试基础操作 ---");
        
        // 测试设置变量
        assert manager.setVariable("KEY1", "value1", EnvironmentVariableManager.VariableType.STRING, 
                                 EnvironmentVariableManager.Scope.PROJECT, "描述", "source") : "设置变量失败";
        assert manager.hasVariable("KEY1") : "变量不存在";
        
        // 测试获取变量
        var var = manager.getVariable("KEY1");
        assert var.isPresent() : "获取变量失败";
        assert var.get().getValue().equals("value1") : "变量值不正确";
        
        // 测试删除变量
        assert manager.removeVariable("KEY1") : "删除变量失败";
        assert !manager.hasVariable("KEY1") : "变量仍然存在";
        
        System.out.println("✓ 基础操作测试通过");
    }
    
    private static void testTypeConversion(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试类型转换 ---");
        
        // 设置不同类型的变量
        manager.setVariable("STR_VAR", "hello", EnvironmentVariableManager.VariableType.STRING);
        manager.setVariable("INT_VAR", 42, EnvironmentVariableManager.VariableType.INTEGER);
        manager.setVariable("LONG_VAR", 123456789L, EnvironmentVariableManager.VariableType.LONG);
        manager.setVariable("DOUBLE_VAR", 3.14, EnvironmentVariableManager.VariableType.DOUBLE);
        manager.setVariable("BOOL_VAR", true, EnvironmentVariableManager.VariableType.BOOLEAN);
        
        // 测试类型转换
        assert "hello".equals(manager.getStringValue("STR_VAR").orElse(null)) : "字符串转换失败";
        assert Integer.valueOf(42).equals(manager.getIntegerValue("INT_VAR").orElse(null)) : "整数转换失败";
        assert Long.valueOf(123456789L).equals(manager.getLongValue("LONG_VAR").orElse(null)) : "长整型转换失败";
        assert Double.valueOf(3.14).equals(manager.getDoubleValue("DOUBLE_VAR").orElse(null)) : "双精度转换失败";
        assert Boolean.TRUE.equals(manager.getBooleanValue("BOOL_VAR").orElse(null)) : "布尔转换失败";
        
        // 测试跨类型转换
        assert "42".equals(manager.getStringValue("INT_VAR").orElse(null)) : "跨类型转换失败";
        
        System.out.println("✓ 类型转换测试通过");
    }
    
    private static void testScopeManagement(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试作用域管理 ---");
        
        // 清理测试数据
        manager.clearScope(EnvironmentVariableManager.Scope.PROJECT);
        
        // 在不同作用域设置变量
        manager.setVariable("SCOPE_VAR", "system", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.SYSTEM, "系统变量", "test");
        manager.setVariable("SCOPE_VAR", "user", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.USER, "用户变量", "test");
        manager.setVariable("SCOPE_VAR", "project", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "项目变量", "test");
        
        // 测试按作用域查询
        assert manager.getVariablesByScope(EnvironmentVariableManager.Scope.SYSTEM).size() >= 1 : "系统作用域查询失败";
        assert manager.getVariablesByScope(EnvironmentVariableManager.Scope.USER).size() >= 1 : "用户作用域查询失败";
        assert manager.getVariablesByScope(EnvironmentVariableManager.Scope.PROJECT).size() >= 1 : "项目作用域查询失败";
        
        // 测试优先级查找（PROJECT应该覆盖USER和SYSTEM）
        var inherited = manager.getVariable("SCOPE_VAR");
        assert inherited.isPresent() : "继承查找失败";
        assert "project".equals(inherited.get().getValue()) : "作用域优先级不正确";
        
        System.out.println("✓ 作用域管理测试通过");
    }
    
    private static void testQueryAndFiltering(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试查询和过滤 ---");
        
        // 设置测试数据
        manager.setVariable("APP_NAME", "MyApp", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "应用名称", "config");
        manager.setVariable("APP_VERSION", "1.0", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "应用版本", "config");
        manager.setVariable("DB_HOST", "localhost", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.PROJECT, "数据库主机", "config");
        
        // 测试键模式查询
        var appVars = manager.findVariablesByKeyPattern("APP_*");
        assert appVars.size() >= 2 : "键模式查询失败";
        
        // 测试搜索功能
        var searchResult = manager.searchVariables("应用");
        assert searchResult.size() >= 2 : "搜索功能失败";
        
        System.out.println("✓ 查询和过滤测试通过");
    }
    
    private static void testImportExport(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试导入导出 ---");
        
        // 设置测试数据
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("EXPORT_TEST_1", "value1");
        testData.put("EXPORT_TEST_2", 42);
        testData.put("EXPORT_TEST_3", true);
        
        // 导入数据
        int imported = manager.importFromMap(testData, EnvironmentVariableManager.Scope.PROJECT, true, "import-test");
        assert imported == 3 : "导入失败";
        
        // 验证导入
        assert manager.hasVariable("EXPORT_TEST_1") : "导入变量1不存在";
        assert manager.hasVariable("EXPORT_TEST_2") : "导入变量2不存在";
        assert manager.hasVariable("EXPORT_TEST_3") : "导入变量3不存在";
        
        // 测试导出为Map
        var exportedMap = manager.exportToMap(EnvironmentVariableManager.Scope.PROJECT);
        assert exportedMap.containsKey("EXPORT_TEST_1") : "导出Map失败";
        assert "value1".equals(exportedMap.get("EXPORT_TEST_1")) : "导出值不正确";
        
        System.out.println("✓ 导入导出测试通过");
    }
    
    private static void testStatistics(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试统计功能 ---");
        
        // 添加一些变量用于统计
        manager.setVariable("STAT_VAR_1", "value1", EnvironmentVariableManager.VariableType.STRING, 
                          EnvironmentVariableManager.Scope.SYSTEM, "统计变量1", "stats");
        manager.setVariable("STAT_VAR_2", 123, EnvironmentVariableManager.VariableType.INTEGER, 
                          EnvironmentVariableManager.Scope.PROJECT, "统计变量2", "stats");
        
        // 测试统计信息
        var stats = manager.getStatistics();
        assert stats.getTotalVariables() >= 2 : "总变量数统计错误";
        assert stats.getSystemScopeCount() >= 1 : "系统作用域统计错误";
        
        // 测试作用域分布
        var scopeDist = manager.getScopeDistribution();
        assert scopeDist.get(EnvironmentVariableManager.Scope.SYSTEM) >= 1 : "作用域分布统计错误";
        
        System.out.println("✓ 统计功能测试通过");
    }
    
    private static void testValidation(EnvironmentVariableManager manager) {
        System.out.println("\n--- 测试验证功能 ---");
        
        // 测试键验证
        assert manager.validateKey("VALID_KEY") : "有效键验证失败";
        assert !manager.validateKey("") : "空键验证失败";
        assert !manager.validateKey("invalid-key") : "无效键验证失败";
        assert !manager.validateKey(null) : "null键验证失败";
        
        // 测试值验证
        assert manager.validateValue("string", EnvironmentVariableManager.VariableType.STRING) : "字符串值验证失败";
        assert manager.validateValue(42, EnvironmentVariableManager.VariableType.INTEGER) : "整数值验证失败";
        assert manager.validateValue("not_a_number", EnvironmentVariableManager.VariableType.INTEGER) == false : "无效整数值验证失败";
        
        // 测试类型转换
        assert "42".equals(manager.convertValue(42, EnvironmentVariableManager.VariableType.STRING)) : "类型转换失败";
        assert Integer.valueOf(42).equals(manager.convertValue("42", EnvironmentVariableManager.VariableType.INTEGER)) : "反向类型转换失败";
        
        System.out.println("✓ 验证功能测试通过");
    }
}