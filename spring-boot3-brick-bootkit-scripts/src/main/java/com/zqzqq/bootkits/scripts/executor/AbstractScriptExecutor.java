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


package com.zqzqq.bootkits.scripts.executor;

import com.zqzqq.bootkits.scripts.core.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 脚本执行器抽象基类
 * 提供脚本执行的基础实现
 *
 * @author starBlues
 * @since 4.0.1
 */
public abstract class AbstractScriptExecutor implements ScriptExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(AbstractScriptExecutor.class);
    
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    
    /**
     * 用于执行输出收集的线程池，避免每次执行创建新线程
     */
    private static final ExecutorService OUTPUT_COLLECTOR_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "OutputCollector");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * 线程池关闭超时时间（秒）
     */
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final long OUTPUT_COLLECTION_WAIT_TIMEOUT_MS = 2000;
    
    static {
        // 注册JVM关闭钩子，确保线程池被正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownExecutor();
        }));
    }
    
    /**
     * 关闭线程池
     */
    public static void shutdownExecutor() {
        OUTPUT_COLLECTOR_EXECUTOR.shutdown();
        try {
            if (!OUTPUT_COLLECTOR_EXECUTOR.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                OUTPUT_COLLECTOR_EXECUTOR.shutdownNow();
                if (!OUTPUT_COLLECTOR_EXECUTOR.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    // 线程池仍未完全关闭，记录警告日志
                    log.error("OutputCollector executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            OUTPUT_COLLECTOR_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public boolean supports(OperatingSystem os, ScriptType scriptType) {
        OperatingSystem[] supportedOS = getSupportedOperatingSystems();
        ScriptType[] supportedTypes = getSupportedScriptTypes();
        
        for (OperatingSystem supportedOSItem : supportedOS) {
            if (supportedOSItem == os) {
                for (ScriptType supportedType : supportedTypes) {
                    if (supportedType == scriptType) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public ScriptExecutionResult execute(String scriptPath, String[] arguments, ScriptConfiguration configuration) throws Exception {
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本路径不能为空", null);
        }
        
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本文件不存在: " + scriptPath, null);
        }
        
        if (!scriptFile.canRead()) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本文件不可读: " + scriptPath, null);
        }
        
        ScriptType detectedType = ScriptType.fromFileName(scriptPath);
        if (detectedType == null || !isScriptTypeSupported(detectedType)) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "不支持的脚本类型: " + scriptPath, null);
        }
        
        if (configuration == null) {
            configuration = ScriptConfiguration.defaultConfiguration();
        }
        
        // 验证脚本文件是否可以执行
        if (!canExecuteScript(scriptFile, configuration)) {
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本文件不可执行: " + scriptPath, null);
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            return doExecute(scriptFile, arguments, configuration, startTime);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
                "脚本执行异常: " + e.getMessage(),
                e);
        }
    }
    
    /**
     * 执行脚本的核心方法，子类必须实现
     *
     * @param scriptFile 脚本文件
     * @param arguments 执行参数
     * @param configuration 执行配置
     * @param startTime 开始时间
     * @return 执行结果
     * @throws Exception 执行异常
     */
    protected abstract ScriptExecutionResult doExecute(File scriptFile, String[] arguments, 
                                                       ScriptConfiguration configuration, 
                                                       LocalDateTime startTime) throws Exception;
    
    /**
     * 检查脚本文件是否可以执行
     *
     * @param scriptFile 脚本文件
     * @param configuration 执行配置
     * @return 是否可以执行
     */
    protected boolean canExecuteScript(File scriptFile, ScriptConfiguration configuration) {
        if (scriptFile == null || !scriptFile.exists()) {
            return false;
        }
        
        // 检查文件大小
        if (scriptFile.length() == 0) {
            return false;
        }
        
        // 检查文件权限（针对Unix系统）
        if (!OperatingSystem.isWindows()) {
            return scriptFile.canExecute() || scriptFile.canRead();
        }
        
        return scriptFile.canRead();
    }
    
    /**
     * 检查是否支持指定的脚本类型
     *
     * @param scriptType 脚本类型
     * @return 是否支持
     */
    protected boolean isScriptTypeSupported(ScriptType scriptType) {
        ScriptType[] supportedTypes = getSupportedScriptTypes();
        for (ScriptType supportedType : supportedTypes) {
            if (supportedType == scriptType) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 创建进程构建器
     *
     * @param scriptFile 脚本文件
     * @param arguments 执行参数
     * @param configuration 执行配置
     * @return 进程构建器
     */
    protected ProcessBuilder createProcessBuilder(File scriptFile, String[] arguments, ScriptConfiguration configuration) {
        List<String> command = new ArrayList<>();
        
        // 构建执行命令
        buildCommand(command, scriptFile, arguments);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        
        // 设置工作目录
        if (configuration.getWorkingDirectory() != null && !configuration.getWorkingDirectory().trim().isEmpty()) {
            processBuilder.directory(new File(configuration.getWorkingDirectory()));
        } else {
            processBuilder.directory(scriptFile.getParentFile());
        }
        
        // 设置环境变量
        Map<String, String> env = processBuilder.environment();
        Map<String, String> customEnv = configuration.getEnvironmentVariables();
        if (customEnv != null && !customEnv.isEmpty()) {
            env.putAll(customEnv);
        }
        
        // 设置输出重定向
        if (configuration.isMergeOutputStreams()) {
            processBuilder.redirectErrorStream(true);
        }
        
        return processBuilder;
    }
    
    /**
     * 构建执行命令，子类必须实现
     *
     * @param command 命令列表
     * @param scriptFile 脚本文件
     * @param arguments 执行参数
     */
    protected abstract void buildCommand(List<String> command, File scriptFile, String[] arguments);
    
    /**
     * 执行进程并获取结果
     *
     * @param processBuilder 进程构建器
     * @param configuration 执行配置
     * @param startTime 开始时间
     * @param scriptFile 脚本文件
     * @return 执行结果
     * @throws Exception 执行异常
     */
    protected ScriptExecutionResult executeProcess(ProcessBuilder processBuilder, ScriptConfiguration configuration, 
                                                  LocalDateTime startTime, File scriptFile) throws Exception {
        Process process = processBuilder.start();
        
        // 创建输出收集器
        OutputCollector outputCollector = new OutputCollector(process, configuration);
        outputCollector.start();
        
        try {
            // 等待进程完成或超时
            boolean finished = process.waitFor(configuration.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                outputCollector.stop();
                
                return ScriptExecutionResult.timeout(
                    outputCollector.getStdout(), 
                    outputCollector.getStderr(),
                    this, 
                    scriptFile.getAbsolutePath(),
                    ScriptType.fromFileName(scriptFile.getName()),
                    OperatingSystem.detectCurrentOS());
            }
            
            int exitCode = process.exitValue();
            outputCollector.stop();
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            return ScriptExecutionResult.success(
                exitCode,
                outputCollector.getStdout(),
                outputCollector.getStderr(),
                startTime,
                endTime,
                this,
                scriptFile.getAbsolutePath(),
                ScriptType.fromFileName(scriptFile.getName()),
                OperatingSystem.detectCurrentOS()
            ).setExecutionTimeMs(executionTimeMs)
             .setStatus(exitCode == 0
                 ? ScriptExecutionResult.ExecutionStatus.SUCCESS
                 : ScriptExecutionResult.ExecutionStatus.FAILED)
             .setErrorMessage(exitCode == 0
                 ? null
                 : "脚本执行失败，退出码: " + exitCode);
            
        } catch (InterruptedException e) {
            process.destroyForcibly();
            outputCollector.stop();
            
            return ScriptExecutionResult.failed(
                ScriptExecutionResult.ExecutionStatus.INTERRUPTED,
                "脚本执行被中断: " + e.getMessage(),
                e);
        }
    }
    
    /**
     * 输出收集器
     * 负责收集进程的标准输出和错误输出
     */
    protected static class OutputCollector {
        private final java.util.List<String> stdout = new ArrayList<>();
        private final java.util.List<String> stderr = new ArrayList<>();
        private final java.io.BufferedReader stdoutReader;
        private final java.io.BufferedReader stderrReader;
        private final long maxOutputSize;
        private final Charset outputCharset;
        private final AtomicLong currentOutputSize = new AtomicLong(0);
        private Future<?> stdoutTask;
        private Future<?> stderrTask;
        private volatile boolean running = false;

        public OutputCollector(Process process, ScriptConfiguration configuration) {
            this.maxOutputSize = configuration.getMaxOutputSize();
            String encoding = configuration.getEncoding();
            this.outputCharset = (encoding == null || encoding.trim().isEmpty())
                ? DEFAULT_CHARSET
                : Charset.forName(encoding);
            this.stdoutReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), outputCharset));
            this.stderrReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream(), outputCharset));
        }

        public void start() {
            if (!running) {
                running = true;
                stdoutTask = OUTPUT_COLLECTOR_EXECUTOR.submit(() ->
                    collectOutput(stdoutReader, stdout, "... [输出被截断，超出最大大小限制]", "标准输出收集异常: "));
                stderrTask = OUTPUT_COLLECTOR_EXECUTOR.submit(() ->
                    collectOutput(stderrReader, stderr, "... [错误输出被截断，超出最大大小限制]", "错误输出收集异常: "));
            }
        }

        public void stop() {
            awaitCollectionTasks(OUTPUT_COLLECTION_WAIT_TIMEOUT_MS);
            closeQuietly(stdoutReader);
            closeQuietly(stderrReader);
            awaitCollectionTasks(OUTPUT_COLLECTION_WAIT_TIMEOUT_MS);
            running = false;
        }

        private void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        private void awaitCollectionTasks(long timeoutMs) {
            waitTask(stdoutTask, timeoutMs);
            waitTask(stderrTask, timeoutMs);
        }

        private void waitTask(Future<?> task, long timeoutMs) {
            if (task == null) {
                return;
            }
            try {
                task.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                task.cancel(true);
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void collectOutput(java.io.BufferedReader reader, List<String> target, String overflowMessage, String errorPrefix) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!appendLine(target, line, overflowMessage)) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (running) {
                    appendError(errorPrefix + e.getMessage());
                }
            }
        }

        private boolean appendLine(List<String> target, String line, String overflowMessage) {
            int lineBytes = line.getBytes(outputCharset).length;
            long current = currentOutputSize.addAndGet(lineBytes);
            if (current <= maxOutputSize) {
                synchronized (target) {
                    target.add(line);
                }
                return true;
            }
            currentOutputSize.addAndGet(-lineBytes);
            synchronized (target) {
                target.add(overflowMessage);
            }
            return false;
        }

        private void appendError(String message) {
            synchronized (stderr) {
                stderr.add(message);
            }
        }

        public java.util.List<String> getStdout() {
            synchronized (stdout) {
                return new ArrayList<>(stdout);
            }
        }

        public java.util.List<String> getStderr() {
            synchronized (stderr) {
                return new ArrayList<>(stderr);
            }
        }
    }
}
