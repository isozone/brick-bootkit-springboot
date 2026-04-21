package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawControlService;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawIntegrationRegistry;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationResponse;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.IntegrationSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskCancelRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskClaimResponse;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskLeaseRenewRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskRoutingDecision;
import com.zqzqq.bootkits.openclaw.protocol.TaskRetryRequest;
import com.zqzqq.bootkits.openclaw.protocol.TaskResultReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${openclaw.control.api-base-path:/openclaw/control}")
public class OpenClawControlController {

    private final OpenClawControlService controlService;
    private final OpenClawIntegrationRegistry integrationRegistry;
    private final OpenClawClientAuthVerifier clientAuthVerifier;

    public OpenClawControlController(OpenClawControlService controlService,
                                     OpenClawIntegrationRegistry integrationRegistry,
                                     OpenClawClientAuthVerifier clientAuthVerifier) {
        this.controlService = controlService;
        this.integrationRegistry = integrationRegistry;
        this.clientAuthVerifier = clientAuthVerifier;
    }

    @PostMapping("/clients/register")
    public ClientRegistrationResponse registerClient(@RequestBody ClientRegistrationRequest request,
                                                     HttpServletRequest servletRequest) {
        clientAuthVerifier.verifyRegistration(request, OpenClawClientAuthSupport.extractHeaders(servletRequest));
        return controlService.registerClient(request);
    }

    @PostMapping("/clients/{clientId}/heartbeat")
    public ClientHeartbeatResponse heartbeat(@PathVariable String clientId,
                                             @RequestBody(required = false) ClientHeartbeatRequest request,
                                             HttpServletRequest servletRequest) {
        clientAuthVerifier.verifyClientAccess(
                clientId,
                request == null ? OpenClawClientAuthSupport.header(OpenClawClientAuthSupport.extractHeaders(servletRequest), OpenClawClientAuthSupport.HEADER_SESSION_ID) : request.getSessionId(),
                request,
                OpenClawClientAuthSupport.extractHeaders(servletRequest)
        );
        return controlService.heartbeat(clientId, request);
    }

    @GetMapping("/clients")
    public List<ClientSnapshot> listClients() {
        return controlService.listClients();
    }

    @GetMapping("/clients/{clientId}")
    public ClientSnapshot getClient(@PathVariable String clientId) {
        return controlService.getClient(clientId);
    }

    @PostMapping("/clients/{clientId}/tasks/claim")
    public TaskClaimResponse claimNextTask(@PathVariable String clientId,
                                           @RequestBody(required = false) TaskClaimRequest request,
                                           HttpServletRequest servletRequest) {
        clientAuthVerifier.verifyClientAccess(
                clientId,
                request == null ? OpenClawClientAuthSupport.header(OpenClawClientAuthSupport.extractHeaders(servletRequest), OpenClawClientAuthSupport.HEADER_SESSION_ID) : request.getSessionId(),
                request,
                OpenClawClientAuthSupport.extractHeaders(servletRequest)
        );
        return controlService.claimNextTask(clientId, request);
    }

    @PostMapping("/tasks")
    public TaskSnapshot dispatchTask(@RequestBody TaskDispatchRequest request) {
        return controlService.dispatchTask(request);
    }

    @PostMapping("/tasks/routing-preview")
    public TaskRoutingDecision previewTaskRouting(@RequestBody TaskDispatchRequest request) {
        return controlService.previewTaskRouting(request);
    }

    @GetMapping("/tasks")
    public List<TaskSnapshot> listTasks() {
        return controlService.listTasks();
    }

    @GetMapping("/tasks/{taskId}")
    public TaskSnapshot getTask(@PathVariable String taskId) {
        return controlService.getTask(taskId);
    }

    @PostMapping("/tasks/{taskId}/progress")
    public TaskSnapshot reportProgress(@PathVariable String taskId,
                                       @RequestBody TaskProgressReport report,
                                       HttpServletRequest servletRequest) {
        TaskSnapshot task = controlService.getTask(taskId);
        Map<String, String> authHeaders = OpenClawClientAuthSupport.extractHeaders(servletRequest);
        clientAuthVerifier.verifyClientAccess(
                task.getAssignedClientId(),
                OpenClawClientAuthSupport.header(authHeaders, OpenClawClientAuthSupport.HEADER_SESSION_ID),
                report,
                authHeaders
        );
        return controlService.reportProgress(taskId, report);
    }

    @PostMapping("/tasks/{taskId}/result")
    public TaskSnapshot completeTask(@PathVariable String taskId,
                                     @RequestBody TaskResultReport report,
                                     HttpServletRequest servletRequest) {
        TaskSnapshot task = controlService.getTask(taskId);
        Map<String, String> authHeaders = OpenClawClientAuthSupport.extractHeaders(servletRequest);
        clientAuthVerifier.verifyClientAccess(
                task.getAssignedClientId(),
                OpenClawClientAuthSupport.header(authHeaders, OpenClawClientAuthSupport.HEADER_SESSION_ID),
                report,
                authHeaders
        );
        return controlService.completeTask(taskId, report);
    }

    @PostMapping("/tasks/{taskId}/lease-renew")
    public TaskSnapshot renewTaskLease(@PathVariable String taskId,
                                       @RequestBody(required = false) TaskLeaseRenewRequest request,
                                       HttpServletRequest servletRequest) {
        TaskSnapshot task = controlService.getTask(taskId);
        Map<String, String> authHeaders = OpenClawClientAuthSupport.extractHeaders(servletRequest);
        clientAuthVerifier.verifyClientAccess(
                task.getAssignedClientId(),
                request == null ? OpenClawClientAuthSupport.header(authHeaders, OpenClawClientAuthSupport.HEADER_SESSION_ID) : request.getSessionId(),
                request,
                authHeaders
        );
        return controlService.renewTaskLease(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public TaskSnapshot cancelTask(@PathVariable String taskId,
                                   @RequestBody(required = false) TaskCancelRequest request) {
        return controlService.cancelTask(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/retry")
    public TaskSnapshot retryTask(@PathVariable String taskId,
                                  @RequestBody(required = false) TaskRetryRequest request) {
        return controlService.retryTask(taskId, request);
    }

    @GetMapping("/integrations")
    public List<IntegrationSnapshot> listIntegrations() {
        return integrationRegistry.list();
    }

    @ExceptionHandler(ControlPlaneException.class)
    public ResponseEntity<Map<String, Object>> handleControlPlaneException(ControlPlaneException ex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", "OPENCLAW_CONTROL_ERROR");
        payload.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }
}
