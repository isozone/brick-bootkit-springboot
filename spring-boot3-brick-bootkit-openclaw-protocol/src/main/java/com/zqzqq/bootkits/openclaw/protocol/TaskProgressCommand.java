package com.zqzqq.bootkits.openclaw.protocol;

public class TaskProgressCommand {

    private String taskId;
    private TaskProgressReport report;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskProgressReport getReport() {
        return report;
    }

    public void setReport(TaskProgressReport report) {
        this.report = report;
    }
}
