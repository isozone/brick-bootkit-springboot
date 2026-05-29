package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

public class ConfigurableOpenClawClientAuthVerifier implements OpenClawClientAuthVerifier {

    private static final HexFormat HEX = HexFormat.of();

    private final OpenClawControlProperties.Auth properties;
    private final ObjectMapper objectMapper;

    public ConfigurableOpenClawClientAuthVerifier(OpenClawControlProperties.Auth properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper == null
                ? JsonMapper.builder()
                .findAndAddModules()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build()
                : objectMapper.copy()
                .findAndRegisterModules()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    @Override
    public void verifyRegistration(ClientRegistrationRequest request, Map<String, String> authHeaders) {
        if (!properties.isEnabled()) {
            return;
        }
        if (request == null || isBlank(request.getClientId())) {
            throw new ControlPlaneException("clientId is required");
        }
        verifyClientAccess(request.getClientId(), null, request, authHeaders);
    }

    @Override
    public void verifyClientAccess(String clientId, String sessionId, Object payload, Map<String, String> authHeaders) {
        if (!properties.isEnabled()) {
            return;
        }
        if (isBlank(clientId)) {
            throw new ControlPlaneException("clientId is required for authentication");
        }
        String mode = properties.getMode() == null ? "token" : properties.getMode().trim().toLowerCase();
        if ("signature".equals(mode)) {
            verifySignature(clientId, sessionId, payload, authHeaders);
            return;
        }
        verifyToken(clientId, payload, authHeaders);
    }

    private void verifyToken(String clientId, Object payload, Map<String, String> authHeaders) {
        String expected = valueForClient(clientId, properties.getClientTokens(), properties.getSharedToken());
        if (isBlank(expected)) {
            throw new ControlPlaneException("no token configured for client: " + clientId);
        }
        String actual = resolveValue(payload, authHeaders, "authToken", OpenClawClientAuthSupport.HEADER_CLIENT_TOKEN);
        if (!Objects.equals(expected, actual)) {
            throw new ControlPlaneException("invalid client token");
        }
    }

    private void verifySignature(String clientId, String sessionId, Object payload, Map<String, String> authHeaders) {
        String secret = valueForClient(clientId, properties.getClientSecrets(), properties.getSharedSecret());
        if (isBlank(secret)) {
            throw new ControlPlaneException("no signature secret configured for client: " + clientId);
        }
        String timestamp = resolveValue(payload, authHeaders, "authTimestamp", OpenClawClientAuthSupport.HEADER_AUTH_TIMESTAMP);
        String nonce = resolveValue(payload, authHeaders, "authNonce", OpenClawClientAuthSupport.HEADER_AUTH_NONCE);
        String signature = resolveValue(payload, authHeaders, "authSignature", OpenClawClientAuthSupport.HEADER_AUTH_SIGNATURE);
        if (isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new ControlPlaneException("incomplete client signature");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new ControlPlaneException("invalid auth timestamp");
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - ts);
        if (skew > properties.getClockSkewSeconds()) {
            throw new ControlPlaneException("client signature expired");
        }

        String payloadHash = sha256Hex(canonicalPayload(payload));
        String stringToSign = clientId + "\n"
                + (sessionId == null ? "" : sessionId) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + payloadHash;
        String expectedSignature = hmacSha256Hex(secret, stringToSign);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new ControlPlaneException("invalid client signature");
        }
    }

    private String canonicalPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        JsonNode jsonNode = objectMapper.valueToTree(payload);
        if (jsonNode.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("authToken");
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("authTimestamp");
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("authNonce");
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("authSignature");
        }
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new ControlPlaneException("failed to build auth payload signature", ex);
        }
    }

    private String resolveValue(Object payload, Map<String, String> authHeaders, String payloadField, String headerName) {
        String value = OpenClawClientAuthSupport.header(authHeaders, headerName);
        if (!isBlank(value)) {
            return value;
        }
        if (payload == null) {
            return null;
        }
        JsonNode jsonNode = objectMapper.valueToTree(payload);
        JsonNode fieldNode = jsonNode.get(payloadField);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText();
    }

    private String valueForClient(String clientId, Map<String, String> scopedValues, String sharedValue) {
        if (scopedValues != null) {
            String scoped = scopedValues.get(clientId);
            if (!isBlank(scoped)) {
                return scoped;
            }
        }
        return sharedValue;
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ControlPlaneException("failed to compute auth digest", ex);
        }
    }

    private String hmacSha256Hex(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ControlPlaneException("failed to compute client signature", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
