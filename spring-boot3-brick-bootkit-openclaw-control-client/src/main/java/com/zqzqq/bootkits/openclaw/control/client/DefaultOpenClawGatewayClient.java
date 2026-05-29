package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.gateway.protocol.AdminRpcRequest;
import com.zqzqq.bootkits.openclaw.gateway.protocol.AdminRpcResponse;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayAuth;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayConnectParams;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayHelloOk;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayRequestFrame;
import com.zqzqq.bootkits.openclaw.gateway.protocol.GatewayResponseFrame;
import com.zqzqq.bootkits.openclaw.gateway.protocol.OpenClawGatewayConstants;
import com.zqzqq.bootkits.openclaw.gateway.protocol.ToolInvokeRequest;
import com.zqzqq.bootkits.openclaw.gateway.protocol.ToolInvokeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class DefaultOpenClawGatewayClient implements OpenClawGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOpenClawGatewayClient.class);

    private final OpenClawGatewayClientProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final ConcurrentMap<String, CompletableFuture<GatewayResponseFrame>> pendingRequests = new ConcurrentHashMap<>();
    private volatile StandardWebSocketClient webSocketClient;
    private volatile WebSocketSession webSocketSession;

    public DefaultOpenClawGatewayClient(OpenClawGatewayClientProperties properties) {
        this(properties, JsonMapper.builder().findAndAddModules().build());
    }

    public DefaultOpenClawGatewayClient(OpenClawGatewayClientProperties properties, ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = objectMapper == null ? JsonMapper.builder().findAndAddModules().build() : objectMapper;
        assertAllowedEndpoint(URI.create(this.properties.getBaseUrl()));
        this.restClient = RestClient.builder()
                .baseUrl(this.properties.getBaseUrl())
                .requestFactory(requestFactory(this.properties.getRequestTimeout()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public GatewayHelloOk connect(GatewayConnectParams params) {
        connectWebSocketIfNecessary();
        GatewayConnectParams payload = prepareConnectParams(params);
        GatewayResponseFrame response = rpc(OpenClawGatewayConstants.METHOD_CONNECT, payload);
        return objectMapper.convertValue(response.getPayload(), GatewayHelloOk.class);
    }

    @Override
    public GatewayResponseFrame rpc(String method, Object params) {
        if (isBlank(method)) {
            throw new OpenClawControlClientException("OpenClaw Gateway RPC method is required");
        }
        if (!isConnected()) {
            throw new OpenClawControlClientException("OpenClaw Gateway websocket is not connected");
        }
        GatewayRequestFrame frame = GatewayRequestFrame.request(method, params);
        CompletableFuture<GatewayResponseFrame> future = new CompletableFuture<>();
        pendingRequests.put(frame.getId(), future);
        try {
            webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
            GatewayResponseFrame response = future.get(
                    properties.getWebSocketRequestTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (Boolean.FALSE.equals(response.getOk())) {
                String message = response.getError() == null ? "OpenClaw Gateway RPC failed" : response.getError().getMessage();
                throw new OpenClawControlClientException(message);
            }
            return response;
        } catch (OpenClawControlClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenClawControlClientException("OpenClaw Gateway RPC failed: " + method, ex);
        } finally {
            pendingRequests.remove(frame.getId());
        }
    }

    @Override
    public ToolInvokeResponse invokeTool(ToolInvokeRequest request) {
        try {
            return restClient.post()
                    .uri(OpenClawGatewayConstants.TOOL_INVOKE_PATH)
                    .headers(this::applyAuthHeaders)
                    .body(request)
                    .retrieve()
                    .body(ToolInvokeResponse.class);
        } catch (Exception ex) {
            throw new OpenClawControlClientException("OpenClaw Gateway tool invocation failed", ex);
        }
    }

    @Override
    public AdminRpcResponse adminRpc(AdminRpcRequest request) {
        try {
            return restClient.post()
                    .uri(OpenClawGatewayConstants.ADMIN_RPC_PATH)
                    .headers(this::applyAuthHeaders)
                    .body(request)
                    .retrieve()
                    .body(AdminRpcResponse.class);
        } catch (Exception ex) {
            throw new OpenClawControlClientException("OpenClaw Gateway admin RPC failed", ex);
        }
    }

    @Override
    public boolean isConnected() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    @Override
    public void close() {
        pendingRequests.values().forEach(future -> future.completeExceptionally(
                new OpenClawControlClientException("OpenClaw Gateway client closed")));
        pendingRequests.clear();
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                webSocketSession.close(CloseStatus.NORMAL);
            } catch (IOException ex) {
                log.debug("Failed to close OpenClaw Gateway websocket session", ex);
            }
        }
    }

    private void connectWebSocketIfNecessary() {
        if (isConnected()) {
            return;
        }
        String target = properties.resolveWebSocketUrl();
        assertAllowedEndpoint(URI.create(target));
        try {
            StandardWebSocketClient client = webSocketClient;
            if (client == null) {
                client = new StandardWebSocketClient();
                webSocketClient = client;
            }
            this.webSocketSession = client.execute(
                    new GatewayWebSocketHandler(),
                    new WebSocketHttpHeaders(),
                    URI.create(target)
            ).get(properties.getWebSocketRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new OpenClawControlClientException("failed to connect OpenClaw Gateway websocket", ex);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = toTimeoutMillis(timeout);
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return requestFactory;
    }

    private int toTimeoutMillis(Duration timeout) {
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        long millis = Math.max(1L, effectiveTimeout.toMillis());
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }

    private GatewayConnectParams prepareConnectParams(GatewayConnectParams params) {
        GatewayConnectParams payload = params == null ? new GatewayConnectParams() : params;
        GatewayAuth auth = payload.getAuth();
        if (auth == null) {
            auth = new GatewayAuth();
            payload.setAuth(auth);
        }
        if (isBlank(auth.getToken()) && !isBlank(properties.getAuthToken())) {
            auth.setToken(properties.getAuthToken());
        }
        if (isBlank(auth.getPassword()) && !isBlank(properties.getAuthPassword())) {
            auth.setPassword(properties.getAuthPassword());
        }
        return payload;
    }

    void applyAuthHeaders(HttpHeaders headers) {
        if (!isBlank(properties.getAuthToken())) {
            headers.setBearerAuth(properties.getAuthToken());
            return;
        }
        if (!isBlank(properties.getAuthPassword())) {
            headers.setBearerAuth(properties.getAuthPassword());
        }
    }

    private void assertAllowedEndpoint(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"ws".equalsIgnoreCase(scheme)) {
            return;
        }
        if (properties.isAllowPlaintextRemoteGateway() || isLoopbackHost(uri.getHost())) {
            return;
        }
        throw new OpenClawControlClientException(
                "OpenClaw Gateway plaintext endpoints are allowed only on loopback by default");
    }

    private boolean isLoopbackHost(String host) {
        return host == null
                || "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private class GatewayWebSocketHandler extends TextWebSocketHandler {

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if (!OpenClawGatewayConstants.FRAME_RESPONSE.equals(node.path("type").asText())) {
                return;
            }
            String id = node.path("id").asText(null);
            if (id == null) {
                return;
            }
            CompletableFuture<GatewayResponseFrame> future = pendingRequests.get(id);
            if (future != null) {
                future.complete(objectMapper.treeToValue(node, GatewayResponseFrame.class));
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            pendingRequests.values().forEach(future -> future.completeExceptionally(exception));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            webSocketSession = null;
            pendingRequests.values().forEach(future -> future.completeExceptionally(
                    new OpenClawControlClientException("OpenClaw Gateway websocket closed")));
        }
    }
}
