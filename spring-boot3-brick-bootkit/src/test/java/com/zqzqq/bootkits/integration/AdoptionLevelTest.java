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


package com.zqzqq.bootkits.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 渐进式接入级别的推导规则测试。
 */
class AdoptionLevelTest {

    @Test
    void shouldBeActiveByDefault() {
        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();

        assertThat(configuration.autoLoadPlugins()).isTrue();
        assertThat(configuration.autoStartPlugins()).isTrue();
        assertThat(configuration.adoptionLevel()).isEqualTo(AdoptionLevel.ACTIVE);
    }

    @Test
    void shouldBeShadowWhenLoadingIsDisabled() {
        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();
        configuration.setAutoLoadPlugins(false);

        assertThat(configuration.adoptionLevel()).isEqualTo(AdoptionLevel.SHADOW);
    }

    @Test
    void shouldBeObserveWhenOnlyAutoStartIsDisabled() {
        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();
        configuration.setAutoStartPlugins(false);

        assertThat(configuration.adoptionLevel()).isEqualTo(AdoptionLevel.OBSERVE);
    }

    @Test
    void shadowShouldTakePrecedenceOverObserve() {
        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();
        configuration.setAutoLoadPlugins(false);
        configuration.setAutoStartPlugins(false);

        assertThat(configuration.adoptionLevel()).isEqualTo(AdoptionLevel.SHADOW);
    }

    @Test
    void nullValueShouldFallBackToDefault() {
        AutoIntegrationConfiguration configuration = new AutoIntegrationConfiguration();
        configuration.setAutoLoadPlugins(null);
        configuration.setAutoStartPlugins(null);

        assertThat(configuration.adoptionLevel()).isEqualTo(AdoptionLevel.ACTIVE);
    }

    @Test
    void levelShouldCarryChineseDescription() {
        assertThat(AdoptionLevel.SHADOW.getDescription()).isEqualTo("影子模式");
        assertThat(AdoptionLevel.OBSERVE.getDescription()).isEqualTo("观察模式");
        assertThat(AdoptionLevel.ACTIVE.getDescription()).isEqualTo("全量模式");
    }
}
