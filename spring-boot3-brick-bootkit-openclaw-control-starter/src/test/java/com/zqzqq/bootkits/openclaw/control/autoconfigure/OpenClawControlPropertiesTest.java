package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawControlPropertiesTest {

    @Test
    void defaultsShouldNotExposeUnauthenticatedControlPlane() {
        OpenClawControlProperties properties = new OpenClawControlProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isExposeHttpEndpoints()).isFalse();
        assertThat(properties.getAuth().isEnabled()).isTrue();
        assertThat(properties.getWebSocket().isEnabled()).isFalse();
        assertThat(properties.getWebSocket().getAllowedOriginPatterns()).isEmpty();
    }

    @Test
    void nullAllowedOriginPatternsShouldRemainClosed() {
        OpenClawControlProperties.WebSocket webSocket = new OpenClawControlProperties.WebSocket();

        webSocket.setAllowedOriginPatterns(null);

        assertThat(webSocket.getAllowedOriginPatterns()).isEmpty();
    }

    @Test
    void configuredAllowedOriginPatternsShouldBeCopied() {
        OpenClawControlProperties.WebSocket webSocket = new OpenClawControlProperties.WebSocket();
        List<String> origins = List.of("https://example.com");

        webSocket.setAllowedOriginPatterns(origins);

        assertThat(webSocket.getAllowedOriginPatterns()).containsExactly("https://example.com");
    }
}
