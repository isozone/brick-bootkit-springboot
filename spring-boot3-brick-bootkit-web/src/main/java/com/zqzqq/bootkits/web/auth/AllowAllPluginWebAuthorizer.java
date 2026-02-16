package com.zqzqq.bootkits.web.auth;

/**
 * Default fallback authorizer.
 */
public class AllowAllPluginWebAuthorizer implements PluginWebAuthorizer {

    @Override
    public PluginWebAuthorizationDecision authorize(PluginWebAuthorizationContext context) {
        return PluginWebAuthorizationDecision.allow();
    }

    @Override
    public boolean isFallback() {
        return true;
    }
}

