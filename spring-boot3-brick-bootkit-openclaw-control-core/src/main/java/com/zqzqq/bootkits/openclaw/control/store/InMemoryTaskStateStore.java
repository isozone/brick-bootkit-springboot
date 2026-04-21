package com.zqzqq.bootkits.openclaw.control.store;

import com.zqzqq.bootkits.openclaw.control.spi.TaskStateStore;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTaskStateStore implements TaskStateStore {

    private final ConcurrentMap<String, TaskSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public TaskSnapshot findByTaskId(String taskId) {
        TaskSnapshot snapshot = store.get(taskId);
        return snapshot == null ? null : new TaskSnapshot(snapshot);
    }

    @Override
    public void save(TaskSnapshot snapshot) {
        if (snapshot == null || snapshot.getTaskId() == null || snapshot.getTaskId().isBlank()) {
            return;
        }
        store.put(snapshot.getTaskId(), new TaskSnapshot(snapshot));
    }

    @Override
    public List<TaskSnapshot> findAll() {
        List<TaskSnapshot> result = new ArrayList<>();
        for (TaskSnapshot snapshot : store.values()) {
            result.add(new TaskSnapshot(snapshot));
        }
        return result;
    }
}
