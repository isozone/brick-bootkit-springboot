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



package com.zqzqq.bootkits.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件代码安全扫描器
 *
 * @author starBlues
 * @since 4.0.0
 */
public class PluginCodeScanner {

    private static final Logger logger = LoggerFactory.getLogger(PluginCodeScanner.class);

    private final PluginSecurityConfiguration configuration;
    private final Set<String> dangerousPatterns;
    private final Set<String> suspiciousPatterns;

    public PluginCodeScanner(PluginSecurityConfiguration configuration) {
        this.configuration = configuration;
        this.dangerousPatterns = initializeDangerousPatterns();
        this.suspiciousPatterns = initializeSuspiciousPatterns();
    }

    /**
     * 扫描插件
     *
     * @param pluginPath 插件路径
     * @return 扫描结果
     */
    public PluginCodeScanResult scanPlugin(Path pluginPath) {
        logger.info("开始扫描插件代码: {}", pluginPath);
        
        PluginCodeScanResult result = new PluginCodeScanResult(pluginPath.toString());
        
        try {
            if (Files.isDirectory(pluginPath)) {
                scanDirectory(pluginPath, result);
            } else if (pluginPath.toString().endsWith(".jar")) {
                scanJarFile(pluginPath, result);
            } else {
                result.addIssue(SecurityIssueType.UNKNOWN_FORMAT, "不支持的插件格式", pluginPath.toString());
            }
            
            result.calculateRiskScore();
            logger.info("插件代码扫描完成: {}, 风险评分: {}", pluginPath, result.getRiskScore());
            
        } catch (Exception e) {
            logger.error("插件代码扫描失败: " + pluginPath, e);
            result.addIssue(SecurityIssueType.SCAN_ERROR, "扫描过程中发生错误: " + e.getMessage(), pluginPath.toString());
        }
        
        return result;
    }

    /**
     * 扫描目录
     */
    private void scanDirectory(Path directory, PluginCodeScanResult result) throws IOException {
        Files.walk(directory)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(classFile -> {
                    try {
                        scanClassFile(Files.readAllBytes(classFile), classFile.toString(), result);
                    } catch (IOException e) {
                        logger.warn("无法读取类文件: " + classFile, e);
                    }
                });
    }

    /**
     * 扫描JAR文件
     */
    private void scanJarFile(Path jarPath, PluginCodeScanResult result) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class")) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        byte[] classBytes = inputStream.readAllBytes();
                        scanClassFile(classBytes, entry.getName(), result);
                    }
                }
            }
        }
    }

    /**
     * 扫描类文件
     */
    private void scanClassFile(byte[] classBytes, String className, PluginCodeScanResult result) {
        try {
            String classContent = new String(classBytes, StandardCharsets.ISO_8859_1);
            
            // 检查危险模式
            for (String pattern : dangerousPatterns) {
                if (classContent.contains(pattern)) {
                    result.addIssue(SecurityIssueType.DANGEROUS_CODE, 
                            "发现危险代码模式: " + pattern, className);
                }
            }
            
            // 检查可疑模式
            for (String pattern : suspiciousPatterns) {
                if (classContent.contains(pattern)) {
                    result.addIssue(SecurityIssueType.SUSPICIOUS_CODE, 
                            "发现可疑代码模式: " + pattern, className);
                }
            }
            
        } catch (Exception e) {
            logger.warn("扫描类文件时发生错误: " + className, e);
        }
    }

    /**
     * 初始化危险模式
     */
    private Set<String> initializeDangerousPatterns() {
        Set<String> patterns = new HashSet<>(configuration.getDangerousPatterns());
        
        // 添加额外的危险模式
        patterns.add("Runtime.getRuntime().exec");
        patterns.add("ProcessBuilder");
        patterns.add("System.exit");
        patterns.add("System.setProperty");
        patterns.add("System.setSecurityManager");
        patterns.add("java.lang.reflect.Method.invoke");
        patterns.add("javax.script.ScriptEngine");
        patterns.add("java.security.AccessController");
        patterns.add("System.load");
        patterns.add("System.loadLibrary");
        
        return patterns;
    }

    /**
     * 初始化可疑模式
     */
    private Set<String> initializeSuspiciousPatterns() {
        Set<String> patterns = new HashSet<>();
        patterns.add("java/lang/reflect/");
        patterns.add("java/net/Socket");
        patterns.add("java/net/ServerSocket");
        patterns.add("java/io/File");
        patterns.add("java/nio/file/");
        patterns.add("java/lang/ClassLoader");
        patterns.add("java/net/URLClassLoader");
        patterns.add("java/security/");
        
        return patterns;
    }
}
