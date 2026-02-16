package com.zqzqq.bootkits.core.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FilePluginMigrationHistoryStoreChecksumTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistAndClearChecksumWithScriptState() {
        FilePluginMigrationHistoryStore store = new FilePluginMigrationHistoryStore(tempDir);

        store.addAppliedScript("demo-plugin", "1.0.0", "db/001_init.sql");
        store.upsertScriptChecksum("demo-plugin", "db/001_init.sql", "abc123");

        assertThat(store.getScriptChecksum("demo-plugin", "db/001_init.sql")).isEqualTo("abc123");

        store.removeAppliedScript("demo-plugin", "db/001_init.sql");
        assertThat(store.getScriptChecksum("demo-plugin", "db/001_init.sql")).isNull();
    }
}
