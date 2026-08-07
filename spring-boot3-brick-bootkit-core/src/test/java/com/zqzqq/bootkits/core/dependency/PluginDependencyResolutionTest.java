package com.zqzqq.bootkits.core.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件依赖解析结果测试
 */
@DisplayName("PluginDependencyResolution Test")
class PluginDependencyResolutionTest {

    @Test
    @DisplayName("测试创建成功的解析结果")
    void testCreateSuccessfulResolution() {
        List<String> resolvedDependencies = Arrays.asList("plugin-a", "plugin-b", "plugin-c");

        PluginDependencyResolution resolution = PluginDependencyResolution.success(resolvedDependencies);

        assertThat(resolution.isSuccessful()).isTrue();
        assertThat(resolution.getDependencies()).isEqualTo(resolvedDependencies);
        assertThat(resolution.getErrors()).isEmpty();
        assertThat(resolution.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试创建失败的解析结果")
    void testCreateFailureResolution() {
        List<String> errors = Arrays.asList("Error 1", "Error 2");

        PluginDependencyResolution resolution = PluginDependencyResolution.failure(errors);

        assertThat(resolution.isSuccessful()).isFalse();
        assertThat(resolution.getDependencies()).isEmpty();
        assertThat(resolution.getErrors()).isEqualTo(errors);
        assertThat(resolution.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试创建部分成功解析结果")
    void testCreatePartialSuccessResolution() {
        List<String> warnings = Arrays.asList("Warning message");
        List<String> dependencies = Arrays.asList("plugin-a", "plugin-b");

        PluginDependencyResolution resolution = PluginDependencyResolution.partialSuccess(warnings, dependencies);

        assertThat(resolution.isSuccessful()).isTrue();
        assertThat(resolution.isPartialSuccess()).isTrue();
        assertThat(resolution.getDependencies()).isEqualTo(dependencies);
        assertThat(resolution.getWarnings()).isEqualTo(warnings);
        assertThat(resolution.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("测试静态工厂方法")
    void testStaticFactoryMethods() {
        PluginDependencyResolution success1 = PluginDependencyResolution.success(new ArrayList<>());
        assertThat(success1.isSuccessful()).isTrue();

        PluginDependencyResolution success2 = PluginDependencyResolution.success(Arrays.asList("dep1"));
        assertThat(success2.isSuccessful()).isTrue();
        assertThat(success2.getDependencies()).containsExactly("dep1");

        PluginDependencyResolution failure1 = PluginDependencyResolution.failure(new ArrayList<>());
        assertThat(failure1.isSuccessful()).isFalse();

        PluginDependencyResolution failure2 = PluginDependencyResolution.failure(Arrays.asList("error"));
        assertThat(failure2.isSuccessful()).isFalse();
        assertThat(failure2.getErrors()).containsExactly("error");
    }

    @Test
    @DisplayName("测试防御性复制")
    void testDefensiveCopies() {
        List<String> dependencies = new ArrayList<>();
        dependencies.add("dep1");
        dependencies.add("dep2");

        List<String> warnings = new ArrayList<>();
        warnings.add("warning1");

        PluginDependencyResolution resolution = PluginDependencyResolution.partialSuccess(warnings, dependencies);

        // 修改返回的集合不应影响原始对象
        resolution.getDependencies().add("new-dep");
        resolution.getErrors().add("new-error");
        resolution.getWarnings().add("new-warning");

        assertThat(resolution.getDependencies()).hasSize(2);
        assertThat(resolution.getErrors()).isEmpty();
        assertThat(resolution.getWarnings()).hasSize(1);
    }

    @Test
    @DisplayName("测试空参数处理")
    void testEmptyParameterHandling() {
        PluginDependencyResolution resolution1 = PluginDependencyResolution.success(new ArrayList<>());
        assertThat(resolution1.getDependencies()).isEmpty();

        PluginDependencyResolution resolution2 = PluginDependencyResolution.failure(new ArrayList<>());
        assertThat(resolution2.getErrors()).isEmpty();

        PluginDependencyResolution resolution3 = PluginDependencyResolution.partialSuccess(new ArrayList<>(), new ArrayList<>());
        assertThat(resolution3.getWarnings()).isEmpty();
        assertThat(resolution3.getDependencies()).isEmpty();
    }

    @Test
    @DisplayName("测试复杂解析结果")
    void testComplexResolutionResult() {
        List<String> resolved = Arrays.asList(
            "core-plugin",
            "database-plugin",
            "logging-plugin",
            "security-plugin"
        );

        List<String> warnings = Arrays.asList(
            "Database plugin version is deprecated",
            "Consider upgrading to the latest logging plugin"
        );

        PluginDependencyResolution resolution = PluginDependencyResolution.partialSuccess(warnings, resolved);

        assertThat(resolution.isSuccessful()).isTrue();
        assertThat(resolution.isPartialSuccess()).isTrue();
        assertThat(resolution.getDependencies()).hasSize(4);
        assertThat(resolution.getDependencies()).containsExactlyElementsOf(resolved);
        assertThat(resolution.getWarnings()).hasSize(2);
        assertThat(resolution.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("测试带有错误的解析结果")
    void testResolutionWithErrors() {
        List<String> errors = Arrays.asList("Plugin-d not found");

        PluginDependencyResolution resolution = PluginDependencyResolution.failure(errors);

        assertThat(resolution.isSuccessful()).isFalse();
        assertThat(resolution.getDependencies()).isEmpty();
        assertThat(resolution.getErrors()).isEqualTo(errors);
        assertThat(resolution.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("测试大规模依赖解析")
    void testLargeScaleDependencyResolution() {
        List<String> largeDependencyList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeDependencyList.add("plugin-" + i);
        }

        PluginDependencyResolution resolution = PluginDependencyResolution.success(largeDependencyList);

        assertThat(resolution.isSuccessful()).isTrue();
        assertThat(resolution.getDependencies()).hasSize(1000);
        assertThat(resolution.getDependencies().get(0)).isEqualTo("plugin-0");
        assertThat(resolution.getDependencies().get(999)).isEqualTo("plugin-999");
    }

    @Test
    @DisplayName("测试依赖解析结果的复制")
    void testResolutionResultCopying() {
        List<String> originalDeps = Arrays.asList("dep1", "dep2");
        List<String> originalWarnings = Arrays.asList("warning1");

        PluginDependencyResolution original = PluginDependencyResolution.partialSuccess(originalWarnings, originalDeps);

        List<String> copiedDeps = new ArrayList<>(original.getDependencies());
        List<String> copiedWarnings = new ArrayList<>(original.getWarnings());

        copiedDeps.add("dep3");
        copiedWarnings.add("warning2");

        assertThat(original.getDependencies()).hasSize(2);
        assertThat(original.getWarnings()).hasSize(1);
        assertThat(copiedDeps).hasSize(3);
        assertThat(copiedWarnings).hasSize(2);
    }

    @Test
    @DisplayName("测试解析结果的迭代")
    void testResolutionResultIteration() {
        List<String> dependencies = Arrays.asList("plugin-a", "plugin-b", "plugin-c");
        PluginDependencyResolution resolution = PluginDependencyResolution.success(dependencies);

        int count = 0;
        for (String dep : resolution.getDependencies()) {
            assertThat(dep).isNotNull();
            count++;
        }
        assertThat(count).isEqualTo(3);

        count = 0;
        for (String error : resolution.getErrors()) {
            count++;
        }
        assertThat(count).isZero();

        List<String> warnings = Arrays.asList("warning1", "warning2");
        PluginDependencyResolution resolutionWithWarnings = PluginDependencyResolution.partialSuccess(warnings, dependencies);

        count = 0;
        for (String warning : resolutionWithWarnings.getWarnings()) {
            assertThat(warning).isNotNull();
            count++;
        }
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("测试解析结果的序列化安全性")
    void testResolutionResultSerializationSafety() {
        List<String> dependencies = Arrays.asList("dep1", "dep2");
        PluginDependencyResolution resolution = PluginDependencyResolution.success(dependencies);

        String serialized = resolution.toString();
        assertThat(serialized).isNotNull();
        assertThat(serialized).contains("dep1");

        assertThat(resolution.getDependencies()).isEqualTo(dependencies);
        assertThat(resolution.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("测试解析结果的内存效率")
    void testResolutionResultMemoryEfficiency() {
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add("plugin-" + i);
        }

        PluginDependencyResolution resolution = PluginDependencyResolution.success(largeList);

        assertThat(resolution.getDependencies()).isEqualTo(largeList);

        // 修改返回的副本不应影响原始对象
        resolution.getDependencies().clear();
        assertThat(resolution.getDependencyCount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("测试解析结果状态一致性")
    void testResolutionResultStateConsistency() {
        PluginDependencyResolution successResolution = PluginDependencyResolution.success(new ArrayList<>());
        PluginDependencyResolution failureResolution = PluginDependencyResolution.failure(new ArrayList<>());

        assertThat(successResolution.isSuccessful()).isTrue();
        assertThat(successResolution.getErrorCount()).isZero();
        assertThat(successResolution.getWarningCount()).isZero();
        assertThat(successResolution.getDependencies().isEmpty()).isTrue();

        assertThat(failureResolution.isSuccessful()).isFalse();
        assertThat(failureResolution.getErrorCount()).isZero();
        assertThat(failureResolution.getWarningCount()).isZero();
        assertThat(failureResolution.getDependencies().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("测试依赖状态跟踪")
    void testDependencyStatusTracking() {
        PluginDependencyResolution resolution = PluginDependencyResolution.success(Arrays.asList("plugin-a"));

        resolution.addDependencyStatus("plugin-a", PluginDependencyResolution.DependencyStatus.RESOLVED);
        resolution.addDependencyStatus("plugin-b", PluginDependencyResolution.DependencyStatus.NOT_FOUND);

        assertThat(resolution.hasDependencyStatus("plugin-a")).isTrue();
        assertThat(resolution.hasDependencyStatus("plugin-b")).isTrue();
        assertThat(resolution.hasDependencyStatus("plugin-c")).isFalse();
        assertThat(resolution.getDependencyStatus("plugin-a"))
            .isEqualTo(PluginDependencyResolution.DependencyStatus.RESOLVED);
        assertThat(resolution.getDependencyStatus("plugin-b"))
            .isEqualTo(PluginDependencyResolution.DependencyStatus.NOT_FOUND);
        assertThat(resolution.getAllDependencyStatus()).hasSize(2);
    }
}
