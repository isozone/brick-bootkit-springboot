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


package com.zqzqq.bootkits.integration.doctor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginFriendlyMessageResolverTest {

    @Test
    void shouldAppendMainPackageSuggestion() {
        PluginFriendlyMessageResolver.Resolution resolved =
                PluginFriendlyMessageResolver.resolve("插件配置: [plugin.mainPackage] 不能为空");

        assertThat(resolved.getErrorKey()).isEqualTo("MAIN_PACKAGE_MISSING");
        assertThat(resolved.getHintPath()).isEqualTo("/troubleshooting");
        assertThat(resolved.getHintAnchor()).isEqualTo("common-errors");
        assertThat(resolved.getMessage())
                .contains("plugin.mainPackage")
                .contains("建议：")
                .contains("@SpringBootApplication");
    }

    @Test
    void shouldKeepMessageWhenNoSuggestionMatches() {
        PluginFriendlyMessageResolver.Resolution resolved =
                PluginFriendlyMessageResolver.resolve("自定义错误");

        assertThat(resolved.getMessage()).isEqualTo("自定义错误");
        assertThat(resolved.getIssue()).isNull();
    }
}
