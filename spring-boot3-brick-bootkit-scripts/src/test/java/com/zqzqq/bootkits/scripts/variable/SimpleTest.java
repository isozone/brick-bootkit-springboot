package com.zqzqq.bootkits.scripts.variable;

import com.zqzqq.bootkits.scripts.variable.impl.DefaultScriptVariableResolver;

/**
 * 简单测试验证基本功能
 */
public class SimpleTest {
    public static void main(String[] args) {
        try {
            DefaultScriptVariableResolver resolver = new DefaultScriptVariableResolver();
            VariableContext context = new VariableContext();
            
            // 设置基本变量
            context.setVariable("username", "testuser");
            context.setVariable("age", 25);
            
            String script = "Hello ${username}, you are ${age} years old";
            
            System.out.println("原始脚本: " + script);
            
            ScriptVariableResolver.VariableReplacementResult result = resolver.resolveVariables(script, context);
            
            System.out.println("解析成功: " + result.isSuccess());
            System.out.println("解析结果: " + result.getReplacedContent());
            
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                System.out.println("错误信息:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }
            
            if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                System.out.println("警告信息:");
                for (String warning : result.getWarnings()) {
                    System.out.println("  - " + warning);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}