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

import com.zqzqq.bootkits.scripts.executor.ShellExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 脚本执行结果测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("脚本执行结果测试")
class ScriptExecutionResultTest {

    @Test
    @DisplayName("测试默认构造函数")
    void testDefaultConstructor() {
        ScriptExecutionResult result = new ScriptExecutionResult();
        
        assertThat(result.getStatus()).isNull();
        assertThat(result.getExitCode()).isEqualTo(0);
        assertThat(result.getStartTime()).isNull();
        assertThat(result.getEndTime()).isNull();
        assertThat(result.getExecutionTimeMs()).isEqualTo(0);
        assertThat(result.getStdout()).isNotNull().isEmpty();
        assertThat(result.getStderr()).isNotNull().isEmpty();
        assertThat(result.getExecutionInfo()).isNull();
        assertThat(result.getResultValue()).isNull();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getThrowable()).isNull();
        assertThat(result.getExecutor()).isNull();
        assertThat(result.getScriptPath()).isNull();
        assertThat(result.getScriptType()).isNull();
        assertThat(result.getOperatingSystem()).isNull();
    }

    @Test
    @DisplayName("测试带参数构造函数")
    void testConstructorWithParameters() {
        ScriptExecutionResult.ExecutionStatus status = ScriptExecutionResult.ExecutionStatus.SUCCESS;
        int exitCode = 0;
        
        ScriptExecutionResult result = new ScriptExecutionResult(status, exitCode);
        
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getExitCode()).isEqualTo(exitCode);
    }

    @Test
    @DisplayName("测试成功结果创建")
    void testSuccessResultCreation() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(5);
        
        List<String> stdout = Arrays.asList("line1", "line2", "line3");
        List<String> stderr = Arrays.asList("error1", "error2");
        
        ShellExecutor executor = new ShellExecutor();
        String scriptPath = "/test/script.sh";
        ScriptType scriptType = ScriptType.SHELL;
        OperatingSystem os = OperatingSystem.LINUX;
        
        ScriptExecutionResult result = ScriptExecutionResult.success(
            0, stdout, stderr, startTime, endTime, executor, scriptPath, scriptType, os
        );
        
        assertThat(result.getStatus()).isEqualTo(ScriptExecutionResult.ExecutionStatus.SUCCESS);
        assertThat(result.getExitCode()).isEqualTo(0);
        assertThat(result.getStdout()).isEqualTo(stdout);
        assertThat(result.getStderr()).isEqualTo(stderr);
        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        assertThat(result.getExecutor()).isEqualTo(executor);
        assertThat(result.getScriptPath()).isEqualTo(scriptPath);
        assertThat(result.getScriptType()).isEqualTo(scriptType);
        assertThat(result.getOperatingSystem()).isEqualTo(os);
        assertThat(result.getExecutionInfo()).isEqualTo("line1\nline2\nline3\nerror1\nerror2");
        assertThat(result.getResultValue()).isEqualTo("line1\nline2\nline3\nerror1\nerror2");
        assertThat(result.getExecutionTimeMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("测试失败结果创建")
    void testFailedResultCreation() {
        ScriptExecutionResult.ExecutionStatus status = ScriptExecutionResult.ExecutionStatus.FAILED;
        String errorMessage = "Script execution failed";
        RuntimeException exception = new RuntimeException("Test exception");
        
        ScriptExecutionResult result = ScriptExecutionResult.failed(status, errorMessage, exception);
        
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getExitCode()).isEqualTo(-1);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(result.getExecutionInfo()).isEqualTo(errorMessage);
        assertThat(result.getResultValue()).isEqualTo(errorMessage);
        assertThat(result.getThrowable()).isEqualTo(exception);
    }

    @Test
    @DisplayName("测试超时结果创建")
    void testTimeoutResultCreation() {
        List<String> stdout = Arrays.asList("partial output");
        List<String> stderr = Arrays.asList("timeout error");
        
        ShellExecutor executor = new ShellExecutor();
        String scriptPath = "/test/script.sh";
        ScriptType scriptType = ScriptType.SHELL;
        OperatingSystem os = OperatingSystem.LINUX;
        
        ScriptExecutionResult result = ScriptExecutionResult.timeout(
            stdout, stderr, executor, scriptPath, scriptType, os
        );
        
        assertThat(result.getStatus()).isEqualTo(ScriptExecutionResult.ExecutionStatus.TIMEOUT);
        assertThat(result.getExitCode()).isEqualTo(-1);
        assertThat(result.getStdout()).isEqualTo(stdout);
        assertThat(result.getStderr()).isEqualTo(stderr);
        assertThat(result.getExecutor()).isEqualTo(executor);
        assertThat(result.getScriptPath()).isEqualTo(scriptPath);
        assertThat(result.getScriptType()).isEqualTo(scriptType);
        assertThat(result.getOperatingSystem()).isEqualTo(os);
        assertThat(result.getExecutionInfo()).isEqualTo("partial output\ntimeout error");
        assertThat(result.getResultValue()).isEqualTo("partial output\ntimeout error");
    }

    @Test
    @DisplayName("测试成功状态判断")
    void testSuccessStatusJudgment() {
        // 测试成功状态
        ScriptExecutionResult successResult = new ScriptExecutionResult(
            ScriptExecutionResult.ExecutionStatus.SUCCESS, 0
        );
        assertThat(successResult.isSuccess()).isTrue();
        assertThat(successResult.isFailed()).isFalse();
        assertThat(successResult.isTimeout()).isFalse();
        
        // 测试失败状态
        ScriptExecutionResult failedResult = new ScriptExecutionResult(
            ScriptExecutionResult.ExecutionStatus.FAILED, 1
        );
        assertThat(failedResult.isSuccess()).isFalse();
        assertThat(failedResult.isFailed()).isTrue();
        assertThat(failedResult.isTimeout()).isFalse();
        
        // 测试超时状态
        ScriptExecutionResult timeoutResult = new ScriptExecutionResult(
            ScriptExecutionResult.ExecutionStatus.TIMEOUT, -1
        );
        assertThat(timeoutResult.isSuccess()).isFalse();
        assertThat(timeoutResult.isFailed()).isFalse();
        assertThat(timeoutResult.isTimeout()).isTrue();
    }

    @Test
    @DisplayName("测试合并输出获取")
    void testMergedOutputRetrieval() {
        List<String> stdout = Arrays.asList("stdout1", "stdout2");
        List<String> stderr = Arrays.asList("stderr1", "stderr2");
        
        ScriptExecutionResult result = new ScriptExecutionResult();
        result.setStdout(stdout);
        result.setStderr(stderr);
        
        List<String> merged = result.getMergedOutput();
        assertThat(merged).hasSize(4);
        assertThat(merged).containsExactly("stdout1", "stdout2", "stderr1", "stderr2");
        
        // 测试合并输出字符串
        String mergedString = result.getMergedOutputString();
        assertThat(mergedString).contains("stdout1");
        assertThat(mergedString).contains("stderr1");
        assertThat(mergedString).contains("\n");
        assertThat(result.getResultValue()).isEqualTo(mergedString);
    }

    @Test
    @DisplayName("测试空输出的合并")
    void testEmptyOutputMerging() {
        ScriptExecutionResult result = new ScriptExecutionResult();
        
        List<String> merged = result.getMergedOutput();
        assertThat(merged).isNotNull().isEmpty();
        
        String mergedString = result.getMergedOutputString();
        assertThat(mergedString).isNotNull().isEmpty();
        assertThat(result.getResultValue()).isNull();
    }

    @Test
    @DisplayName("测试只有标准输出的情况")
    void testStdoutOnlyMerging() {
        List<String> stdout = Arrays.asList("only stdout");
        
        ScriptExecutionResult result = new ScriptExecutionResult();
        result.setStdout(stdout);
        result.setStderr(Arrays.asList()); // 空的stderr
        
        List<String> merged = result.getMergedOutput();
        assertThat(merged).hasSize(1);
        assertThat(merged).containsExactly("only stdout");
        assertThat(result.getResultValue()).isEqualTo("only stdout");
    }

    @Test
    @DisplayName("测试只有错误输出的情况")
    void testStderrOnlyMerging() {
        List<String> stderr = Arrays.asList("only stderr");
        
        ScriptExecutionResult result = new ScriptExecutionResult();
        result.setStdout(Arrays.asList()); // 空的stdout
        result.setStderr(stderr);
        
        List<String> merged = result.getMergedOutput();
        assertThat(merged).hasSize(1);
        assertThat(merged).containsExactly("only stderr");
        assertThat(result.getResultValue()).isEqualTo("only stderr");
    }

    @Test
    @DisplayName("测试toString方法")
    void testToStringMethod() {
        ScriptExecutionResult result = new ScriptExecutionResult(
            ScriptExecutionResult.ExecutionStatus.SUCCESS, 0
        );
        result.setExecutionTimeMs(2500);
        result.setScriptPath("/test/script.sh");
        result.setScriptType(ScriptType.SHELL);
        result.setOperatingSystem(OperatingSystem.LINUX);
        result.setResultValue("test result");
        result.setErrorMessage("test error");
        
        String resultString = result.toString();
        
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("status=SUCCESS");
        assertThat(resultString).contains("exitCode=0");
        assertThat(resultString).contains("executionTimeMs=2500");
        assertThat(resultString).contains("scriptPath='/test/script.sh'");
        assertThat(resultString).contains("scriptType=SHELL");
        assertThat(resultString).contains("operatingSystem=LINUX");
        assertThat(resultString).contains("resultValue='test result'");
        assertThat(resultString).contains("errorMessage='test error'");
    }

    @Test
    @DisplayName("测试执行时间计算")
    void testExecutionTimeCalculation() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(3);
        
        ScriptExecutionResult result = new ScriptExecutionResult();
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        
        long expectedTime = java.time.Duration.between(startTime, endTime).toMillis();
        assertThat(result.getExecutionTimeMs()).isEqualTo(expectedTime);
    }

    @Test
    @DisplayName("测试执行结果状态枚举")
    void testExecutionStatusEnum() {
        ScriptExecutionResult.ExecutionStatus[] statuses = ScriptExecutionResult.ExecutionStatus.values();
        
        assertThat(statuses).containsExactly(
            ScriptExecutionResult.ExecutionStatus.SUCCESS,
            ScriptExecutionResult.ExecutionStatus.FAILED,
            ScriptExecutionResult.ExecutionStatus.TIMEOUT,
            ScriptExecutionResult.ExecutionStatus.INTERRUPTED,
            ScriptExecutionResult.ExecutionStatus.ENVIRONMENT_ERROR,
            ScriptExecutionResult.ExecutionStatus.UNKNOWN
        );
    }

    @Test
    @DisplayName("测试结果对象状态修改")
    void testResultObjectStateModification() {
        ScriptExecutionResult result = new ScriptExecutionResult();
        
        // 设置各种属性
        result.setStatus(ScriptExecutionResult.ExecutionStatus.SUCCESS);
        result.setExitCode(1);
        result.setErrorMessage("modified error");
        result.setExecutionInfo("modified info");
        
        assertThat(result.getStatus()).isEqualTo(ScriptExecutionResult.ExecutionStatus.SUCCESS);
        assertThat(result.getExitCode()).isEqualTo(1);
        assertThat(result.getErrorMessage()).isEqualTo("modified error");
        assertThat(result.getExecutionInfo()).isEqualTo("modified info");
        assertThat(result.getResultValue()).isEqualTo("modified info");
    }

    @Test
    @DisplayName("测试结果值别名设置")
    void testResultValueAlias() {
        ScriptExecutionResult result = new ScriptExecutionResult();

        result.setResultValue("script output value");

        assertThat(result.getResultValue()).isEqualTo("script output value");
        assertThat(result.getExecutionInfo()).isEqualTo("script output value");
    }

    @Test
    @DisplayName("测试执行结果不可变性")
    void testResultImmutability() {
        List<String> originalStdout = Arrays.asList("original");
        ScriptExecutionResult result = new ScriptExecutionResult();
        result.setStdout(originalStdout);
        
        // 验证结果中的stdout不能被修改（不可变列表）
        assertThatThrownBy(() -> result.getStdout().add("modified"))
            .isInstanceOf(UnsupportedOperationException.class);
        
        // 验证结果中的stdout包含原始内容
        assertThat(result.getStdout()).containsExactly("original");
        
        // 验证修改原始列表不会影响结果中的内容
        List<String> modifiedOriginal = new ArrayList<>(originalStdout);
        modifiedOriginal.add("modified");
        assertThat(result.getStdout()).hasSize(1); // 仍然是原来的大小
        assertThat(result.getStdout()).containsOnly("original");
    }
}
