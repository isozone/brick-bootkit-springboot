package com.zqzqq.bootkits.openclaw.control.storage.jdbc;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

public class OpenClawJdbcSchemaInitializer implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;
    private final String clientsTable;
    private final String tasksTable;

    public OpenClawJdbcSchemaInitializer(JdbcTemplate jdbcTemplate, String clientsTable, String tasksTable) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientsTable = clientsTable;
        this.tasksTable = tasksTable;
    }

    @Override
    public void afterPropertiesSet() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + clientsTable + " ("
                + "client_id VARCHAR(128) PRIMARY KEY, "
                + "machine_id VARCHAR(256), "
                + "display_name VARCHAR(256), "
                + "status VARCHAR(32), "
                + "session_id VARCHAR(128), "
                + "host_name VARCHAR(256), "
                + "os_name VARCHAR(128), "
                + "os_version VARCHAR(128), "
                + "version VARCHAR(64), "
                + "sdk_version VARCHAR(64), "
                + "registered_at TIMESTAMP NULL, "
                + "last_seen_at TIMESTAMP NULL, "
                + "updated_at TIMESTAMP NULL, "
                + "snapshot_json TEXT NOT NULL"
                + ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tasksTable + " ("
                + "task_id VARCHAR(128) PRIMARY KEY, "
                + "task_type VARCHAR(128) NOT NULL, "
                + "requested_by VARCHAR(128), "
                + "assigned_client_id VARCHAR(128), "
                + "status VARCHAR(32), "
                + "created_at TIMESTAMP NULL, "
                + "updated_at TIMESTAMP NULL, "
                + "claimed_at TIMESTAMP NULL, "
                + "started_at TIMESTAMP NULL, "
                + "finished_at TIMESTAMP NULL, "
                + "snapshot_json TEXT NOT NULL"
                + ")");
    }
}
