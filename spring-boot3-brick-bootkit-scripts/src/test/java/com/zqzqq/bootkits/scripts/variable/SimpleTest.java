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