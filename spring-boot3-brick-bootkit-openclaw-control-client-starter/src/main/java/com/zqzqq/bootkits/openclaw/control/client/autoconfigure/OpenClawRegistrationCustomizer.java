package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;

public interface OpenClawRegistrationCustomizer {

    void customize(ClientRegistrationRequest request);
}
