package com.zqzqq.bootkits.openclaw.control.spi;

import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskDispatchRequest;

public interface TaskRoutingPolicy {

    boolean supports(ClientSnapshot client, TaskDispatchRequest request);

    default int score(ClientSnapshot client, TaskDispatchRequest request) {
        return 0;
    }
}
