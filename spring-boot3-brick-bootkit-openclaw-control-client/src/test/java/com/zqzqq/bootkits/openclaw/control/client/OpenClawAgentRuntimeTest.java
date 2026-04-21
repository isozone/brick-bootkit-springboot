package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OpenClawAgentRuntimeTest {

    @Test
    void shouldClaimAndExecuteTaskWhenAssignedMessageArrives() throws Exception {
        RecordingClient delegate = new RecordingClient(task("task-1", "tentacle.exec"));
        OpenClawControlClientProperties properties = new OpenClawControlClientProperties();
        properties.setPreferredTransport(OpenClawControlTransport.REST);
        ManagedOpenClawControlClient managedClient = new ManagedOpenClawControlClient(delegate, properties);
        OpenClawAgentRuntime runtime = new OpenClawAgentRuntime(
                managedClient,
                List.of(new ReportingTaskHandler()),
                runtimeProperties()
        );

        ControlMessageEnvelope envelope = new ControlMessageEnvelope();
        envelope.setType(ControlMessageType.TASK_ASSIGNED);
        runtime.onMessage(envelope);

        assertThat(delegate.completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(delegate.claimCalls.get()).isEqualTo(1);
        assertThat(delegate.progressCalls.get()).isEqualTo(1);
        assertThat(delegate.lastProgressReport).isNotNull();
        assertThat(delegate.lastProgressReport.getProgressPercent()).isEqualTo(50);
        assertThat(delegate.lastResultReport).isNotNull();
        assertThat(delegate.lastResultReport.getStatus()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(delegate.lastResultReport.getResult()).containsEntry("executed", Boolean.TRUE);

        runtime.close();
        managedClient.close();
    }

    @Test
    void shouldConvertSuccessfulResultToCancelledWhenCancelWasRequested() throws Exception {
        RecordingClient delegate = new RecordingClient(task("task-2", "tentacle.exec"));
        OpenClawControlClientProperties properties = new OpenClawControlClientProperties();
        properties.setPreferredTransport(OpenClawControlTransport.REST);
        ManagedOpenClawControlClient managedClient = new ManagedOpenClawControlClient(delegate, properties);
        CancellingTaskHandler handler = new CancellingTaskHandler();
        OpenClawAgentRuntime runtime = new OpenClawAgentRuntime(
                managedClient,
                List.of(handler),
                runtimeProperties()
        );

        ControlMessageEnvelope assigned = new ControlMessageEnvelope();
        assigned.setType(ControlMessageType.TASK_ASSIGNED);
        runtime.onMessage(assigned);
        assertThat(handler.started.await(2, TimeUnit.SECONDS)).isTrue();

        ControlMessageEnvelope cancelled = new ControlMessageEnvelope();
        cancelled.setType(ControlMessageType.TASK_CANCELLED);
        cancelled.setPayload(Map.of("taskId", "task-2"));
        runtime.onMessage(cancelled);

        assertThat(delegate.completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(delegate.lastResultReport).isNotNull();
        assertThat(delegate.lastResultReport.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(delegate.lastResultReport.getMessage()).isEqualTo("task cancelled during execution");

        runtime.close();
        managedClient.close();
    }

    private OpenClawAgentRuntimeProperties runtimeProperties() {
        OpenClawAgentRuntimeProperties properties = new OpenClawAgentRuntimeProperties();
        properties.setPollEnabled(false);
        properties.setExecutorThreads(1);
        properties.setClaimPollInterval(Duration.ofSeconds(30));
        properties.setLeaseRenewAhead(Duration.ofSeconds(30));
        return properties;
    }

    private TaskSnapshot task(String taskId, String taskType) {
        TaskSnapshot task = new TaskSnapshot();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setLeaseSeconds(300L);
        return task;
    }

    private static class ReportingTaskHandler implements OpenClawTaskHandler {

        @Override
        public boolean supports(TaskSnapshot task) {
            return "tentacle.exec".equals(task.getTaskType());
        }

        @Override
        public OpenClawTaskExecutionResult handle(OpenClawTaskExecutionContext context) {
            TaskProgressReport report = new TaskProgressReport();
            report.setStatus(TaskStatus.RUNNING);
            report.setProgressPercent(50);
            report.setMessage("halfway");
            context.reportProgress(report);
            return OpenClawTaskExecutionResult.succeeded().setResult(Map.of("executed", Boolean.TRUE));
        }
    }

    private static class CancellingTaskHandler implements OpenClawTaskHandler {

        private final CountDownLatch started = new CountDownLatch(1);

        @Override
        public boolean supports(TaskSnapshot task) {
            return "tentacle.exec".equals(task.getTaskType());
        }

        @Override
        public OpenClawTaskExecutionResult handle(OpenClawTaskExecutionContext context) throws Exception {
            started.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                if (context.isCancellationRequested()) {
                    return OpenClawTaskExecutionResult.succeeded().setMessage("ignored");
                }
                Thread.sleep(20L);
            }
            return OpenClawTaskExecutionResult.succeeded().setMessage("finished");
        }
    }

    private static class RecordingClient implements OpenClawControlClient {

        private final TaskSnapshot task;
        private final AtomicInteger claimCalls = new AtomicInteger();
        private final AtomicInteger progressCalls = new AtomicInteger();
        private final CountDownLatch completed = new CountDownLatch(1);
        private volatile TaskProgressReport lastProgressReport;
        private volatile TaskResultReport lastResultReport;

        private RecordingClient(TaskSnapshot task) {
            this.task = task;
        }

        @Override
        public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
            return new ClientRegistrationResponse();
        }

        @Override
        public ClientRegistrationResponse registerClientOverWebSocket(ClientRegistrationRequest request) {
            return registerClient(request);
        }

        @Override
        public ClientHeartbeatResponse heartbeat(ClientHeartbeatRequest request) {
            return new ClientHeartbeatResponse();
        }

        @Override
        public ClientHeartbeatResponse heartbeatOverWebSocket(ClientHeartbeatRequest request) {
            return heartbeat(request);
        }

        @Override
        public TaskClaimResponse claimNextTask(TaskClaimRequest request) {
            claimCalls.incrementAndGet();
            TaskClaimResponse response = new TaskClaimResponse();
            response.setAccepted(true);
            response.setTask(new TaskSnapshot(task));
            return response;
        }

        @Override
        public TaskClaimResponse claimNextTaskOverWebSocket(TaskClaimRequest request) {
            return claimNextTask(request);
        }

        @Override
        public TaskSnapshot renewTaskLease(String taskId, TaskLeaseRenewRequest request) {
            return new TaskSnapshot(task);
        }

        @Override
        public TaskSnapshot renewTaskLeaseOverWebSocket(String taskId, TaskLeaseRenewRequest request) {
            return renewTaskLease(taskId, request);
        }

        @Override
        public TaskSnapshot reportProgress(String taskId, TaskProgressReport report) {
            progressCalls.incrementAndGet();
            lastProgressReport = report;
            return new TaskSnapshot(task);
        }

        @Override
        public TaskSnapshot reportProgressOverWebSocket(String taskId, TaskProgressReport report) {
            return reportProgress(taskId, report);
        }

        @Override
        public TaskSnapshot completeTask(String taskId, TaskResultReport report) {
            lastResultReport = report;
            completed.countDown();
            return new TaskSnapshot(task);
        }

        @Override
        public TaskSnapshot completeTaskOverWebSocket(String taskId, TaskResultReport report) {
            return completeTask(taskId, report);
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
