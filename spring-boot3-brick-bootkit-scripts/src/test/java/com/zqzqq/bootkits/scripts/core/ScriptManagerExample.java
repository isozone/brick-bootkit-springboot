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


package com.zqzqq.bootkits.scripts.core;

import com.zqzqq.bootkits.scripts.core.impl.DefaultScriptManager;
import com.zqzqq.bootkits.scripts.utils.OSDetectionUtils;

import java.util.List;

/**
 * 脚本管理器测试示例
 * 演示如何使用脚本管理器执行各种类型的脚本
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptManagerExample {
    
    public static void main(String[] args) {
        try {
            // 显示系统信息
            System.out.println("=== 脚本管理系统示例 ===");
            System.out.println(OSDetectionUtils.getSystemSummary());
            System.out.println();
            
            // 创建脚本管理器
            ScriptManager scriptManager = new DefaultScriptManager();
            
            // 初始化
            System.out.println("正在初始化脚本管理器...");
            scriptManager.initialize();
            System.out.println("脚本管理器初始化完成");
            System.out.println();
            
            // 显示可用的执行器
            System.out.println("=== 可用的脚本执行器 ===");
            List<ScriptExecutor> executors = scriptManager.getRegisteredExecutors();
            for (ScriptExecutor executor : executors) {
                System.out.println("- " + scriptManager.getExecutorDescription(executor));
            }
            System.out.println();
            
            // 检测当前操作系统和脚本类型
            OperatingSystem currentOS = scriptManager.getCurrentOperatingSystem();
            System.out.println("当前操作系统: " + currentOS.getDisplayName());
            
            // 示例脚本路径
            String[] scriptPaths = {
                "system-info.sh",
                "system-info.py", 
                "system-info.js",
                "system-info.bat"
            };
            
            // 执行脚本示例
            System.out.println("\n=== 脚本执行示例 ===");
            executeScripts(scriptManager, scriptPaths);
            
            // 演示脚本内容执行
            System.out.println("\n=== 脚本内容执行示例 ===");
            executeScriptContent(scriptManager);
            
            // 清理
            scriptManager.destroy();
            System.out.println("\n脚本管理器已清理");
            
        } catch (Exception e) {
            System.err.println("示例执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 执行脚本文件
     */
    private static void executeScripts(ScriptManager scriptManager, String[] scriptPaths) {
        for (String scriptPath : scriptPaths) {
            try {
                System.out.println("\n--- 执行脚本: " + scriptPath + " ---");
                
                // 检测脚本类型
                ScriptType scriptType = scriptManager.detectScriptType(scriptPath);
                if (scriptType == null) {
                    System.out.println("跳过无法识别的脚本: " + scriptPath);
                    continue;
                }
                
                System.out.println("检测到的脚本类型: " + scriptType);
                
                // 检查是否可以执行
                if (scriptManager.canExecute(scriptPath)) {
                    // 执行脚本（带参数）
                    ScriptExecutionResult result = scriptManager.executeScript(scriptPath, "参数1", "参数2");
                    
                    // 显示执行结果
                    displayExecutionResult(scriptPath, result);
                } else {
                    System.out.println("脚本不可执行或未找到: " + scriptPath);
                }
                
            } catch (Exception e) {
                System.out.println("执行脚本失败 " + scriptPath + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 执行脚本内容
     */
    private static void executeScriptContent(ScriptManager scriptManager) {
        // Shell脚本内容示例
        String shellScript = 
            "#!/bin/bash\n" +
            "echo 'Hello from Shell script!'\n" +
            "echo 'Current directory: '$(pwd)\n" +
            "echo 'Arguments: '$@\n";
        
        try {
            ScriptExecutionResult result = scriptManager.executeScript(
                ScriptType.SHELL, 
                shellScript, 
                new String[]{"test", "args"}, 
                ScriptConfiguration.quickConfiguration()
            );
            
            System.out.println("Shell脚本内容执行结果:");
            displayExecutionResult("inline-shell-script", result);
            
        } catch (Exception e) {
            System.out.println("Shell脚本内容执行失败: " + e.getMessage());
        }
        
        // Python脚本内容示例
        String pythonScript = 
            "import sys\n" +
            "print('Hello from Python script!')\n" +
            "print('Arguments:', sys.argv[1:])\n" +
            "print('Current directory:', __import__('os').getcwd())\n";
        
        try {
            ScriptExecutionResult result = scriptManager.executeScript(
                ScriptType.PYTHON, 
                pythonScript, 
                new String[]{"python", "args"}, 
                ScriptConfiguration.quickConfiguration()
            );
            
            System.out.println("\nPython脚本内容执行结果:");
            displayExecutionResult("inline-python-script", result);
            
        } catch (Exception e) {
            System.out.println("Python脚本内容执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示执行结果
     */
    private static void displayExecutionResult(String scriptPath, ScriptExecutionResult result) {
        System.out.println("执行状态: " + result.getStatus());
        System.out.println("退出码: " + result.getExitCode());
        
        if (result.getStartTime() != null && result.getEndTime() != null) {
            System.out.println("执行时间: " + result.getExecutionTimeMs() + "ms");
        }
        
        if (!result.getStdout().isEmpty()) {
            System.out.println("标准输出:");
            result.getStdout().forEach(line -> System.out.println("  " + line));
        }
        
        if (!result.getStderr().isEmpty()) {
            System.out.println("错误输出:");
            result.getStderr().forEach(line -> System.out.println("  " + line));
        }
        
        if (result.getErrorMessage() != null) {
            System.out.println("错误信息: " + result.getErrorMessage());
        }
        
        if (result.getThrowable() != null) {
            System.out.println("异常信息: " + result.getThrowable().getMessage());
        }
    }
}