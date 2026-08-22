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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginWebFileSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateBackupUnderPluginsBackupDirectory() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);
        Path pluginFile = pluginRoot.resolve("demo-plugin.jar");
        Files.writeString(pluginFile, "demo");

        PluginWebFileSupport fileSupport = createFileSupport();

        Path backupPath = fileSupport.backupPluginFile(pluginRoot, pluginFile);

        assertThat(backupPath).exists();
        assertThat(backupPath.getParent().getFileName().toString()).isEqualTo("demo-plugin");
        assertThat(backupPath.toString()).contains("plugins-backup");
    }

    @Test
    void shouldRejectAccessOutsideManagedPluginDirectories() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);
        Path externalFile = Files.createTempFile("outside-plugin", ".jar");

        PluginWebFileSupport fileSupport = createFileSupport();

        assertThatThrownBy(() -> fileSupport.resolveManagedPluginPath(externalFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("仅允许访问插件上传目录或插件根目录中的文件");
    }

    private PluginWebFileSupport createFileSupport() {
        BrickWebProperties properties = new BrickWebProperties();
        properties.setUploadTempPath(tempDir.resolve("upload-temp").toString());
        properties.setPluginPaths(List.of(tempDir.resolve("plugins").toString()));
        return new PluginWebFileSupport(properties);
    }
}
