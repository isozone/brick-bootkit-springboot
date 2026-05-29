package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayConnectParams;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayRequestFrame;
import com.zqzqq.bootkits.openclaw.gateway.protocol.OpenClawGatewayConstants;
import com.zqzqq.bootkits.openclaw.gateway.protocol.ToolInvokeRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenClawGatewayProtocolTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldSerializeOfficialConnectFrame() throws Exception {
        GatewayConnectParams params = new GatewayConnectParams();
        params.getClient().setId("brick-bootkit");
        params.getAuth().setToken("token-1");

        GatewayRequestFrame frame = GatewayRequestFrame.request(OpenClawGatewayConstants.METHOD_CONNECT, params);
        frame.setId("req-1");
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(frame));

        assertThat(json.path("type").asText()).isEqualTo("req");
        assertThat(json.path("id").asText()).isEqualTo("req-1");
        assertThat(json.path("method").asText()).isEqualTo("connect");
        assertThat(json.path("params").path("minProtocol").asInt()).isEqualTo(3);
        assertThat(json.path("params").path("maxProtocol").asInt()).isEqualTo(4);
        assertThat(json.path("params").path("auth").path("token").asText()).isEqualTo("token-1");
    }

    @Test
    void shouldModelOfficialToolInvokeHttpPayload() throws Exception {
        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setTool("browser");
        request.setAction("click");
        request.setArgs(Map.of("selector", "#save"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.path("tool").asText()).isEqualTo("browser");
        assertThat(json.path("action").asText()).isEqualTo("click");
        assertThat(json.path("args").path("selector").asText()).isEqualTo("#save");
    }

    @Test
    void shouldDefaultGatewayToLoopbackAndRejectRemotePlaintext() {
        OpenClawGatewayClientProperties properties = new OpenClawGatewayClientProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("http://127.0.0.1:18789");
        assertThat(properties.resolveWebSocketUrl()).isEqualTo("ws://127.0.0.1:18789");

        properties.setBaseUrl("http://192.168.1.10:18789");
        assertThatThrownBy(() -> new DefaultOpenClawGatewayClient(properties, objectMapper))
                .isInstanceOf(OpenClawControlClientException.class)
                .hasMessageContaining("plaintext endpoints");
    }
}
