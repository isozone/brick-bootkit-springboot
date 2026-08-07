package com.zqzqq.bootkits.core.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 插件依赖类型测试
 */
@DisplayName("PluginDependencyType Test")
class PluginDependencyTypeTest {

    @Test
    @DisplayName("测试所有依赖类型枚举值")
    void testAllDependencyTypeValues() {
        assertThat(PluginDependencyType.values()).containsExactly(
            PluginDependencyType.INTERNAL,
            PluginDependencyType.EXTERNAL,
            PluginDependencyType.SYSTEM,
            PluginDependencyType.RUNTIME,
            PluginDependencyType.COMPILE,
            PluginDependencyType.OPTIONAL,
            PluginDependencyType.TEST,
            PluginDependencyType.PROVIDED
        );
    }

    @Test
    @DisplayName("测试依赖类型的基本属性")
    void testDependencyTypeBasicProperties() {
        // 测试 INTERNAL 类型
        assertThat(PluginDependencyType.INTERNAL.getCode()).isEqualTo("internal");
        assertThat(PluginDependencyType.INTERNAL.getDescription()).isEqualTo("内部依赖");
        assertThat(PluginDependencyType.INTERNAL.isRequired()).isTrue();
        assertThat(PluginDependencyType.INTERNAL.isOptional()).isFalse();
        assertThat(PluginDependencyType.INTERNAL.isRuntime()).isTrue();

        // 测试 EXTERNAL 类型
        assertThat(PluginDependencyType.EXTERNAL.getCode()).isEqualTo("external");
        assertThat(PluginDependencyType.EXTERNAL.getDescription()).isEqualTo("外部依赖");
        assertThat(PluginDependencyType.EXTERNAL.isRequired()).isTrue();
        assertThat(PluginDependencyType.EXTERNAL.isOptional()).isFalse();

        // 测试 OPTIONAL 类型
        assertThat(PluginDependencyType.OPTIONAL.getCode()).isEqualTo("optional");
        assertThat(PluginDependencyType.OPTIONAL.getDescription()).isEqualTo("可选依赖");
        assertThat(PluginDependencyType.OPTIONAL.isRequired()).isFalse();
        assertThat(PluginDependencyType.OPTIONAL.isOptional()).isTrue();

        // 测试 TEST 类型
        assertThat(PluginDependencyType.TEST.getCode()).isEqualTo("test");
        assertThat(PluginDependencyType.TEST.getDescription()).isEqualTo("测试依赖");
        assertThat(PluginDependencyType.TEST.isRequired()).isFalse();
        assertThat(PluginDependencyType.TEST.isOptional()).isTrue();

        // 测试 COMPILE / PROVIDED 构建时依赖
        assertThat(PluginDependencyType.COMPILE.isBuildTime()).isTrue();
        assertThat(PluginDependencyType.PROVIDED.isBuildTime()).isTrue();
        assertThat(PluginDependencyType.RUNTIME.isBuildTime()).isFalse();
    }

    @Test
    @DisplayName("测试依赖类型的枚举序")
    void testDependencyTypeEnumOrder() {
        PluginDependencyType[] types = PluginDependencyType.values();

        assertThat(types[0]).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(types[1]).isEqualTo(PluginDependencyType.EXTERNAL);
        assertThat(types[2]).isEqualTo(PluginDependencyType.SYSTEM);
        assertThat(types[3]).isEqualTo(PluginDependencyType.RUNTIME);
        assertThat(types[4]).isEqualTo(PluginDependencyType.COMPILE);
        assertThat(types[5]).isEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(types[6]).isEqualTo(PluginDependencyType.TEST);
        assertThat(types[7]).isEqualTo(PluginDependencyType.PROVIDED);
    }

    @Test
    @DisplayName("测试依赖类型的字符串表示")
    void testDependencyTypeStringRepresentation() {
        assertThat(PluginDependencyType.INTERNAL.name()).isEqualTo("INTERNAL");
        assertThat(PluginDependencyType.EXTERNAL.name()).isEqualTo("EXTERNAL");
        assertThat(PluginDependencyType.OPTIONAL.name()).isEqualTo("OPTIONAL");
        assertThat(PluginDependencyType.PROVIDED.name()).isEqualTo("PROVIDED");
        assertThat(PluginDependencyType.TEST.name()).isEqualTo("TEST");
        assertThat(PluginDependencyType.RUNTIME.name()).isEqualTo("RUNTIME");
        assertThat(PluginDependencyType.SYSTEM.name()).isEqualTo("SYSTEM");
        assertThat(PluginDependencyType.COMPILE.name()).isEqualTo("COMPILE");
    }

