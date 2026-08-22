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


package com.zqzqq.bootkits.core.version;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 版本号工具类测试
 */
@DisplayName("VersionUtils Test")
class VersionUtilsTest {

    @Test
    @DisplayName("测试解析语义化版本号")
    void testParseSemanticVersion() {
        // 测试标准语义化版本
        VersionUtils.VersionInfo v1 = VersionUtils.parseVersion("1.2.3");
        assertThat(v1.getMajor()).isEqualTo(1);
        assertThat(v1.getMinor()).isEqualTo(2);
        assertThat(v1.getPatch()).isEqualTo(3);
        assertThat(v1.getQualifier()).isNull();

        // 测试带预发布版本的语义化版本
        VersionUtils.VersionInfo v2 = VersionUtils.parseVersion("1.2.3-alpha.1");
        assertThat(v2.getMajor()).isEqualTo(1);
        assertThat(v2.getMinor()).isEqualTo(2);
        assertThat(v2.getPatch()).isEqualTo(3);
        assertThat(v2.getQualifier()).isEqualTo("alpha.1");

        // 测试带构建元数据的语义化版本
        VersionUtils.VersionInfo v3 = VersionUtils.parseVersion("1.2.3+build.1");
        assertThat(v3.getMajor()).isEqualTo(1);
        assertThat(v3.getMinor()).isEqualTo(2);
        assertThat(v3.getPatch()).isEqualTo(3);

        // 测试完整格式
        VersionUtils.VersionInfo v4 = VersionUtils.parseVersion("2.1.0-beta.2+build.456");
        assertThat(v4.getMajor()).isEqualTo(2);
        assertThat(v4.getMinor()).isEqualTo(1);
        assertThat(v4.getPatch()).isEqualTo(0);
        assertThat(v4.getQualifier()).isEqualTo("beta.2");
    }

    @Test
    @DisplayName("测试解析传统版本号")
    void testParseTraditionalVersion() {
        // 测试简单版本
        VersionUtils.VersionInfo v1 = VersionUtils.parseVersion("1.0");
        assertThat(v1.getMajor()).isEqualTo(1);
        assertThat(v1.getMinor()).isEqualTo(0);
        assertThat(v1.getPatch()).isEqualTo(0);
        assertThat(v1.getQualifier()).isNull();

        // 测试单数字版本
        VersionUtils.VersionInfo v2 = VersionUtils.parseVersion("1");
        assertThat(v2.getMajor()).isEqualTo(1);
        assertThat(v2.getMinor()).isEqualTo(0);
        assertThat(v2.getPatch()).isEqualTo(0);

        // 测试带限定符的版本
        VersionUtils.VersionInfo v3 = VersionUtils.parseVersion("1.2.3-SNAPSHOT");
        assertThat(v3.getMajor()).isEqualTo(1);
        assertThat(v3.getMinor()).isEqualTo(2);
        assertThat(v3.getPatch()).isEqualTo(3);
        assertThat(v3.getQualifier()).isEqualTo("SNAPSHOT");

        // 测试复杂限定符
        VersionUtils.VersionInfo v4 = VersionUtils.parseVersion("2.1.0-rc.1+build.123");
        assertThat(v4.getMajor()).isEqualTo(2);
        assertThat(v4.getMinor()).isEqualTo(1);
        assertThat(v4.getPatch()).isEqualTo(0);
        assertThat(v4.getQualifier()).isEqualTo("rc.1");
    }

