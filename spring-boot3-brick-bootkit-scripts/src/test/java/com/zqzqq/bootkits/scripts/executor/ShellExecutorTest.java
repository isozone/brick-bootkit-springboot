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

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shell执行器测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("Shell执行器测试")
class ShellExecutorTest {

    private ShellExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ShellExecutor();
    }

    @Test
    @DisplayName("测试默认构造函数")
    void testDefaultConstructor() {
        ShellExecutor defaultExecutor = new ShellExecutor();
        assertThat(defaultExecutor).isNotNull();
        if (OperatingSystem.isWindows()) {
            assertThat(defaultExecutor.getShellPath()).isNull();
        } else {
            assertThat(defaultExecutor.getShellPath()).isNotNull();
            assertThat(defaultExecutor.getShellPath().trim()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("测试自定义shell路径构造函数")
    void testCustomShellPathConstructor() {
        String customShellPath = "/bin/zsh";
        ShellExecutor customExecutor = new ShellExecutor(customShellPath);
        
        assertThat(customExecutor).isNotNull();
        assertThat(customExecutor.getShellPath()).isEqualTo(customShellPath);
    }

    @Test
    @DisplayName("测试支持的操作系统")
    void testSupportedOperatingSystems() {
        OperatingSystem[] supportedOS = executor.getSupportedOperatingSystems();
        
        assertThat(supportedOS).isNotNull();
        assertThat(supportedOS).hasSize(3);
        assertThat(supportedOS).containsExactlyInAnyOrder(
            OperatingSystem.LINUX,
            OperatingSystem.MACOS,
            OperatingSystem.UNIX
        );
    }

    @Test
    @DisplayName("测试支持的脚本类型")
    void testSupportedScriptTypes() {
        ScriptType[] supportedTypes = executor.getSupportedScriptTypes();
        
        assertThat(supportedTypes).isNotNull();
        assertThat(supportedTypes).hasSize(3);
        assertThat(supportedTypes).containsExactlyInAnyOrder(
            ScriptType.SHELL,
            ScriptType.PYTHON,
            ScriptType.JAVASCRIPT
        );
    }

    @Test
    @DisplayName("测试操作系统支持检查")
    void testOperatingSystemSupportCheck() {
        // 测试支持Linux
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.SHELL)).isTrue();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.PYTHON)).isTrue();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.JAVASCRIPT)).isTrue();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.BATCH)).isFalse();
        
        // 测试支持macOS
        assertThat(executor.supports(OperatingSystem.MACOS, ScriptType.SHELL)).isTrue();
        assertThat(executor.supports(OperatingSystem.MACOS, ScriptType.PYTHON)).isTrue();
        assertThat(executor.supports(OperatingSystem.MACOS, ScriptType.JAVASCRIPT)).isTrue();
        
        // 测试不支持Windows
        assertThat(executor.supports(OperatingSystem.WINDOWS, ScriptType.SHELL)).isFalse();
        assertThat(executor.supports(OperatingSystem.WINDOWS, ScriptType.PYTHON)).isFalse();
        
        // 测试不支持未知系统
        assertThat(executor.supports(OperatingSystem.UNKNOWN, ScriptType.SHELL)).isFalse();
    }

    @Test
    @DisplayName("测试脚本类型支持检查")
    void testScriptTypeSupportCheck() {
        // 测试支持的脚本类型
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.SHELL)).isTrue();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.PYTHON)).isTrue();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.JAVASCRIPT)).isTrue();
        
        // 测试不支持的脚本类型
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.BATCH)).isFalse();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.POWERSHELL)).isFalse();
        assertThat(executor.supports(OperatingSystem.LINUX, ScriptType.EXECUTABLE)).isFalse();
    }

    @Test
    @DisplayName("测试shell路径设置和获取")
    void testShellPathSettingAndGetting() {
        String newShellPath = "/usr/bin/zsh";
        
        executor.setShellPath(newShellPath);
        assertThat(executor.getShellPath()).isEqualTo(newShellPath);
        
        String anotherShellPath = "/bin/bash";
        executor.setShellPath(anotherShellPath);
        assertThat(executor.getShellPath()).isEqualTo(anotherShellPath);
    }

    @Test
    @DisplayName("测试shell可用性检查")
    void testShellAvailabilityCheck() {
        // 由于我们的测试环境可能有shell，所以检查结果应该是一致的
        boolean isAvailable = executor.isShellAvailable();
        
        // 如果shell可用，路径应该不为空
        if (isAvailable) {
            assertThat(executor.getShellPath()).isNotNull();
            assertThat(executor.getShellPath().trim()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("测试toString方法")
    void testToStringMethod() {
        String executorString = executor.toString();
        
        assertThat(executorString).isNotNull();
        assertThat(executorString).contains("ShellExecutor");
        assertThat(executorString).contains("shellPath=");
        assertThat(executorString).contains("supportedOS=");
        assertThat(executorString).contains("supportedTypes=");
    }

    @Test
    @DisplayName("测试空shell路径处理")
    void testEmptyShellPathHandling() {
        // 测试设置空路径
        executor.setShellPath("");
        assertThat(executor.getShellPath()).isEqualTo("");
        
        executor.setShellPath(null);
        assertThat(executor.getShellPath()).isNull();
        
        executor.setShellPath("   ");
        assertThat(executor.getShellPath()).isEqualTo("   ");
    }

    @Test
    @DisplayName("测试构造函数异常情况")
    void testConstructorExceptionCases() {
        // 测试null路径构造函数
        assertThat(new ShellExecutor(null)).isNotNull();
        
        // 测试空字符串路径构造函数
        assertThat(new ShellExecutor("")).isNotNull();
    }

    @Test
    @DisplayName("测试shell路径验证")
    void testShellPathValidation() {
        // 由于我们的执行器可能使用默认路径，验证逻辑应该是合理的
        String shellPath = executor.getShellPath();
        
        if (shellPath != null && !shellPath.trim().isEmpty()) {
            // 验证shell路径格式
            assertThat(shellPath).contains("/"); // Unix-like路径
        }
    }

    @Test
    @DisplayName("测试执行器equals和hashCode")
    void testExecutorEqualsAndHashCode() {
        ShellExecutor executor1 = new ShellExecutor("/bin/bash");
        ShellExecutor executor2 = new ShellExecutor("/bin/bash");
        ShellExecutor executor3 = new ShellExecutor("/bin/zsh");
        
        // 测试相同路径的equals
        assertThat(executor1).isEqualTo(executor2);
        assertThat(executor1.hashCode()).isEqualTo(executor2.hashCode());
        
        // 测试不同路径的equals
        assertThat(executor1).isNotEqualTo(executor3);
        assertThat(executor1.hashCode()).isNotEqualTo(executor3.hashCode());
        
        // 测试与null比较
        assertThat(executor1).isNotEqualTo(null);
        
        // 测试自比较
        assertThat(executor1).isEqualTo(executor1);
    }

    @Test
    @DisplayName("测试执行器线程安全性")
    void testExecutorThreadSafety() throws InterruptedException {
        ShellExecutor sharedExecutor = new ShellExecutor();
        
        Runnable task = () -> {
            // 多次调用各种方法
            sharedExecutor.getSupportedOperatingSystems();
            sharedExecutor.getSupportedScriptTypes();
            sharedExecutor.getShellPath();
            sharedExecutor.isShellAvailable();
        };
        
        // 创建多个线程同时访问执行器
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证执行器仍然正常工作
        assertThat(sharedExecutor.getSupportedOperatingSystems()).isNotNull();
        assertThat(sharedExecutor.getSupportedScriptTypes()).isNotNull();
    }
}
