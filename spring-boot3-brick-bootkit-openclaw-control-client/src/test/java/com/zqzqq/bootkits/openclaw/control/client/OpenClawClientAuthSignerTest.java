package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawClientAuthSignerTest {

    @Test
    void shouldSignTokenRequest() {
        OpenClawControlClientProperties properties = new OpenClawControlClientProperties();
        properties.setClientId("client-a");
        properties.setAuthToken("token-1");
        properties.setAuthMode(OpenClawClientAuthMode.TOKEN);

        OpenClawClientAuthSigner signer = new OpenClawClientAuthSigner(properties, JsonMapper.builder().findAndAddModules().build());
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-a");

        OpenClawSignedRequest signed = signer.sign("client-a", null, request);

        assertThat(signed.getHeaders().get(OpenClawClientAuthSigner.HEADER_CLIENT_TOKEN)).isEqualTo("token-1");
        assertThat(request.getAuthToken()).isEqualTo("token-1");
    }

    @Test
    void shouldSignSignatureRequest() throws Exception {
        OpenClawControlClientProperties properties = new OpenClawControlClientProperties();
        properties.setClientId("client-a");
        properties.setAuthSecret("secret-1");
        properties.setAuthMode(OpenClawClientAuthMode.SIGNATURE);
        JsonMapper mapper = JsonMapper.builder()
                .findAndAddModules()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();

        OpenClawClientAuthSigner signer = new OpenClawClientAuthSigner(properties, mapper);
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-a");
        request.setDisplayName("Client A");

        OpenClawSignedRequest signed = signer.sign("client-a", null, request);
        String timestamp = signed.getHeaders().get(OpenClawClientAuthSigner.HEADER_AUTH_TIMESTAMP);
        String nonce = signed.getHeaders().get(OpenClawClientAuthSigner.HEADER_AUTH_NONCE);

        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.valueToTree(request);
        node.remove("authToken");
        node.remove("authTimestamp");
        node.remove("authNonce");
        node.remove("authSignature");
        String payloadJson = mapper.writeValueAsString(node);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String payloadHash = HexFormat.of().formatHex(digest.digest(payloadJson.getBytes(StandardCharsets.UTF_8)));
        String stringToSign = "client-a\n\n" + timestamp + "\n" + nonce + "\n" + payloadHash;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret-1".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));

        assertThat(signed.getHeaders().get(OpenClawClientAuthSigner.HEADER_AUTH_SIGNATURE)).isEqualTo(expected);
        assertThat(request.getAuthSignature()).isEqualTo(expected);
    }
}
