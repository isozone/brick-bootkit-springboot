package com.zqzqq.bootkits.web.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Authorization context passed to host application authorizer.
 */
@Data
@Builder
public class PluginWebAuthorizationContext {
    private PluginWebPermission permission;
    private String pluginId;
    private String principal;
    private String requestUri;
    private String httpMethod;
    private String remoteAddress;
}

