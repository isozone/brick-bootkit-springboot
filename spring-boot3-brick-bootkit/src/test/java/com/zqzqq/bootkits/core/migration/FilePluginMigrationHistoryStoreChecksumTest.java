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
