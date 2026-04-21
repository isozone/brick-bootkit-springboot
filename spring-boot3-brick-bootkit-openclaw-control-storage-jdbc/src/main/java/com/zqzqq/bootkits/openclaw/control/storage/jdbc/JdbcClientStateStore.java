package com.zqzqq.bootkits.openclaw.control.storage.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.openclaw.control.spi.ClientStateStore;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcClientStateStore extends AbstractJdbcSnapshotStore<ClientSnapshot> implements ClientStateStore {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final RowMapper<ClientSnapshot> rowMapper = this::mapRow;

    public JdbcClientStateStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, String tableName) {
        super(objectMapper, ClientSnapshot.class);
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public ClientSnapshot findByClientId(String clientId) {
        List<ClientSnapshot> items = jdbcTemplate.query(
                "SELECT snapshot_json FROM " + tableName + " WHERE client_id = ?",
                rowMapper,
                clientId
        );
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public void save(ClientSnapshot snapshot) {
        String json = toJson(snapshot);
        int updated = jdbcTemplate.update(
                "UPDATE " + tableName + " SET machine_id = ?, display_name = ?, status = ?, session_id = ?, "
                        + "host_name = ?, os_name = ?, os_version = ?, version = ?, sdk_version = ?, "
                        + "registered_at = ?, last_seen_at = ?, updated_at = ?, snapshot_json = ? WHERE client_id = ?",
                snapshot.getMachineId(),
                snapshot.getDisplayName(),
                snapshot.getStatus() == null ? null : snapshot.getStatus().name(),
                snapshot.getSessionId(),
                snapshot.getHostName(),
                snapshot.getOsName(),
                snapshot.getOsVersion(),
                snapshot.getVersion(),
                snapshot.getSdkVersion(),
                toTimestamp(snapshot.getRegisteredAt()),
                toTimestamp(snapshot.getLastSeenAt()),
                toTimestamp(snapshot.getUpdatedAt()),
                json,
                snapshot.getClientId()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO " + tableName + " (client_id, machine_id, display_name, status, session_id, "
                            + "host_name, os_name, os_version, version, sdk_version, registered_at, last_seen_at, updated_at, snapshot_json) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    snapshot.getClientId(),
                    snapshot.getMachineId(),
                    snapshot.getDisplayName(),
                    snapshot.getStatus() == null ? null : snapshot.getStatus().name(),
                    snapshot.getSessionId(),
                    snapshot.getHostName(),
                    snapshot.getOsName(),
                    snapshot.getOsVersion(),
                    snapshot.getVersion(),
                    snapshot.getSdkVersion(),
                    toTimestamp(snapshot.getRegisteredAt()),
                    toTimestamp(snapshot.getLastSeenAt()),
                    toTimestamp(snapshot.getUpdatedAt()),
                    json
            );
        }
    }

    @Override
    public List<ClientSnapshot> findAll() {
        return jdbcTemplate.query(
                "SELECT snapshot_json FROM " + tableName + " ORDER BY updated_at DESC",
                rowMapper
        );
    }

    private ClientSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
        return fromJson(rs.getString("snapshot_json"));
    }
}
