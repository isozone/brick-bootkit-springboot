package com.zqzqq.bootkits.openclaw.control.storage.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.control.ControlPlaneException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

abstract class AbstractJdbcSnapshotStore<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    protected AbstractJdbcSnapshotStore(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
    }

    protected String toJson(T snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new ControlPlaneException("failed to serialize snapshot: " + ex.getMessage(), ex);
        }
    }

    protected T fromJson(String json) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException ex) {
            throw new ControlPlaneException("failed to deserialize snapshot: " + ex.getMessage(), ex);
        }
    }

    protected Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    protected Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
