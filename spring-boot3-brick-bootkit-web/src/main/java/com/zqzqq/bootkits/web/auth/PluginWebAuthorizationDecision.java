package com.zqzqq.bootkits.web.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Authorization decision for a single request.
 */
@Data
@AllArgsConstructor
public class PluginWebAuthorizationDecision {
    private boolean allowed;
    private String reason;

    public static PluginWebAuthorizationDecision allow() {
        return new PluginWebAuthorizationDecision(true, "allowed");
    }

    public static PluginWebAuthorizationDecision deny(String reason) {
        return new PluginWebAuthorizationDecision(false, reason == null ? "permission denied" : reason);
    }
}

