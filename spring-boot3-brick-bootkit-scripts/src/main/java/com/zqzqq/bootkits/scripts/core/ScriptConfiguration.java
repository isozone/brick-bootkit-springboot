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

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本执行配置
 * 用于配置脚本执行的各种参数
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptConfiguration {
    
    /**
     * 执行超时时间（毫秒），默认30秒
     */
    private long timeoutMs = 30000;
    
    /**
     * 工作目录
     */
    private String workingDirectory;
    
    /**
     * 环境变量
     */
    private Map<String, String> environmentVariables = new HashMap<>();
    
    /**
     * 是否合并输出流
     */
    private boolean mergeOutputStreams = true;
    
    /**
     * 字符编码
     */
    private String encoding = "UTF-8";
    
    /**
     * 是否启用调试模式
     */
    private boolean debugMode = false;
    
    /**
     * 最大输出大小（字节），默认10MB
     */
    private long maxOutputSize = 10 * 1024 * 1024;
    
    /**
     * 脚本执行用户
     */
    private String user;
    
    /**
     * 脚本执行组
     */
    private String group;
    
    /**
     * 脚本路径
     */
    private String scriptPath;
    
    /**
     * 脚本内容
     */
    private String scriptContent;
    
    /**
     * 构造函数
     */
    public ScriptConfiguration() {
    }
    
    /**
     * 构造函数
     *
     * @param timeoutMs 超时时间
     */
    public ScriptConfiguration(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * 构造函数
     *
     * @param timeoutMs 超时时间
     * @param workingDirectory 工作目录
     */
    public ScriptConfiguration(long timeoutMs, String workingDirectory) {
        this.timeoutMs = timeoutMs;
        this.workingDirectory = workingDirectory;
    }
    
    // Getter和Setter方法
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public ScriptConfiguration setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }
    
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    public ScriptConfiguration setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }
    
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
    
    public ScriptConfiguration setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = (environmentVariables == null) ? new HashMap<>() : environmentVariables;
        return this;
    }
    
    public ScriptConfiguration addEnvironmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
        return this;
    }
    
    public boolean isMergeOutputStreams() {
        return mergeOutputStreams;
    }
    
    public ScriptConfiguration setMergeOutputStreams(boolean mergeOutputStreams) {
        this.mergeOutputStreams = mergeOutputStreams;
        return this;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public ScriptConfiguration setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public ScriptConfiguration setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }
    
    public long getMaxOutputSize() {
        return maxOutputSize;
    }
    
    public ScriptConfiguration setMaxOutputSize(long maxOutputSize) {
        this.maxOutputSize = maxOutputSize;
        return this;
    }
    
    public String getUser() {
        return user;
    }
    
    public ScriptConfiguration setUser(String user) {
        this.user = user;
        return this;
    }
    
    public String getGroup() {
        return group;
    }
    
    public ScriptConfiguration setGroup(String group) {
        this.group = group;
        return this;
    }
    
    public String getScriptPath() {
        return scriptPath;
    }
    
    public ScriptConfiguration setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
        return this;
    }
    
    public String getScriptContent() {
        return scriptContent;
    }
    
    public ScriptConfiguration setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
        return this;
    }
    
    /**
     * 创建默认配置
     *
     * @return 默认配置
     */
    public static ScriptConfiguration defaultConfiguration() {
        return new ScriptConfiguration();
    }
    
    /**
     * 创建快速配置（短超时时间）
     *
     * @return 快速配置
     */
    public static ScriptConfiguration quickConfiguration() {
        return new ScriptConfiguration(5000);
    }
    
    /**
     * 创建调试配置
     *
     * @return 调试配置
     */
    public static ScriptConfiguration debugConfiguration() {
        return new ScriptConfiguration().setDebugMode(true).setTimeoutMs(60000);
    }
    
    @Override
    public String toString() {
        return "ScriptConfiguration{" +
                "timeoutMs=" + timeoutMs +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", mergeOutputStreams=" + mergeOutputStreams +
                ", encoding='" + encoding + '\'' +
                ", debugMode=" + debugMode +
                ", maxOutputSize=" + maxOutputSize +
                ", user='" + user + '\'' +
                ", group='" + group + '\'' +
                ", scriptPath='" + scriptPath + '\'' +
                ", scriptContent='" + scriptContent + '\'' +
                '}';
    }
}
