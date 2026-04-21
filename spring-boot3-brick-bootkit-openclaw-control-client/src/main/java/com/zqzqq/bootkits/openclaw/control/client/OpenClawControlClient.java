package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

import java.util.List;

public interface OpenClawControlClient extends AutoCloseable {

    ClientRegistrationResponse registerClient(ClientRegistrationRequest request);

    ClientRegistrationResponse registerClientOverWebSocket(ClientRegistrationRequest request);

    ClientHeartbeatResponse heartbeat(ClientHeartbeatRequest request);

    ClientHeartbeatResponse heartbeatOverWebSocket(ClientHeartbeatRequest request);

    TaskClaimResponse claimNextTask(TaskClaimRequest request);

    TaskClaimResponse claimNextTaskOverWebSocket(TaskClaimRequest request);

    TaskSnapshot renewTaskLease(String taskId, TaskLeaseRenewRequest request);

    TaskSnapshot renewTaskLeaseOverWebSocket(String taskId, TaskLeaseRenewRequest request);

    TaskSnapshot reportProgress(String taskId, TaskProgressReport report);

    TaskSnapshot reportProgressOverWebSocket(String taskId, TaskProgressReport report);

    TaskSnapshot completeTask(String taskId, TaskResultReport report);

    TaskSnapshot completeTaskOverWebSocket(String taskId, TaskResultReport report);

    List<IntegrationSnapshot> listIntegrations();

    void connectWebSocket(OpenClawControlMessageListener listener);

    boolean isWebSocketConnected();

    String getSessionId();

    @Override
    void close();
}
