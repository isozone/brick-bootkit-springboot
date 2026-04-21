package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OpenClawClientAuthSigner {

    public static final String HEADER_CLIENT_TOKEN = "X-OpenClaw-Client-Token";
    public static final String HEADER_AUTH_TIMESTAMP = "X-OpenClaw-Auth-Ts";
    public static final String HEADER_AUTH_NONCE = "X-OpenClaw-Auth-Nonce";
    public static final String HEADER_AUTH_SIGNATURE = "X-OpenClaw-Auth-Signature";
    public static final String HEADER_SESSION_ID = "X-OpenClaw-Session-Id";
    public static final String HEADER_CLIENT_ID = "X-OpenClaw-Client-Id";

    private static final HexFormat HEX = HexFormat.of();

    private final OpenClawControlClientProperties properties;
    private final ObjectMapper objectMapper;

    public OpenClawClientAuthSigner(OpenClawControlClientProperties properties, ObjectMapper objectMapper) {
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

    public OpenClawSignedRequest sign(String clientId, String sessionId, Object payload) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (!isBlank(clientId)) {
            headers.put(HEADER_CLIENT_ID, clientId);
        }
        if (!isBlank(sessionId)) {
            headers.put(HEADER_SESSION_ID, sessionId);
        }
        OpenClawClientAuthMode authMode = properties.getAuthMode() == null
                ? OpenClawClientAuthMode.TOKEN
                : properties.getAuthMode();
        if (authMode == OpenClawClientAuthMode.SIGNATURE) {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String canonicalPayload = canonicalPayload(payload);
            String payloadHash = sha256Hex(canonicalPayload);
            String stringToSign = nullToBlank(clientId) + "\n"
                    + nullToBlank(sessionId) + "\n"
                    + timestamp + "\n"
                    + nonce + "\n"
                    + payloadHash;
            String signature = hmacSha256Hex(requiredSecret(), stringToSign);
            headers.put(HEADER_AUTH_TIMESTAMP, timestamp);
            headers.put(HEADER_AUTH_NONCE, nonce);
            headers.put(HEADER_AUTH_SIGNATURE, signature);
            injectAuthFields(payload, null, timestamp, nonce, signature);
            return new OpenClawSignedRequest(payload, headers);
        }
        String token = properties.getAuthToken();
        if (!isBlank(token)) {
            headers.put(HEADER_CLIENT_TOKEN, token);
            injectAuthFields(payload, token, null, null, null);
        }
        return new OpenClawSignedRequest(payload, headers);
    }

    private String canonicalPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        ObjectNode node = objectMapper.valueToTree(payload);
        node.remove("authToken");
        node.remove("authTimestamp");
        node.remove("authNonce");
        node.remove("authSignature");
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new OpenClawControlClientException("failed to serialize signed payload", ex);
        }
    }

    private void injectAuthFields(Object payload,
                                  String token,
                                  String timestamp,
                                  String nonce,
                                  String signature) {
        if (payload instanceof ClientRegistrationRequest request) {
            request.setAuthToken(token);
            request.setAuthTimestamp(timestamp);
            request.setAuthNonce(nonce);
            request.setAuthSignature(signature);
            return;
        }
        if (payload instanceof ClientHeartbeatRequest request) {
            request.setAuthToken(token);
            request.setAuthTimestamp(timestamp);
            request.setAuthNonce(nonce);
            request.setAuthSignature(signature);
            return;
        }
        if (payload instanceof TaskClaimRequest request) {
            request.setAuthToken(token);
            request.setAuthTimestamp(timestamp);
            request.setAuthNonce(nonce);
            request.setAuthSignature(signature);
            return;
        }
        if (payload instanceof TaskLeaseRenewRequest request) {
            request.setAuthToken(token);
            request.setAuthTimestamp(timestamp);
            request.setAuthNonce(nonce);
            request.setAuthSignature(signature);
        }
    }

    private String requiredSecret() {
        if (isBlank(properties.getAuthSecret())) {
            throw new OpenClawControlClientException("auth secret is required for signature mode");
        }
        return properties.getAuthSecret();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new OpenClawControlClientException("failed to compute payload digest", ex);
        }
    }

    private String hmacSha256Hex(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new OpenClawControlClientException("failed to compute request signature", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
