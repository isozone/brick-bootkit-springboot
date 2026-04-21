package com.zqzqq.bootkits.openclaw.control.service;

import com.zqzqq.bootkits.openclaw.control.spi.TaskRoutingPolicy;
import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;

import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultTaskRoutingPolicy implements TaskRoutingPolicy {

    @Override
    public boolean supports(ClientSnapshot client, TaskDispatchRequest request) {
        if (client == null || request == null) {
            return false;
        }
        if (request.getTargetClientId() != null && request.getTargetClientId().equals(client.getClientId())) {
            return true;
        }
        if (request.getRequiredCapabilities() != null && !request.getRequiredCapabilities().isEmpty()) {
            return true;
        }
        Set<String> enabledCapabilities = enabledCapabilityIds(client);
        if (enabledCapabilities.isEmpty()) {
            return true;
        }
        Set<String> taskTypeCapabilities = expandTaskTypeCapabilities(request.getTaskType());
        if (taskTypeCapabilities.isEmpty()) {
            return true;
        }
        return taskTypeCapabilities.stream().anyMatch(enabledCapabilities::contains);
    }

    @Override
    public int score(ClientSnapshot client, TaskDispatchRequest request) {
        if (client == null || request == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        if (request.getTargetClientId() != null && request.getTargetClientId().equals(client.getClientId())) {
            score += 100_000;
        }

        Set<String> enabledCapabilities = enabledCapabilityIds(client);
        Set<String> requiredCapabilities = request.getRequiredCapabilities() == null
                ? Set.of()
                : request.getRequiredCapabilities();
        for (String capabilityId : requiredCapabilities) {
            if (enabledCapabilities.contains(capabilityId)) {
                score += 10_000;
            }
        }

        Set<String> taskTypeCapabilities = expandTaskTypeCapabilities(request.getTaskType());
        int weight = 2_000;
        for (String capabilityId : taskTypeCapabilities) {
            if (enabledCapabilities.contains(capabilityId)) {
                score += weight;
                break;
            }
            weight = Math.max(250, weight / 2);
        }

        score += Math.max(0, 10 - client.getCurrentTaskIds().size()) * 10;
        return score;
    }

    private Set<String> enabledCapabilityIds(ClientSnapshot client) {
        Set<String> result = new LinkedHashSet<>();
        if (client == null || client.getCapabilities() == null) {
            return result;
        }
        for (ClientCapabilityDescriptor capability : client.getCapabilities()) {
            if (capability == null || !Boolean.TRUE.equals(capability.getEnabled())) {
                continue;
            }
            if (capability.getCapabilityId() != null && !capability.getCapabilityId().isBlank()) {
                result.add(capability.getCapabilityId());
            }
        }
        return result;
    }

    private Set<String> expandTaskTypeCapabilities(String taskType) {
        Set<String> result = new LinkedHashSet<>();
        if (taskType == null || taskType.isBlank()) {
            return result;
        }
        String current = taskType.trim();
        result.add(current);
        while (current.contains(".")) {
            current = current.substring(0, current.lastIndexOf('.'));
            result.add(current);
        }
        return result;
    }
}
