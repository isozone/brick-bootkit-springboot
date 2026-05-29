package com.zqzqq.bootkits.openclaw.control.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageType;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenClawAgentRuntime implements OpenClawControlMessageListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpenClawAgentRuntime.class);

    private final ManagedOpenClawControlClient managedClient;
    private final List<OpenClawTaskHandler> taskHandlers;
    private final OpenClawAgentRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService taskExecutor;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<String, RunningTask> runningTasks = new ConcurrentHashMap<>();
    private volatile ScheduledFuture<?> pollFuture;

    public OpenClawAgentRuntime(ManagedOpenClawControlClient managedClient,
                                List<OpenClawTaskHandler> taskHandlers,
                                OpenClawAgentRuntimeProperties properties) {
        this(managedClient, taskHandlers, properties, null);
    }

    public OpenClawAgentRuntime(ManagedOpenClawControlClient managedClient,
                                List<OpenClawTaskHandler> taskHandlers,
                                OpenClawAgentRuntimeProperties properties,
                                ObjectMapper objectMapper) {
        this.managedClient = Objects.requireNonNull(managedClient, "managedClient");
        this.taskHandlers = taskHandlers == null ? List.of() : List.copyOf(taskHandlers);
        this.properties = properties == null ? new OpenClawAgentRuntimeProperties() : properties;
        this.objectMapper = objectMapper == null ? new ObjectMapper().findAndRegisterModules() : objectMapper;
        this.taskExecutor = Executors.newFixedThreadPool(this.properties.getExecutorThreads(), runnable -> {
            Thread thread = new Thread(runnable, "openclaw-agent-task");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "openclaw-agent-poll");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void startPolling() {
        if (!properties.isPollEnabled()) {
            return;
        }
        long intervalMillis = Math.max(1_000L, properties.getClaimPollInterval().toMillis());
        pollFuture = scheduler.scheduleWithFixedDelay(this::claimAndExecuteSafely, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onConnected() {
        log.info("OpenClaw agent websocket connected");
    }

    @Override
    public void onMessage(ControlMessageEnvelope envelope) {
        if (envelope == null || envelope.getType() == null) {
            return;
        }
        if (envelope.getType() == ControlMessageType.TASK_ASSIGNED && properties.isAutoClaimOnAssigned()) {
            claimAndExecuteSafely();
            return;
        }
        if (envelope.getType() == ControlMessageType.TASK_CANCELLED) {
            TaskSnapshot task = convertPayload(envelope, TaskSnapshot.class);
            if (task != null) {
                RunningTask runningTask = runningTasks.get(task.getTaskId());
                if (runningTask != null) {
                    runningTask.cancelRequested.set(true);
                }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.warn("OpenClaw agent websocket error", throwable);
    }

    @Override
    public void close() {
        if (pollFuture != null) {
            pollFuture.cancel(true);
        }
        scheduler.shutdownNow();
        runningTasks.values().forEach(runningTask -> {
            if (runningTask.leaseRenewFuture != null) {
                runningTask.leaseRenewFuture.cancel(true);
            }
            runningTask.cancelRequested.set(true);
        });
        taskExecutor.shutdownNow();
    }

    private void claimAndExecuteSafely() {
        try {
            TaskClaimResponse claimResponse = managedClient.claimNextTask();
            if (claimResponse != null && claimResponse.getTask() != null) {
                executeTask(claimResponse.getTask());
            }
        } catch (Exception ex) {
            log.debug("OpenClaw task claim skipped: {}", ex.getMessage());
        }
    }

    private void executeTask(TaskSnapshot task) {
        if (task == null || task.getTaskId() == null || runningTasks.containsKey(task.getTaskId())) {
            return;
        }
        OpenClawTaskHandler handler = taskHandlers.stream()
                .filter(item -> item.supports(task))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            TaskResultReport report = new TaskResultReport();
            report.setStatus(TaskStatus.FAILED);
            report.setError("No task handler found for taskType=" + task.getTaskType());
            managedClient.completeTask(task.getTaskId(), report);
            return;
        }
        RunningTask runningTask = new RunningTask(task);
        runningTasks.put(task.getTaskId(), runningTask);
        scheduleLeaseRenewal(runningTask);
        taskExecutor.submit(() -> runTask(handler, runningTask));
    }

    private void runTask(OpenClawTaskHandler handler, RunningTask runningTask) {
        try {
            OpenClawTaskExecutionContext context = new RuntimeTaskExecutionContext(runningTask);
            OpenClawTaskExecutionResult result = handler.handle(context);
            completeRunningTask(runningTask, normalizeResult(result));
        } catch (Exception ex) {
            TaskResultReport report = new TaskResultReport();
            report.setStatus(runningTask.cancelRequested.get() ? TaskStatus.CANCELLED : TaskStatus.FAILED);
            report.setError(ex.getMessage());
            report.setMessage(ex.getClass().getSimpleName());
            managedClient.completeTask(runningTask.task.getTaskId(), report);
        } finally {
            cleanupRunningTask(runningTask.task.getTaskId());
        }
    }

    private OpenClawTaskExecutionResult normalizeResult(OpenClawTaskExecutionResult result) {
        if (result != null) {
            return result;
        }
        return OpenClawTaskExecutionResult.succeeded();
    }

    private void completeRunningTask(RunningTask runningTask, OpenClawTaskExecutionResult result) {
        TaskResultReport report = new TaskResultReport();
        if (runningTask.cancelRequested.get() && result.getStatus() == TaskStatus.SUCCEEDED) {
            report.setStatus(TaskStatus.CANCELLED);
            report.setMessage("task cancelled during execution");
        } else {
            report.setStatus(result.getStatus());
            report.setMessage(result.getMessage());
        }
        report.setOutput(result.getOutput());
        report.setError(result.getError());
        report.setResult(result.getResult());
        report.setArtifacts(result.getArtifacts());
        managedClient.completeTask(runningTask.task.getTaskId(), report);
    }

    private void scheduleLeaseRenewal(RunningTask runningTask) {
        long leaseSeconds = runningTask.task.getLeaseSeconds() == null || runningTask.task.getLeaseSeconds() <= 0
                ? 300L
                : runningTask.task.getLeaseSeconds();
        long renewDelay = Math.max(5L, leaseSeconds - properties.getLeaseRenewAhead().toSeconds());
        runningTask.leaseRenewFuture = scheduler.scheduleWithFixedDelay(() -> {
            if (runningTask.cancelRequested.get()) {
                return;
            }
            try {
                managedClient.renewTaskLease(runningTask.task.getTaskId(), runningTask.task.getLeaseSeconds());
            } catch (Exception ex) {
                log.warn("Failed to renew task lease: {}", runningTask.task.getTaskId(), ex);
            }
        }, renewDelay, renewDelay, TimeUnit.SECONDS);
    }

    private void cleanupRunningTask(String taskId) {
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask != null && runningTask.leaseRenewFuture != null) {
            runningTask.leaseRenewFuture.cancel(true);
        }
    }

    private <T> T convertPayload(ControlMessageEnvelope envelope, Class<T> type) {
        Object payload = envelope.getPayload();
        if (payload == null) {
            return null;
        }
        return objectMapper.convertValue(payload, type);
    }

    private final class RuntimeTaskExecutionContext implements OpenClawTaskExecutionContext {

        private final RunningTask runningTask;

        private RuntimeTaskExecutionContext(RunningTask runningTask) {
            this.runningTask = runningTask;
        }

        @Override
        public TaskSnapshot getTask() {
            return new TaskSnapshot(runningTask.task);
        }

        @Override
        public boolean isCancellationRequested() {
            return runningTask.cancelRequested.get();
        }

        @Override
        public void reportProgress(TaskProgressReport report) {
            managedClient.reportProgress(runningTask.task.getTaskId(), report);
        }
    }

    private static final class RunningTask {

        private final TaskSnapshot task;
        private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> leaseRenewFuture;

        private RunningTask(TaskSnapshot task) {
            this.task = new TaskSnapshot(task);
        }
    }
}
