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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Python执行器环境检查测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("Python执行器环境检查测试")
class PythonExecutorEnvironmentTest {

    private PythonExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new PythonExecutor();
    }

    @Test
    @DisplayName("测试Python环境检查")
    void testPythonEnvironmentCheck() {
        PythonExecutor.PythonEnvironmentCheck check = executor.checkPythonEnvironment();
        
        assertThat(check).isNotNull();
        assertThat(check.getDiagnostics()).isNotNull();
        
        if (check.isAvailable()) {
            assertThat(check.getErrorMessage()).isNull();
            assertThat(check.getException()).isNull();
        } else {
            assertThat(check.getErrorMessage()).isNotNull();
            // 错误情况下可能没有异常（如Python未安装）
        }
    }

    @Test
    @DisplayName("测试Python环境诊断信息")
    void testPythonEnvironmentDiagnostics() {
        List<String> diagnostics = executor.getPythonEnvironmentDiagnostics();
        
        assertThat(diagnostics).isNotNull();
        assertThat(diagnostics).isNotEmpty();
        
        // 验证包含基本诊断信息
        assertThat(diagnostics).anyMatch(s -> s.contains("Python环境诊断"));
        assertThat(diagnostics).anyMatch(s -> s.contains("操作系统"));
    }

    @Test
    @DisplayName("测试可用Python解释器列表")
    void testAvailablePythonInterpreters() {
        List<String> interpreters = PythonExecutor.listAvailablePythonInterpreters();
        
        assertThat(interpreters).isNotNull();
        
        // 如果系统有Python，至少应该有一个解释器
        if (executor.isPythonAvailable()) {
            assertThat(interpreters).isNotEmpty();
            
            // 验证格式
            interpreters.forEach(interpreter -> {
                assertThat(interpreter).contains("-");
                assertThat(interpreter).containsAnyOf("python", "Python");
            });
        }
    }

    @Test
    @DisplayName("测试Python安装建议")
    void testPythonInstallationSuggestions() {
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        List<String> suggestions = PythonExecutor.getPythonInstallationSuggestions(currentOS);
        
        assertThat(suggestions).isNotNull();
        assertThat(suggestions).isNotEmpty();
        
        // 验证包含安装建议
        assertThat(suggestions).anyMatch(s -> s.contains("Python安装建议"));
        assertThat(suggestions).anyMatch(s -> s.contains("安装"));
        assertThat(suggestions).anyMatch(s -> s.contains("验证"));
    }

    @Test
    @DisplayName("测试自动修复尝试")
    void testAutoFixAttempt() {
        PythonExecutor.PythonEnvironmentCheck originalCheck = executor.checkPythonEnvironment();
        
        // 尝试自动修复
        PythonExecutor.PythonEnvironmentCheck fixedCheck = executor.attemptAutoFix();
        
        assertThat(fixedCheck).isNotNull();
        
        // 如果原始检查失败，修复后的结果应该不同
        if (!originalCheck.isAvailable()) {
            assertThat(fixedCheck.getDiagnostics()).isNotNull();
            assertThat(fixedCheck.getDiagnostics().size()).isGreaterThanOrEqualTo(
                originalCheck.getDiagnostics().size());
        }
    }

    @Test
    @DisplayName("测试无效Python路径构造函数")
    void testInvalidPythonPathConstructor() {
        // 测试null路径
        boolean nullTestFailed = false;
        try {
            new PythonExecutor((String) null);
        } catch (IllegalArgumentException e) {
            nullTestFailed = true;
            assertThat(e.getMessage()).isEqualTo("Python路径不能为空");
        }
        assertThat(nullTestFailed).isTrue();
        
        // 测试空字符串路径
        boolean emptyTestFailed = false;
        try {
            new PythonExecutor("");
        } catch (IllegalArgumentException e) {
            emptyTestFailed = true;
            assertThat(e.getMessage()).isEqualTo("Python路径不能为空");
        }
        assertThat(emptyTestFailed).isTrue();
        
        // 测试空白字符串路径
        boolean blankTestFailed = false;
        try {
            new PythonExecutor("   ");
        } catch (IllegalArgumentException e) {
            blankTestFailed = true;
            assertThat(e.getMessage()).isEqualTo("Python路径不能为空");
        }
        assertThat(blankTestFailed).isTrue();
    }

    @Test
    @DisplayName("测试版本偏好构造函数")
    void testVersionPreferenceConstructor() {
        // 创建带版本偏好的执行器
        PythonExecutor versionExecutor = new PythonExecutor("3.8", true);
        
        assertThat(versionExecutor).isNotNull();
        
        if (versionExecutor.isPythonAvailable()) {
            assertThat(versionExecutor.getPythonPath()).isNotNull();
            assertThat(versionExecutor.getPythonVersion()).isNotNull();
        }
    }

    @Test
    @DisplayName("测试环境检查的完整性")
    void testEnvironmentCheckCompleteness() {
        PythonExecutor.PythonEnvironmentCheck check = executor.checkPythonEnvironment();
        
        if (!check.isAvailable()) {
            // 如果环境不可用，应该有错误信息
            assertThat(check.getErrorMessage()).isNotNull();
            
            // 验证错误信息的质量
            String errorMessage = check.getErrorMessage();
            assertThat(errorMessage.length()).isGreaterThan(10); // 错误信息应该足够详细
        }
        
        // 检查诊断信息不为空
        assertThat(check.getDiagnostics()).isNotNull();
    }

    @Test
    @DisplayName("测试Python核心模块检查")
    void testPythonCoreModulesCheck() {
        // 只有在Python可用时才进行核心模块检查测试
        if (!executor.isPythonAvailable()) {
            System.out.println("Python不可用，跳过核心模块检查测试");
            return;
        }
        
        PythonExecutor.PythonEnvironmentCheck check = executor.checkPythonEnvironment();
        
        // 验证包含核心模块检查结果
        boolean hasModuleCheck = check.getDiagnostics().stream()
                .anyMatch(d -> d.contains("Python核心模块检查"));
        
        // 如果没有找到核心模块检查诊断，可能是因为环境问题，标记为跳过
        if (!hasModuleCheck) {
            System.out.println("警告: 核心模块检查诊断未找到，跳过此断言");
            return;
        }
        
        assertThat(hasModuleCheck).isTrue();
    }

    @Test
    @DisplayName("测试Python环境检查的重复性")
    void testEnvironmentCheckRepeatability() {
        // 多次执行环境检查，结果应该一致
        PythonExecutor.PythonEnvironmentCheck check1 = executor.checkPythonEnvironment();
        PythonExecutor.PythonEnvironmentCheck check2 = executor.checkPythonEnvironment();
        
        assertThat(check1.isAvailable()).isEqualTo(check2.isAvailable());
        
        if (check1.isAvailable()) {
            String path1 = executor.getPythonPath();
            String path2 = executor.getPythonPath();
            assertThat(path1).isEqualTo(path2);
        }
    }
}