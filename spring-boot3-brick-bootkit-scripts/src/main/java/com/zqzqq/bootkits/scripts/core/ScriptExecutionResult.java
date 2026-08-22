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

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 脚本执行结果
 * 包含脚本执行的详细信息和结果
 *
 * @author starBlues
 * @since 4.0.1
 */
public class ScriptExecutionResult {
    
    /**
     * 执行状态
     */
    public enum ExecutionStatus {
        /**
         * 执行成功
         */
        SUCCESS,
        /**
         * 执行失败
         */
        FAILED,
        /**
         * 执行超时
         */
        TIMEOUT,
        /**
         * 执行被中断
         */
        INTERRUPTED,
        /**
         * 执行环境错误
         */
        ENVIRONMENT_ERROR,
        /**
         * 未知状态
         */
        UNKNOWN
    }
    
    /**
     * 执行状态
     */
    private ExecutionStatus status;
    
    /**
     * 退出码
     */
    private int exitCode;
    
    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 标准输出
     */
    private List<String> stdout = new ArrayList<>();
    
    /**
     * 标准错误输出
     */
    private List<String> stderr = new ArrayList<>();
    
    /**
     * 执行信息
     */
    private String executionInfo;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 异常信息
     */
    private Throwable throwable;
    
    /**
     * 使用的执行器
     */
    private ScriptExecutor executor;
    
    /**
     * 执行的脚本路径
     */
    private String scriptPath;
    
    /**
     * 执行的脚本类型
     */
    private ScriptType scriptType;
    
    /**
     * 执行的操作系统
     */
    private OperatingSystem operatingSystem;
    
    /**
     * 构造函数
     */
    public ScriptExecutionResult() {
    }
    
    /**
     * 构造函数
     *
     * @param status 执行状态
     * @param exitCode 退出码
     */
    public ScriptExecutionResult(ExecutionStatus status, int exitCode) {
        this.status = status;
        this.exitCode = exitCode;
    }
    
    /**
     * 创建成功结果
     *
     * @param exitCode 退出码
     * @param stdout 标准输出
     * @param stderr 标准错误输出
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param executor 执行器
     * @param scriptPath 脚本路径
     * @param scriptType 脚本类型
     * @param operatingSystem 操作系统
     * @return 成功结果
     */
    public static ScriptExecutionResult success(int exitCode, List<String> stdout, List<String> stderr,
                                              LocalDateTime startTime, LocalDateTime endTime,
                                              ScriptExecutor executor, String scriptPath,
                                              ScriptType scriptType, OperatingSystem operatingSystem) {
        ScriptExecutionResult result = new ScriptExecutionResult(ExecutionStatus.SUCCESS, exitCode);
        result.setStdout(stdout);
        result.setStderr(stderr);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setExecutor(executor);
        result.setScriptPath(scriptPath);
        result.setScriptType(scriptType);
        result.setOperatingSystem(operatingSystem);
        result.setExecutionInfo(buildResultValue(stdout, stderr, null));
        result.setExecutionTimeMs(java.time.Duration.between(startTime, endTime).toMillis());
        return result;
    }
    
    /**
     * 创建失败结果
     *
     * @param status 执行状态
     * @param errorMessage 错误信息
     * @param throwable 异常信息
     * @return 失败结果
     */
    public static ScriptExecutionResult failed(ExecutionStatus status, String errorMessage, Throwable throwable) {
        ScriptExecutionResult result = new ScriptExecutionResult(status, -1);
        result.setErrorMessage(errorMessage);
        result.setThrowable(throwable);
        result.setExecutionInfo(buildResultValue(null, null, errorMessage));
        return result;
    }
    
    /**
     * 创建超时结果
     *
     * @param stdout 标准输出
     * @param stderr 标准错误输出
     * @param executor 执行器
     * @param scriptPath 脚本路径
     * @param scriptType 脚本类型
     * @param operatingSystem 操作系统
     * @return 超时结果
     */
    public static ScriptExecutionResult timeout(List<String> stdout, List<String> stderr,
                                              ScriptExecutor executor, String scriptPath,
                                              ScriptType scriptType, OperatingSystem operatingSystem) {
        ScriptExecutionResult result = new ScriptExecutionResult(ExecutionStatus.TIMEOUT, -1);
        result.setStdout(stdout);
        result.setStderr(stderr);
        result.setExecutor(executor);
        result.setScriptPath(scriptPath);
        result.setScriptType(scriptType);
        result.setOperatingSystem(operatingSystem);
        result.setExecutionInfo(buildResultValue(stdout, stderr, null));
        return result;
    }
    
