package com.zqzqq.bootkits.openclaw.control.service;

import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;
import com.zqzqq.bootkits.openclaw.control.spi.ClientStateStore;
import com.zqzqq.bootkits.openclaw.control.spi.TaskRoutingPolicy;
import com.zqzqq.bootkits.openclaw.control.spi.TaskLifecycleListener;
import com.zqzqq.bootkits.openclaw.control.spi.TaskStateStore;
import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.ClientStatus;
import com.zqzqq.bootkits.openclaw.protocol.TaskCancelRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskRoutingCandidate;
import com.zqzqq.bootkits.openclaw.protocol.TaskRoutingDecision;
import com.zqzqq.bootkits.openclaw.protocol.TaskRetryRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenClawControlService {

    private static final Logger log = LoggerFactory.getLogger(OpenClawControlService.class);

    private final ClientStateStore clientStateStore;
    private final TaskStateStore taskStateStore;
    private final TaskRoutingPolicy taskRoutingPolicy;
    private final List<TaskLifecycleListener> taskLifecycleListeners;
    private final Duration staleAfter;
    private final Duration offlineAfter;
    private final long heartbeatIntervalSeconds;
    private final long defaultTaskLeaseSeconds;

    public OpenClawControlService(ClientStateStore clientStateStore,
                                  TaskStateStore taskStateStore,
                                  TaskRoutingPolicy taskRoutingPolicy,
                                  Duration staleAfter,
                                  Duration offlineAfter,
                                  long heartbeatIntervalSeconds) {
        this(clientStateStore, taskStateStore, taskRoutingPolicy, List.of(), staleAfter, offlineAfter, heartbeatIntervalSeconds, 300L);
    }

    public OpenClawControlService(ClientStateStore clientStateStore,
                                  TaskStateStore taskStateStore,
                                  TaskRoutingPolicy taskRoutingPolicy,
                                  Duration staleAfter,
                                  Duration offlineAfter,
                                  long heartbeatIntervalSeconds,
                                  long defaultTaskLeaseSeconds) {
        this(clientStateStore, taskStateStore, taskRoutingPolicy, List.of(), staleAfter, offlineAfter, heartbeatIntervalSeconds, defaultTaskLeaseSeconds);
    }

    public OpenClawControlService(ClientStateStore clientStateStore,
                                  TaskStateStore taskStateStore,
                                  TaskRoutingPolicy taskRoutingPolicy,
                                  List<TaskLifecycleListener> taskLifecycleListeners,
                                  Duration staleAfter,
                                  Duration offlineAfter,
                                  long heartbeatIntervalSeconds,
                                  long defaultTaskLeaseSeconds) {
        this.clientStateStore = Objects.requireNonNull(clientStateStore, "clientStateStore");
        this.taskStateStore = Objects.requireNonNull(taskStateStore, "taskStateStore");
        this.taskRoutingPolicy = Objects.requireNonNull(taskRoutingPolicy, "taskRoutingPolicy");
        this.taskLifecycleListeners = taskLifecycleListeners == null ? List.of() : List.copyOf(taskLifecycleListeners);
        this.staleAfter = staleAfter == null ? Duration.ofSeconds(90) : staleAfter;
        this.offlineAfter = offlineAfter == null ? Duration.ofMinutes(5) : offlineAfter;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds <= 0 ? 30L : heartbeatIntervalSeconds;
        this.defaultTaskLeaseSeconds = defaultTaskLeaseSeconds <= 0 ? 300L : defaultTaskLeaseSeconds;
    }

    public synchronized ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
        if (request == null || isBlank(request.getClientId())) {
            throw new ControlPlaneException("clientId is required");
        }
        Instant now = Instant.now();
        ClientSnapshot snapshot = clientStateStore.findByClientId(request.getClientId());
        if (snapshot == null) {
            snapshot = new ClientSnapshot();
            snapshot.setRegisteredAt(now);
        }
        snapshot.setClientId(request.getClientId());
        snapshot.setMachineId(request.getMachineId());
        snapshot.setDisplayName(isBlank(request.getDisplayName()) ? request.getClientId() : request.getDisplayName());
        snapshot.setVersion(request.getVersion());
        snapshot.setSdkVersion(request.getSdkVersion());
        snapshot.setHostName(request.getHostName());
        snapshot.setOsName(request.getOsName());
        snapshot.setOsVersion(request.getOsVersion());
        snapshot.setTags(request.getTags());
        snapshot.setAttributes(request.getAttributes());
        snapshot.setCapabilities(copyCapabilities(request.getCapabilities()));
        snapshot.setSessionId(UUID.randomUUID().toString());
        snapshot.setLastSeenAt(now);
        snapshot.setUpdatedAt(now);
        snapshot.setStatus(ClientStatus.ONLINE);
        clientStateStore.save(snapshot);

        ClientRegistrationResponse response = new ClientRegistrationResponse();
        response.setAccepted(true);
        response.setMessage("client registered");
        response.setSessionId(snapshot.getSessionId());
        response.setServerTime(now);
        response.setHeartbeatIntervalSeconds(heartbeatIntervalSeconds);
        response.setClient(resolveClient(snapshot));
        return response;
    }

    public synchronized ClientHeartbeatResponse heartbeat(String clientId, ClientHeartbeatRequest request) {
        if (isBlank(clientId)) {
            throw new ControlPlaneException("clientId is required");
        }
        ClientSnapshot snapshot = mustFindClient(clientId);
        if (request != null && !isBlank(request.getSessionId())
                && !Objects.equals(request.getSessionId(), snapshot.getSessionId())) {
            throw new ControlPlaneException("sessionId mismatch");
        }
        Instant now = Instant.now();
        if (request != null) {
            snapshot.setMetrics(request.getMetrics());
            snapshot.setCurrentTaskIds(request.getCurrentTaskIds());
            if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
                snapshot.setCapabilities(copyCapabilities(request.getCapabilities()));
            }
        }
        snapshot.setLastSeenAt(now);
        snapshot.setUpdatedAt(now);
        snapshot.setStatus(ClientStatus.ONLINE);
        clientStateStore.save(snapshot);

        ClientHeartbeatResponse response = new ClientHeartbeatResponse();
        response.setAccepted(true);
        response.setMessage("heartbeat accepted");
        response.setServerTime(now);
        response.setNextHeartbeatIntervalSeconds(heartbeatIntervalSeconds);
        response.setQueuedTaskCount(countQueuedTasks(clientId));
        response.setClientStatus(resolveStatus(snapshot, now));
        return response;
    }

    public synchronized List<ClientSnapshot> listClients() {
        Instant now = Instant.now();
        return clientStateStore.findAll().stream()
                .map(snapshot -> resolveClient(snapshot, now))
                .sorted(Comparator.comparing(ClientSnapshot::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public synchronized ClientSnapshot getClient(String clientId) {
        return resolveClient(mustFindClient(clientId));
    }

    public synchronized TaskSnapshot dispatchTask(TaskDispatchRequest request) {
        if (request == null || isBlank(request.getTaskType())) {
            throw new ControlPlaneException("taskType is required");
        }
        ClientSnapshot target = selectTargetClient(request);
        Instant now = Instant.now();

        TaskSnapshot task = new TaskSnapshot();
        task.setTaskId("oc-task-" + UUID.randomUUID());
        task.setTaskType(request.getTaskType());
        task.setRequestedBy(request.getRequestedBy());
        task.setAssignedClientId(target.getClientId());
        task.setStatus(TaskStatus.QUEUED);
        task.setRequiredCapabilities(request.getRequiredCapabilities());
        task.setPayload(request.getPayload());
        task.setMetadata(request.getMetadata());
        task.setTimeoutSeconds(request.getTimeoutSeconds());
        task.setLeaseSeconds(normalizeLeaseSeconds(request.getLeaseSeconds()));
        task.setRetryCount(0);
        task.setMaxRetries(request.getMaxRetries() == null || request.getMaxRetries() < 0 ? 0 : request.getMaxRetries());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskStateStore.save(task);
        TaskSnapshot dispatched = new TaskSnapshot(task);
        notifyTaskDispatched(dispatched);

        log.info("Dispatched control task {} to client {} ({})",
                task.getTaskId(), target.getClientId(), task.getTaskType());
        return dispatched;
    }

    public synchronized TaskClaimResponse claimNextTask(String clientId) {
        return claimNextTask(clientId, null);
    }

    public synchronized TaskClaimResponse claimNextTask(String clientId, TaskClaimRequest request) {
        ClientSnapshot client = mustFindClient(clientId);
        TaskSnapshot nextTask = taskStateStore.findAll().stream()
                .filter(task -> Objects.equals(task.getAssignedClientId(), clientId))
                .filter(task -> task.getStatus() == TaskStatus.QUEUED)
                .sorted(Comparator.comparing(TaskSnapshot::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);

        TaskClaimResponse response = new TaskClaimResponse();
        response.setAccepted(true);
        response.setServerTime(Instant.now());
        if (nextTask == null) {
            response.setMessage("no queued task");
            return response;
        }

        nextTask.setStatus(TaskStatus.CLAIMED);
        Instant now = Instant.now();
        nextTask.setClaimedAt(now);
        nextTask.setUpdatedAt(now);
        nextTask.setLeaseExpiresAt(now.plusSeconds(normalizeLeaseSeconds(
                request == null ? null : request.getRequestedLeaseSeconds(),
                nextTask.getLeaseSeconds()
        )));
        taskStateStore.save(nextTask);

        List<String> taskIds = new ArrayList<>(client.getCurrentTaskIds());
        if (!taskIds.contains(nextTask.getTaskId())) {
            taskIds.add(nextTask.getTaskId());
        }
        client.setCurrentTaskIds(taskIds);
        client.setUpdatedAt(Instant.now());
        clientStateStore.save(client);
        notifyTaskClaimed(new TaskSnapshot(nextTask), resolveClient(client));

        response.setMessage("task claimed");
        response.setTask(new TaskSnapshot(nextTask));
        response.setLeaseExpiresAt(nextTask.getLeaseExpiresAt());
        return response;
    }

    public synchronized TaskSnapshot reportProgress(String taskId, TaskProgressReport report) {
        TaskSnapshot task = mustFindTask(taskId);
        TaskStatus nextStatus = report == null || report.getStatus() == null ? TaskStatus.RUNNING : report.getStatus();
        if (isFinalStatus(nextStatus)) {
            throw new ControlPlaneException("progress endpoint does not accept final status");
        }
        task.setStatus(nextStatus);
        task.setMessage(report == null ? null : report.getMessage());
        task.setProgressPercent(report == null ? null : report.getProgressPercent());
        task.setDetails(report == null ? task.getDetails() : report.getDetails());
        task.setMetrics(report == null ? task.getMetrics() : report.getMetrics());
        task.setArtifacts(report == null ? task.getArtifacts() : report.getArtifacts());
        if (task.getStartedAt() == null) {
            task.setStartedAt(report != null && report.getOccurredAt() != null ? report.getOccurredAt() : Instant.now());
        }
        task.setUpdatedAt(report != null && report.getOccurredAt() != null ? report.getOccurredAt() : Instant.now());
        if (task.getLeaseExpiresAt() == null) {
            task.setLeaseExpiresAt(task.getUpdatedAt().plusSeconds(normalizeLeaseSeconds(task.getLeaseSeconds())));
        }
        taskStateStore.save(task);
        TaskSnapshot updated = new TaskSnapshot(task);
        notifyTaskUpdated(updated);
        return updated;
    }

    public synchronized TaskSnapshot completeTask(String taskId, TaskResultReport report) {
        TaskSnapshot task = mustFindTask(taskId);
        TaskStatus finalStatus = report == null || report.getStatus() == null ? TaskStatus.SUCCEEDED : report.getStatus();
        if (!isFinalStatus(finalStatus)) {
            throw new ControlPlaneException("result endpoint requires final task status");
        }
        task.setStatus(finalStatus);
        task.setMessage(report == null ? null : report.getMessage());
        task.setOutput(report == null ? null : report.getOutput());
        task.setError(report == null ? null : report.getError());
        task.setResult(report == null ? task.getResult() : report.getResult());
        task.setArtifacts(report == null ? task.getArtifacts() : report.getArtifacts());
        if (task.getStartedAt() == null) {
            task.setStartedAt(report != null && report.getOccurredAt() != null ? report.getOccurredAt() : Instant.now());
        }
        task.setFinishedAt(report != null && report.getOccurredAt() != null ? report.getOccurredAt() : Instant.now());
        task.setUpdatedAt(task.getFinishedAt());
        taskStateStore.save(task);

        ClientSnapshot client = clientStateStore.findByClientId(task.getAssignedClientId());
        if (client != null) {
            List<String> currentTaskIds = client.getCurrentTaskIds().stream()
                    .filter(id -> !Objects.equals(id, task.getTaskId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            client.setCurrentTaskIds(currentTaskIds);
            client.setUpdatedAt(Instant.now());
            clientStateStore.save(client);
        }
        TaskSnapshot updated = new TaskSnapshot(task);
        notifyTaskUpdated(updated);
        return updated;
    }

    public synchronized TaskSnapshot renewTaskLease(String taskId, TaskLeaseRenewRequest request) {
        TaskSnapshot task = mustFindTask(taskId);
        if (task.getStatus() != TaskStatus.CLAIMED && task.getStatus() != TaskStatus.RUNNING) {
            throw new ControlPlaneException("task lease can only be renewed for claimed or running tasks");
        }
        if (request != null && !isBlank(request.getClientId())
                && !Objects.equals(request.getClientId(), task.getAssignedClientId())) {
            throw new ControlPlaneException("task client mismatch");
        }
        Instant now = Instant.now();
        task.setLeaseExpiresAt(now.plusSeconds(normalizeLeaseSeconds(
                request == null ? null : request.getLeaseSeconds(),
                task.getLeaseSeconds()
        )));
        task.setUpdatedAt(now);
        taskStateStore.save(task);
        TaskSnapshot updated = new TaskSnapshot(task);
        notifyTaskUpdated(updated);
        return updated;
    }

    public synchronized TaskSnapshot cancelTask(String taskId, TaskCancelRequest request) {
        TaskSnapshot task = mustFindTask(taskId);
        if (isFinalStatus(task.getStatus())) {
            return new TaskSnapshot(task);
        }
        Instant now = Instant.now();
        task.setStatus(TaskStatus.CANCELLED);
        task.setCancelledBy(request == null || isBlank(request.getRequestedBy()) ? "system" : request.getRequestedBy());
        task.setCancelRequestedAt(now);
        task.setFinishedAt(now);
        task.setUpdatedAt(now);
        if (request != null && !isBlank(request.getReason())) {
            task.setMessage(request.getReason());
        }
        taskStateStore.save(task);
        removeTaskFromClient(task);
        TaskSnapshot updated = new TaskSnapshot(task);
        notifyTaskUpdated(updated);
        return updated;
    }

    public synchronized TaskSnapshot retryTask(String taskId, TaskRetryRequest request) {
        TaskSnapshot previous = mustFindTask(taskId);
        if (!isRetryableStatus(previous.getStatus())) {
            throw new ControlPlaneException("task is not retryable: " + previous.getStatus());
        }
        int nextRetryCount = previous.getRetryCount() == null ? 1 : previous.getRetryCount() + 1;
        int maxRetries = previous.getMaxRetries() == null ? 0 : previous.getMaxRetries();
        if (maxRetries > 0 && nextRetryCount > maxRetries) {
            throw new ControlPlaneException("task retry limit exceeded");
        }

        ClientSnapshot target = resolveRetryTarget(previous, request);
        Instant now = Instant.now();
        TaskSnapshot retried = new TaskSnapshot(previous);
        retried.setTaskId("oc-task-" + UUID.randomUUID());
        retried.setAssignedClientId(target.getClientId());
        retried.setStatus(TaskStatus.QUEUED);
        retried.setRetryCount(nextRetryCount);
        retried.setParentTaskId(previous.getTaskId());
        retried.setLeaseSeconds(normalizeLeaseSeconds(request == null ? null : request.getLeaseSeconds(), previous.getLeaseSeconds()));
        retried.setClaimedAt(null);
        retried.setStartedAt(null);
        retried.setFinishedAt(null);
        retried.setLeaseExpiresAt(null);
        retried.setCancelRequestedAt(null);
        retried.setCancelledBy(null);
        retried.setProgressPercent(null);
        retried.setOutput(null);
        retried.setError(null);
        retried.setArtifacts(new ArrayList<>());
        retried.setDetails(new java.util.LinkedHashMap<>());
        retried.setMetrics(new java.util.LinkedHashMap<>());
        retried.setResult(new java.util.LinkedHashMap<>());
        retried.setCreatedAt(now);
        retried.setUpdatedAt(now);
        if (request != null && !isBlank(request.getReason())) {
            retried.setMessage(request.getReason());
        }
        taskStateStore.save(retried);
        TaskSnapshot dispatched = new TaskSnapshot(retried);
        notifyTaskDispatched(dispatched);
        return dispatched;
    }

    public synchronized List<TaskSnapshot> listTasks() {
        return taskStateStore.findAll().stream()
                .map(TaskSnapshot::new)
                .sorted(Comparator.comparing(TaskSnapshot::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public synchronized TaskSnapshot getTask(String taskId) {
        return new TaskSnapshot(mustFindTask(taskId));
    }

    public synchronized TaskRoutingDecision previewTaskRouting(TaskDispatchRequest request) {
        if (request == null || isBlank(request.getTaskType())) {
            throw new ControlPlaneException("taskType is required");
        }
        List<RoutingCandidateContext> candidates = buildRoutingCandidates(request, Set.of());
        TaskRoutingDecision decision = new TaskRoutingDecision();
        decision.setTaskType(request.getTaskType());
        decision.setRequiredCapabilities(request.getRequiredCapabilities());
        decision.setPolicy(taskRoutingPolicy.getClass().getSimpleName());
        decision.setCandidates(candidates.stream()
                .map(RoutingCandidateContext::candidate)
                .collect(Collectors.toList()));
        candidates.stream()
                .filter(context -> context.candidate().isEligible())
                .findFirst()
                .ifPresent(context -> {
                    decision.setSelectedClientId(context.client().getClientId());
                    decision.setSelectedScore(context.candidate().getScore());
                });
        return decision;
    }

    private ClientSnapshot selectTargetClient(TaskDispatchRequest request) {
        return selectTargetClient(request, Set.of());
    }

    private ClientSnapshot selectTargetClient(TaskDispatchRequest request, Set<String> excludedClientIds) {
        List<RoutingCandidateContext> candidates = buildRoutingCandidates(request, excludedClientIds);
        RoutingCandidateContext selected = candidates.stream()
                .filter(context -> context.candidate().isEligible())
                .findFirst()
                .orElse(null);
        if (selected == null) {
            throw new ControlPlaneException("no eligible client found for task dispatch");
        }
        return selected.client();
    }

    private boolean matchesTags(ClientSnapshot client, Set<String> targetTags) {
        if (targetTags == null || targetTags.isEmpty()) {
            return true;
        }
        return client.getTags().containsAll(targetTags);
    }

    private boolean matchesCapabilities(ClientSnapshot client, Set<String> requiredCapabilities) {
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            return true;
        }
        Set<String> provided = client.getCapabilities().stream()
                .filter(capability -> Boolean.TRUE.equals(capability.getEnabled()))
                .map(ClientCapabilityDescriptor::getCapabilityId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return provided.containsAll(requiredCapabilities);
    }

    private int countQueuedTasks(String clientId) {
        return (int) taskStateStore.findAll().stream()
                .filter(task -> Objects.equals(task.getAssignedClientId(), clientId))
                .filter(task -> task.getStatus() == TaskStatus.QUEUED)
                .count();
    }

    private int countOpenTasks(String clientId) {
        return (int) taskStateStore.findAll().stream()
                .filter(task -> Objects.equals(task.getAssignedClientId(), clientId))
                .filter(task -> !isFinalStatus(task.getStatus()))
                .count();
    }

    private List<RoutingCandidateContext> buildRoutingCandidates(TaskDispatchRequest request, Set<String> excludedClientIds) {
        return listClients().stream()
                .map(client -> evaluateRoutingCandidate(client, request, excludedClientIds))
                .sorted(Comparator
                        .comparing((RoutingCandidateContext context) -> context.candidate().isEligible(),
                                Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingInt(
                                (RoutingCandidateContext context) -> context.candidate().getScore()).reversed())
                        .thenComparingInt(context -> context.candidate().getOpenTaskCount())
                        .thenComparing(context -> context.client().getLastSeenAt(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(context -> context.client().getRegisteredAt(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private RoutingCandidateContext evaluateRoutingCandidate(ClientSnapshot client,
                                                             TaskDispatchRequest request,
                                                             Set<String> excludedClientIds) {
        TaskRoutingCandidate candidate = new TaskRoutingCandidate();
        candidate.setClientId(client.getClientId());
        candidate.setDisplayName(client.getDisplayName());
        candidate.setStatus(client.getStatus());
        candidate.setOpenTaskCount(countOpenTasks(client.getClientId()));

        List<String> reasons = new ArrayList<>();
        Set<String> matchedCapabilities = matchedCapabilities(client, request);
        if (!matchedCapabilities.isEmpty()) {
            reasons.add("matched capabilities: " + matchedCapabilities);
        }

        boolean eligible = true;
        if (request.getTargetClientId() != null && !Objects.equals(request.getTargetClientId(), client.getClientId())) {
            eligible = false;
            reasons.add("targetClientId mismatch");
        }
        if (eligible && excludedClientIds != null && excludedClientIds.contains(client.getClientId())) {
            eligible = false;
            reasons.add("client excluded from routing");
        }
        if (eligible && !matchesTags(client, request.getTargetTags())) {
            eligible = false;
            reasons.add("target tags mismatch");
        } else if (eligible && request.getTargetTags() != null && !request.getTargetTags().isEmpty()) {
            reasons.add("target tags matched: " + request.getTargetTags());
        }
        if (eligible && !matchesCapabilities(client, request.getRequiredCapabilities())) {
            eligible = false;
            reasons.add("required capabilities mismatch");
        }
        if (eligible && !Boolean.TRUE.equals(request.getAllowStaleClient()) && client.getStatus() != ClientStatus.ONLINE) {
            eligible = false;
            reasons.add("client is not online");
        }
        if (eligible && !taskRoutingPolicy.supports(client, request)) {
            eligible = false;
            reasons.add("routing policy rejected client");
        }

        candidate.setMatchedCapabilities(matchedCapabilities);
        candidate.setEligible(eligible);
        candidate.setScore(eligible ? taskRoutingPolicy.score(client, request) : Integer.MIN_VALUE);
        if (eligible && request.getTaskType() != null) {
            String taskTypeCapability = matchTaskTypeCapability(client, request.getTaskType());
            if (taskTypeCapability != null) {
                reasons.add("taskType affinity matched: " + taskTypeCapability);
            }
        }
        if (eligible) {
            reasons.add("routing score=" + candidate.getScore());
        }
        candidate.setReasons(reasons);
        return new RoutingCandidateContext(client, candidate);
    }

    private ClientSnapshot mustFindClient(String clientId) {
        ClientSnapshot snapshot = clientStateStore.findByClientId(clientId);
        if (snapshot == null) {
            throw new ControlPlaneException("client not found: " + clientId);
        }
        return snapshot;
    }

    private TaskSnapshot mustFindTask(String taskId) {
        TaskSnapshot snapshot = taskStateStore.findByTaskId(taskId);
        if (snapshot == null) {
            throw new ControlPlaneException("task not found: " + taskId);
        }
        return snapshot;
    }

    private ClientSnapshot resolveClient(ClientSnapshot snapshot) {
        return resolveClient(snapshot, Instant.now());
    }

    private ClientSnapshot resolveClient(ClientSnapshot snapshot, Instant now) {
        ClientSnapshot copy = new ClientSnapshot(snapshot);
        copy.setStatus(resolveStatus(copy, now));
        return copy;
    }

    private ClientStatus resolveStatus(ClientSnapshot snapshot, Instant now) {
        if (snapshot == null || snapshot.getLastSeenAt() == null) {
            return ClientStatus.REGISTERED;
        }
        Duration age = Duration.between(snapshot.getLastSeenAt(), now);
        if (age.compareTo(offlineAfter) >= 0) {
            return ClientStatus.OFFLINE;
        }
        if (age.compareTo(staleAfter) >= 0) {
            return ClientStatus.STALE;
        }
        return ClientStatus.ONLINE;
    }

    private List<ClientCapabilityDescriptor> copyCapabilities(List<ClientCapabilityDescriptor> capabilities) {
        List<ClientCapabilityDescriptor> result = new ArrayList<>();
        if (capabilities == null) {
            return result;
        }
        for (ClientCapabilityDescriptor capability : capabilities) {
            result.add(new ClientCapabilityDescriptor(capability));
        }
        return result;
    }

    private boolean isFinalStatus(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED;
    }

    private boolean isRetryableStatus(TaskStatus status) {
        return status == TaskStatus.FAILED || status == TaskStatus.CANCELLED;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long normalizeLeaseSeconds(Long requestedLeaseSeconds) {
        return normalizeLeaseSeconds(requestedLeaseSeconds, null);
    }

    private long normalizeLeaseSeconds(Long requestedLeaseSeconds, Long fallbackLeaseSeconds) {
        if (requestedLeaseSeconds != null && requestedLeaseSeconds > 0) {
            return requestedLeaseSeconds;
        }
        if (fallbackLeaseSeconds != null && fallbackLeaseSeconds > 0) {
            return fallbackLeaseSeconds;
        }
        return defaultTaskLeaseSeconds;
    }

    private ClientSnapshot resolveRetryTarget(TaskSnapshot previous, TaskRetryRequest request) {
        String targetClientId = request == null ? null : request.getTargetClientId();
        if (!isBlank(targetClientId)) {
            ClientSnapshot client = mustFindClient(targetClientId);
            if (client.getStatus() != ClientStatus.ONLINE) {
                throw new ControlPlaneException("retry target client is not online: " + targetClientId);
            }
            return client;
        }
        TaskDispatchRequest dispatchRequest = new TaskDispatchRequest();
        dispatchRequest.setTaskType(previous.getTaskType());
        dispatchRequest.setRequiredCapabilities(previous.getRequiredCapabilities());
        ClientSnapshot previousClient = isBlank(previous.getAssignedClientId())
                ? null
                : clientStateStore.findByClientId(previous.getAssignedClientId());
        if (previousClient != null) {
            dispatchRequest.setTargetTags(previousClient.getTags());
        }
        Set<String> excludedClientIds = new HashSet<>();
        if (!isBlank(previous.getAssignedClientId())) {
            excludedClientIds.add(previous.getAssignedClientId());
        }
        try {
            return selectTargetClient(dispatchRequest, excludedClientIds);
        } catch (ControlPlaneException ex) {
            if (excludedClientIds.isEmpty()) {
                throw ex;
            }
            return selectTargetClient(dispatchRequest);
        }
    }

    private Set<String> matchedCapabilities(ClientSnapshot client, TaskDispatchRequest request) {
        Set<String> requiredCapabilities = request == null ? Set.of() : request.getRequiredCapabilities();
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            String taskTypeCapability = matchTaskTypeCapability(client, request == null ? null : request.getTaskType());
            return taskTypeCapability == null ? Set.of() : Set.of(taskTypeCapability);
        }

        Set<String> provided = client.getCapabilities().stream()
                .filter(capability -> capability != null && Boolean.TRUE.equals(capability.getEnabled()))
                .map(ClientCapabilityDescriptor::getCapabilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> matched = new LinkedHashSet<>();
        for (String requiredCapability : requiredCapabilities) {
            if (provided.contains(requiredCapability)) {
                matched.add(requiredCapability);
            }
        }
        return matched;
    }

    private String matchTaskTypeCapability(ClientSnapshot client, String taskType) {
        if (client == null || taskType == null || taskType.isBlank()) {
            return null;
        }
        Map<String, ClientCapabilityDescriptor> provided = new LinkedHashMap<>();
        for (ClientCapabilityDescriptor capability : client.getCapabilities()) {
            if (capability == null || !Boolean.TRUE.equals(capability.getEnabled())) {
                continue;
            }
            if (capability.getCapabilityId() != null && !capability.getCapabilityId().isBlank()) {
                provided.put(capability.getCapabilityId(), capability);
            }
        }
        String current = taskType;
        while (current != null && !current.isBlank()) {
            if (provided.containsKey(current)) {
                return current;
            }
            int index = current.lastIndexOf('.');
            if (index < 0) {
                break;
            }
            current = current.substring(0, index);
        }
        return null;
    }

    private void removeTaskFromClient(TaskSnapshot task) {
        if (task == null || isBlank(task.getAssignedClientId())) {
            return;
        }
        ClientSnapshot client = clientStateStore.findByClientId(task.getAssignedClientId());
        if (client == null) {
            return;
        }
        List<String> currentTaskIds = client.getCurrentTaskIds().stream()
                .filter(id -> !Objects.equals(id, task.getTaskId()))
                .collect(Collectors.toCollection(ArrayList::new));
        client.setCurrentTaskIds(currentTaskIds);
        client.setUpdatedAt(Instant.now());
        clientStateStore.save(client);
    }

    private void notifyTaskDispatched(TaskSnapshot task) {
        for (TaskLifecycleListener listener : taskLifecycleListeners) {
            try {
                listener.onTaskDispatched(new TaskSnapshot(task));
            } catch (Exception ex) {
                log.warn("Task dispatch listener failed for task {}", task.getTaskId(), ex);
            }
        }
    }

    private void notifyTaskClaimed(TaskSnapshot task, ClientSnapshot client) {
        for (TaskLifecycleListener listener : taskLifecycleListeners) {
            try {
                listener.onTaskClaimed(new TaskSnapshot(task), new ClientSnapshot(client));
            } catch (Exception ex) {
                log.warn("Task claim listener failed for task {}", task.getTaskId(), ex);
            }
        }
    }

    private void notifyTaskUpdated(TaskSnapshot task) {
        for (TaskLifecycleListener listener : taskLifecycleListeners) {
            try {
                listener.onTaskUpdated(new TaskSnapshot(task));
            } catch (Exception ex) {
                log.warn("Task update listener failed for task {}", task.getTaskId(), ex);
            }
        }
    }

    private record RoutingCandidateContext(ClientSnapshot client, TaskRoutingCandidate candidate) {
    }
}
