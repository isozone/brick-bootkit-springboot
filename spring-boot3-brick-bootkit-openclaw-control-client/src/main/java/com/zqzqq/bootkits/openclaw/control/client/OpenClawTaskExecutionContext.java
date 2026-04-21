package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.TaskProgressReport;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

public interface OpenClawTaskExecutionContext {

    TaskSnapshot getTask();

    boolean isCancellationRequested();

    void reportProgress(TaskProgressReport report);
}
