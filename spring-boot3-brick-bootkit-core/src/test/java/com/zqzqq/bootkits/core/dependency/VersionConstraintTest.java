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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 版本约束测试
 */
@DisplayName("VersionConstraint Test")
class VersionConstraintTest {

    @Test
    @DisplayName("测试版本约束解析 - 相等")
    void testVersionConstraintParseEquals() {
        VersionConstraint constraint = VersionConstraint.parse("=1.0.0");
        assertThat(constraint).isNotNull();
        assertThat(constraint.getOperator()).isEqualTo(VersionConstraint.ConstraintOperator.EQUALS);
        assertThat(constraint.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("测试版本约束解析 - 大于等于")
    void testVersionConstraintParseGreaterThanEqual() {
        VersionConstraint constraint = VersionConstraint.parse(">=1.0.0");
        assertThat(constraint).isNotNull();
        assertThat(constraint.getOperator()).isEqualTo(VersionConstraint.ConstraintOperator.GREATER_THAN_EQUAL);
        assertThat(constraint.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("测试版本约束解析 - 小于")
    void testVersionConstraintParseLessThan() {
        VersionConstraint constraint = VersionConstraint.parse("<2.0.0");
        assertThat(constraint).isNotNull();
        assertThat(constraint.getOperator()).isEqualTo(VersionConstraint.ConstraintOperator.LESS_THAN);
        assertThat(constraint.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("测试版本约束满足性 - 等于")
    void testVersionConstraintSatisfactionEquals() {
        VersionConstraint constraint = VersionConstraint.parse("=1.0.0");
        
        assertThat(constraint.isSatisfied("1.0.0")).isTrue();
        assertThat(constraint.isSatisfied("1.0.1")).isFalse();
        assertThat(constraint.isSatisfied("0.9.9")).isFalse();
    }

    @Test
    @DisplayName("测试版本约束满足性 - 大于等于")
    void testVersionConstraintSatisfactionGreaterThanEqual() {
        VersionConstraint constraint = VersionConstraint.parse(">=1.0.0");
        
        assertThat(constraint.isSatisfied("1.0.0")).isTrue();
        assertThat(constraint.isSatisfied("1.0.1")).isTrue();
        assertThat(constraint.isSatisfied("2.0.0")).isTrue();
        assertThat(constraint.isSatisfied("0.9.9")).isFalse();
    }

    @Test
    @DisplayName("测试版本约束满足性 - 小于")
    void testVersionConstraintSatisfactionLessThan() {
        VersionConstraint constraint = VersionConstraint.parse("<2.0.0");
        
        assertThat(constraint.isSatisfied("1.0.0")).isTrue();
        assertThat(constraint.isSatisfied("1.9.9")).isTrue();
        assertThat(constraint.isSatisfied("2.0.0")).isFalse();
        assertThat(constraint.isSatisfied("2.1.0")).isFalse();
    }

    @Test
    @DisplayName("测试版本约束解析失败")
    void testVersionConstraintParseFailure() {
        assertThatThrownBy(() -> VersionConstraint.parse(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("版本约束不能为空");
            
        assertThatThrownBy(() -> VersionConstraint.parse(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("版本约束不能为空");
            
        assertThatThrownBy(() -> VersionConstraint.parse("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("无效的版本约束格式");
    }

    @Test
    @DisplayName("测试版本约束原始字符串")
    void testVersionConstraintOriginalString() {
        String original = ">=1.0.0";
        VersionConstraint constraint = VersionConstraint.parse(original);
        assertThat(constraint.getOriginalConstraint()).isEqualTo(original);
    }

    @Test
    @DisplayName("测试版本约束字符串表示")
    void testVersionConstraintToString() {
        VersionConstraint constraint = VersionConstraint.parse(">=1.0.0");
        assertThat(constraint.toString()).isEqualTo(">=1.0.0");
    }

    @Test
    @DisplayName("测试null版本检查")
    void testNullVersionCheck() {
        VersionConstraint constraint = VersionConstraint.parse(">=1.0.0");
        assertThat(constraint.isSatisfied(null)).isFalse();
        assertThat(constraint.isSatisfied("")).isFalse();
    }
}