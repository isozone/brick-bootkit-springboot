package com.zqzqq.bootkits.openclaw.protocol;

public class TaskResultCommand {

    private String taskId;
    private TaskResultReport report;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskResultReport getReport() {
        return report;
    }

    public void setReport(TaskResultReport report) {
        this.report = report;
    }
}
