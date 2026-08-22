/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
