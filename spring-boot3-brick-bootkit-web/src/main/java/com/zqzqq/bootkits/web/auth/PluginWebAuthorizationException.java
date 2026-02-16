package com.zqzqq.bootkits.web.auth;

/**
 * Exception thrown when authorization fails.
 */
public class PluginWebAuthorizationException extends RuntimeException {
    public PluginWebAuthorizationException(String message) {
        super(message);
    }
}