    @Test
    @DisplayName("测试依赖类型的toString方法")
    void testDependencyTypeToString() {
        assertThat(PluginDependencyType.INTERNAL.toString()).isEqualTo("INTERNAL(internal - 内部依赖)");
        assertThat(PluginDependencyType.OPTIONAL.toString()).isEqualTo("OPTIONAL(optional - 可选依赖)");
        assertThat(PluginDependencyType.TEST.toString()).isEqualTo("TEST(test - 测试依赖)");
    }

    @Test
    @DisplayName("测试依赖类型的值方法")
    void testDependencyTypeValueMethod() {
        assertThat(PluginDependencyType.INTERNAL.getCode()).isEqualTo("internal");
        assertThat(PluginDependencyType.EXTERNAL.getCode()).isEqualTo("external");
        assertThat(PluginDependencyType.OPTIONAL.getCode()).isEqualTo("optional");
        assertThat(PluginDependencyType.PROVIDED.getCode()).isEqualTo("provided");
        assertThat(PluginDependencyType.TEST.getCode()).isEqualTo("test");
        assertThat(PluginDependencyType.RUNTIME.getCode()).isEqualTo("runtime");
        assertThat(PluginDependencyType.SYSTEM.getCode()).isEqualTo("system");
        assertThat(PluginDependencyType.COMPILE.getCode()).isEqualTo("compile");
    }

    @Test
    @DisplayName("测试从代码创建依赖类型")
    void testCreateDependencyTypeFromCode() {
        assertThat(PluginDependencyType.fromCode("internal")).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(PluginDependencyType.fromCode("INTERNAL")).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(PluginDependencyType.fromCode("Internal")).isEqualTo(PluginDependencyType.INTERNAL);

        assertThat(PluginDependencyType.fromCode("optional")).isEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(PluginDependencyType.fromCode("OPTIONAL")).isEqualTo(PluginDependencyType.OPTIONAL);

        assertThat(PluginDependencyType.fromCode("provided")).isEqualTo(PluginDependencyType.PROVIDED);
        assertThat(PluginDependencyType.fromCode("test")).isEqualTo(PluginDependencyType.TEST);
        assertThat(PluginDependencyType.fromCode("runtime")).isEqualTo(PluginDependencyType.RUNTIME);
        assertThat(PluginDependencyType.fromCode("system")).isEqualTo(PluginDependencyType.SYSTEM);
        assertThat(PluginDependencyType.fromCode("compile")).isEqualTo(PluginDependencyType.COMPILE);
        assertThat(PluginDependencyType.fromCode("external")).isEqualTo(PluginDependencyType.EXTERNAL);
    }

