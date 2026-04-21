package com.zqzqq.bootkits.openclaw.control.spi;

import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

import java.util.List;

public interface TaskStateStore {

    TaskSnapshot findByTaskId(String taskId);

    void save(TaskSnapshot snapshot);

    List<TaskSnapshot> findAll();
}
