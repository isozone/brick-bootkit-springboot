/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.web.auth;

import com.zqzqq.bootkits.web.config.BrickWebProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central authorization service for plugin web endpoints.
 */
@Slf4j
public class PluginWebAuthorizationService {

    private final PluginWebAuthMode mode;
    private final PluginWebAuthorizer authorizer;

    public PluginWebAuthorizationService(BrickWebProperties properties, PluginWebAuthorizer authorizer) {
        this.mode = PluginWebAuthMode.from(properties.getAuthMode());
        this.authorizer = authorizer;
        validateMode();
    }

    public PluginWebAuthMode getMode() {
        return mode;
    }

    public void check(PluginWebPermission permission, String pluginId) {
        if (mode == PluginWebAuthMode.DISABLED) {
            return;
        }
        PluginWebAuthorizationContext context = buildContext(permission, pluginId);
        PluginWebAuthorizationDecision decision = authorizer.authorize(context);
        if (decision == null || !decision.isAllowed()) {
            String reason = decision == null ? "permission denied" : decision.getReason();
            log.warn("Plugin web authorization denied. principal={}, permission={}, pluginId={}, uri={}, reason={}",
                    context.getPrincipal(), permission, pluginId, context.getRequestUri(), reason);
            throw new PluginWebAuthorizationException(reason);
        }
        log.debug("Plugin web authorization passed. principal={}, permission={}, pluginId={}, uri={}",
                context.getPrincipal(), permission, pluginId, context.getRequestUri());
    }

    public Map<String, Boolean> getCapabilities(String pluginId) {
        Map<String, Boolean> capabilities = new LinkedHashMap<>();
        for (PluginWebPermission permission : PluginWebPermission.values()) {
            capabilities.put(permission.name(), can(permission, pluginId));
        }
        return capabilities;
    }

    public String currentPrincipal() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "anonymous";
        }
        Principal principal = request.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            return "anonymous";
        }
        return principal.getName();
    }

    private boolean can(PluginWebPermission permission, String pluginId) {
        if (mode == PluginWebAuthMode.DISABLED) {
            return true;
        }
        try {
            PluginWebAuthorizationDecision decision = authorizer.authorize(buildContext(permission, pluginId));
            return decision != null && decision.isAllowed();
        } catch (Exception e) {
            log.debug("Capability check failed. permission={}, pluginId={}, reason={}",
                    permission, pluginId, e.getMessage());
            return false;
        }
    }

    private void validateMode() {
        if (mode == PluginWebAuthMode.STRICT && authorizer.isFallback()) {
            throw new IllegalStateException(
                    "plugin.web.auth.mode=strict requires a host-defined PluginWebAuthorizer bean");
        }
        if (mode == PluginWebAuthMode.DELEGATE && authorizer.isFallback()) {
            log.warn("Plugin web authorization is running in delegate mode with fallback authorizer (deny all). "
                    + "Provide a custom PluginWebAuthorizer bean in host application before exposing plugin web endpoints.");
        }
    }

    private PluginWebAuthorizationContext buildContext(PluginWebPermission permission, String pluginId) {
        HttpServletRequest request = currentRequest();
        String principal = "anonymous";
        String uri = "";
        String method = "";
        String remoteAddress = "";
        if (request != null) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null && userPrincipal.getName() != null && !userPrincipal.getName().trim().isEmpty()) {
                principal = userPrincipal.getName();
            }
            uri = request.getRequestURI();
            method = request.getMethod();
            remoteAddress = request.getRemoteAddr();
        }
        return PluginWebAuthorizationContext.builder()
                .permission(permission)
                .pluginId(pluginId)
                .principal(principal)
                .requestUri(uri)
                .httpMethod(method)
                .remoteAddress(remoteAddress)
                .build();
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception ignored) {
            return null;
        }
    }
}
