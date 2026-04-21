package com.zqzqq.bootkits.openclaw.control.spi;

import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

public interface TaskLifecycleListener {

    default void onTaskDispatched(TaskSnapshot task) {
    }

    default void onTaskClaimed(TaskSnapshot task, ClientSnapshot client) {
    }

    default void onTaskUpdated(TaskSnapshot task) {
    }
}
