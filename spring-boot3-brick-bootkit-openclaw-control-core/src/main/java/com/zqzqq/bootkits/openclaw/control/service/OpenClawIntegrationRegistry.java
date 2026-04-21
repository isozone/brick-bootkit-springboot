package com.zqzqq.bootkits.openclaw.control.service;

import com.zqzqq.bootkits.openclaw.control.spi.OpenClawRuntimeIntegration;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenClawIntegrationRegistry {

    private final List<OpenClawRuntimeIntegration> integrations;

    public OpenClawIntegrationRegistry(List<OpenClawRuntimeIntegration> integrations) {
        this.integrations = integrations == null ? Collections.emptyList() : new ArrayList<>(integrations);
    }

    public List<IntegrationSnapshot> list() {
        List<IntegrationSnapshot> result = new ArrayList<>();
        for (OpenClawRuntimeIntegration integration : integrations) {
            IntegrationSnapshot snapshot = new IntegrationSnapshot();
            snapshot.setIntegrationId(integration.integrationId());
            snapshot.setDisplayName(integration.displayName());
            snapshot.setAvailable(integration.available());
            snapshot.setCapabilities(integration.capabilities());
            snapshot.setDetails(integration.details());
            result.add(snapshot);
        }
        return result;
    }
}
