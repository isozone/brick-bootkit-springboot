package com.zqzqq.bootkits.core.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 插件依赖测试
 */
@DisplayName("PluginDependency Test")
class PluginDependencyTest {

    @Test
    @DisplayName("测试创建基本插件依赖")
    void testBasicPluginDependency() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .build();

        assertThat(dependency.getPluginId()).isEqualTo("test-plugin");
        assertThat(dependency.getVersion()).isEqualTo("1.0.0");
        assertThat(dependency.getDependencyType()).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(dependency.getRequiredDependencies()).isEmpty();
        assertThat(dependency.getOptionalDependencies()).isEmpty();
        assertThat(dependency.getVersionConstraints()).isEmpty();
        assertThat(dependency.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("测试构建器参数验证")
    void testBuilderParameterValidation() {
        assertThatThrownBy(() -> PluginDependency.newBuilder(null, "1.0.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件ID不能为空");

        assertThatThrownBy(() -> PluginDependency.newBuilder("", "1.0.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件ID不能为空");

        assertThatThrownBy(() -> PluginDependency.newBuilder("plugin", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件版本不能为空");
    }

    @Test
    @DisplayName("测试设置依赖类型")
    void testSetDependencyType() {
        PluginDependency compileDep = PluginDependency.newBuilder("plugin-a", "1.0.0")
            .setDependencyType(PluginDependencyType.COMPILE)
            .build();

        PluginDependency optionalDep = PluginDependency.newBuilder("plugin-b", "1.0.0")
            .setDependencyType(PluginDependencyType.OPTIONAL)
            .build();

        PluginDependency providedDep = PluginDependency.newBuilder("plugin-c", "1.0.0")
            .setDependencyType(PluginDependencyType.PROVIDED)
            .build();

        assertThat(compileDep.getDependencyType()).isEqualTo(PluginDependencyType.COMPILE);
        assertThat(optionalDep.getDependencyType()).isEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(providedDep.getDependencyType()).isEqualTo(PluginDependencyType.PROVIDED);
    }

    @Test
    @DisplayName("测试添加版本约束")
    void testAddVersionConstraints() {
        VersionConstraint constraint1 = VersionConstraint.parse(">=1.0.0");
        VersionConstraint constraint2 = VersionConstraint.parse("<2.0.0");

        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addVersionConstraint("constraint1", constraint1)
            .addVersionConstraint("constraint2", constraint2)
            .build();

        Map<String, VersionConstraint> constraints = dependency.getVersionConstraints();
        assertThat(constraints).hasSize(2);
        assertThat(constraints).containsKey("constraint1");
        assertThat(constraints).containsKey("constraint2");
        assertThat(constraints.get("constraint1")).isEqualTo(constraint1);
        assertThat(constraints.get("constraint2")).isEqualTo(constraint2);
    }

    @Test
    @DisplayName("测试添加元数据")
    void testAddMetadata() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addMetadata("author", "Test Author")
            .addMetadata("version", "1.0.0")
            .addMetadata("category", "utility")
            .build();

        Map<String, Object> metadata = dependency.getMetadata();
        assertThat(metadata).hasSize(3);
        assertThat(metadata.get("author")).isEqualTo("Test Author");
        assertThat(metadata.get("version")).isEqualTo("1.0.0");
        assertThat(metadata.get("category")).isEqualTo("utility");
        assertThat(dependency.getMetadata("author")).isEqualTo("Test Author");
    }

    @Test
    @DisplayName("测试添加必需和可选依赖")
    void testAddRequiredAndOptionalDependencies() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addRequiredDependencies("plugin-a", "plugin-b")
            .addOptionalDependency("plugin-c")
            .build();

        assertThat(dependency.getRequiredDependencies()).containsExactlyInAnyOrder("plugin-a", "plugin-b");
        assertThat(dependency.getOptionalDependencies()).containsExactly("plugin-c");
        assertThat(dependency.hasDependency("plugin-a")).isTrue();
        assertThat(dependency.hasDependency("plugin-c")).isTrue();
        assertThat(dependency.hasDependency("plugin-x")).isFalse();
        assertThat(dependency.getDependencyCount()).isEqualTo(3);
        assertThat(dependency.getRequiredDependencyCount()).isEqualTo(2);
        assertThat(dependency.getOptionalDependencyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试添加冲突依赖")
    void testAddConflicts() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addConflicts("plugin-x", "plugin-y")
            .build();

        assertThat(dependency.getConflicts()).containsExactlyInAnyOrder("plugin-x", "plugin-y");
        assertThat(dependency.hasConflict("plugin-x")).isTrue();
        assertThat(dependency.hasConflict("plugin-z")).isFalse();
    }

    @Test
    @DisplayName("测试设置描述")
    void testSetDescription() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .setDescription("This plugin provides essential database functionality")
            .build();

        assertThat(dependency.getDescription()).isEqualTo("This plugin provides essential database functionality");
    }

    @Test
    @DisplayName("测试设置弃用标记")
    void testSetDeprecated() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .setDeprecated("Use plugin-new instead")
            .build();

        assertThat(dependency.isDeprecated()).isTrue();
        assertThat(dependency.getDeprecationMessage()).isEqualTo("Use plugin-new instead");
    }

    @Test
    @DisplayName("测试toString方法")
    void testToString() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .setDependencyType(PluginDependencyType.COMPILE)
            .addMetadata("author", "Test Author")
            .build();

        String str = dependency.toString();
        assertThat(str).contains("test-plugin");
        assertThat(str).contains("1.0.0");
    }

    @Test
    @DisplayName("测试构建器方法链式调用")
    void testBuilderMethodChaining() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .setDependencyType(PluginDependencyType.OPTIONAL)
            .addRequiredDependency("plugin-a")
            .addOptionalDependency("plugin-b")
            .addMetadata("key1", "value1")
            .addMetadata("key2", "value2")
            .setDescription("Test explanation")
            .build();

        assertThat(dependency).isNotNull();
        assertThat(dependency.getPluginId()).isEqualTo("test-plugin");
        assertThat(dependency.getDependencyType()).isEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(dependency.getRequiredDependencies()).containsExactly("plugin-a");
        assertThat(dependency.getOptionalDependencies()).containsExactly("plugin-b");
        assertThat(dependency.getMetadata()).hasSize(2);
        assertThat(dependency.getDescription()).isEqualTo("Test explanation");
    }

