package com.zqzqq.bootkits.openclaw.control.storage.jdbc;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.ClientStatus;
import com.zqzqq.bootkits.openclaw.protocol.TaskSnapshot;
import com.zqzqq.bootkits.openclaw.protocol.TaskStatus;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcStateStoreTest {

    @Test
    void shouldPersistClientAndTaskSnapshots() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:openclaw;MODE=MySQL;DB_CLOSE_DELAY=-1");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        new OpenClawJdbcSchemaInitializer(jdbcTemplate, "oc_control_clients", "oc_control_tasks").afterPropertiesSet();

        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        JdbcClientStateStore clientStateStore = new JdbcClientStateStore(jdbcTemplate, mapper, "oc_control_clients");
        JdbcTaskStateStore taskStateStore = new JdbcTaskStateStore(jdbcTemplate, mapper, "oc_control_tasks");

        ClientSnapshot client = new ClientSnapshot();
        client.setClientId("client-jdbc");
        client.setDisplayName("JDBC Client");
        client.setStatus(ClientStatus.ONLINE);
        client.setRegisteredAt(Instant.now());
        clientStateStore.save(client);

        TaskSnapshot task = new TaskSnapshot();
        task.setTaskId("task-jdbc");
        task.setTaskType("gateway.sync");
        task.setStatus(TaskStatus.QUEUED);
        task.setCreatedAt(Instant.now());
        taskStateStore.save(task);

        assertThat(clientStateStore.findByClientId("client-jdbc")).isNotNull();
        assertThat(taskStateStore.findByTaskId("task-jdbc")).isNotNull();
        assertThat(clientStateStore.findAll()).hasSize(1);
        assertThat(taskStateStore.findAll()).hasSize(1);
    }
}
