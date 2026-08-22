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

import com.zqzqq.bootkits.core.exception.PluginException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * File-backed migration ledger shared by cluster nodes.
 */
public class FilePluginMigrationHistoryStore {

    private static final String KEY_PLUGIN_VERSION = "plugin.version";
    private static final String KEY_APPLIED = "applied";
    private static final String KEY_CHECKSUM_PREFIX = "checksum.";

    private final Path stateDirectory;

    public FilePluginMigrationHistoryStore(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
        try {
            Files.createDirectories(stateDirectory);
        } catch (IOException e) {
            throw new PluginException("Create migration state directory failed: " + stateDirectory, e);
        }
    }

    public LinkedHashSet<String> loadAppliedScripts(String pluginId) {
        Properties properties = load(pluginId);
        String applied = properties.getProperty(KEY_APPLIED);
        if (applied == null || applied.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(applied.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void addAppliedScript(String pluginId, String pluginVersion, String upScriptPath) {
        Properties properties = load(pluginId);
        LinkedHashSet<String> applied = parseApplied(properties);
        if (applied.add(upScriptPath)) {
            properties.setProperty(KEY_APPLIED, joinApplied(applied));
            if (pluginVersion != null) {
                properties.setProperty(KEY_PLUGIN_VERSION, pluginVersion);
            }
            save(pluginId, properties);
        }
    }

    public void upsertScriptChecksum(String pluginId, String upScriptPath, String checksum) {
        if (upScriptPath == null || checksum == null) {
            return;
        }
        Properties properties = load(pluginId);
        properties.setProperty(checksumKey(upScriptPath), checksum);
        save(pluginId, properties);
    }

    public String getScriptChecksum(String pluginId, String upScriptPath) {
        if (upScriptPath == null) {
            return null;
        }
        Properties properties = load(pluginId);
        String key = checksumKey(upScriptPath);
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return null;
    }

    public void removeAppliedScript(String pluginId, String upScriptPath) {
        Properties properties = load(pluginId);
        LinkedHashSet<String> applied = parseApplied(properties);
        if (!applied.remove(upScriptPath)) {
            return;
        }
        properties.remove(checksumKey(upScriptPath));
        if (applied.isEmpty()) {
            delete(pluginId);
            return;
        }
        properties.setProperty(KEY_APPLIED, joinApplied(applied));
        save(pluginId, properties);
    }

    private LinkedHashSet<String> parseApplied(Properties properties) {
        String applied = properties.getProperty(KEY_APPLIED);
        if (applied == null || applied.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(applied.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String joinApplied(Set<String> applied) {
        return String.join(",", applied);
    }

    private Properties load(String pluginId) {
        Path file = recordFile(pluginId);
        Properties properties = new Properties();
        if (!Files.exists(file)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new PluginException("Load migration state failed: " + file, e);
        }
    }

    private void save(String pluginId, Properties properties) {
        Path target = recordFile(pluginId);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp." + UUID.randomUUID());
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(tmp)) {
                properties.store(out, "plugin migration state");
            }
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PluginException("Save migration state failed: " + target, e);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    private void delete(String pluginId) {
        try {
            Files.deleteIfExists(recordFile(pluginId));
        } catch (IOException e) {
            throw new PluginException("Delete migration state failed for plugin: " + pluginId, e);
        }
    }

    private Path recordFile(String pluginId) {
        String fileName = sanitize(pluginId) + ".properties";
        return stateDirectory.resolve(fileName);
    }

    private String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String checksumKey(String scriptPath) {
        return KEY_CHECKSUM_PREFIX + sanitize(scriptPath);
    }
}
