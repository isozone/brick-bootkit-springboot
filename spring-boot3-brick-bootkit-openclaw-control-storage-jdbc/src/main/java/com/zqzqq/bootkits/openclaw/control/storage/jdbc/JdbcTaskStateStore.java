package com.zqzqq.bootkits.openclaw.control.storage.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.control.spi.TaskStateStore;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcTaskStateStore extends AbstractJdbcSnapshotStore<TaskSnapshot> implements TaskStateStore {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final RowMapper<TaskSnapshot> rowMapper = this::mapRow;

    public JdbcTaskStateStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, String tableName) {
        super(objectMapper, TaskSnapshot.class);
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public TaskSnapshot findByTaskId(String taskId) {
        List<TaskSnapshot> items = jdbcTemplate.query(
                "SELECT snapshot_json FROM " + tableName + " WHERE task_id = ?",
                rowMapper,
                taskId
        );
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public void save(TaskSnapshot snapshot) {
        String json = toJson(snapshot);
        int updated = jdbcTemplate.update(
                "UPDATE " + tableName + " SET task_type = ?, requested_by = ?, assigned_client_id = ?, status = ?, "
                        + "created_at = ?, updated_at = ?, claimed_at = ?, started_at = ?, finished_at = ?, snapshot_json = ? WHERE task_id = ?",
                snapshot.getTaskType(),
                snapshot.getRequestedBy(),
                snapshot.getAssignedClientId(),
                snapshot.getStatus() == null ? null : snapshot.getStatus().name(),
                toTimestamp(snapshot.getCreatedAt()),
                toTimestamp(snapshot.getUpdatedAt()),
                toTimestamp(snapshot.getClaimedAt()),
                toTimestamp(snapshot.getStartedAt()),
                toTimestamp(snapshot.getFinishedAt()),
                json,
                snapshot.getTaskId()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO " + tableName + " (task_id, task_type, requested_by, assigned_client_id, status, "
                            + "created_at, updated_at, claimed_at, started_at, finished_at, snapshot_json) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    snapshot.getTaskId(),
                    snapshot.getTaskType(),
                    snapshot.getRequestedBy(),
                    snapshot.getAssignedClientId(),
                    snapshot.getStatus() == null ? null : snapshot.getStatus().name(),
                    toTimestamp(snapshot.getCreatedAt()),
                    toTimestamp(snapshot.getUpdatedAt()),
                    toTimestamp(snapshot.getClaimedAt()),
                    toTimestamp(snapshot.getStartedAt()),
                    toTimestamp(snapshot.getFinishedAt()),
                    json
            );
        }
    }

    @Override
    public List<TaskSnapshot> findAll() {
        return jdbcTemplate.query(
                "SELECT snapshot_json FROM " + tableName + " ORDER BY created_at DESC",
                rowMapper
        );
    }

    private TaskSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
        return fromJson(rs.getString("snapshot_json"));
    }
}
