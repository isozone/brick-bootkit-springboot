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

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Executes plugin SQL migrations during install/uninstall.
 */
public class PluginMigrationService {

    private static final Logger log = LoggerFactory.getLogger(PluginMigrationService.class);

    private final ApplicationContext applicationContext;
    private final FilePluginMigrationHistoryStore historyStore;
    private final PluginMigrationOptions options;

    public PluginMigrationService(ApplicationContext applicationContext, Path stateRootDir) {
        this(applicationContext, stateRootDir, PluginMigrationOptions.defaults());
    }

    public PluginMigrationService(ApplicationContext applicationContext,
                                  Path stateRootDir,
                                  PluginMigrationOptions options) {
        this.applicationContext = applicationContext;
        this.historyStore = new FilePluginMigrationHistoryStore(stateRootDir);
        this.options = options == null ? PluginMigrationOptions.defaults() : options;
    }

    public void applyInstallMigrations(InsidePluginDescriptor descriptor) {
        PluginMigrationPlan plan = PluginMigrationPlan.from(descriptor);
        if (!plan.hasMigrations()) {
            return;
        }

        DataSource dataSource = resolveDataSource(plan);
        LinkedHashSet<String> applied = historyStore.loadAppliedScripts(plan.getPluginId());

        for (String upScript : plan.getUpScripts()) {
            String sql = loadScript(descriptor, upScript);
            String checksum = checksum(sql);
            if (applied.contains(upScript)) {
                if (options.isValidateChecksum()) {
                    String storedChecksum = historyStore.getScriptChecksum(plan.getPluginId(), upScript);
                    if (storedChecksum != null && !storedChecksum.equals(checksum)) {
                        throw new PluginException("Applied migration script changed: " + upScript
                                + ", plugin=" + plan.getPluginId());
                    }
                }
                continue;
            }
            executeSqlWithPolicy(dataSource, plan.getPluginId(), "UP", upScript, sql);
            historyStore.addAppliedScript(plan.getPluginId(), plan.getPluginVersion(), upScript);
            historyStore.upsertScriptChecksum(plan.getPluginId(), upScript, checksum);
            applied.add(upScript);
        }
    }

    public void applyUninstallMigrations(InsidePluginDescriptor descriptor) {
        PluginMigrationPlan plan = PluginMigrationPlan.from(descriptor);
        if (!plan.hasMigrations()) {
            return;
        }

        DataSource dataSource = resolveDataSource(plan);
        LinkedHashSet<String> applied = historyStore.loadAppliedScripts(plan.getPluginId());
        if (applied.isEmpty()) {
            return;
        }

        List<String> upScripts = plan.getUpScripts();
        List<String> downScripts = plan.getDownScripts();
        for (int i = upScripts.size() - 1; i >= 0; i--) {
            String upScript = upScripts.get(i);
            if (!applied.contains(upScript)) {
                continue;
            }
            if (i >= downScripts.size()) {
                throw new PluginException("Missing down migration for up script: " + upScript);
            }
            String downScript = downScripts.get(i);
            if (ObjectUtils.isEmpty(downScript)) {
                throw new PluginException("Empty down migration for up script: " + upScript);
            }
            String sql = loadScript(descriptor, downScript);
            executeSqlWithPolicy(dataSource, plan.getPluginId(), "DOWN", downScript, sql);
            historyStore.removeAppliedScript(plan.getPluginId(), upScript);
            applied.remove(upScript);
        }
    }

    private DataSource resolveDataSource(PluginMigrationPlan plan) {
        String ref = plan.getDataSourceRef();
        if (ObjectUtils.isEmpty(ref) || "main".equalsIgnoreCase(ref)) {
            String[] beanNames = applicationContext.getBeanNamesForType(DataSource.class, false, false);
            if (beanNames == null || beanNames.length == 0) {
                throw new PluginException("No DataSource bean found for plugin migration: " + plan.getPluginId());
            }
            if (beanNames.length > 1) {
                throw new PluginException("Multiple DataSource beans found. Configure "
                        + PluginMigrationPlan.KEY_DATASOURCE + "=bean:<beanName>");
            }
            return applicationContext.getBean(beanNames[0], DataSource.class);
        }

        String beanName = ref;
        if (ref.startsWith("bean:")) {
            beanName = ref.substring("bean:".length());
        }

        DataSource dataSource = SpringBeanUtils.getExistBean(applicationContext, beanName, DataSource.class);
        if (dataSource == null) {
            throw new PluginException("DataSource bean not found: " + beanName);
        }
        return dataSource;
    }

    private String loadScript(InsidePluginDescriptor descriptor, String scriptPath) {
        String normalized = normalizeScriptPath(scriptPath);
        Path pluginPath = Paths.get(descriptor.getPluginPath());

        if (Files.isDirectory(pluginPath)) {
            return loadScriptFromDirectory(pluginPath, normalized);
        }
        return loadScriptFromJar(pluginPath.toFile(), normalized);
    }

    private String loadScriptFromDirectory(Path pluginDir, String normalizedPath) {
        Path relative = Paths.get(normalizedPath.replace("/", File.separator));
        Path[] candidates = new Path[]{
                pluginDir.resolve(relative),
                pluginDir.resolve("classes").resolve(relative)
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new PluginException("Read migration script failed: " + candidate, e);
                }
            }
        }
        throw new PluginException("Migration script not found: " + normalizedPath + " in plugin dir " + pluginDir);
    }

    private String loadScriptFromJar(File jarFile, String normalizedPath) {
        String prefixedPath = normalizedPath.startsWith("classes/") ? normalizedPath : "classes/" + normalizedPath;
        String[] candidates = new String[]{normalizedPath, prefixedPath};
        try (JarFile jf = new JarFile(jarFile)) {
            for (String candidate : candidates) {
                JarEntry entry = jf.getJarEntry(candidate);
                if (entry == null) {
                    continue;
                }
                try (InputStream inputStream = jf.getInputStream(entry)) {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new PluginException("Load migration script from jar failed: " + jarFile, e);
        }
        throw new PluginException("Migration script not found: " + normalizedPath + " in plugin jar " + jarFile);
    }

    private String normalizeScriptPath(String scriptPath) {
        if (ObjectUtils.isEmpty(scriptPath)) {
            return "";
        }
        String normalized = scriptPath.trim().replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void executeSql(DataSource dataSource,
                            String pluginId,
                            String direction,
                            String scriptPath,
                            String scriptContent) {
        List<String> statements = splitSqlStatements(scriptContent);
        if (statements.isEmpty()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (String statementSql : statements) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(statementSql);
                    }
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw new PluginException("Execute " + direction + " migration failed for plugin "
                        + pluginId + ", script=" + scriptPath, e);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new PluginException("SQL execution error for plugin " + pluginId
                    + ", script=" + scriptPath, e);
        }
    }

    private void executeSqlWithPolicy(DataSource dataSource,
                                      String pluginId,
                                      String direction,
                                      String scriptPath,
                                      String scriptContent) {
        if (!options.isContinueOnError()) {
            executeSql(dataSource, pluginId, direction, scriptPath, scriptContent);
            return;
        }
        try {
            executeSql(dataSource, pluginId, direction, scriptPath, scriptContent);
        } catch (Exception ex) {
            log.warn("Migration continue-on-error enabled, ignore failure. plugin={}, direction={}, script={}",
                    pluginId, direction, scriptPath, ex);
        }
    }

    private String checksum(String scriptContent) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(scriptContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new PluginException("SHA-256 algorithm not available", e);
        }
    }

    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return statements;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1) < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '-' && next == '-') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (c == '/' && next == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }

            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }
}
