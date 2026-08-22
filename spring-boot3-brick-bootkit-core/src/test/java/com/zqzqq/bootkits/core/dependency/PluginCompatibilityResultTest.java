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


package com.zqzqq.bootkits.core.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件兼容性结果测试
 */
@DisplayName("PluginCompatibilityResult Test")
class PluginCompatibilityResultTest {

    @Test
    @DisplayName("测试创建兼容结果")
    void testCreateCompatibleResult() {
        PluginCompatibilityResult result = PluginCompatibilityResult.compatible();

        assertThat(result.isCompatible()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试创建兼容但有警告的结果")
    void testCreateCompatibleWithWarnings() {
        List<String> warnings = Arrays.asList(
            "Plugin version is outdated",
            "Consider upgrading dependencies"
        );

        PluginCompatibilityResult result = PluginCompatibilityResult.compatibleWithWarnings(warnings);

        assertThat(result.isCompatible()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEqualTo(warnings);
        assertThat(result.hasWarnings()).isTrue();
    }

    @Test
    @DisplayName("测试创建不兼容结果")
    void testCreateIncompatibleResult() {
        List<String> errors = Arrays.asList(
            "Version conflict detected",
            "Missing required dependency"
        );

        PluginCompatibilityResult result = PluginCompatibilityResult.incompatible(errors);

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.isIncompatible()).isTrue();
        assertThat(result.getErrors()).isEqualTo(errors);
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试创建不兼容但有警告的结果")
    void testCreateIncompatibleWithWarnings() {
        List<String> errors = Arrays.asList("Critical version conflict");
        List<String> warnings = Arrays.asList("Minor compatibility warning");

        PluginCompatibilityResult result = new PluginCompatibilityResult(false, errors, warnings);

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getErrors()).isEqualTo(errors);
        assertThat(result.getWarnings()).isEqualTo(warnings);
    }

    @Test
    @DisplayName("测试静态工厂方法")
    void testStaticFactoryMethods() {
        PluginCompatibilityResult result1 = PluginCompatibilityResult.compatible();
        assertThat(result1.isCompatible()).isTrue();
        assertThat(result1.getErrors()).isEmpty();

        PluginCompatibilityResult result2 = PluginCompatibilityResult.compatibleWithWarnings(Arrays.asList("warning"));
        assertThat(result2.isCompatible()).isTrue();
        assertThat(result2.getWarnings()).containsExactly("warning");

        PluginCompatibilityResult result3 = PluginCompatibilityResult.incompatible(Arrays.asList("error"));
        assertThat(result3.isCompatible()).isFalse();
        assertThat(result3.getErrors()).containsExactly("error");
    }

    @Test
    @DisplayName("测试空参数处理")
    void testEmptyParameterHandling() {
        PluginCompatibilityResult result1 = PluginCompatibilityResult.compatibleWithWarnings(new ArrayList<>());
        assertThat(result1.getWarnings()).isEmpty();

        PluginCompatibilityResult result2 = PluginCompatibilityResult.incompatible(new ArrayList<>());
        assertThat(result2.getErrors()).isEmpty();

        PluginCompatibilityResult result3 = new PluginCompatibilityResult(false, new ArrayList<>(), new ArrayList<>());
        assertThat(result3.getErrors()).isEmpty();
        assertThat(result3.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试防御性复制")
    void testDefensiveCopies() {
        List<String> errors = Arrays.asList("error1", "error2");
        List<String> warnings = Arrays.asList("warning1", "warning2");

        PluginCompatibilityResult result = new PluginCompatibilityResult(false, errors, warnings);

        // 修改返回的集合不应影响原始对象
        result.getErrors().add("new-error");
        result.getWarnings().add("new-warning");

        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getWarnings()).hasSize(2);
    }

    @Test
    @DisplayName("测试复杂兼容性场景")
    void testComplexCompatibilityScenarios() {
        // 场景1: 完全兼容
        PluginCompatibilityResult fullyCompatible = PluginCompatibilityResult.compatible();
        assertThat(fullyCompatible.isCompatible()).isTrue();
        assertThat(fullyCompatible.getErrorCount()).isZero();
        assertThat(fullyCompatible.hasWarnings()).isFalse();

        // 场景2: 兼容但有警告
        List<String> minorWarnings = Arrays.asList(
            "Plugin uses deprecated API",
            "Version is one minor release behind"
        );
        PluginCompatibilityResult compatibleWithWarnings = PluginCompatibilityResult.compatibleWithWarnings(minorWarnings);
        assertThat(compatibleWithWarnings.isCompatible()).isTrue();
        assertThat(compatibleWithWarnings.getWarningCount()).isEqualTo(2);
        assertThat(compatibleWithWarnings.getWarnings()).hasSize(2);

        // 场景3: 不兼容
        List<String> criticalErrors = Arrays.asList(
            "Major version mismatch",
            "Incompatible dependency versions"
        );
        PluginCompatibilityResult incompatible = PluginCompatibilityResult.incompatible(criticalErrors);
        assertThat(incompatible.isCompatible()).isFalse();
        assertThat(incompatible.getErrorCount()).isEqualTo(2);
        assertThat(incompatible.hasWarnings()).isFalse();

        // 场景4: 不兼容但有额外警告
        List<String> additionalWarnings = Arrays.asList("Some additional context");
        PluginCompatibilityResult incompatibleWithWarnings = new PluginCompatibilityResult(false, criticalErrors, additionalWarnings);
        assertThat(incompatibleWithWarnings.isCompatible()).isFalse();
        assertThat(incompatibleWithWarnings.getErrors()).hasSize(2);
        assertThat(incompatibleWithWarnings.getWarnings()).hasSize(1);
    }

    @Test
    @DisplayName("测试兼容性结果的一致性")
    void testCompatibilityResultConsistency() {
        PluginCompatibilityResult compatible = PluginCompatibilityResult.compatible();
        PluginCompatibilityResult incompatible = PluginCompatibilityResult.incompatible(new ArrayList<>());

        assertThat(compatible.isCompatible()).isTrue();
        assertThat(compatible.getErrorCount()).isZero();
        assertThat(compatible.hasWarnings()).isFalse();

        assertThat(incompatible.isCompatible()).isFalse();
        assertThat(incompatible.getErrorCount()).isZero();
        assertThat(incompatible.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("测试大量错误和警告处理")
    void testLargeNumberOfErrorsAndWarnings() {
        List<String> manyErrors = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            manyErrors.add("Error " + i);
        }

        PluginCompatibilityResult resultWithManyErrors = PluginCompatibilityResult.incompatible(manyErrors);
        assertThat(resultWithManyErrors.isCompatible()).isFalse();
        assertThat(resultWithManyErrors.getErrors()).hasSize(1000);
        assertThat(resultWithManyErrors.getErrors().get(0)).isEqualTo("Error 0");
        assertThat(resultWithManyErrors.getErrors().get(999)).isEqualTo("Error 999");

        List<String> manyWarnings = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            manyWarnings.add("Warning " + i);
        }

        PluginCompatibilityResult resultWithManyWarnings = PluginCompatibilityResult.compatibleWithWarnings(manyWarnings);
        assertThat(resultWithManyWarnings.isCompatible()).isTrue();
        assertThat(resultWithManyWarnings.getWarnings()).hasSize(500);
        assertThat(resultWithManyWarnings.getWarnings().get(0)).isEqualTo("Warning 0");
        assertThat(resultWithManyWarnings.getWarnings().get(499)).isEqualTo("Warning 499");
    }

    @Test
    @DisplayName("测试兼容性结果的复制")
    void testCompatibilityResultCopying() {
        List<String> originalErrors = Arrays.asList("error1", "error2");
        List<String> originalWarnings = Arrays.asList("warning1", "warning2");

        PluginCompatibilityResult original = new PluginCompatibilityResult(false, originalErrors, originalWarnings);

        List<String> copiedErrors = new ArrayList<>(original.getErrors());
        List<String> copiedWarnings = new ArrayList<>(original.getWarnings());

        copiedErrors.add("error3");
        copiedWarnings.add("warning3");

        assertThat(original.getErrors()).hasSize(2);
        assertThat(original.getWarnings()).hasSize(2);
        assertThat(copiedErrors).hasSize(3);
        assertThat(copiedWarnings).hasSize(3);
    }

    @Test
    @DisplayName("测试兼容性结果的迭代")
    void testCompatibilityResultIteration() {
        List<String> errors = Arrays.asList("error1", "error2", "error3");
        List<String> warnings = Arrays.asList("warning1", "warning2");

        PluginCompatibilityResult result = new PluginCompatibilityResult(false, errors, warnings);

        int errorCount = 0;
        for (String error : result.getErrors()) {
            assertThat(error).isNotNull();
            errorCount++;
        }
        assertThat(errorCount).isEqualTo(3);

        int warningCount = 0;
        for (String warning : result.getWarnings()) {
            assertThat(warning).isNotNull();
            warningCount++;
        }
        assertThat(warningCount).isEqualTo(2);
    }

    @Test
    @DisplayName("测试特殊字符和Unicode处理")
    void testSpecialCharactersAndUnicodeHandling() {
        List<String> errorsWithSpecialChars = Arrays.asList(
            "错误: 版本冲突",
            "Error: 版本冲突",
            "Plugin with émojis 🚀",
            "Unicode: ñáéíóú",
            "Special chars: !@#$%^&*()"
        );

        PluginCompatibilityResult result = PluginCompatibilityResult.incompatible(errorsWithSpecialChars);

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getErrors()).hasSize(5);
        assertThat(result.getErrors().get(0)).isEqualTo("错误: 版本冲突");
        assertThat(result.getErrors().get(2)).isEqualTo("Plugin with émojis 🚀");
    }

    @Test
    @DisplayName("测试兼容性结果的线程安全")
    void testCompatibilityResultThreadSafety() throws InterruptedException {
        PluginCompatibilityResult result = PluginCompatibilityResult.compatibleWithWarnings(Arrays.asList("warning"));

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] readsSuccessful = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    boolean compatible = result.isCompatible();
                    List<String> errors = result.getErrors();
                    List<String> warnings = result.getWarnings();
                    int errorCount = result.getErrorCount();
                    boolean hasWarnings = result.hasWarnings();
                    readsSuccessful[index] = true;
                } catch (Exception e) {
                    readsSuccessful[index] = false;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (boolean success : readsSuccessful) {
            assertThat(success).isTrue();
        }
    }

    @Test
    @DisplayName("测试兼容性结果的边界条件")
    void testCompatibilityResultBoundaryConditions() {
        List<String> emptyStringErrors = Arrays.asList("");
        List<String> emptyStringWarnings = Arrays.asList("");

        PluginCompatibilityResult result1 = new PluginCompatibilityResult(false, emptyStringErrors, emptyStringWarnings);
        assertThat(result1.isCompatible()).isFalse();
        assertThat(result1.getErrors()).containsExactly("");
        assertThat(result1.getWarnings()).containsExactly("");

        List<String> whitespaceMessages = Arrays.asList("   ", "\t\n");
        PluginCompatibilityResult result2 = PluginCompatibilityResult.incompatible(whitespaceMessages);
        assertThat(result2.isCompatible()).isFalse();
        assertThat(result2.getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("测试兼容性结果的内存效率")
    void testCompatibilityResultMemoryEfficiency() {
        List<String> largeErrors = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeErrors.add("Error message " + i);
        }

        PluginCompatibilityResult result = PluginCompatibilityResult.incompatible(largeErrors);

        assertThat(result.getErrors()).isEqualTo(largeErrors);

        // 修改返回的副本不应影响原始对象
        result.getErrors().clear();
        assertThat(result.getErrorCount()).isEqualTo(10000);
    }
}
