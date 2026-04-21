package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.ManagedOpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawAgentRuntime;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlClientProperties;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlMessageListener;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlTransport;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawHeartbeatSupplier;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawTaskExecutionContext;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawTaskExecutionResult;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawTaskHandler;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawControlClientAutoConfigurationTest {

    private final OpenClawControlClientAutoConfiguration autoConfiguration = new OpenClawControlClientAutoConfiguration();

    @Test
    void shouldMapAgentPropertiesToClientProperties() {
        OpenClawAgentProperties properties = new OpenClawAgentProperties();
        properties.setBaseUrl("http://127.0.0.1:8080");
        properties.setApiBasePath("/control/api");
        properties.setWebSocketUrl("ws://127.0.0.1:8080");
        properties.setWebSocketPath("/control/ws");
        properties.setClientId("client-starter");
        properties.setAuthToken("token-1");
        properties.setAuthSecret("secret-1");
        properties.setTransport(OpenClawControlTransport.WEBSOCKET);
        properties.setRequestTimeout(Duration.ofSeconds(7));
        properties.setWebSocketRequestTimeout(Duration.ofSeconds(9));
        properties.setHeartbeatInterval(Duration.ofSeconds(15));

        OpenClawControlClientProperties target = autoConfiguration.openClawControlClientProperties(properties);

        assertThat(target.getBaseUrl()).isEqualTo("http://127.0.0.1:8080");
        assertThat(target.getApiBasePath()).isEqualTo("/control/api");
        assertThat(target.getWebSocketUrl()).isEqualTo("ws://127.0.0.1:8080");
        assertThat(target.getWebSocketPath()).isEqualTo("/control/ws");
        assertThat(target.getClientId()).isEqualTo("client-starter");
        assertThat(target.getAuthToken()).isEqualTo("token-1");
        assertThat(target.getAuthSecret()).isEqualTo("secret-1");
        assertThat(target.getPreferredTransport()).isEqualTo(OpenClawControlTransport.WEBSOCKET);
        assertThat(target.getRequestTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(target.getWebSocketRequestTimeout()).isEqualTo(Duration.ofSeconds(9));
        assertThat(target.getHeartbeatInterval()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void shouldBuildRegistrationRequestAndComposeListeners() {
        OpenClawAgentProperties properties = new OpenClawAgentProperties();
        properties.setClientId("client-starter");
        properties.getRegistration().setDisplayName("Starter Client");
        properties.getRegistration().setMachineId("machine-1");
        properties.getRegistration().setVersion("1.0.0");
        properties.getRegistration().setSdkVersion("sdk-1");
        properties.getRegistration().setHostName("agent-host");
        properties.getRegistration().setOsName("Linux");
        properties.getRegistration().setOsVersion("6.0");
        properties.getRegistration().setTags(Set.of("edge", "tentacle"));
        properties.getRegistration().setAttributes(Map.of("zone", "cn-hz"));

        OpenClawAgentProperties.Capability capability = new OpenClawAgentProperties.Capability();
        capability.setCapabilityId("tentacle.exec");
        capability.setCategory("tentacle");
        capability.setName("Tentacle Exec");
        capability.setEnabled(Boolean.TRUE);
        capability.setMetadata(Map.of("mode", "sdk"));
        properties.getRegistration().setCapabilities(List.of(capability));

        ClientRegistrationRequest request = autoConfiguration.openClawClientRegistrationRequest(
                properties,
                providerOf((OpenClawRegistrationCustomizer) candidate -> {
                    candidate.getAttributes().put("customized", Boolean.TRUE);
                    candidate.getTags().add("customized");
                })
        );

        assertThat(request.getClientId()).isEqualTo("client-starter");
        assertThat(request.getDisplayName()).isEqualTo("Starter Client");
        assertThat(request.getMachineId()).isEqualTo("machine-1");
        assertThat(request.getVersion()).isEqualTo("1.0.0");
        assertThat(request.getSdkVersion()).isEqualTo("sdk-1");
        assertThat(request.getHostName()).isEqualTo("agent-host");
        assertThat(request.getOsName()).isEqualTo("Linux");
        assertThat(request.getOsVersion()).isEqualTo("6.0");
        assertThat(request.getTags()).contains("edge", "tentacle", "customized");
        assertThat(request.getAttributes()).containsEntry("zone", "cn-hz").containsEntry("customized", Boolean.TRUE);
        assertThat(request.getCapabilities()).hasSize(1);
        assertThat(request.getCapabilities().get(0).getCapabilityId()).isEqualTo("tentacle.exec");
        assertThat(request.getCapabilities().get(0).getAttributes()).containsEntry("mode", "sdk");

        OpenClawAgentRuntime runtime = autoConfiguration.openClawAgentRuntime(
                autoConfiguration.managedOpenClawControlClient(new StubControlClient(), new OpenClawControlClientProperties()),
                providerOf(new DummyTaskHandler()),
                autoConfiguration.openClawAgentRuntimeProperties(properties)
        );
        RecordingListener extraListener = new RecordingListener();
        OpenClawControlMessageListener composite = autoConfiguration.openClawAgentCompositeMessageListener(
                runtime,
                providerOf(runtime, extraListener)
        );
        OpenClawHeartbeatSupplier heartbeatSupplier = autoConfiguration.openClawHeartbeatSupplier();

        composite.onConnected();
        composite.onMessage(new ControlMessageEnvelope());

        assertThat(heartbeatSupplier.get()).isNotNull().isInstanceOf(ClientHeartbeatRequest.class);
        assertThat(composite).isInstanceOf(CompositeOpenClawControlMessageListener.class);
        assertThat(extraListener.connected).isTrue();
        assertThat(extraListener.messages).isEqualTo(1);

        runtime.close();
    }

    @Test
    void shouldCreateLifecycleWhenAutoStartupEnabled() {
        OpenClawAgentProperties properties = new OpenClawAgentProperties();
        properties.setAutoStartup(true);
        properties.setClientId("client-lifecycle");

        OpenClawControlClient client = new StubControlClient();
        ManagedOpenClawControlClient managed = autoConfiguration.managedOpenClawControlClient(client, new OpenClawControlClientProperties());
        OpenClawAgentRuntime runtime = autoConfiguration.openClawAgentRuntime(
                managed,
                providerOf(new DummyTaskHandler()),
                autoConfiguration.openClawAgentRuntimeProperties(properties)
        );

        OpenClawAgentLifecycle lifecycle = autoConfiguration.openClawAgentLifecycle(
                managed,
                runtime,
                autoConfiguration.openClawClientRegistrationRequest(properties, providerOf()),
                autoConfiguration.openClawHeartbeatSupplier(),
                runtime,
                properties
        );

        assertThat(lifecycle).isNotNull();

        runtime.close();
        managed.close();
    }

    @SafeVarargs
    private static <T> ObjectProvider<T> providerOf(T... values) {
        List<T> items = values == null ? List.of() : List.of(values);
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                if (items.isEmpty()) {
                    throw new IllegalStateException("No bean available");
                }
                return items.get(0);
            }

            @Override
            public T getObject(Object... args) {
                if (items.isEmpty()) {
                    throw new IllegalStateException("No bean available");
                }
                return items.get(0);
            }

            @Override
            public T getIfAvailable() {
                return items.isEmpty() ? null : items.get(0);
            }

            @Override
            public T getIfUnique() {
                return items.size() == 1 ? items.get(0) : null;
            }

            @Override
            public Stream<T> stream() {
                return items.stream();
            }

            @Override
            public Stream<T> orderedStream() {
                return items.stream();
            }
        };
    }

    private static class DummyTaskHandler implements OpenClawTaskHandler {

        @Override
        public boolean supports(TaskSnapshot task) {
            return true;
        }

        @Override
        public OpenClawTaskExecutionResult handle(OpenClawTaskExecutionContext context) {
            return OpenClawTaskExecutionResult.succeeded().setMessage("ok");
        }
    }

    private static class RecordingListener implements OpenClawControlMessageListener {

        private boolean connected;
        private int messages;

        @Override
        public void onConnected() {
            connected = true;
        }

        @Override
        public void onMessage(ControlMessageEnvelope envelope) {
            messages++;
        }
    }

    private static class StubControlClient implements OpenClawControlClient {

        @Override
        public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
            ClientRegistrationResponse response = new ClientRegistrationResponse();
            response.setSessionId("session-1");
            return response;
        }

        @Override
        public ClientRegistrationResponse registerClientOverWebSocket(ClientRegistrationRequest request) {
            return registerClient(request);
        }

        @Override
        public com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse heartbeat(ClientHeartbeatRequest request) {
            return new com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse();
        }

        @Override
        public com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse heartbeatOverWebSocket(ClientHeartbeatRequest request) {
            return heartbeat(request);
        }

        @Override
        public TaskClaimResponse claimNextTask(TaskClaimRequest request) {
            return new TaskClaimResponse();
        }

        @Override
        public TaskClaimResponse claimNextTaskOverWebSocket(TaskClaimRequest request) {
            return new TaskClaimResponse();
        }

        @Override
        public TaskSnapshot renewTaskLease(String taskId, TaskLeaseRenewRequest request) {
            return new TaskSnapshot();
        }

        @Override
        public TaskSnapshot renewTaskLeaseOverWebSocket(String taskId, TaskLeaseRenewRequest request) {
            return new TaskSnapshot();
        }

        @Override
        public TaskSnapshot reportProgress(String taskId, TaskProgressReport report) {
            return new TaskSnapshot();
        }

        @Override
        public TaskSnapshot reportProgressOverWebSocket(String taskId, TaskProgressReport report) {
            return new TaskSnapshot();
        }

        @Override
        public TaskSnapshot completeTask(String taskId, TaskResultReport report) {
            return new TaskSnapshot();
        }

        @Override
        public TaskSnapshot completeTaskOverWebSocket(String taskId, TaskResultReport report) {
            return new TaskSnapshot();
        }

        @Override
        public List<IntegrationSnapshot> listIntegrations() {
            return List.of();
        }

        @Override
        public void connectWebSocket(OpenClawControlMessageListener listener) {
        }

        @Override
        public boolean isWebSocketConnected() {
            return false;
        }

        @Override
        public String getSessionId() {
            return "session-1";
        }

        @Override
        public void close() {
        }
    }
}
