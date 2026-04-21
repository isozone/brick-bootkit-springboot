package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.ManagedOpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawAgentRuntime;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlMessageListener;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawHeartbeatSupplier;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import org.springframework.context.SmartLifecycle;

public class OpenClawAgentLifecycle implements SmartLifecycle {

    private final ManagedOpenClawControlClient managedClient;
    private final OpenClawAgentRuntime agentRuntime;
    private final ClientRegistrationRequest registrationRequest;
    private final OpenClawHeartbeatSupplier heartbeatSupplier;
    private final OpenClawControlMessageListener messageListener;
    private final boolean autoStartup;
    private volatile boolean running;

    public OpenClawAgentLifecycle(ManagedOpenClawControlClient managedClient,
                                  OpenClawAgentRuntime agentRuntime,
                                  ClientRegistrationRequest registrationRequest,
                                  OpenClawHeartbeatSupplier heartbeatSupplier,
                                  OpenClawControlMessageListener messageListener,
                                  boolean autoStartup) {
        this.managedClient = managedClient;
        this.agentRuntime = agentRuntime;
        this.registrationRequest = registrationRequest;
        this.heartbeatSupplier = heartbeatSupplier;
        this.messageListener = messageListener;
        this.autoStartup = autoStartup;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        managedClient.start(registrationRequest, heartbeatSupplier, messageListener);
        agentRuntime.startPolling();
        running = true;
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        agentRuntime.close();
        managedClient.close();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
