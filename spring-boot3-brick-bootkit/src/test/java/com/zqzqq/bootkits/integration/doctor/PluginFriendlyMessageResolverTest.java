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
