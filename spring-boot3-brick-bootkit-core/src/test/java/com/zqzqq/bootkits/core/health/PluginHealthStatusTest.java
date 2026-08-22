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


package com.zqzqq.bootkits.core.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件健康状态枚举测试
 */
@DisplayName("PluginHealthStatus Test")
class PluginHealthStatusTest {

    @Test
    @DisplayName("测试所有健康状态枚举值")
    void testAllHealthStatusValues() {
        assertThat(PluginHealthStatus.values()).containsExactly(
            PluginHealthStatus.HEALTHY,
            PluginHealthStatus.WARNING,
            PluginHealthStatus.CRITICAL,
            PluginHealthStatus.DEAD,
            PluginHealthStatus.UNKNOWN
        );
    }

    @Test
    @DisplayName("测试从字符串创建健康状态")
    void testFromString() {
        assertThat(PluginHealthStatus.valueOf("HEALTHY")).isEqualTo(PluginHealthStatus.HEALTHY);
        assertThat(PluginHealthStatus.valueOf("WARNING")).isEqualTo(PluginHealthStatus.WARNING);
        assertThat(PluginHealthStatus.valueOf("CRITICAL")).isEqualTo(PluginHealthStatus.CRITICAL);
        assertThat(PluginHealthStatus.valueOf("DEAD")).isEqualTo(PluginHealthStatus.DEAD);
        assertThat(PluginHealthStatus.valueOf("UNKNOWN")).isEqualTo(PluginHealthStatus.UNKNOWN);
    }

    @Test
    @DisplayName("测试健康状态的排序")
    void testHealthStatusPriority() {
        assertThat(PluginHealthStatus.HEALTHY.ordinal()).isLessThan(PluginHealthStatus.WARNING.ordinal());
        assertThat(PluginHealthStatus.WARNING.ordinal()).isLessThan(PluginHealthStatus.CRITICAL.ordinal());
        assertThat(PluginHealthStatus.CRITICAL.ordinal()).isLessThan(PluginHealthStatus.DEAD.ordinal());
        assertThat(PluginHealthStatus.DEAD.ordinal()).isLessThan(PluginHealthStatus.UNKNOWN.ordinal());
    }

    @Test
    @DisplayName("测试健康状态的字符串表示")
    void testHealthStatusToString() {
        assertThat(PluginHealthStatus.HEALTHY.name()).isEqualTo("HEALTHY");
        assertThat(PluginHealthStatus.WARNING.name()).isEqualTo("WARNING");
        assertThat(PluginHealthStatus.CRITICAL.name()).isEqualTo("CRITICAL");
        assertThat(PluginHealthStatus.DEAD.name()).isEqualTo("DEAD");
        assertThat(PluginHealthStatus.UNKNOWN.name()).isEqualTo("UNKNOWN");
        assertThat(PluginHealthStatus.HEALTHY.getStatus()).isEqualTo("healthy");
        assertThat(PluginHealthStatus.HEALTHY.getDescription()).isEqualTo("插件运行正常");
    }

    @Test
    @DisplayName("测试健康状态的辅助方法")
    void testHealthStatusHelpers() {
        assertThat(PluginHealthStatus.HEALTHY.isHealthy()).isTrue();
        assertThat(PluginHealthStatus.WARNING.isHealthy()).isFalse();
        assertThat(PluginHealthStatus.CRITICAL.isHealthy()).isFalse();
        assertThat(PluginHealthStatus.DEAD.isHealthy()).isFalse();
        assertThat(PluginHealthStatus.UNKNOWN.isHealthy()).isFalse();

        assertThat(PluginHealthStatus.HEALTHY.needsAutoRecovery()).isFalse();
        assertThat(PluginHealthStatus.WARNING.needsAutoRecovery()).isFalse();
        assertThat(PluginHealthStatus.CRITICAL.needsAutoRecovery()).isTrue();
        assertThat(PluginHealthStatus.DEAD.needsAutoRecovery()).isTrue();
        assertThat(PluginHealthStatus.UNKNOWN.needsAutoRecovery()).isFalse();

        assertThat(PluginHealthStatus.HEALTHY.needsManualIntervention()).isFalse();
        assertThat(PluginHealthStatus.WARNING.needsManualIntervention()).isFalse();
        assertThat(PluginHealthStatus.CRITICAL.needsManualIntervention()).isFalse();
        assertThat(PluginHealthStatus.DEAD.needsManualIntervention()).isTrue();
        assertThat(PluginHealthStatus.UNKNOWN.needsManualIntervention()).isTrue();

        assertThat(PluginHealthStatus.HEALTHY.getSeverityLevel()).isEqualTo(0);
        assertThat(PluginHealthStatus.WARNING.getSeverityLevel()).isEqualTo(1);
        assertThat(PluginHealthStatus.CRITICAL.getSeverityLevel()).isEqualTo(2);
        assertThat(PluginHealthStatus.DEAD.getSeverityLevel()).isEqualTo(3);
        assertThat(PluginHealthStatus.UNKNOWN.getSeverityLevel()).isEqualTo(2);
    }
}