    @Test
    @DisplayName("测试无效代码的依赖类型创建")
    void testCreateDependencyTypeWithInvalidCode() {
        assertThatThrownBy(() -> PluginDependencyType.fromCode("INVALID"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PluginDependencyType.fromCode("unknown"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PluginDependencyType.fromCode(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PluginDependencyType.fromCode("   "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PluginDependencyType.fromCode(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试依赖类型的必需性判断")
    void testDependencyTypeRequirementJudgement() {
        assertThat(PluginDependencyType.INTERNAL.isRequired()).isTrue();
        assertThat(PluginDependencyType.EXTERNAL.isRequired()).isTrue();
        assertThat(PluginDependencyType.SYSTEM.isRequired()).isTrue();
        assertThat(PluginDependencyType.RUNTIME.isRequired()).isTrue();
        assertThat(PluginDependencyType.COMPILE.isRequired()).isTrue();
        assertThat(PluginDependencyType.PROVIDED.isRequired()).isTrue();

        assertThat(PluginDependencyType.OPTIONAL.isRequired()).isFalse();
        assertThat(PluginDependencyType.TEST.isRequired()).isFalse();
    }

    @Test
    @DisplayName("测试依赖类型的可选性判断")
    void testDependencyTypeOptionalJudgement() {
        assertThat(PluginDependencyType.OPTIONAL.isOptional()).isTrue();
        assertThat(PluginDependencyType.TEST.isOptional()).isTrue();

        assertThat(PluginDependencyType.INTERNAL.isOptional()).isFalse();
        assertThat(PluginDependencyType.EXTERNAL.isOptional()).isFalse();
        assertThat(PluginDependencyType.SYSTEM.isOptional()).isFalse();
        assertThat(PluginDependencyType.RUNTIME.isOptional()).isFalse();
        assertThat(PluginDependencyType.COMPILE.isOptional()).isFalse();
        assertThat(PluginDependencyType.PROVIDED.isOptional()).isFalse();
    }

    @Test
    @DisplayName("测试依赖类型的构建时判断")
    void testDependencyTypeBuildTimeJudgement() {
        assertThat(PluginDependencyType.COMPILE.isBuildTime()).isTrue();
        assertThat(PluginDependencyType.PROVIDED.isBuildTime()).isTrue();

        assertThat(PluginDependencyType.INTERNAL.isBuildTime()).isFalse();
        assertThat(PluginDependencyType.EXTERNAL.isBuildTime()).isFalse();
        assertThat(PluginDependencyType.SYSTEM.isBuildTime()).isFalse();
        assertThat(PluginDependencyType.RUNTIME.isBuildTime()).isFalse();
        assertThat(PluginDependencyType.OPTIONAL.isBuildTime()).isFalse();
        assertThat(PluginDependencyType.TEST.isBuildTime()).isFalse();
    }

    @Test
    @DisplayName("测试依赖类型的运行时判断")
    void testDependencyTypeRuntimeJudgement() {
        assertThat(PluginDependencyType.RUNTIME.isRuntime()).isTrue();
        assertThat(PluginDependencyType.INTERNAL.isRuntime()).isTrue();
        assertThat(PluginDependencyType.EXTERNAL.isRuntime()).isTrue();
        assertThat(PluginDependencyType.SYSTEM.isRuntime()).isTrue();

        assertThat(PluginDependencyType.COMPILE.isRuntime()).isFalse();
        assertThat(PluginDependencyType.OPTIONAL.isRuntime()).isFalse();
        assertThat(PluginDependencyType.TEST.isRuntime()).isFalse();
        assertThat(PluginDependencyType.PROVIDED.isRuntime()).isFalse();
    }

    @Test
    @DisplayName("测试依赖类型的相等性")
    void testDependencyTypeEquality() {
        assertThat(PluginDependencyType.INTERNAL).isEqualTo(PluginDependencyType.INTERNAL);
        assertThat(PluginDependencyType.OPTIONAL).isEqualTo(PluginDependencyType.OPTIONAL);

        assertThat(PluginDependencyType.INTERNAL).isNotEqualTo(PluginDependencyType.OPTIONAL);
        assertThat(PluginDependencyType.PROVIDED).isNotEqualTo(PluginDependencyType.TEST);
    }

    @Test
    @DisplayName("测试依赖类型在集合中的行为")
    void testDependencyTypeInCollections() {
        java.util.Set<PluginDependencyType> typeSet = new java.util.HashSet<>();
        typeSet.add(PluginDependencyType.INTERNAL);
        typeSet.add(PluginDependencyType.OPTIONAL);
        typeSet.add(PluginDependencyType.INTERNAL); // 重复添加

        assertThat(typeSet).hasSize(2);
        assertThat(typeSet).containsExactlyInAnyOrder(PluginDependencyType.INTERNAL, PluginDependencyType.OPTIONAL);

        java.util.List<PluginDependencyType> typeList = new java.util.ArrayList<>();
        typeList.add(PluginDependencyType.PROVIDED);
        typeList.add(PluginDependencyType.TEST);
        typeList.add(PluginDependencyType.PROVIDED); // 重复添加

        assertThat(typeList).hasSize(3);
        assertThat(typeList.get(0)).isEqualTo(PluginDependencyType.PROVIDED);
        assertThat(typeList.get(1)).isEqualTo(PluginDependencyType.TEST);
        assertThat(typeList.get(2)).isEqualTo(PluginDependencyType.PROVIDED);
    }

    @Test
    @DisplayName("测试依赖类型的不可变性")
    void testDependencyTypeImmutability() {
        PluginDependencyType type = PluginDependencyType.INTERNAL;

        assertThat(type.getCode()).isNotNull();
        assertThat(type.getDescription()).isNotNull();
        assertThat(type.name()).isNotNull();
    }
}
