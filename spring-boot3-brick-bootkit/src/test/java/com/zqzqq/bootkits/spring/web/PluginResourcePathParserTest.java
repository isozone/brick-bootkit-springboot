package com.zqzqq.bootkits.spring.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link PluginResourcePathParser} 在不同路径前缀和 context-path 场景下的解析行为。
 */
class PluginResourcePathParserTest {

    @Test
    void shouldStripDefaultPluginsPrefix() {
        PluginStaticResourceConfig config = new PluginStaticResourceConfig();
        PluginResourcePathParser parser = new PluginResourcePathParser(config);

        PluginResourcePathParser.ParseResult result = parser.parse("plugins/plugin1/index.html");

        assertThat(result.getPluginId()).isEqualTo("plugin1");
        assertThat(result.getPartialPath()).isEqualTo("index.html");
    }

    @Test
    void shouldHandlePathWithoutPartial() {
        PluginStaticResourceConfig config = new PluginStaticResourceConfig();
        PluginResourcePathParser parser = new PluginResourcePathParser(config);

        PluginResourcePathParser.ParseResult result = parser.parse("plugins/plugin1");

        assertThat(result.getPluginId()).isEqualTo("plugin1");
        assertThat(result.getPartialPath()).isEqualTo("index.html");
    }

    @Test
    void shouldHandleNestedPartialPath() {
        PluginStaticResourceConfig config = new PluginStaticResourceConfig();
        PluginResourcePathParser parser = new PluginResourcePathParser(config);

        PluginResourcePathParser.ParseResult result = parser.parse("plugins/plugin1/assets/img/logo.png");

        assertThat(result.getPluginId()).isEqualTo("plugin1");
        assertThat(result.getPartialPath()).isEqualTo("assets/img/logo.png");
    }

    @Test
    void shouldWorkWithCustomPathPrefix() {
        PluginStaticResourceConfig config = new PluginStaticResourceConfig();
        config.setPathPrefix("static-plugin");
        PluginResourcePathParser parser = new PluginResourcePathParser(config);

        PluginResourcePathParser.ParseResult result = parser.parse("static-plugin/plugin1/index.html");

        assertThat(result.getPluginId()).isEqualTo("plugin1");
        assertThat(result.getPartialPath()).isEqualTo("index.html");
    }

    /**
     * 关键回归: 入参 requestPath 是 Spring 解析后的路径, 已去掉 context-path,
     * 形如 "plugins/plugin1/index.html"。即便主服务配置了 context-path=pre,
     * Spring 传入的 requestPath 也不会带 "pre/" 前缀, 因此解析器不应依赖 request 重新解析。
     */
    @Test
    void shouldNotRequireContextPathInRequestPath() {
        PluginStaticResourceConfig config = new PluginStaticResourceConfig();
        PluginResourcePathParser parser = new PluginResourcePathParser(config);

        // 模拟 context-path=pre 时的真实入参 (Spring 已剥离 context-path)
        PluginResourcePathParser.ParseResult result = parser.parse("plugins/plugin1/index.html");

        assertThat(result.getPluginId()).isEqualTo("plugin1");
        assertThat(result.getPartialPath()).isEqualTo("index.html");
    }
}
