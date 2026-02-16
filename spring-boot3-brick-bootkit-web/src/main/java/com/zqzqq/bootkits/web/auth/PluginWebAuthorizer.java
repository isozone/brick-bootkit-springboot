package com.zqzqq.bootkits.web.auth;

/**
 * Authorization delegate interface implemented by host application.
 */
public interface PluginWebAuthorizer {

    /**
     * Decide whether the request is allowed.
     *
     * @param context authorization context
     * @return authorization decision
     */
    PluginWebAuthorizationDecision authorize(PluginWebAuthorizationContext context);

    /**
     * Indicates whether current authorizer is a fallback implementation.
     *
     * @return true for fallback authorizer
     */
    default boolean isFallback() {
        return false;
    }
}

