package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

public interface OpenClawTaskHandler {

    boolean supports(TaskSnapshot task);

    OpenClawTaskExecutionResult handle(OpenClawTaskExecutionContext context) throws Exception;
}
