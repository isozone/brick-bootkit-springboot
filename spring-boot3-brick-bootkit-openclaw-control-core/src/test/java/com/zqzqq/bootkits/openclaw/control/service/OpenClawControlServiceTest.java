package com.zqzqq.bootkits.openclaw.control.service;

import com.zqzqq.bootkits.openclaw.control.store.InMemoryClientStateStore;
import com.zqzqq.bootkits.openclaw.control.store.InMemoryTaskStateStore;
import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientStatus;
import com.zqzqq.bootkits.openclaw.protocol.TaskCancelRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskRetryRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskRoutingDecision;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawControlServiceTest {

    @Test
    void shouldRegisterHeartbeatDispatchAndCompleteTask() {
        OpenClawControlService service = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                120L
        );

        ClientCapabilityDescriptor capability = new ClientCapabilityDescriptor();
        capability.setCapabilityId("gateway.tasks");
        capability.setCategory("gateway");

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-a");
        request.setDisplayName("Client A");
        request.setTags(Set.of("prod", "mac"));
        request.setCapabilities(List.of(capability));
        String sessionId = service.registerClient(request).getSessionId();

        ClientHeartbeatRequest heartbeat = new ClientHeartbeatRequest();
        heartbeat.setSessionId(sessionId);
        heartbeat.setCurrentTaskIds(List.of());
        assertThat(service.heartbeat("client-a", heartbeat).getClientStatus()).isEqualTo(ClientStatus.ONLINE);

        TaskDispatchRequest dispatchRequest = new TaskDispatchRequest();
        dispatchRequest.setTaskType("workspace.sync");
        dispatchRequest.setRequiredCapabilities(Set.of("gateway.tasks"));
        dispatchRequest.setTargetTags(Set.of("prod"));
        String taskId = service.dispatchTask(dispatchRequest).getTaskId();

        assertThat(service.claimNextTask("client-a").getTask().getTaskId()).isEqualTo(taskId);

        TaskResultReport resultReport = new TaskResultReport();
        resultReport.setStatus(TaskStatus.SUCCEEDED);
        resultReport.setMessage("ok");
        service.completeTask(taskId, resultReport);

        assertThat(service.getTask(taskId).getStatus()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(service.getClient("client-a").getCurrentTaskIds()).isEmpty();
    }

    @Test
    void shouldRenewCancelAndRetryTask() {
        OpenClawControlService service = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                60L
        );

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId("client-b");
        request.setDisplayName("Client B");
        service.registerClient(request);

        TaskDispatchRequest dispatchRequest = new TaskDispatchRequest();
        dispatchRequest.setTaskType("browser.collect");
        dispatchRequest.setTargetClientId("client-b");
        dispatchRequest.setMaxRetries(2);
        String taskId = service.dispatchTask(dispatchRequest).getTaskId();

        service.claimNextTask("client-b");
        TaskLeaseRenewRequest renewRequest = new TaskLeaseRenewRequest();
        renewRequest.setClientId("client-b");
        renewRequest.setLeaseSeconds(180L);
        assertThat(service.renewTaskLease(taskId, renewRequest).getLeaseExpiresAt()).isNotNull();

        TaskCancelRequest cancelRequest = new TaskCancelRequest();
        cancelRequest.setRequestedBy("admin");
        cancelRequest.setReason("manual stop");
        assertThat(service.cancelTask(taskId, cancelRequest).getStatus()).isEqualTo(TaskStatus.CANCELLED);

        TaskRetryRequest retryRequest = new TaskRetryRequest();
        retryRequest.setRequestedBy("admin");
        String retriedTaskId = service.retryTask(taskId, retryRequest).getTaskId();
        assertThat(retriedTaskId).isNotEqualTo(taskId);
        assertThat(service.getTask(retriedTaskId).getRetryCount()).isEqualTo(1);
        assertThat(service.getTask(retriedTaskId).getParentTaskId()).isEqualTo(taskId);
    }

    @Test
    void shouldPreferClientWithTaskTypeCapabilityAndLowerLoad() {
        OpenClawControlService service = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                60L
        );

        service.registerClient(registration("client-a", "tentacle.exec"));
        service.registerClient(registration("client-b", "tentacle.exec"));
        service.registerClient(registration("client-c", "browser.collect"));

        TaskDispatchRequest first = new TaskDispatchRequest();
        first.setTaskType("tentacle.exec.command");
        first.setRequiredCapabilities(Set.of("tentacle.exec"));
        TaskSnapshot firstTask = service.dispatchTask(first);
        assertThat(firstTask.getAssignedClientId()).isIn("client-a", "client-b");

        TaskDispatchRequest second = new TaskDispatchRequest();
        second.setTaskType("tentacle.exec.command");
        TaskSnapshot secondTask = service.dispatchTask(second);
        assertThat(secondTask.getAssignedClientId()).isIn("client-a", "client-b");
        assertThat(secondTask.getAssignedClientId()).isNotEqualTo(firstTask.getAssignedClientId());
    }

    @Test
    void shouldRetryToAnotherEligibleClientWhenOriginalClientFailed() {
        OpenClawControlService service = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                60L
        );

        service.registerClient(registration("client-a", "tentacle.exec"));
        service.registerClient(registration("client-b", "tentacle.exec"));

        TaskDispatchRequest dispatchRequest = new TaskDispatchRequest();
        dispatchRequest.setTaskType("tentacle.exec.command");
        dispatchRequest.setRequiredCapabilities(Set.of("tentacle.exec"));
        TaskSnapshot task = service.dispatchTask(dispatchRequest);
        assertThat(task.getAssignedClientId()).isIn("client-a", "client-b");

        service.claimNextTask(task.getAssignedClientId());
        TaskResultReport failure = new TaskResultReport();
        failure.setStatus(TaskStatus.FAILED);
        failure.setError("boom");
        service.completeTask(task.getTaskId(), failure);

        TaskSnapshot retried = service.retryTask(task.getTaskId(), new TaskRetryRequest());
        assertThat(retried.getAssignedClientId()).isIn("client-a", "client-b");
        assertThat(retried.getAssignedClientId()).isNotEqualTo(task.getAssignedClientId());
        assertThat(retried.getParentTaskId()).isEqualTo(task.getTaskId());
    }

    @Test
    void shouldPreviewRoutingDecisionWithCandidateReasons() {
        OpenClawControlService service = new OpenClawControlService(
                new InMemoryClientStateStore(),
                new InMemoryTaskStateStore(),
                new DefaultTaskRoutingPolicy(),
                Duration.ofSeconds(90),
                Duration.ofMinutes(5),
                30L,
                60L
        );

        service.registerClient(registration("client-a", "tentacle.exec"));
        service.registerClient(registration("client-b", "browser.collect"));

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskType("tentacle.exec.command");
        TaskRoutingDecision decision = service.previewTaskRouting(request);

        assertThat(decision.getSelectedClientId()).isEqualTo("client-a");
        assertThat(decision.getPolicy()).isEqualTo(DefaultTaskRoutingPolicy.class.getSimpleName());
        assertThat(decision.getCandidates()).hasSize(2);
        assertThat(decision.getCandidates().get(0).getClientId()).isEqualTo("client-a");
        assertThat(decision.getCandidates().get(0).isEligible()).isTrue();
        assertThat(decision.getCandidates().get(0).getMatchedCapabilities()).contains("tentacle.exec");
        assertThat(decision.getCandidates().get(0).getReasons())
                .anySatisfy(reason -> assertThat(reason).contains("taskType affinity matched"));
        assertThat(decision.getCandidates().get(1).isEligible()).isFalse();
    }

    private ClientRegistrationRequest registration(String clientId, String capabilityId) {
        ClientCapabilityDescriptor capability = new ClientCapabilityDescriptor();
        capability.setCapabilityId(capabilityId);
        capability.setCategory(capabilityId.contains(".") ? capabilityId.substring(0, capabilityId.indexOf('.')) : capabilityId);

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId(clientId);
        request.setDisplayName(clientId);
        request.setTags(Set.of("prod"));
        request.setAttributes(Map.of("cluster", "default"));
        request.setCapabilities(List.of(capability));
        return request;
    }
}
