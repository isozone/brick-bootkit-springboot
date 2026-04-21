package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenClawClientAuthSupport {

    public static final String HEADER_CLIENT_TOKEN = "X-OpenClaw-Client-Token";
    public static final String HEADER_AUTH_TIMESTAMP = "X-OpenClaw-Auth-Ts";
    public static final String HEADER_AUTH_NONCE = "X-OpenClaw-Auth-Nonce";
    public static final String HEADER_AUTH_SIGNATURE = "X-OpenClaw-Auth-Signature";
    public static final String HEADER_SESSION_ID = "X-OpenClaw-Session-Id";
    public static final String HEADER_CLIENT_ID = "X-OpenClaw-Client-Id";

    private OpenClawClientAuthSupport() {
    }

    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (request == null) {
            return headers;
        }
        copyHeader(request, headers, HEADER_CLIENT_TOKEN);
        copyHeader(request, headers, HEADER_AUTH_TIMESTAMP);
        copyHeader(request, headers, HEADER_AUTH_NONCE);
        copyHeader(request, headers, HEADER_AUTH_SIGNATURE);
        copyHeader(request, headers, HEADER_SESSION_ID);
        copyHeader(request, headers, HEADER_CLIENT_ID);
        return headers;
    }

    public static String header(Map<String, String> headers, String name) {
        return headers == null ? null : headers.get(name);
    }

    private static void copyHeader(HttpServletRequest request, Map<String, String> headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.put(name, value);
        }
    }
}
