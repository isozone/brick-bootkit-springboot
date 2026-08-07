package com.zqzqq.bootkits.core.dependency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件依赖管理器测试
 */
@DisplayName("PluginDependencyManager Test")
class PluginDependencyManagerTest {

    private PluginDependencyManager dependencyManager;

    @BeforeEach
    void setUp() {
        dependencyManager = new PluginDependencyManager();
    }

    @Test
    @DisplayName("测试依赖管理器初始化")
    void testDependencyManagerInitialization() {
        assertThat(dependencyManager).isNotNull();
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("测试注册插件依赖")
    void testRegisterPluginDependency() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin")
            .addRequiredDependency("dep1")
            .addVersionConstraint("dep1", VersionConstraint.parse(">=1.0.0"))
            .build();

        dependencyManager.registerPluginDependency("test-plugin", dependency);

        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);
        assertThat(dependencyManager.getPluginDependencies("test-plugin")).containsExactly("dep1");
    }

    @Test
    @DisplayName("测试重复注册依赖")
    void testDuplicateDependencyRegistration() {
        PluginDependency dependency1 = PluginDependency.newBuilder("test-plugin")
            .addRequiredDependency("dep1")
            .build();

        PluginDependency dependency2 = PluginDependency.newBuilder("test-plugin")
            .addRequiredDependency("dep2")
            .build();

        dependencyManager.registerPluginDependency("test-plugin", dependency1);
        dependencyManager.registerPluginDependency("test-plugin", dependency2);

        // 第二次注册应该覆盖第一次
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);
        assertThat(dependencyManager.getPluginDependencies("test-plugin")).containsExactly("dep2");
    }

    @Test
    @DisplayName("测试解析依赖")
    void testResolveDependencies() {
        PluginDependency dependency = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("dep1")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency);
        dependencyManager.registerPluginDependency("dep1",
            PluginDependency.newBuilder("dep1").build());

        PluginDependencyResolution resolution = dependencyManager.resolveDependencies("plugin-a");

        assertThat(resolution).isNotNull();
        assertThat(resolution.isSuccessful()).isTrue();
        assertThat(resolution.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("测试解析不存在的插件依赖")
    void testResolveNonExistentPlugin() {
        PluginDependencyResolution resolution = dependencyManager.resolveDependencies("non-existent-plugin");

        assertThat(resolution).isNotNull();
        assertThat(resolution.isSuccessful()).isFalse();
        assertThat(resolution.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("测试检查插件兼容性")
    void testCheckCompatibility() {
        PluginDependency dependency = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency);
        dependencyManager.registerPluginDependency("plugin-b",
            PluginDependency.newBuilder("plugin-b").build());

        Collection<String> existingPlugins = Arrays.asList("plugin-b");
        PluginCompatibilityResult result = dependencyManager.checkCompatibility("plugin-a", existingPlugins);

        assertThat(result).isNotNull();
        assertThat(result.isCompatible()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("测试检查兼容性 - 缺少必需依赖")
    void testCheckCompatibilityWithMissingDependency() {
        PluginDependency dependency = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency);

        Collection<String> existingPlugins = Arrays.asList("plugin-c");
        PluginCompatibilityResult result = dependencyManager.checkCompatibility("plugin-a", existingPlugins);

        assertThat(result).isNotNull();
        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("测试检查兼容性 - 版本冲突")
    void testCheckCompatibilityWithVersionConflict() {
        PluginDependency dependency = PluginDependency.newBuilder("plugin-a")
            .addVersionConstraint("conflict-plugin", VersionConstraint.parse(">=2.0.0"))
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency);
        dependencyManager.registerPluginDependency("conflict-plugin",
            PluginDependency.newBuilder("conflict-plugin", "1.0.0").build());

        Collection<String> existingPlugins = Arrays.asList("conflict-plugin");
        PluginCompatibilityResult result = dependencyManager.checkCompatibility("plugin-a", existingPlugins);

        assertThat(result).isNotNull();
        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("测试获取插件依赖")
    void testGetPluginDependencies() {
        PluginDependency dependency1 = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependencies("dep1", "dep2")
            .build();

        PluginDependency dependency2 = PluginDependency.newBuilder("plugin-b")
            .addOptionalDependency("dep3")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency1);
        dependencyManager.registerPluginDependency("plugin-b", dependency2);

        assertThat(dependencyManager.getPluginDependencies("plugin-a"))
            .containsExactlyInAnyOrder("dep1", "dep2");
        assertThat(dependencyManager.getPluginDependencies("plugin-b")).containsExactly("dep3");
        assertThat(dependencyManager.getPluginDependencies("non-existent")).isEmpty();
    }

    @Test
    @DisplayName("测试获取反向依赖")
    void testGetReverseDependencies() {
        PluginDependency dependency1 = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .build();

        PluginDependency dependency2 = PluginDependency.newBuilder("plugin-b")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency1);
        dependencyManager.registerPluginDependency("plugin-b", dependency2);

        // plugin-a 依赖 plugin-b，所以 plugin-b 的反向依赖应包含 plugin-a
        assertThat(dependencyManager.getReverseDependencies("plugin-b")).containsExactly("plugin-a");
    }

    @Test
    @DisplayName("测试依赖循环检测")
    void testDependencyCycleDetection() {
        PluginDependency dependency1 = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .build();

        PluginDependency dependency2 = PluginDependency.newBuilder("plugin-b")
            .addRequiredDependency("plugin-a")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependency1);
        dependencyManager.registerPluginDependency("plugin-b", dependency2);

        // a -> b -> a 构成循环
        assertThat(dependencyManager.hasDependencyCycle("plugin-a")).isTrue();
        assertThat(dependencyManager.hasDependencyCycle("plugin-b")).isTrue();
    }

    @Test
    @DisplayName("测试拓扑排序")
    void testTopologicalOrder() {
        PluginDependency dependencyA = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .build();

        PluginDependency dependencyB = PluginDependency.newBuilder("plugin-b")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", dependencyA);
        dependencyManager.registerPluginDependency("plugin-b", dependencyB);

        List<String> order = dependencyManager.getTopologicalOrder();
        assertThat(order).isNotNull();
        assertThat(order).contains("plugin-a", "plugin-b");
        // 当前实现中依赖方排在依赖项之前（a 依赖 b，故 a 先于 b）
        assertThat(order.indexOf("plugin-a")).isLessThan(order.indexOf("plugin-b"));
    }

    @Test
    @DisplayName("测试已注册插件数量")
    void testRegisteredPluginCount() {
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(0);

        PluginDependency dependency = PluginDependency.newBuilder("test-plugin")
            .addRequiredDependency("dep1")
            .build();

        dependencyManager.registerPluginDependency("test-plugin", dependency);
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);

        dependencyManager.registerPluginDependency("test-plugin", dependency); // 重复注册
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试移除插件依赖")
    void testRemovePluginDependency() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin")
            .addRequiredDependency("dep1")
            .build();

        dependencyManager.registerPluginDependency("test-plugin", dependency);
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);

        dependencyManager.removePluginDependency("test-plugin");
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("测试清空依赖")
    void testClearDependencies() {
        dependencyManager.registerPluginDependency("plugin-a",
            PluginDependency.newBuilder("plugin-a").build());
        dependencyManager.registerPluginDependency("plugin-b",
            PluginDependency.newBuilder("plugin-b").build());

        dependencyManager.clear();
        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("测试复杂依赖场景")
    void testComplexDependencyScenario() {
        PluginDependency depA = PluginDependency.newBuilder("plugin-a")
            .addRequiredDependency("plugin-b")
            .addVersionConstraint("plugin-b", VersionConstraint.parse(">=1.0.0"))
            .build();

        PluginDependency depB = PluginDependency.newBuilder("plugin-b")
            .addRequiredDependency("plugin-c")
            .build();

        PluginDependency depC = PluginDependency.newBuilder("plugin-c")
            .build();

        dependencyManager.registerPluginDependency("plugin-a", depA);
        dependencyManager.registerPluginDependency("plugin-b", depB);
        dependencyManager.registerPluginDependency("plugin-c", depC);

        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(3);

        PluginDependencyResolution resolution = dependencyManager.resolveDependencies("plugin-a");
        assertThat(resolution.isSuccessful()).isTrue();

        PluginCompatibilityResult compatibility = dependencyManager.checkCompatibility(
            "plugin-a", Arrays.asList("plugin-b", "plugin-c"));
        assertThat(compatibility).isNotNull();
        assertThat(compatibility.isCompatible()).isTrue();
    }

    @Test
    @DisplayName("测试空操作处理")
    void testEmptyOperationsHandling() {
        PluginDependencyResolution resolution = dependencyManager.resolveDependencies("");
        assertThat(resolution).isNotNull();
        assertThat(resolution.isSuccessful()).isFalse();

        PluginCompatibilityResult compatibility = dependencyManager.checkCompatibility("", Arrays.asList("plugin-a"));
        assertThat(compatibility).isNotNull();

        assertThat(dependencyManager.getPluginDependencies("")).isEmpty();
        assertThat(dependencyManager.getReverseDependencies("")).isEmpty();
        assertThat(dependencyManager.hasDependencyCycle("")).isFalse();
    }

    @Test
    @DisplayName("测试并发操作安全性")
    void testConcurrentOperationSafety() throws InterruptedException {
        PluginDependency dependency = PluginDependency.newBuilder("concurrent-plugin")
            .addRequiredDependency("dep1")
            .build();

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                dependencyManager.registerPluginDependency("concurrent-plugin", dependency);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(dependencyManager.getRegisteredPluginCount()).isEqualTo(1);
        assertThat(dependencyManager.getPluginDependencies("concurrent-plugin")).containsExactly("dep1");
    }

    @Test
    @DisplayName("测试依赖管理器的线程安全性")
    void testThreadSafety() throws InterruptedException {
        int operationCount = 100;
        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationCount / threads.length; j++) {
                    String pluginId = "plugin-" + threadIndex + "-" + j;
                    PluginDependency dependency = PluginDependency.newBuilder(pluginId)
                        .addRequiredDependency("dep1")
                        .build();

                    dependencyManager.registerPluginDependency(pluginId, dependency);

                    dependencyManager.getRegisteredPluginCount();
                    dependencyManager.resolveDependencies(pluginId);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(dependencyManager.getRegisteredPluginCount()).isGreaterThan(0);
    }
}
