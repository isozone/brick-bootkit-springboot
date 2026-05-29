package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "openclaw.control")
public class OpenClawControlProperties {

    private boolean enabled = true;
    private boolean exposeHttpEndpoints = false;
    private String storeType = "auto";
    private String apiBasePath = "/openclaw/control";
    private long heartbeatIntervalSeconds = 30L;
    private long staleAfterSeconds = 90L;
    private long offlineAfterSeconds = 300L;
    private long defaultTaskLeaseSeconds = 300L;
    private Jdbc jdbc = new Jdbc();
    private Auth auth = new Auth();
    private WebSocket webSocket = new WebSocket();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExposeHttpEndpoints() {
        return exposeHttpEndpoints;
    }

    public void setExposeHttpEndpoints(boolean exposeHttpEndpoints) {
        this.exposeHttpEndpoints = exposeHttpEndpoints;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public long getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(long heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public long getStaleAfterSeconds() {
        return staleAfterSeconds;
    }

    public void setStaleAfterSeconds(long staleAfterSeconds) {
        this.staleAfterSeconds = staleAfterSeconds;
    }

    public long getOfflineAfterSeconds() {
        return offlineAfterSeconds;
    }

    public void setOfflineAfterSeconds(long offlineAfterSeconds) {
        this.offlineAfterSeconds = offlineAfterSeconds;
    }

    public long getDefaultTaskLeaseSeconds() {
        return defaultTaskLeaseSeconds;
    }

    public void setDefaultTaskLeaseSeconds(long defaultTaskLeaseSeconds) {
        this.defaultTaskLeaseSeconds = defaultTaskLeaseSeconds;
    }

    public Jdbc getJdbc() {
        return jdbc;
    }

    public void setJdbc(Jdbc jdbc) {
        this.jdbc = jdbc == null ? new Jdbc() : jdbc;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth == null ? new Auth() : auth;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket == null ? new WebSocket() : webSocket;
    }

    public boolean useJdbcStore(boolean hasDataSource) {
        if ("jdbc".equalsIgnoreCase(storeType)) {
            return true;
        }
        if ("memory".equalsIgnoreCase(storeType)) {
            return false;
        }
        return hasDataSource;
    }

    public static class Auth {

        private boolean enabled = true;
        private String mode = "token";
        private String sharedToken;
        private String sharedSecret;
        private long clockSkewSeconds = 300L;
        private Map<String, String> clientTokens = new LinkedHashMap<>();
        private Map<String, String> clientSecrets = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getSharedToken() {
            return sharedToken;
        }

        public void setSharedToken(String sharedToken) {
            this.sharedToken = sharedToken;
        }

        public String getSharedSecret() {
            return sharedSecret;
        }

        public void setSharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
        }

        public long getClockSkewSeconds() {
            return clockSkewSeconds;
        }

        public void setClockSkewSeconds(long clockSkewSeconds) {
            this.clockSkewSeconds = clockSkewSeconds;
        }

        public Map<String, String> getClientTokens() {
            return clientTokens;
        }

        public void setClientTokens(Map<String, String> clientTokens) {
            this.clientTokens = clientTokens == null ? new LinkedHashMap<>() : new LinkedHashMap<>(clientTokens);
        }

        public Map<String, String> getClientSecrets() {
            return clientSecrets;
        }

        public void setClientSecrets(Map<String, String> clientSecrets) {
            this.clientSecrets = clientSecrets == null ? new LinkedHashMap<>() : new LinkedHashMap<>(clientSecrets);
        }
    }

    public static class Jdbc {

        private boolean initializeSchema = true;
        private String tablePrefix = "oc_control_";

        public boolean isInitializeSchema() {
            return initializeSchema;
        }

        public void setInitializeSchema(boolean initializeSchema) {
            this.initializeSchema = initializeSchema;
        }

        public String getTablePrefix() {
            return tablePrefix;
        }

        public void setTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public String getClientsTableName() {
            return normalizePrefix(tablePrefix) + "clients";
        }

        public String getTasksTableName() {
            return normalizePrefix(tablePrefix) + "tasks";
        }

        private String normalizePrefix(String prefix) {
            if (prefix == null || prefix.isBlank()) {
                return "oc_control_";
            }
            return prefix;
        }
    }

    public static class WebSocket {

        private boolean enabled = false;
        private boolean pushTaskAssignments = true;
        private String endpointPath = "/openclaw/control/ws";
        private List<String> allowedOriginPatterns = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPushTaskAssignments() {
            return pushTaskAssignments;
        }

        public void setPushTaskAssignments(boolean pushTaskAssignments) {
            this.pushTaskAssignments = pushTaskAssignments;
        }

        public String getEndpointPath() {
            return endpointPath;
        }

        public void setEndpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
        }

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns == null
                    ? new ArrayList<>()
                    : new ArrayList<>(allowedOriginPatterns);
        }
    }
}
