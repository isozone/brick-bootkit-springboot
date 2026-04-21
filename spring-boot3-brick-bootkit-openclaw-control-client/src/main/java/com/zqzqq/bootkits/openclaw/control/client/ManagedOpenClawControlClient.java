package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagedOpenClawControlClient implements AutoCloseable {

    private final OpenClawControlClient delegate;
    private final OpenClawControlClientProperties properties;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> heartbeatFuture;
    private volatile OpenClawHeartbeatSupplier heartbeatSupplier;

    public ManagedOpenClawControlClient(OpenClawControlClient delegate,
                                        OpenClawControlClientProperties properties) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "openclaw-control-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    public ClientRegistrationResponse start(ClientRegistrationRequest registrationRequest,
                                            OpenClawHeartbeatSupplier heartbeatSupplier,
                                            OpenClawControlMessageListener messageListener) {
        if (!started.compareAndSet(false, true)) {
            throw new OpenClawControlClientException("managed control client already started");
        }
        this.heartbeatSupplier = heartbeatSupplier;
        ClientRegistrationResponse response;
        if (properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET) {
            delegate.connectWebSocket(messageListener);
            response = delegate.registerClientOverWebSocket(registrationRequest);
        } else {
            if (messageListener != null) {
                delegate.connectWebSocket(messageListener);
            }
            response = delegate.registerClient(registrationRequest);
        }
        long heartbeatSeconds = response == null || response.getHeartbeatIntervalSeconds() <= 0
                ? properties.getHeartbeatInterval().toSeconds()
                : response.getHeartbeatIntervalSeconds();
        scheduleHeartbeat(Duration.ofSeconds(heartbeatSeconds));
        return response;
    }

    public TaskClaimResponse claimNextTask() {
        TaskClaimRequest request = new TaskClaimRequest();
        return properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET
                ? delegate.claimNextTaskOverWebSocket(request)
                : delegate.claimNextTask(request);
    }

    public TaskSnapshot renewTaskLease(String taskId, Long leaseSeconds) {
        TaskLeaseRenewRequest request = new TaskLeaseRenewRequest();
        request.setLeaseSeconds(leaseSeconds);
        return properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET
                ? delegate.renewTaskLeaseOverWebSocket(taskId, request)
                : delegate.renewTaskLease(taskId, request);
    }

    public TaskSnapshot reportProgress(String taskId, TaskProgressReport report) {
        return properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET
                ? delegate.reportProgressOverWebSocket(taskId, report)
                : delegate.reportProgress(taskId, report);
    }

    public TaskSnapshot completeTask(String taskId, TaskResultReport report) {
        return properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET
                ? delegate.completeTaskOverWebSocket(taskId, report)
                : delegate.completeTask(taskId, report);
    }

    public void heartbeatNow() {
        if (heartbeatSupplier == null) {
            return;
        }
        ClientHeartbeatRequest heartbeatRequest = heartbeatSupplier.get();
        if (properties.getPreferredTransport() == OpenClawControlTransport.WEBSOCKET && delegate.isWebSocketConnected()) {
            delegate.heartbeatOverWebSocket(heartbeatRequest);
            return;
        }
        delegate.heartbeat(heartbeatRequest);
    }

    @Override
    public void close() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        scheduler.shutdownNow();
        delegate.close();
        started.set(false);
    }

    private void scheduleHeartbeat(Duration interval) {
        long delayMillis = Math.max(1_000L, interval.toMillis());
        heartbeatFuture = scheduler.scheduleAtFixedRate(this::heartbeatSafely, delayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void heartbeatSafely() {
        try {
            heartbeatNow();
        } catch (Exception ex) {
            throw new OpenClawControlClientException("heartbeat execution failed", ex);
        }
    }
}