    @Test
    @DisplayName("测试解析无效版本号")
    void testParseInvalidVersion() {
        assertThatThrownBy(() -> VersionUtils.parseVersion(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("版本号不能为空");

        assertThatThrownBy(() -> VersionUtils.parseVersion(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("版本号不能为空");

        assertThatThrownBy(() -> VersionUtils.parseVersion("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("版本号不能为空");

        assertThatThrownBy(() -> VersionUtils.parseVersion("invalid.version"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("无法解析版本号");

        assertThatThrownBy(() -> VersionUtils.parseVersion("v1.2.3"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试版本比较")
    void testVersionComparison() {
        assertThat(VersionUtils.compareVersions("1.0.0", "1.0.0")).isEqualTo(0);
        assertThat(VersionUtils.compareVersions("1.0.0", "1.0.1")).isEqualTo(-1);
        assertThat(VersionUtils.compareVersions("1.0.1", "1.0.0")).isEqualTo(1);
        assertThat(VersionUtils.compareVersions("1.0.0", "2.0.0")).isEqualTo(-1);
        assertThat(VersionUtils.compareVersions("2.0.0", "1.0.0")).isEqualTo(1);
        
        // 测试复杂版本
        assertThat(VersionUtils.compareVersions("1.2.3-alpha", "1.2.3")).isEqualTo(-1);
        assertThat(VersionUtils.compareVersions("1.2.3", "1.2.3-alpha")).isEqualTo(1);
        assertThat(VersionUtils.compareVersions("1.2.3-beta.1", "1.2.3-beta.2")).isEqualTo(-1);
    }

    @Test
    @DisplayName("测试版本兼容性检查")
    void testVersionCompatibility() {
        // 主版本相同，且版本1次版本不低于版本2，视为兼容
        assertThat(VersionUtils.isCompatible("1.2.0", "1.1.0")).isTrue();
        assertThat(VersionUtils.isCompatible("1.2.0", "1.2.0")).isTrue();
        assertThat(VersionUtils.isCompatible("1.1.0", "1.0.0")).isTrue();

        // 主版本不兼容
        assertThat(VersionUtils.isCompatible("2.0.0", "1.0.0")).isFalse();
        assertThat(VersionUtils.isCompatible("1.0.0", "2.0.0")).isFalse();

        // 版本1次版本低于版本2，不兼容
        assertThat(VersionUtils.isCompatible("1.2.0", "1.3.0")).isFalse();

        // 测试复杂版本
        assertThat(VersionUtils.isCompatible("2.1.3-alpha", "2.0.0")).isTrue();
        assertThat(VersionUtils.isCompatible("1.2.3", "1.0.0-alpha")).isTrue();
    }

    @Test
    @DisplayName("测试版本需求满足检查")
    void testVersionSatisfaction() {
        // 基本满足测试
        assertThat(VersionUtils.satisfies("1.2.3", "1.0.0", false)).isTrue();
        assertThat(VersionUtils.satisfies("1.0.0", "1.0.0", false)).isTrue();
        assertThat(VersionUtils.satisfies("0.9.0", "1.0.0", false)).isFalse();

        // 主版本检查
        assertThat(VersionUtils.satisfies("2.0.0", "1.0.0", false)).isTrue();
        assertThat(VersionUtils.satisfies("0.5.0", "1.0.0", false)).isFalse();

        // 次版本检查
        assertThat(VersionUtils.satisfies("1.2.0", "1.0.0", false)).isTrue();
        assertThat(VersionUtils.satisfies("1.0.5", "1.1.0", false)).isFalse();

        // 预发布版本检查
        assertThat(VersionUtils.satisfies("1.2.3-alpha", "1.2.3", false)).isTrue();
        assertThat(VersionUtils.satisfies("1.2.3-alpha", "1.2.3", true)).isTrue();
    }

    @Test
    @DisplayName("测试版本范围生成")
    void testVersionRangeGeneration() {
        String range = VersionUtils.getVersionRange("1.0.0", "2.0.0");
        assertThat(range).isEqualTo(">=1.0.0 <2.0.0");

        String range2 = VersionUtils.getVersionRange("0.5.0", "1.0.0");
        assertThat(range2).isEqualTo(">=0.5.0 <1.0.0");
    }

    @Test
    @DisplayName("测试版本格式验证")
    void testVersionFormatValidation() {
        assertThat(VersionUtils.isValidVersion("1.2.3")).isTrue();
        assertThat(VersionUtils.isValidVersion("1.0")).isTrue();
        assertThat(VersionUtils.isValidVersion("1.2.3-alpha.1")).isTrue();
        assertThat(VersionUtils.isValidVersion("2.1.0-SNAPSHOT")).isTrue();
        
        assertThat(VersionUtils.isValidVersion(null)).isFalse();
        assertThat(VersionUtils.isValidVersion("")).isFalse();
        assertThat(VersionUtils.isValidVersion("   ")).isFalse();
        assertThat(VersionUtils.isValidVersion("invalid")).isFalse();
        assertThat(VersionUtils.isValidVersion("v1.2.3")).isFalse();
    }

    @Test
    @DisplayName("测试版本号递增功能")
    void testVersionIncrement() {
        // 补丁版本递增
        assertThat(VersionUtils.getNextPatchVersion("1.2.3")).isEqualTo("1.2.4");
        assertThat(VersionUtils.getNextPatchVersion("1.2.3-alpha")).isEqualTo("1.2.4");
        assertThat(VersionUtils.getNextPatchVersion("1.2")).isEqualTo("1.2.1");

        // 次版本递增
        assertThat(VersionUtils.getNextMinorVersion("1.2.3")).isEqualTo("1.3.0");
        assertThat(VersionUtils.getNextMinorVersion("1.2.3-alpha")).isEqualTo("1.3.0");
        assertThat(VersionUtils.getNextMinorVersion("1.2")).isEqualTo("1.3.0");

        // 主版本递增
        assertThat(VersionUtils.getNextMajorVersion("1.2.3")).isEqualTo("2.0.0");
        assertThat(VersionUtils.getNextMajorVersion("1.2.3-alpha")).isEqualTo("2.0.0");
        assertThat(VersionUtils.getNextMajorVersion("1.2")).isEqualTo("2.0.0");

        // 无效版本递增
        assertThatThrownBy(() -> VersionUtils.getNextPatchVersion("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionUtils.getNextMinorVersion("abc"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionUtils.getNextMajorVersion("v1.2.3"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试版本信息对象")
    void testVersionInfoObject() {
        VersionUtils.VersionInfo v1 = VersionUtils.parseVersion("1.2.3");
        VersionUtils.VersionInfo v2 = VersionUtils.parseVersion("1.2.3");
        VersionUtils.VersionInfo v3 = VersionUtils.parseVersion("1.2.4");

        // 测试equals和hashCode
        assertThat(v1).isEqualTo(v2);
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        assertThat(v1).isNotEqualTo(v3);
        assertThat(v1.hashCode()).isNotEqualTo(v3.hashCode());

        // 测试toString
        assertThat(v1.toString()).isEqualTo("1.2.3");

        // 测试hasPrerelease
        assertThat(v1.hasPrerelease()).isFalse();

        VersionUtils.VersionInfo v4 = VersionUtils.parseVersion("1.2.3-alpha.1");
        assertThat(v4.hasPrerelease()).isTrue();
    }

    @Test
    @DisplayName("测试版本信息比较")
    void testVersionInfoComparison() {
        // 基本比较
        VersionUtils.VersionInfo v1 = VersionUtils.parseVersion("1.0.0");
        VersionUtils.VersionInfo v2 = VersionUtils.parseVersion("1.0.0");
        assertThat(v1.compareTo(v2)).isEqualTo(0);

        VersionUtils.VersionInfo v3 = VersionUtils.parseVersion("1.0.1");
        assertThat(v1.compareTo(v3)).isEqualTo(-1);
        assertThat(v3.compareTo(v1)).isEqualTo(1);

        // 补丁版本比较
        VersionUtils.VersionInfo v4 = VersionUtils.parseVersion("1.0");
        assertThat(v4.compareTo(v1)).isEqualTo(0); // 1.0 == 1.0.0

        // 限定符比较
        VersionUtils.VersionInfo v5 = VersionUtils.parseVersion("1.2.3-alpha");
        VersionUtils.VersionInfo v6 = VersionUtils.parseVersion("1.2.3-beta");
        assertThat(v5.compareTo(v6)).isEqualTo(-1); // alpha < beta

        // 有限定符 vs 无限定符
        VersionUtils.VersionInfo v7 = VersionUtils.parseVersion("1.2.3");
        assertThat(v7.compareTo(v5)).isEqualTo(1); // 1.2.3 > 1.2.3-alpha
        assertThat(v5.compareTo(v7)).isEqualTo(-1);
    }

    @Test
    @DisplayName("测试边界情况")
    void testEdgeCases() {
        // 最大版本号
        assertThat(VersionUtils.isValidVersion("999.999.999")).isTrue();

        // 零版本号
        assertThat(VersionUtils.isValidVersion("0.0.0")).isTrue();
        VersionUtils.VersionInfo v0 = VersionUtils.parseVersion("0.0.0");
        assertThat(v0.getMajor()).isEqualTo(0);
        assertThat(v0.getMinor()).isEqualTo(0);
        assertThat(v0.getPatch()).isEqualTo(0);

        // 非常长的版本号
        String longVersion = "1.2.3-alpha.1.beta.2.gamma.3";
        assertThat(VersionUtils.isValidVersion(longVersion)).isTrue();

        // 空字符串边界
        assertThat(VersionUtils.parseVersion("  1.2.3  ")).isNotNull(); // 应该能够解析带空格的版本
    }

    @Test
    @DisplayName("测试性能场景")
    void testPerformanceScenarios() {
        // 测试大量版本解析性能
        String[] testVersions = {
            "1.0.0", "1.1.0", "1.2.0", "2.0.0", "2.1.0",
            "1.0.1", "1.1.1", "1.2.1", "2.0.1", "2.1.1",
            "1.0.0-alpha", "1.1.0-beta", "1.2.0-rc"
        };

        for (String version : testVersions) {
            assertThat(VersionUtils.isValidVersion(version)).isTrue();
            VersionUtils.VersionInfo info = VersionUtils.parseVersion(version);
            assertThat(info).isNotNull();
        }
    }

    @Test
    @DisplayName("测试版本解析容错性")
    void testVersionParsingFaultTolerance() {
        // 解析失败时回退为字符串比较
        assertThat(VersionUtils.compareVersions("invalid1", "invalid2"))
            .isEqualTo("invalid1".compareTo("invalid2"));
        assertThat(VersionUtils.isCompatible("invalid1", "invalid2")).isFalse();
        assertThat(VersionUtils.satisfies("invalid1", "invalid2", false)).isFalse();
    }
}