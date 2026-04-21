package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedOpenClawControlClientTest {

    @Test
    void shouldStartAndDispatchHeartbeatThroughPreferredTransport() {
        OpenClawControlClientProperties properties = new OpenClawControlClientProperties();
        properties.setPreferredTransport(OpenClawControlTransport.REST);

        AtomicInteger registerCalls = new AtomicInteger();
        AtomicInteger heartbeatCalls = new AtomicInteger();
        OpenClawControlClient delegate = new StubClient(registerCalls, heartbeatCalls);
        ManagedOpenClawControlClient client = new ManagedOpenClawControlClient(delegate, properties);

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-managed");
        client.start(request, ClientHeartbeatRequest::new, null);
        client.heartbeatNow();
        client.close();

        assertThat(registerCalls.get()).isEqualTo(1);
        assertThat(heartbeatCalls.get()).isGreaterThanOrEqualTo(1);
    }

    private static class StubClient implements OpenClawControlClient {

        private final AtomicInteger registerCalls;
        private final AtomicInteger heartbeatCalls;

        private StubClient(AtomicInteger registerCalls, AtomicInteger heartbeatCalls) {
            this.registerCalls = registerCalls;
            this.heartbeatCalls = heartbeatCalls;
        }

        @Override
        public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
            registerCalls.incrementAndGet();
            ClientRegistrationResponse response = new ClientRegistrationResponse();
            response.setHeartbeatIntervalSeconds(30L);
            response.setSessionId("session-1");
            return response;
        }

        @Override
        public ClientRegistrationResponse registerClientOverWebSocket(ClientRegistrationRequest request) {
            return registerClient(request);
        }

        @Override
        public ClientHeartbeatResponse heartbeat(ClientHeartbeatRequest request) {
            heartbeatCalls.incrementAndGet();
            return new ClientHeartbeatResponse();
        }

        @Override
        public ClientHeartbeatResponse heartbeatOverWebSocket(ClientHeartbeatRequest request) {
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
        public List<com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot> listIntegrations() {
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
