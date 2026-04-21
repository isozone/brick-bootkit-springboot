package com.zqzqq.bootkits.openclaw.protocol;

public class TaskLeaseRenewCommand {

    private String taskId;
    private TaskLeaseRenewRequest request;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskLeaseRenewRequest getRequest() {
        return request;
    }

    public void setRequest(TaskLeaseRenewRequest request) {
        this.request = request;
    }
}
