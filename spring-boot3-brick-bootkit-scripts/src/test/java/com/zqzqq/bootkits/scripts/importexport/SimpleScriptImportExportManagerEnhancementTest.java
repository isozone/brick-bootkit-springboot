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


package com.zqzqq.bootkits.scripts.importexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleScriptImportExportManagerEnhancementTest {

    private SimpleScriptImportExportManager manager;

    @BeforeEach
    void setUp() {
        manager = new SimpleScriptImportExportManager();
        manager.setEnvironmentVariable("EXISTING_ENV", "existing_value");
    }

    @Test
    @DisplayName("importAll should expose alias and warning statistics for legacy payloads")
    void testLegacyImportAllStatistics() throws Exception {
        Path tempFile = Files.createTempFile("legacy-stats", ".json");
        try {
            String legacyJson = "{"
                    + "\"exportTime\":\"2024-01-01T00:00:00\","
                    + "\"scriptConfigs\":{"
                    + "  \"legacy_script\":{"
                    + "    \"id\":\"legacy_script\","
                    + "    \"name\":\"Legacy Script\","
                    + "    \"script_type\":\"SHELL\","
                    + "    \"script_path\":\"echo legacy\","
                    + "    \"enabled\":true"
                    + "  }"
                    + "},"
                    + "\"environmentVariables\":{"
                    + "  \"LEGACY_ENV\":\"legacy_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, legacyJson, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeEnvironment(true)
                            .overwriteExisting(true)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertTrue(result.getStatistics().containsKey("alias.hits.total"));
            assertTrue(result.getStatistics().containsKey("alias.hits.byField"));
            assertTrue(result.getStatistics().containsKey("warning.byType"));
            assertTrue(((Number) result.getStatistics().get("alias.hits.total")).intValue() >= 2);

            assertNotNull(manager.getScriptConfig("legacy_script"));
            assertEquals("legacy_value", manager.getEnvironmentVariable("LEGACY_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importScripts should report snake_case alias hit statistics")
    void testImportScriptsAliasStatistics() throws Exception {
        Path tempFile = Files.createTempFile("snake-alias-stats", ".json");
        try {
            String payload = "{"
                    + "\"snake_script\":{"
                    + "  \"id\":\"snake_script\","
                    + "  \"name\":\"Snake Script\","
                    + "  \"script_type\":\"PYTHON\","
                    + "  \"script_path\":\"python main.py\","
                    + "  \"working_directory\":\"/tmp\","
                    + "  \"retry_count\":2,"
                    + "  \"timeout_ms\":15000,"
                    + "  \"is_enabled\":true"
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result = manager.importScripts(tempFile.toString(), true);

            assertTrue(result.isSuccess());
            assertTrue(((Number) result.getStatistics().get("alias.hits.total")).intValue() >= 4);
            @SuppressWarnings("unchecked")
            Map<String, Integer> byField = (Map<String, Integer>) result.getStatistics().get("alias.hits.byField");
            assertNotNull(byField);
            assertTrue(byField.containsKey("script.scriptType<-script_type"));
            assertTrue(byField.containsKey("script.scriptPath<-script_path"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importEnvironment dry-run should not persist variables")
    void testImportEnvironmentDryRun() throws Exception {
        Path tempFile = Files.createTempFile("env-dry-run", ".json");
        try {
            String payload = "{"
                    + "\"NEW_ENV\":\"new_value\","
                    + "\"EXISTING_ENV\":\"override_value\""
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importEnvironment(tempFile.toString(), true, true);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(2L, result.getStatistics().get("environment.total"));
            assertEquals(0L, result.getStatistics().get("environment.skipped"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV"));
            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importEnvironment dry-run with default preview should expose full would* projection")
    void testImportEnvironmentDryRunDefaultPreviewProjection() throws Exception {
        Path tempFile = Files.createTempFile("env-dry-run-default-preview", ".json");
        try {
            String payload = "{"
                    + "\"EXISTING_ENV\":\"override_value\","
                    + "\"NEW_ENV_A\":\"new_value_a\","
                    + "\"NEW_ENV_B\":\"new_value_b\""
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importEnvironment(tempFile.toString(), false, true);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(200, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.FALSE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(0L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(3L, result.getStatistics().get("environment.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("environment.wouldImport"));
            assertEquals(1L, result.getStatistics().get("environment.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("environment.wouldFail"));
            assertEquals(List.of("NEW_ENV_A", "NEW_ENV_B"), result.getStatistics().get("environment.wouldImport.keys"));
            assertEquals(0L, result.getStatistics().get("environment.wouldImport.keys.truncatedCount"));
            assertEquals(List.of("EXISTING_ENV"), result.getStatistics().get("environment.wouldSkip.keys"));
            assertEquals(0L, result.getStatistics().get("environment.wouldSkip.keys.truncatedCount"));

            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_A"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_B"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importEnvironment dry-run should expose would* projection with maxPreviewItems")
    void testImportEnvironmentDryRunProjectionStatistics() throws Exception {
        Path tempFile = Files.createTempFile("env-dry-run-projection", ".json");
        try {
            String payload = "{"
                    + "\"EXISTING_ENV\":\"override_value\","
                    + "\"NEW_ENV_A\":\"new_value_a\","
                    + "\"NEW_ENV_B\":\"new_value_b\""
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importEnvironment(tempFile.toString(), false, true, 1);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(1, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(2L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(1L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(3L, result.getStatistics().get("environment.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("environment.wouldImport"));
            assertEquals(1L, result.getStatistics().get("environment.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("environment.wouldFail"));
            assertEquals(List.of("NEW_ENV_A"), result.getStatistics().get("environment.wouldImport.keys"));
            assertEquals(1L, result.getStatistics().get("environment.wouldImport.keys.truncatedCount"));
            assertEquals(List.of("EXISTING_ENV"), result.getStatistics().get("environment.wouldSkip.keys"));
            assertEquals(0L, result.getStatistics().get("environment.wouldSkip.keys.truncatedCount"));

            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_A"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_B"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importScripts dry-run with default preview should expose full would* projection")
    void testImportScriptsDryRunDefaultPreviewProjection() throws Exception {
        Path tempFile = Files.createTempFile("scripts-dry-run-default-preview", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"dup_script\":{"
                    + "  \"id\":\"dup_script\","
                    + "  \"name\":\"Overwrite Attempt\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo overwrite\""
                    + "},"
                    + "\"new_script_a\":{"
                    + "  \"id\":\"new_script_a\","
                    + "  \"name\":\"Brand New Script A\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo a\""
                    + "},"
                    + "\"new_script_b\":{"
                    + "  \"id\":\"new_script_b\","
                    + "  \"name\":\"Brand New Script B\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo b\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importScripts(tempFile.toString(), false, true);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(200, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.FALSE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(0L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(3L, result.getStatistics().get("scripts.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("scripts.wouldImport"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldFail"));
            assertEquals(List.of("new_script_a", "new_script_b"), result.getStatistics().get("scripts.wouldImport.ids"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldImport.ids.truncatedCount"));
            assertEquals(List.of("dup_script"), result.getStatistics().get("scripts.wouldSkip.ids"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldSkip.ids.truncatedCount"));

            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertNull(manager.getScriptConfig("new_script_a"));
            assertNull(manager.getScriptConfig("new_script_b"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importScripts dry-run should expose would* projection with maxPreviewItems")
    void testImportScriptsDryRunProjectionStatistics() throws Exception {
        Path tempFile = Files.createTempFile("scripts-dry-run-projection", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"dup_script\":{"
                    + "  \"id\":\"dup_script\","
                    + "  \"name\":\"Overwrite Attempt\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo overwrite\""
                    + "},"
                    + "\"new_script_a\":{"
                    + "  \"id\":\"new_script_a\","
                    + "  \"name\":\"Brand New Script A\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo a\""
                    + "},"
                    + "\"new_script_b\":{"
                    + "  \"id\":\"new_script_b\","
                    + "  \"name\":\"Brand New Script B\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo b\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importScripts(tempFile.toString(), false, true, 1);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(1, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(3L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(2L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(1L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(3L, result.getStatistics().get("scripts.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("scripts.wouldImport"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldFail"));
            assertEquals(List.of("new_script_a"), result.getStatistics().get("scripts.wouldImport.ids"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldImport.ids.truncatedCount"));
            assertEquals(List.of("dup_script"), result.getStatistics().get("scripts.wouldSkip.ids"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldSkip.ids.truncatedCount"));

            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertNull(manager.getScriptConfig("new_script_a"));
            assertNull(manager.getScriptConfig("new_script_b"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll dry-run should expose would* projection statistics")
    void testImportAllDryRunProjectionStatistics() throws Exception {
        Path tempFile = Files.createTempFile("import-all-dry-run", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"dup_script\":{"
                    + "    \"id\":\"dup_script\","
                    + "    \"name\":\"Overwrite Attempt\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo overwrite\""
                    + "  },"
                    + "  \"new_script\":{"
                    + "    \"id\":\"new_script\","
                    + "    \"name\":\"Brand New Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo new\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"EXISTING_ENV\":\"override_value\","
                    + "  \"NEW_ENV\":\"new_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeScripts(true)
                            .includeEnvironment(true)
                            .overwriteExisting(false)
                            .validateData(true)
                            .dryRun(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(200, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.FALSE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(4L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(4L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(0L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(2L, result.getStatistics().get("scripts.wouldTotal"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldImport"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldFail"));
            assertEquals(List.of("new_script"), result.getStatistics().get("scripts.wouldImport.ids"));
            assertEquals(List.of("dup_script"), result.getStatistics().get("scripts.wouldSkip.ids"));
            assertEquals(List.of(), result.getStatistics().get("scripts.wouldFail.ids"));

            assertEquals(2L, result.getStatistics().get("environment.wouldTotal"));
            assertEquals(1L, result.getStatistics().get("environment.wouldImport"));
            assertEquals(1L, result.getStatistics().get("environment.wouldSkip"));
            assertEquals(0L, result.getStatistics().get("environment.wouldFail"));
            assertEquals(List.of("NEW_ENV"), result.getStatistics().get("environment.wouldImport.keys"));
            assertEquals(List.of("EXISTING_ENV"), result.getStatistics().get("environment.wouldSkip.keys"));
            assertEquals(List.of(), result.getStatistics().get("environment.wouldFail.keys"));

            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertNull(manager.getScriptConfig("new_script"));
            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll dry-run should truncate detail lists by maxPreviewItems")
    void testImportAllDryRunProjectionStatisticsTruncation() throws Exception {
        Path tempFile = Files.createTempFile("import-all-dry-run-truncate", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"dup_script\":{"
                    + "    \"id\":\"dup_script\","
                    + "    \"name\":\"Overwrite Attempt\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo overwrite\""
                    + "  },"
                    + "  \"new_script_a\":{"
                    + "    \"id\":\"new_script_a\","
                    + "    \"name\":\"Brand New Script A\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo a\""
                    + "  },"
                    + "  \"new_script_b\":{"
                    + "    \"id\":\"new_script_b\","
                    + "    \"name\":\"Brand New Script B\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo b\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"EXISTING_ENV\":\"override_value\","
                    + "  \"NEW_ENV_A\":\"new_value_a\","
                    + "  \"NEW_ENV_B\":\"new_value_b\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeScripts(true)
                            .includeEnvironment(true)
                            .overwriteExisting(false)
                            .validateData(true)
                            .dryRun(true)
                            .maxPreviewItems(1)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun"));
            assertEquals(1, result.getStatistics().get("dryRun.maxPreviewItems"));
            assertEquals(Boolean.TRUE, result.getStatistics().get("dryRun.previewItemsLimited"));
            assertEquals(6L, result.getStatistics().get("dryRun.previewItemsAvailable"));
            assertEquals(4L, result.getStatistics().get("dryRun.previewItemsReturned"));
            assertEquals(2L, result.getStatistics().get("dryRun.previewItemsTruncated"));

            assertEquals(3L, result.getStatistics().get("scripts.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("scripts.wouldImport"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldSkip"));
            assertEquals(List.of("new_script_a"), result.getStatistics().get("scripts.wouldImport.ids"));
            assertEquals(1L, result.getStatistics().get("scripts.wouldImport.ids.truncatedCount"));
            assertEquals(List.of("dup_script"), result.getStatistics().get("scripts.wouldSkip.ids"));
            assertEquals(0L, result.getStatistics().get("scripts.wouldSkip.ids.truncatedCount"));

            assertEquals(3L, result.getStatistics().get("environment.wouldTotal"));
            assertEquals(2L, result.getStatistics().get("environment.wouldImport"));
            assertEquals(1L, result.getStatistics().get("environment.wouldSkip"));
            assertEquals(List.of("NEW_ENV_A"), result.getStatistics().get("environment.wouldImport.keys"));
            assertEquals(1L, result.getStatistics().get("environment.wouldImport.keys.truncatedCount"));
            assertEquals(List.of("EXISTING_ENV"), result.getStatistics().get("environment.wouldSkip.keys"));
            assertEquals(0L, result.getStatistics().get("environment.wouldSkip.keys.truncatedCount"));

            assertNull(manager.getScriptConfig("new_script_a"));
            assertNull(manager.getScriptConfig("new_script_b"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_A"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV_B"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("migration cycle should fail with cycle error statistics")
    void testSchemaMigrationCycleDetection() throws Exception {
        Path tempFile = Files.createTempFile("schema-cycle", ".json");
        try {
            manager.registerSchemaMigration("cycle-a", (data, context) -> "cycle-b");
            manager.registerSchemaMigration("cycle-b", (data, context) -> "cycle-a");

            String payload = "{"
                    + "\"version\":\"cycle-a\","
                    + "\"schemaVersion\":\"cycle-a\","
                    + "\"scripts\":{"
                    + "  \"cycle_script\":{"
                    + "    \"id\":\"cycle_script\","
                    + "    \"name\":\"Cycle Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo cycle\","
                    + "    \"enabled\":true"
                    + "  }"
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .strictSchemaValidation(true)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("cycle"));
            assertEquals("cycle", result.getStatistics().get("migration.error.type"));
            assertEquals("cycle-a", result.getStatistics().get("migration.error.at"));
            assertNull(manager.getScriptConfig("cycle_script"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("migration chain beyond max steps should fail with step_limit")
    void testSchemaMigrationStepLimit() throws Exception {
        Path tempFile = Files.createTempFile("schema-step-limit", ".json");
        try {
            final int chainLength = 300;
            for (int i = 0; i < chainLength; i++) {
                final int next = i + 1;
                manager.registerSchemaMigration("chain-" + i, (data, context) -> "chain-" + next);
            }
            manager.registerSchemaMigration("chain-" + chainLength, (data, context) -> "4.0.1");

            String payload = "{"
                    + "\"version\":\"chain-0\","
                    + "\"schemaVersion\":\"chain-0\","
                    + "\"scripts\":{"
                    + "  \"chain_script\":{"
                    + "    \"id\":\"chain_script\","
                    + "    \"name\":\"Chain Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo chain\","
                    + "    \"enabled\":true"
                    + "  }"
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .strictSchemaValidation(true)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("step limit"));
            assertEquals("step_limit", result.getStatistics().get("migration.error.type"));
            assertNull(manager.getScriptConfig("chain_script"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("custom maxMigrationSteps should fail earlier than default")
    void testCustomMaxMigrationSteps() throws Exception {
        Path tempFile = Files.createTempFile("schema-custom-step-limit", ".json");
        try {
            manager.registerSchemaMigration("custom-0", (data, context) -> "custom-1");
            manager.registerSchemaMigration("custom-1", (data, context) -> "custom-2");
            manager.registerSchemaMigration("custom-2", (data, context) -> "4.0.1");

            String payload = "{"
                    + "\"version\":\"custom-0\","
                    + "\"schemaVersion\":\"custom-0\","
                    + "\"scripts\":{"
                    + "  \"custom_script\":{"
                    + "    \"id\":\"custom_script\","
                    + "    \"name\":\"Custom Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo custom\","
                    + "    \"enabled\":true"
                    + "  }"
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .strictSchemaValidation(true)
                            .validateData(true)
                            .maxMigrationSteps(1)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess());
            assertEquals("step_limit", result.getStatistics().get("migration.error.type"));
            assertEquals(1, result.getStatistics().get("migration.maxSteps"));
            assertNull(manager.getScriptConfig("custom_script"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("maxMigrationSteps should reject non-positive values")
    void testMaxMigrationStepsValidation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                        .maxMigrationSteps(0)
                        .build()
        );
        assertTrue(ex.getMessage().contains("maxMigrationSteps"));
    }

    @Test
    @DisplayName("maxPreviewItems should reject non-positive values")
    void testMaxPreviewItemsValidation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                        .maxPreviewItems(0)
                        .build()
        );
        assertTrue(ex.getMessage().contains("maxPreviewItems"));
    }

    @Test
    @DisplayName("importScripts should expose skipped statistics when overwrite disabled")
    void testImportScriptsSkipStatistics() throws Exception {
        Path tempFile = Files.createTempFile("scripts-skip-stats", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"dup_script\":{"
                    + "  \"id\":\"dup_script\","
                    + "  \"name\":\"New Script\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo new\","
                    + "  \"enabled\":true"
                    + "},"
                    + "\"new_script\":{"
                    + "  \"id\":\"new_script\","
                    + "  \"name\":\"Brand New Script\","
                    + "  \"scriptType\":\"SHELL\","
                    + "  \"scriptPath\":\"echo brand-new\","
                    + "  \"enabled\":true"
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result = manager.importScripts(tempFile.toString(), false);

            assertTrue(result.isSuccess());
            assertEquals(2L, result.getStatistics().get("scripts.total"));
            assertEquals(1L, result.getStatistics().get("scripts.imported"));
            assertEquals(1L, result.getStatistics().get("scripts.skipped"));
            assertEquals(0L, result.getStatistics().get("scripts.failed"));
            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertEquals("Brand New Script", manager.getScriptConfig("new_script").getName());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importEnvironment should expose skipped statistics when overwrite disabled")
    void testImportEnvironmentSkipStatistics() throws Exception {
        Path tempFile = Files.createTempFile("env-skip-stats", ".json");
        try {
            String payload = "{"
                    + "\"EXISTING_ENV\":\"override_value\","
                    + "\"NEW_ENV\":\"new_value\""
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportResult result =
                    manager.importEnvironment(tempFile.toString(), false, false);

            assertTrue(result.isSuccess());
            assertEquals(2L, result.getStatistics().get("environment.total"));
            assertEquals(1L, result.getStatistics().get("environment.imported"));
            assertEquals(1L, result.getStatistics().get("environment.skipped"));
            assertEquals(0L, result.getStatistics().get("environment.failed"));
            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertEquals("new_value", manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll should expose scripts/environment breakdown when overwrite disabled")
    void testImportAllBreakdownStatistics() throws Exception {
        Path tempFile = Files.createTempFile("import-all-breakdown", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"dup_script\":{"
                    + "    \"id\":\"dup_script\","
                    + "    \"name\":\"Overwrite Attempt\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo overwrite\""
                    + "  },"
                    + "  \"new_script\":{"
                    + "    \"id\":\"new_script\","
                    + "    \"name\":\"Brand New Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo new\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"EXISTING_ENV\":\"override_value\","
                    + "  \"NEW_ENV\":\"new_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeEnvironment(true)
                            .overwriteExisting(false)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertEquals(2L, result.getStatistics().get("scripts.total"));
            assertEquals(1L, result.getStatistics().get("scripts.imported"));
            assertEquals(1L, result.getStatistics().get("scripts.skipped"));
            assertEquals(0L, result.getStatistics().get("scripts.failed"));

            assertEquals(2L, result.getStatistics().get("environment.total"));
            assertEquals(1L, result.getStatistics().get("environment.imported"));
            assertEquals(1L, result.getStatistics().get("environment.skipped"));
            assertEquals(0L, result.getStatistics().get("environment.failed"));

            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertEquals("Brand New Script", manager.getScriptConfig("new_script").getName());
            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertEquals("new_value", manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll breakdown should ignore environment when includeEnvironment is false")
    void testImportAllBreakdownWithoutEnvironmentImport() throws Exception {
        Path tempFile = Files.createTempFile("import-all-no-env", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"dup_script\":{"
                    + "    \"id\":\"dup_script\","
                    + "    \"name\":\"Overwrite Attempt\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo overwrite\""
                    + "  },"
                    + "  \"new_script\":{"
                    + "    \"id\":\"new_script\","
                    + "    \"name\":\"Brand New Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo new\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"EXISTING_ENV\":\"override_value\","
                    + "  \"NEW_ENV\":\"new_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeEnvironment(false)
                            .overwriteExisting(false)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertEquals(2L, result.getStatistics().get("scripts.total"));
            assertEquals(1L, result.getStatistics().get("scripts.imported"));
            assertEquals(1L, result.getStatistics().get("scripts.skipped"));
            assertEquals(0L, result.getStatistics().get("scripts.failed"));

            assertEquals(0L, result.getStatistics().get("environment.total"));
            assertEquals(0L, result.getStatistics().get("environment.imported"));
            assertEquals(0L, result.getStatistics().get("environment.skipped"));
            assertEquals(0L, result.getStatistics().get("environment.failed"));

            assertEquals("existing_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll validation failure should include migration and breakdown statistics")
    void testImportAllValidationFailureStatistics() throws Exception {
        Path tempFile = Files.createTempFile("import-all-validation-failure", ".json");
        try {
            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"metadata\":{"
                    + "  \"source\":\"broken\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Data validation failed"));

            assertEquals(Boolean.FALSE, result.getStatistics().get("dryRun"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.original"));
            assertEquals("4.0.1", result.getStatistics().get("schemaVersion.final"));
            assertEquals(0, result.getStatistics().get("migration.steps"));
            assertEquals(128, result.getStatistics().get("migration.maxSteps"));

            assertEquals(0L, result.getStatistics().get("scripts.total"));
            assertEquals(0L, result.getStatistics().get("scripts.imported"));
            assertEquals(0L, result.getStatistics().get("scripts.skipped"));
            assertEquals(0L, result.getStatistics().get("scripts.failed"));

            assertEquals(0L, result.getStatistics().get("environment.total"));
            assertEquals(0L, result.getStatistics().get("environment.imported"));
            assertEquals(0L, result.getStatistics().get("environment.skipped"));
            assertEquals(0L, result.getStatistics().get("environment.failed"));

            assertEquals(0, result.getStatistics().get("warning.total"));
            assertTrue(result.getStatistics().containsKey("warning.byType"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll should ignore scripts when includeScripts is false")
    void testImportAllWithoutScriptsImport() throws Exception {
        Path tempFile = Files.createTempFile("import-all-no-scripts", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"dup_script\":{"
                    + "    \"id\":\"dup_script\","
                    + "    \"name\":\"Overwrite Attempt\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo overwrite\""
                    + "  },"
                    + "  \"new_script\":{"
                    + "    \"id\":\"new_script\","
                    + "    \"name\":\"Brand New Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo new\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"EXISTING_ENV\":\"override_value\","
                    + "  \"NEW_ENV\":\"new_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeScripts(false)
                            .includeEnvironment(true)
                            .overwriteExisting(true)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertTrue(result.isSuccess());
            assertEquals(0L, result.getStatistics().get("scripts.total"));
            assertEquals(0L, result.getStatistics().get("scripts.imported"));
            assertEquals(0L, result.getStatistics().get("scripts.skipped"));
            assertEquals(0L, result.getStatistics().get("scripts.failed"));

            assertEquals(2L, result.getStatistics().get("environment.total"));
            assertEquals(2L, result.getStatistics().get("environment.imported"));
            assertEquals(0L, result.getStatistics().get("environment.skipped"));
            assertEquals(0L, result.getStatistics().get("environment.failed"));

            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertNull(manager.getScriptConfig("new_script"));
            assertEquals("override_value", manager.getEnvironmentVariable("EXISTING_ENV"));
            assertEquals("new_value", manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("importAll should fail validation when scripts and environment are both disabled")
    void testImportAllWithBothScopesDisabled() throws Exception {
        Path tempFile = Files.createTempFile("import-all-disabled-scopes", ".json");
        try {
            SimpleScriptImportExportManager.SimpleScriptConfig existing = new SimpleScriptImportExportManager.SimpleScriptConfig();
            existing.setId("dup_script");
            existing.setName("Existing Script");
            existing.setScriptType("SHELL");
            existing.setScriptPath("echo existing");
            manager.addScriptConfig("dup_script", existing);

            String payload = "{"
                    + "\"version\":\"4.0.1\","
                    + "\"schemaVersion\":\"4.0.1\","
                    + "\"scripts\":{"
                    + "  \"new_script\":{"
                    + "    \"id\":\"new_script\","
                    + "    \"name\":\"Brand New Script\","
                    + "    \"scriptType\":\"SHELL\","
                    + "    \"scriptPath\":\"echo new\""
                    + "  }"
                    + "},"
                    + "\"environment\":{"
                    + "  \"NEW_ENV\":\"new_value\""
                    + "}"
                    + "}";
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);

            SimpleScriptImportExportManager.ImportExportOptions options =
                    SimpleScriptImportExportManager.ImportExportOptions.newBuilder()
                            .includeScripts(false)
                            .includeEnvironment(false)
                            .validateData(true)
                            .build();

            SimpleScriptImportExportManager.ImportExportResult result = manager.importAll(tempFile.toString(), options);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Data validation failed"));
            assertEquals(0L, result.getStatistics().get("scripts.total"));
            assertEquals(0L, result.getStatistics().get("environment.total"));
            assertEquals("Existing Script", manager.getScriptConfig("dup_script").getName());
            assertNull(manager.getScriptConfig("new_script"));
            assertNull(manager.getEnvironmentVariable("NEW_ENV"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
