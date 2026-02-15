package com.zqzqq.bootkits.core.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

class FilePluginMigrationHistoryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistAndRemoveAppliedScripts() {
        FilePluginMigrationHistoryStore store = new FilePluginMigrationHistoryStore(tempDir);

        store.addAppliedScript("demo-plugin", "1.0.0", "db/001_init.sql");
        store.addAppliedScript("demo-plugin", "1.0.0", "db/002_alter.sql");

        LinkedHashSet<String> applied = store.loadAppliedScripts("demo-plugin");
        assertThat(applied).containsExactly("db/001_init.sql", "db/002_alter.sql");

        store.removeAppliedScript("demo-plugin", "db/002_alter.sql");
        assertThat(store.loadAppliedScripts("demo-plugin")).containsExactly("db/001_init.sql");

        store.removeAppliedScript("demo-plugin", "db/001_init.sql");
        assertThat(store.loadAppliedScripts("demo-plugin")).isEmpty();
    }
}
