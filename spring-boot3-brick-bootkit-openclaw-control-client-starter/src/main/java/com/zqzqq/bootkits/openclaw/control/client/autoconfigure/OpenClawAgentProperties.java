package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.OpenClawClientAuthMode;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlTransport;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "openclaw.agent")
public class OpenClawAgentProperties {

    private boolean enabled;
    private boolean autoStartup = true;
    private String baseUrl;
    private String apiBasePath = "/openclaw/control";
    private String webSocketUrl;
    private String webSocketPath = "/openclaw/control/ws";
    private String clientId;
    private String authToken;
    private String authSecret;
    private OpenClawClientAuthMode authMode = OpenClawClientAuthMode.TOKEN;
    private OpenClawControlTransport transport = OpenClawControlTransport.WEBSOCKET;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Duration webSocketRequestTimeout = Duration.ofSeconds(10);
    private Duration heartbeatInterval = Duration.ofSeconds(30);
    private Registration registration = new Registration();
    private Runtime runtime = new Runtime();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoStartup() {
        return autoStartup;
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public String getWebSocketPath() {
        return webSocketPath;
    }

    public void setWebSocketPath(String webSocketPath) {
        this.webSocketPath = webSocketPath;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthSecret() {
        return authSecret;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    public OpenClawClientAuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(OpenClawClientAuthMode authMode) {
        this.authMode = authMode == null ? OpenClawClientAuthMode.TOKEN : authMode;
    }

    public OpenClawControlTransport getTransport() {
        return transport;
    }

    public void setTransport(OpenClawControlTransport transport) {
        this.transport = transport == null ? OpenClawControlTransport.WEBSOCKET : transport;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
    }

    public Duration getWebSocketRequestTimeout() {
        return webSocketRequestTimeout;
    }

    public void setWebSocketRequestTimeout(Duration webSocketRequestTimeout) {
        this.webSocketRequestTimeout = webSocketRequestTimeout == null ? Duration.ofSeconds(10) : webSocketRequestTimeout;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(30) : heartbeatInterval;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration == null ? new Registration() : registration;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime == null ? new Runtime() : runtime;
    }

    public static class Registration {

        private String displayName;
        private String machineId;
        private String version;
        private String sdkVersion;
        private String hostName;
        private String osName;
        private String osVersion;
        private Set<String> tags = new LinkedHashSet<>();
        private Map<String, Object> attributes = new LinkedHashMap<>();
        private List<Capability> capabilities = new ArrayList<>();

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMachineId() {
            return machineId;
        }

        public void setMachineId(String machineId) {
            this.machineId = machineId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSdkVersion() {
            return sdkVersion;
        }

        public void setSdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void setTags(Set<String> tags) {
            this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
        }

        public List<Capability> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<Capability> capabilities) {
            this.capabilities = capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
        }
    }

    public static class Capability {

        private String capabilityId;
        private String category;
        private String name;
        private Boolean enabled = Boolean.TRUE;
        private Map<String, Object> metadata = new LinkedHashMap<>();

        public String getCapabilityId() {
            return capabilityId;
        }

        public void setCapabilityId(String capabilityId) {
            this.capabilityId = capabilityId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        }
    }

    public static class Runtime {

        private boolean autoClaimOnAssigned = true;
        private boolean pollEnabled = true;
        private Duration claimPollInterval = Duration.ofSeconds(15);
        private Duration leaseRenewAhead = Duration.ofSeconds(30);
        private int executorThreads = 2;

        public boolean isAutoClaimOnAssigned() {
            return autoClaimOnAssigned;
        }

        public void setAutoClaimOnAssigned(boolean autoClaimOnAssigned) {
            this.autoClaimOnAssigned = autoClaimOnAssigned;
        }

        public boolean isPollEnabled() {
            return pollEnabled;
        }

        public void setPollEnabled(boolean pollEnabled) {
            this.pollEnabled = pollEnabled;
        }

        public Duration getClaimPollInterval() {
            return claimPollInterval;
        }

        public void setClaimPollInterval(Duration claimPollInterval) {
            this.claimPollInterval = claimPollInterval == null ? Duration.ofSeconds(15) : claimPollInterval;
        }

        public Duration getLeaseRenewAhead() {
            return leaseRenewAhead;
        }

        public void setLeaseRenewAhead(Duration leaseRenewAhead) {
            this.leaseRenewAhead = leaseRenewAhead == null ? Duration.ofSeconds(30) : leaseRenewAhead;
        }

        public int getExecutorThreads() {
            return executorThreads;
        }

        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads <= 0 ? 2 : executorThreads;
        }
    }
}
