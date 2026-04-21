package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;

import java.util.Map;

public interface OpenClawClientAuthVerifier {

    void verifyRegistration(ClientRegistrationRequest request, Map<String, String> authHeaders);

    void verifyClientAccess(String clientId, String sessionId, Object payload, Map<String, String> authHeaders);
}
