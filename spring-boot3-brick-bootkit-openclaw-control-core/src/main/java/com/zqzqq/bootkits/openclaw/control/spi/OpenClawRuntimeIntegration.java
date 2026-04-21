package com.zqzqq.bootkits.openclaw.control.spi;

import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;

import java.util.List;
import java.util.Map;

public interface OpenClawRuntimeIntegration {

    String integrationId();

    String displayName();

    boolean available();

    List<ClientCapabilityDescriptor> capabilities();

    Map<String, Object> details();
}
