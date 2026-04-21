package com.zqzqq.bootkits.openclaw.control.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenClawSignedRequest {

    private final Object payload;
    private final Map<String, String> headers;

    public OpenClawSignedRequest(Object payload, Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