    // Getter和Setter方法
    public ExecutionStatus getStatus() {
        return status;
    }
    
    public ScriptExecutionResult setStatus(ExecutionStatus status) {
        this.status = status;
        return this;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public ScriptExecutionResult setExitCode(int exitCode) {
        this.exitCode = exitCode;
        return this;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public ScriptExecutionResult setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public ScriptExecutionResult setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }
    
    public long getExecutionTimeMs() {
        if (executionTimeMs > 0) {
            return executionTimeMs;
        }
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return executionTimeMs;
    }
    
    public ScriptExecutionResult setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }
    
    public List<String> getStdout() {
        return stdout;
    }
    
    public ScriptExecutionResult setStdout(List<String> stdout) {
        this.stdout = stdout;
        return this;
    }
    
    public List<String> getStderr() {
        return stderr;
    }
    
    public ScriptExecutionResult setStderr(List<String> stderr) {
        this.stderr = stderr;
        return this;
    }
    
    public String getExecutionInfo() {
        return executionInfo != null ? executionInfo : buildResultValue(stdout, stderr, errorMessage);
    }
    
    public ScriptExecutionResult setExecutionInfo(String executionInfo) {
        this.executionInfo = executionInfo;
        return this;
    }

    public String getResultValue() {
        return getExecutionInfo();
    }

    public ScriptExecutionResult setResultValue(String resultValue) {
        return setExecutionInfo(resultValue);
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public ScriptExecutionResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
    public ScriptExecutionResult setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }
    
    public ScriptExecutor getExecutor() {
        return executor;
    }
    
    public ScriptExecutionResult setExecutor(ScriptExecutor executor) {
        this.executor = executor;
        return this;
    }
    
    public String getScriptPath() {
        return scriptPath;
    }
    
    public ScriptExecutionResult setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
        return this;
    }
    
    public ScriptType getScriptType() {
        return scriptType;
    }
    
    public ScriptExecutionResult setScriptType(ScriptType scriptType) {
        this.scriptType = scriptType;
        return this;
    }
    
    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }
    
    public ScriptExecutionResult setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        return this;
    }
    
    /**
     * 判断是否执行成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS && exitCode == 0;
    }
    
    /**
     * 判断是否执行失败
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }
    
    /**
     * 判断是否超时
     *
     * @return 是否超时
     */
    public boolean isTimeout() {
        return status == ExecutionStatus.TIMEOUT;
    }
    
    /**
     * 获取合并的输出（标准输出 + 标准错误输出）
     *
     * @return 合并的输出
     */
    public List<String> getMergedOutput() {
        List<String> merged = new ArrayList<>();
        merged.addAll(stdout);
        if (!stderr.isEmpty()) {
            merged.addAll(stderr);
        }
        return merged;
    }
    
    /**
     * 获取合并的输出字符串
     *
     * @return 合并的输出字符串
     */
    public String getMergedOutputString() {
        return String.join("\n", getMergedOutput());
    }

    private static String buildResultValue(List<String> stdout, List<String> stderr, String fallback) {
        List<String> merged = new ArrayList<>();
        if (stdout != null) {
            merged.addAll(stdout);
        }
        if (stderr != null && !stderr.isEmpty()) {
            merged.addAll(stderr);
        }

        if (!merged.isEmpty()) {
            return String.join("\n", merged);
        }

        return fallback;
    }

    private static String abbreviateForLog(String value) {
        if (value == null || value.length() <= 200) {
            return value;
        }
        return value.substring(0, 197) + "...";
    }
    
    @Override
    public String toString() {
        return "ScriptExecutionResult{" +
                "status=" + status +
                ", exitCode=" + exitCode +
                ", executionTimeMs=" + executionTimeMs +
                ", scriptPath='" + scriptPath + '\'' +
                ", scriptType=" + scriptType +
                ", operatingSystem=" + operatingSystem +
                ", resultValue='" + abbreviateForLog(getResultValue()) + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