    @Test
    @DisplayName("测试防御性复制")
    void testDefensiveCopies() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addMetadata("key", "value")
            .addVersionConstraint("constraint", VersionConstraint.parse(">=1.0.0"))
            .build();

        // 修改返回的集合不应影响原始对象
        dependency.getMetadata().put("new-key", "value");
        dependency.getVersionConstraints().put("new-constraint", VersionConstraint.parse(">=2.0.0"));

        assertThat(dependency.getMetadata()).hasSize(1);
        assertThat(dependency.getVersionConstraints()).hasSize(1);
    }

    @Test
    @DisplayName("测试复杂依赖配置")
    void testComplexDependencyConfiguration() {
        Map<String, VersionConstraint> constraints = new HashMap<>();
        constraints.put("core", VersionConstraint.parse(">=2.0.0"));
        constraints.put("database", VersionConstraint.parse(">=1.5.0 <3.0.0"));

        PluginDependency dependency = PluginDependency.newBuilder("complex-plugin", "1.0.0")
            .setDependencyType(PluginDependencyType.COMPILE)
            .addRequiredDependency("core")
            .addRequiredDependency("database")
            .addOptionalDependency("cache")
            .addVersionConstraint("core", constraints.get("core"))
            .addVersionConstraint("database", constraints.get("database"))
            .addMetadata("author", "Test Team")
            .addMetadata("license", "MIT")
            .build();

        assertThat(dependency.getPluginId()).isEqualTo("complex-plugin");
        assertThat(dependency.getDependencyType()).isEqualTo(PluginDependencyType.COMPILE);
        assertThat(dependency.getRequiredDependencies()).containsExactlyInAnyOrder("core", "database");
        assertThat(dependency.getOptionalDependencies()).containsExactly("cache");
        assertThat(dependency.getVersionConstraints()).isEqualTo(constraints);
        assertThat(dependency.getMetadata()).hasSize(2);
    }

    @Test
    @DisplayName("测试空操作的处理")
    void testEmptyOperationsHandling() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0")
            .addMetadata(null, "value")
            .addMetadata("key", null)
            .addVersionConstraint(null, null)
            .addRequiredDependency(null)
            .addOptionalDependency("")
            .setDescription(null)
            .build();

        assertThat(dependency).isNotNull();
        assertThat(dependency.getMetadata()).hasSize(1); // 只有 "key"->null 被保留
        assertThat(dependency.getMetadata().get("key")).isNull();
        assertThat(dependency.getVersionConstraints()).isEmpty();
        assertThat(dependency.getRequiredDependencies()).isEmpty();
        assertThat(dependency.getOptionalDependencies()).isEmpty();
        assertThat(dependency.getDescription()).isNull();
    }

    @Test
    @DisplayName("测试默认值的正确性")
    void testDefaultValuesCorrectness() {
        PluginDependency dependency = PluginDependency.newBuilder("test-plugin", "1.0.0").build();

        assertThat(dependency.getDependencyType()).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(dependency.isDeprecated()).isFalse();
        assertThat(dependency.getDeprecationMessage()).isNull();
        assertThat(dependency.getVersionConstraints()).isEmpty();
        assertThat(dependency.getMetadata()).isEmpty();
        assertThat(dependency.getDescription()).isNull();
        assertThat(dependency.getRequiredDependencies()).isEmpty();
        assertThat(dependency.getOptionalDependencies()).isEmpty();
        assertThat(dependency.getConflicts()).isEmpty();
    }

    @Test
    @DisplayName("测试构建器状态独立性")
    void testBuilderStateIndependence() {
        PluginDependency.Builder builder1 = PluginDependency.newBuilder("plugin-1", "1.0.0")
            .setDependencyType(PluginDependencyType.OPTIONAL)
            .addMetadata("key1", "value1");

        PluginDependency.Builder builder2 = PluginDependency.newBuilder("plugin-2", "1.0.0")
            .setDependencyType(PluginDependencyType.COMPILE)
            .addMetadata("key2", "value2");

        PluginDependency dep1 = builder1.build();
        PluginDependency dep2 = builder2.build();

        assertThat(dep1.getPluginId()).isEqualTo("plugin-1");
        assertThat(dep1.getDependencyType()).isEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(dep1.getMetadata().get("key1")).isEqualTo("value1");

        assertThat(dep2.getPluginId()).isEqualTo("plugin-2");
        assertThat(dep2.getDependencyType()).isEqualTo(PluginDependencyType.COMPILE);
        assertThat(dep2.getMetadata().get("key2")).isEqualTo("value2");

        assertThat(dep1.getMetadata()).doesNotContainKey("key2");
        assertThat(dep2.getMetadata()).doesNotContainKey("key1");
    }
}
