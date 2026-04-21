package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurableOpenClawClientAuthVerifierTest {

    @Test
    void shouldVerifyTokenAuth() {
        OpenClawControlProperties.Auth auth = new OpenClawControlProperties.Auth();
        auth.setEnabled(true);
        auth.setMode("token");
        auth.setClientTokens(Map.of("client-auth", "token-123"));

        ConfigurableOpenClawClientAuthVerifier verifier =
                new ConfigurableOpenClawClientAuthVerifier(auth, JsonMapper.builder().findAndAddModules().build());

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-auth");
        request.setAuthToken("token-123");

        assertThatCode(() -> verifier.verifyRegistration(request, Map.of())).doesNotThrowAnyException();
        request.setAuthToken("bad-token");
        assertThatThrownBy(() -> verifier.verifyRegistration(request, Map.of()))
                .isInstanceOf(ControlPlaneException.class)
                .hasMessageContaining("invalid client token");
    }

    @Test
    void shouldVerifySignatureAuth() throws Exception {
        OpenClawControlProperties.Auth auth = new OpenClawControlProperties.Auth();
        auth.setEnabled(true);
        auth.setMode("signature");
        auth.setClientSecrets(Map.of("client-sign", "secret-xyz"));
        auth.setClockSkewSeconds(300L);

        JsonMapper mapper = JsonMapper.builder()
                .findAndAddModules()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        ConfigurableOpenClawClientAuthVerifier verifier =
                new ConfigurableOpenClawClientAuthVerifier(auth, mapper);

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-sign");
        request.setDisplayName("Signed Client");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "nonce-1";
        request.setAuthTimestamp(timestamp);
        request.setAuthNonce(nonce);
        request.setAuthSignature(sign("client-sign", null, timestamp, nonce, canonicalPayload(mapper, request), "secret-xyz"));

        assertThatCode(() -> verifier.verifyRegistration(request, Map.of())).doesNotThrowAnyException();
    }

    private String canonicalPayload(JsonMapper mapper, ClientRegistrationRequest request) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.valueToTree(request);
        node.remove("authToken");
        node.remove("authTimestamp");
        node.remove("authNonce");
        node.remove("authSignature");
        return mapper.writeValueAsString(node);
    }

    private String sign(String clientId, String sessionId, String timestamp, String nonce, String canonicalPayload, String secret) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String payloadHash = HexFormat.of().formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        String stringToSign = clientId + "\n" + (sessionId == null ? "" : sessionId) + "\n" + timestamp + "\n" + nonce + "\n" + payloadHash;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }
}
